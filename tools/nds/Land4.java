import java.nio.file.*;
import java.util.*;

/**
 * Gen 4 movement-permission reader: dumps the per-tile (type, collision) byte pair under every
 * warp tile, using the SAME header/matrix/event plumbing as Warps4.java. This is the
 * calibration pass for warp gating: doors, stairs, gates and cave mouths cluster into distinct
 * type-byte values, which is where real Gen 4 keeps its facing/step semantics (the warp records
 * themselves carry no direction).
 *
 * Land data: each map header's matrix carries a LAND FILE layer (u16 per 32x32-tile cell).
 * A land chunk is 4 u32 section sizes (permissions, buildings, model, bdhc) followed by the
 * sections; the permission section is 32x32 u16LE pairs. Which of the pair's bytes is the TYPE
 * and which the COLLISION is settled empirically by the histogram (collision is bimodal
 * 0x00/0x80).
 *
 * Usage: java Land4.java <rom> <region 3|4> <out>  - every header's full permission matrix
 * Output rows: region;bank;map;x;y;b0;b1  (b0/b1 = the pair's bytes at the warp tile)
 */
public class Land4 {
  static byte[] rom;
  static Map<String, Integer> paths = new LinkedHashMap<>();

  static int u16(byte[] b, int o) { return (b[o] & 0xFF) | ((b[o + 1] & 0xFF) << 8); }
  static int u32(byte[] b, int o) {
    return (b[o] & 0xFF) | ((b[o + 1] & 0xFF) << 8) | ((b[o + 2] & 0xFF) << 16)
        | ((b[o + 3] & 0xFF) << 24);
  }

  record Hdr(int matrix, int events) {}

