import java.nio.file.*;
import java.util.*;

/**
 * Unova's Gen 5 camera rails (Castelia's streets, Skyarrow Bridge, the League lobby...), read
 * from the ROM the way the client reads them, into `server.game/nds-rails-2.txt`.
 *
 * Where: /a/0/7/9 holds 34 files - 17 "pointdata" files (magic 0x54C52 "RL", client f.DG1) at
 * 0..16 and their 17 cell grids (client f.AL1) at 17..33. A rail AREA i = pointdata i + grid 17+i;
 * a map header picks its area through the client's fixed table (f.DG1.w41), emitted as `area`
 * rows here.
 *
 * Pointdata (f.DG1): 8 header ints [magic, 28, pointsOff, linesOff, qdOff, ip1Off, ...]; points
 * are 112 bytes (f.sZ1: 4 x (byte link + 3 pad), 4 ints, 4 ints, position as three 16.16 fixed
 * (frac u16, int s16) x/y/z, int, 48-byte name); lines are 72 bytes (f.qQ0: ints from-point,
 * to-point, AM1, Bo0, ri0, Bl0, 48-byte name). The LINE INDEX is the rail line id the warp
 * records, the npc records and LoadEntity's Z byte carry.
 *
 * Grid (f.AL1): int count, then per line: s16 width, s16 length, then length x width cells of two
 * u16 (plane 0, plane 1 - plane 1 bit 0 set = blocked, same as the land record's flag word). The
 * client indexes a cell as In1[x][y - hF] with hF = -width/2: x runs 0 at the from-point to
 * length-1 at the to-point, y is the lateral offset, -width/2..width/2 (client f.k90.LPt9/Qt1).
 *
 * Verified against live movement 2026-09-23: Skyarrow's line 13 is 8 long and the client's
 * coordinates jumped at x 7; line 5 is 36 long, shares its to-point with line 12's from-point,
 * and was entered at x 35 walking off line 12's x 0.
 *
 * Usage: java tools/nds/Rail5.java <rom> <out>
 */
public class Rail5 {
  static byte[] rom;
  static Map<String, Integer> paths = new LinkedHashMap<>();
  /** f.DG1.w41: map header -> rail area index. */
  static final int[][] AREA_HEADERS = {
    {36}, {66}, {114}, {121}, {137}, {28}, {30}, {209}, {211}, {214}, {241, 242, 243, 244},
    {249}, {255}, {338}, {339}, {340}, {341}
  };

  static int u16(byte[] b, int o) { return (b[o] & 0xFF) | ((b[o + 1] & 0xFF) << 8); }
  static int s16(byte[] b, int o) { int v = u16(b, o); return v >= 0x8000 ? v - 0x10000 : v; }
  static int u32(byte[] b, int o) { return (b[o] & 0xFF) | ((b[o + 1] & 0xFF) << 8) | ((b[o + 2] & 0xFF) << 16) | ((b[o + 3] & 0xFF) << 24); }
  static float fx(byte[] b, int o) { return (u16(b, o) / 65536f) + s16(b, o + 2); }

  public static void main(String[] a) throws Exception {
    rom = Files.readAllBytes(Paths.get(a[0]));
    walk(u32(rom, 0x40), 0, "");
    int fat = u32(rom, 0x48);
    int s = u32(rom, fat + paths.get("/a/0/7/9") * 8);
    int p = s + 0x10; int[] st = null, en = null; int img = 0; int c = 0;
    while (true) {
      String m = new String(rom, p, 4, "US-ASCII"); int cs = u32(rom, p + 4);
      if (m.equals("BTAF")) { c = u16(rom, p + 8); st = new int[c]; en = new int[c]; for (int i = 0; i < c; i++) { st[i] = u32(rom, p + 12 + i * 8); en[i] = u32(rom, p + 12 + i * 8 + 4); } }
      else if (m.equals("GMIF")) { img = p + 8; break; }
      if (cs <= 0) break; p += cs;
    }
    int areas = c / 2;
    StringBuilder out = new StringBuilder();
    out.append("# area;areaIdx;header | point;area;id;x;y;z | line;area;id;fromPoint;toPoint;length;width | cell;area;line;x;y;plane0;plane1 (blocked cells only)\n");
    for (int area = 0; area < areas; area++) {
      for (int h : AREA_HEADERS[area]) out.append("area;").append(area).append(';').append(h).append('\n');
      byte[] f = Arrays.copyOfRange(rom, img + st[area], img + en[area]);
      if (u32(f, 0) != 347218) throw new IllegalStateException("area " + area + ": not a pointdata file");
      int pOff = u32(f, 8), lOff = u32(f, 12), qOff = u32(f, 16);
      int np = (lOff - pOff) / 112, nl = (qOff - lOff) / 72;
      for (int i = 0; i < np; i++) {
        int o = pOff + i * 112;
        out.append(String.format(Locale.ROOT, "point;%d;%d;%.3f;%.3f;%.3f%n", area, i, fx(f, o + 48), fx(f, o + 52), fx(f, o + 56)));
      }
      byte[] g = Arrays.copyOfRange(rom, img + st[areas + area], img + en[areas + area]);
      int gn = u32(g, 0);
      if (gn != nl) throw new IllegalStateException("area " + area + ": " + nl + " lines but " + gn + " grids");
      int o = 4;
      for (int i = 0; i < nl; i++) {
        int lo = lOff + i * 72;
        int w = s16(g, o), len = s16(g, o + 2); o += 4;
        out.append("line;").append(area).append(';').append(i).append(';').append(u32(f, lo)).append(';').append(u32(f, lo + 4))
            .append(';').append(len).append(';').append(w).append('\n');
        for (int x = 0; x < len; x++) for (int y = 0; y < w; y++) {
          int p0 = u16(g, o), p1 = u16(g, o + 2); o += 4;
          if ((p1 & 1) != 0) out.append("cell;").append(area).append(';').append(i).append(';').append(x).append(';').append(y - w / 2).append(';').append(p0).append(';').append(p1).append('\n');
        }
      }
      System.out.println("area " + area + ": headers " + Arrays.toString(AREA_HEADERS[area]) + " points=" + np + " lines=" + nl);
    }
    Files.write(Paths.get(a[1]), out.toString().getBytes("US-ASCII"));
  }

  static void walk(int fnt, int dir, String pre) {
    int e = fnt + dir * 8; int sub = fnt + u32(rom, e); int fid = u16(rom, e + 4); int p2 = sub;
    while (true) {
      int t = rom[p2] & 0xFF; if (t == 0) break;
      if (t < 0x80) { paths.put(pre + "/" + new String(rom, p2 + 1, t, java.nio.charset.StandardCharsets.US_ASCII), fid++); p2 += 1 + t; }
      else { int l = t & 0x7F; String nm = new String(rom, p2 + 1, l, java.nio.charset.StandardCharsets.US_ASCII); int sd = u16(rom, p2 + 1 + l) & 0x0FFF; p2 += 1 + l + 2; walk(fnt, sd, pre + "/" + nm); }
    }
  }
}