  public static void main(String[] a) throws Exception {
    rom = Files.readAllBytes(Paths.get(a[0]));
    walk(u32(rom, 0x40), 0, "");
    int region = Integer.parseInt(a[1]);
    int mA = region == 3 ? 1573448 : 33489410, mB = region == 3 ? 1573449 : 11076050;
    int skip = region == 3 ? 0 : 8, offMatrix = region == 3 ? 2 : 4, offEvents = 16;
    String evPath = region == 3 ? "/fielddata/eventdata/zone_event.narc" : "/a/0/3/2";
    String mxPath = region == 3 ? "/fielddata/mapmatrix/map_matrix.narc" : "/a/0/4/1";

    byte[] arm9 = arm9();
    int tab = scanPair(arm9, mA, mB);
    if (tab < 0) { arm9 = blz(arm9); tab = scanPair(arm9, mA, mB); }
    if (tab < 0) throw new RuntimeException("header-table marker not found");
    tab += skip;

    int fat = u32(rom, 0x48);
    int nameIdx = paths.get("/fielddata/maptable/mapname.bin");
    int maps = (u32(rom, fat + nameIdx * 8 + 4) - u32(rom, fat + nameIdx * 8)) / 16;

    Hdr[] hdr = new Hdr[maps];
    for (int i = 0; i < maps; i++) {
      int o = tab + i * 24;
      hdr[i] = new Hdr(u16(arm9, o + offMatrix), u16(arm9, o + offEvents));
    }

    // Locate the land-data narc: Platinum by path; HG by structure (a narc whose file 0 parses
    // as a land chunk: 4 section sizes + 16 == file size, permission section 2048 bytes).
    String landPath = region == 3 ? "/fielddata/land_data/land_data.narc" : null;
    if (landPath == null) {
      for (Map.Entry<String, Integer> e : paths.entrySet()) {
        if (!e.getKey().startsWith("/a/")) continue;
        try {
          int[][] idx = narcIndex(e.getKey());
          if (idx.length < 100) continue;
          int ok = 0;
          for (int i = 0; i < Math.min(20, idx.length); i++) {
            byte[] f = Arrays.copyOfRange(rom, idx[i][0], idx[i][1]);
            if (isLandChunk(f)) ok++;
          }
          if (ok >= 18) { landPath = e.getKey(); break; }
        } catch (Exception ignore) {}
      }
      if (landPath == null) throw new RuntimeException("no land-data narc found by structure");
      System.err.println("# HG land narc = " + landPath);
    }
    int[][] land = narcIndex(landPath);

    // Per-header matrix: land-file layer. Matrix format: w, h, hasHeadersLayer, hasAltLayer,
    // nameLen, name, [headers u16*w*h], [altitude u8*w*h], land-file u16*w*h.
    int[][] mxIdx = narcIndex(mxPath);
    Map<Integer, int[]> matrixCache = new HashMap<>(); // matrix -> {w, h, landLayerOffset...}
    int rows = 0, missing = 0;
    int[][] evIdx = narcIndex(evPath);
    int[] histB0 = new int[256], histB1 = new int[256];

    // Every header's whole walkable extent: the cells the header owns on a shared matrix (the
    // world matrix packs all exteriors), or the whole matrix for single-map matrices. Rows are
    // region;bank;map;x;y;type;coll with matrix-global coordinates, the same frame the client
    // reports outdoor movement in.
    java.io.PrintWriter out = new java.io.PrintWriter(a[2], "UTF-8");
    int tiles = 0, headersOut = 0;
    for (int i = 0; i < maps; i++) {
      int mxId = hdr[i].matrix();
      if (mxId >= mxIdx.length) continue;
      byte[] mx = Arrays.copyOfRange(rom, mxIdx[mxId][0], mxIdx[mxId][1]);
      if (mx.length < 7) continue;
      int w = mx[0] & 0xFF, h = mx[1] & 0xFF, fH = mx[2] & 0xFF, fA = mx[3] & 0xFF, nl = mx[4] & 0xFF;
      int p = 5 + nl;
      int headersLayer = fH != 0 ? p : -1;
      if (fH != 0) p += w * h * 2;
      if (fA != 0) p += w * h;
      int landLayer = p;
      if (landLayer + w * h * 2 > mx.length) continue;
      boolean any = false;
      for (int cr = 0; cr < h; cr++) {
        for (int cc = 0; cc < w; cc++) {
          if (headersLayer >= 0 && u16(mx, headersLayer + (cr * w + cc) * 2) != i) continue;
          int lf = u16(mx, landLayer + (cr * w + cc) * 2);
          if (lf >= land.length) continue;
          byte[] ch = Arrays.copyOfRange(rom, land[lf][0], land[lf][1]);
          if (!isLandChunk(ch)) continue;
          int po = region == 3 ? 16 : bestPermOffset(ch);
          for (int ty = 0; ty < 32; ty++) {
            for (int tx = 0; tx < 32; tx++) {
              int oo = po + (ty * 32 + tx) * 2;
              out.println(region + ";" + (i & 0xFF) + ";" + (i >> 8) + ";" + (cc * 32 + tx) + ";" + (cr * 32 + ty) + ";" + (ch[oo] & 0xFF) + ";" + (ch[oo + 1] & 0xFF));
              tiles++;
            }
          }
          any = true;
        }
      }
      if (any) headersOut++;
    }
    out.close();
    System.err.println("# headers=" + headersOut + " tiles=" + tiles);
  }

  /** Score both candidate headers by how bimodal (0x00/0x80) the collision plane looks. */
  static int bestPermOffset(byte[] chunk) {
    int best = 16, bestScore = -1;
    for (int off : new int[] {16, 20}) {
      if (off + 2048 > chunk.length) continue;
      int score = 0;
      for (int t = 0; t < 1024; t++) {
        int coll = chunk[off + t * 2 + 1] & 0xFF;
        if (coll == 0x00 || coll == 0x80) score++;
      }
      if (score > bestScore) { bestScore = score; best = off; }
    }
    return best;
  }

  static boolean isLandChunk(byte[] f) {
    if (f.length < 16) return false;
    long s0 = u32(f, 0) & 0xFFFFFFFFL, s1 = u32(f, 4) & 0xFFFFFFFFL;
    long s2 = u32(f, 8) & 0xFFFFFFFFL, s3 = u32(f, 12) & 0xFFFFFFFFL;
    return s0 == 2048 && 16 + s0 + s1 + s2 + s3 <= f.length + 3 && s2 > 0;
  }

  /** Warp records only, same 20/32/12/16-byte section walk as Warps4.parseWarps. */
  static List<int[]> parseWarpCoords(byte[] f) {
    List<int[]> out = new ArrayList<>();
    int p = 0;
    int furn = u32(f, p); p += 4 + furn * 20;
    if (p + 4 > f.length) return out;
    int npc = u32(f, p); p += 4 + npc * 32;
    if (p + 4 > f.length) return out;
    int warps = u32(f, p); p += 4;
    for (int i = 0; i < warps && p + 12 <= f.length; i++, p += 12) {
      out.add(new int[] {s16(f, p), s16(f, p + 2)});
    }
    return out;
  }

  static int s16(byte[] b, int o) { int v = u16(b, o); return v >= 0x8000 ? v - 0x10000 : v; }

  static byte[] arm9() {
    int off = u32(rom, 0x20), size = u32(rom, 0x2C);
    return Arrays.copyOfRange(rom, off, off + size);
  }

  static int scanPair(byte[] b, int x, int y) {
    for (int i = 0; i + 8 <= b.length; i += 4) if (u32(b, i) == x && u32(b, i + 4) == y) return i + 8;
    return -1;
  }

  static byte[] blz(byte[] in) {
    int end = u32(in, in.length - 8), extra = u32(in, in.length - 4);
    int hdr = (end >>> 24) & 0xFF; int cmpLen = end & 0xFFFFFF;
    byte[] out = new byte[in.length + extra];
    System.arraycopy(in, 0, out, 0, in.length);
    int src = in.length - hdr, dst = in.length + extra, stop = in.length - cmpLen;
    while (src > stop) {
      int flags = out[--src] & 0xFF;
      for (int i = 0; i < 8 && src > stop; i++) {
        if ((flags & 0x80) == 0) out[--dst] = out[--src];
        else {
          int b1 = out[--src] & 0xFF, b2 = out[--src] & 0xFF;
          int len = (b1 >> 4) + 3, disp = ((b1 & 0xF) << 8 | b2) + 3;
          for (int j = 0; j < len; j++) { out[dst - 1] = out[dst - 1 + disp]; dst--; }
        }
        flags <<= 1;
      }
    }
    return out;
  }

  static int[][] narcIndex(String path) {
    int fat = u32(rom, 0x48);
    int s = u32(rom, fat + paths.get(path) * 8);
    int p = s + 0x10; int[] st = null, en = null; int img = 0; int count = 0;
    while (true) {
      String m = new String(rom, p, 4, java.nio.charset.StandardCharsets.US_ASCII);
      int cs = u32(rom, p + 4);
      if (m.equals("BTAF")) {
        count = u16(rom, p + 8); st = new int[count]; en = new int[count];
        for (int i = 0; i < count; i++) { st[i] = u32(rom, p + 12 + i * 8); en[i] = u32(rom, p + 12 + i * 8 + 4); }
      } else if (m.equals("GMIF")) { img = p + 8; break; }
      if (cs <= 0) break;
      p += cs;
    }
    int[][] out = new int[count][2];
    for (int i = 0; i < count; i++) { out[i][0] = img + st[i]; out[i][1] = img + en[i]; }
    return out;
  }

  static void walk(int fnt, int dir, String pre) {
    int e = fnt + dir * 8; int sub = fnt + u32(rom, e); int fid = u16(rom, e + 4); int p = sub;
    while (true) {
      int t = rom[p] & 0xFF; if (t == 0) break;
      if (t < 0x80) { paths.put(pre + "/" + new String(rom, p + 1, t, java.nio.charset.StandardCharsets.US_ASCII), fid++); p += 1 + t; }
      else {
        int l = t & 0x7F; String nm = new String(rom, p + 1, l, java.nio.charset.StandardCharsets.US_ASCII);
        int sd = u16(rom, p + 1 + l) & 0x0FFF; p += 1 + l + 2;
        walk(fnt, sd, pre + "/" + nm);
      }
    }
  }
}
