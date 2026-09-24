import java.nio.file.*;
import java.util.*;

/**
 * Unova (Pokemon White, IRAO) elevator floor lists, read from the ROM into
 * `server.game/nds-elevators-2.txt`, plus the engine's script-id range table into
 * `server.game/nds-script-ranges-2.txt`. Background and verification: docs/gen5-elevators.md.
 *
 * WHERE THE DATA LIVES (verified against the ARM code, 2026-09-23):
 *
 * 1. Script ids >= 2000 are resolved by a range table in overlay 10 (RAM 0x021701C8, 46 records
 *    of 10 bytes: u16 first, u16 last, u16 script file (/a/0/5/7), u16 arc (always 3), u16 text
 *    bank (/a/0/0/3)). The lookup (ov10 0x02158CF0) walks the records in order, returns the
 *    first match and the entry index id - first; a miss with id >= 1 is a map-local script (the
 *    header's script file and text bank, entry id - 1). So the "engine standard scripts" 10000+
 *    DO live in /a/0/5/7: 10405-10424 = file 888, text bank 261.
 * 2. The elevator panel script (file 888 entries 0-13 = ids 10405-10418) runs
 *    `0x1CB <s32 offset>`: the offset is relative to the end of the command and points at the
 *    building's FLOOR TABLE inside the same script file (handler ov21 0x021B6AF8 stores
 *    pc + offset in the script work). Floor table = 10-byte rows until a row whose first u16 is
 *    0xFFFF: u16 label (entry in the script's text bank, "1F", "47F"...), u16 destination map
 *    header, u16 x, u16 height, u16 row (tile units; the handler shifts each left 16 into a
 *    VecFx32 x/y/z, the same x, height, row order as the warp records).
 * 3. `0x1CC` (no args, ov21 0x021B6B14) adds one menu entry per row whose header is NOT the
 *    current map (text = label, value = row index); the script then adds Cancel (text 0, value
 *    255) with `0xAF` and shows the list with `0xB0`. `0x1CD var` (ov21 0x021B6B84) warps to row
 *    `var` if var < row count: header, (x, height, row), direction constant 1.
 *
 * Output rows (one per panel bg event x floor row, current floor INCLUDED and flagged so the
 * server can drop it exactly as 0x1CC does):
 *   elevator;2;bank;map;panelScript;floorIndex;floorLabelTextId;destHeader;destBank;destMap;
 *            destX;destY;destLine;destZ;facing;current;labelBank;labelEntry;label;scriptFile;
 *            scriptEntry;tableOffset;panelX;panelY
 *   bank/map        the map the panel stands in (header = map << 8 | bank)
 *   panelScript     the bg event's script id (10405-10418)
 *   floorIndex      the row index = the menu value 0x1CC gives the row = 0x1CD's argument
 *   floorLabelTextId server DS text id (2 << 28 | 1 << 27 | bank << 16 | entry) of the label
 *   destX/destY     landing tile (record x and record row); destZ = record height
 *   destLine        -1 (no elevator map is a rail map)
 *   facing          the ROM's direction constant passed by 0x1CD (1 = south in the Gen 4/5 enum)
 *   current         1 when destHeader is this map (0x1CC leaves that row out of the menu)
 *   tableOffset     byte offset of the floor table in scriptFile
 * Ranges file: range;2;first;last;scriptFile;arc;textBank (ROM table order = lookup order).
 *
 * Usage: java tools/nds/Elevators5.java <white.nds> <out-elevators> [<out-ranges>]
 *        [<command-table>]   (default tools/nds/bw1-script-commands.txt, used only to step over
 *        the panel script's first few commands to reach 0x1CB)
 */
public class Elevators5 {
  static byte[] rom;
  static Map<String, Integer> paths = new LinkedHashMap<>();
  static final int REGION = 2;

  static int u16(byte[] b, int o) { return (b[o] & 0xFF) | ((b[o + 1] & 0xFF) << 8); }
  static int u32(byte[] b, int o) { return (b[o] & 0xFF) | ((b[o + 1] & 0xFF) << 8) | ((b[o + 2] & 0xFF) << 16) | ((b[o + 3] & 0xFF) << 24); }

  record Range(int first, int last, int file, int arc, int text) {}
  record Resolved(int file, int text, int entry) {}
  record Floor(int label, int header, int x, int height, int row) {}

  public static void main(String[] a) throws Exception {
    rom = Files.readAllBytes(Paths.get(a[0]));
    String code = new String(rom, 0x0C, 4, "US-ASCII");
    if (!code.equals("IRAO")) throw new IllegalStateException("expected Pokemon White (IRAO), got " + code);
    walk(u32(rom, 0x40), 0, "");
    String tablePath = a.length > 3 ? a[3] : "tools/nds/bw1-script-commands.txt";

    List<Range> ranges = readRanges(overlay(10));
    Map<Integer, String> sizes = new HashMap<>();
    for (String l : Files.readAllLines(Paths.get(tablePath))) {
      String[] p = l.split(";", -1);
      if (p.length >= 2 && !p[0].isBlank()) sizes.put(Integer.parseInt(p[0].trim()), p.length > 2 ? p[2] : "");
    }
    // Sizes read off the handlers (ov21): 0x1CB reads a u32, 0x1CC nothing, 0x1CD a u16.
    sizes.put(0x1CB, "L"); sizes.put(0x1CC, ""); sizes.put(0x1CD, "H");

    byte[] hdr = narcFile("/a/0/1/2", 0);
    int maps = hdr.length / 48;
    int[][] ev = narcIndex("/a/1/2/5");
    int[][] scripts = narcIndex("/a/0/5/7");
    Map<Integer, List<String>> textCache = new HashMap<>();

    StringBuilder out = new StringBuilder();
    out.append("# Unova elevators from Pokemon White (tools/nds/Elevators5.java; docs/gen5-elevators.md)\n");
    out.append("# elevator;2;bank;map;panelScript;floorIndex;floorLabelTextId;destHeader;destBank;destMap;destX;destY;destLine;destZ;facing;current;labelBank;labelEntry;label;scriptFile;scriptEntry;tableOffset;panelX;panelY\n");
    int rows = 0;
    Map<String, Integer> perTable = new TreeMap<>();
    for (int header = 0; header < maps; header++) {
      int file = u16(hdr, header * 48 + 22);
      if (file >= ev.length) continue;
      byte[] f = Arrays.copyOfRange(rom, ev[file][0], ev[file][1]);
      if (f.length < 8) continue;
      int bgCount = f[4] & 0xFF;
      for (int i = 0; i < bgCount && 8 + i * 20 + 20 <= f.length; i++) {
        int o = 8 + i * 20;
        int script = u16(f, o), x = u32(f, o + 8), y = u32(f, o + 12);
        Resolved r = resolve(ranges, hdr, header, script);
        if (r == null || r.file() >= scripts.length) continue;
        byte[] s = Arrays.copyOfRange(rom, scripts[r.file()][0], scripts[r.file()][1]);
        int entryOff = entryOffset(s, r.entry());
        if (entryOff < 0) continue;
        int table = findElevatorTable(s, entryOff, sizes);
        if (table < 0) continue;
        List<Floor> floors = readFloors(s, table, maps);
        if (floors == null) { System.err.printf("header %d bg %d: 0x1CB target %d is not a floor table%n", header, i, table); continue; }
        List<String> texts = textCache.computeIfAbsent(r.text(), t -> decodeText(narcFile("/a/0/0/3", t)));
        for (int k = 0; k < floors.size(); k++) {
          Floor fl = floors.get(k);
          int textId = (REGION << 28) | (1 << 27) | (r.text() << 16) | fl.label();
          String label = fl.label() < texts.size() ? texts.get(fl.label()).replace(';', ',') : "?";
          out.append("elevator;").append(REGION).append(';').append(header & 0xFF).append(';').append(header >> 8).append(';')
              .append(script).append(';').append(k).append(';').append(textId).append(';')
              .append(fl.header()).append(';').append(fl.header() & 0xFF).append(';').append(fl.header() >> 8).append(';')
              .append(fl.x()).append(';').append(fl.row()).append(";-1;").append(fl.height()).append(";1;")
              .append(fl.header() == header ? 1 : 0).append(';').append(r.text()).append(';').append(fl.label()).append(';')
              .append(label).append(';').append(r.file()).append(';').append(r.entry()).append(';').append(table).append(';')
              .append(x).append(';').append(y).append('\n');
          rows++;
        }
        perTable.merge("file " + r.file() + " table @" + table + " floors " + describe(floors, texts), 1, Integer::sum);
      }
    }
    Files.write(Paths.get(a[1]), out.toString().getBytes("UTF-8"));
    System.out.println("elevator rows: " + rows);
    perTable.forEach((k, v) -> System.out.println("  " + k + "  <- " + v + " panel(s)"));

    if (a.length > 2) {
      StringBuilder rs = new StringBuilder("# Pokemon White script-id ranges (ov10 RAM 0x021701C8), lookup order; ids outside all ranges are map-local (entry id-1)\n");
      rs.append("# range;2;first;last;scriptFile;arc;textBank\n");
      for (Range g : ranges) rs.append("range;2;").append(g.first()).append(';').append(g.last()).append(';').append(g.file()).append(';').append(g.arc()).append(';').append(g.text()).append('\n');
      Files.write(Paths.get(a[2]), rs.toString().getBytes("UTF-8"));
      System.out.println("ranges: " + ranges.size());
    }
  }

  static String describe(List<Floor> floors, List<String> texts) {
    StringBuilder sb = new StringBuilder();
    for (Floor f : floors) sb.append(f.label() < texts.size() ? texts.get(f.label()) : "?").append("->").append(f.header()).append('(').append(f.x()).append(',').append(f.row()).append(") ");
    return sb.toString().trim();
  }

  /** The ROM's own lookup (ov10 0x02158CF0): first matching range, else the map's own file. */
  static Resolved resolve(List<Range> ranges, byte[] hdr, int header, int id) {
    for (Range g : ranges) if (id >= g.first() && id <= g.last()) return new Resolved(g.file(), g.text(), id - g.first());
    if (id < 1) return null;
    return new Resolved(u16(hdr, header * 48 + 6), u16(hdr, header * 48 + 10), id - 1);
  }

  /** Script file header: u32 offsets relative to the end of each entry, until the u16 0xFD13. */
  static int entryOffset(byte[] s, int entry) {
    int p = 0, n = 0;
    while (p + 4 <= s.length && u16(s, p) != 0xFD13) {
      int target = p + 4 + u32(s, p);
      if (target < 0 || target > s.length) return -1;
      if (n == entry) return target;
      n++; p += 4;
    }
    return -1;
  }

  /** Steps over the entry's straight-line prologue; the 0x1CB target, or -1. */
  static int findElevatorTable(byte[] s, int pc, Map<Integer, String> sizes) {
    for (int step = 0; step < 48 && pc + 2 <= s.length; step++) {
      int op = u16(s, pc);
      if (op == 0x1CB) return pc + 6 + u32(s, pc + 2);
      if (op == 0x02 || op == 0x05 || op == 0x1E) return -1;
      String sig = sizes.get(op);
      if (sig == null) return -1;
      int q = pc + 2;
      for (char c : sig.toCharArray()) q += c == 'B' ? 1 : c == 'H' ? 2 : 4;
      pc = q;
    }
    return -1;
  }

  static List<Floor> readFloors(byte[] s, int at, int maps) {
    List<Floor> out = new ArrayList<>();
    for (int p = at; p + 2 <= s.length; p += 10) {
      if (u16(s, p) == 0xFFFF) return out.isEmpty() ? null : out;
      if (p + 10 > s.length || out.size() >= 32) return null;
      Floor f = new Floor(u16(s, p), u16(s, p + 2), u16(s, p + 4), u16(s, p + 6), u16(s, p + 8));
      if (f.header() >= maps) return null;
      out.add(f);
    }
    return null;
  }

  /** The 46-record id-range table: found by its last record (2000-2099) and walked backwards. */
  static List<Range> readRanges(byte[] ov10) {
    for (int o = 0; o + 10 <= ov10.length; o += 2) {
      if (u16(ov10, o) != 2000 || u16(ov10, o + 2) != 2099 || u16(ov10, o + 6) != 3) continue;
      int start = o;
      while (start - 10 >= 0 && valid(ov10, start - 10)) start -= 10;
      List<Range> out = new ArrayList<>();
      for (int p = start; p <= o; p += 10) out.add(new Range(u16(ov10, p), u16(ov10, p + 2), u16(ov10, p + 4), u16(ov10, p + 6), u16(ov10, p + 8)));
      if (out.size() != 46) System.err.println("warning: range table has " + out.size() + " records, the ROM's loop expects 46");
      return out;
    }
    throw new IllegalStateException("script-id range table not found in overlay 10");
  }

  static boolean valid(byte[] b, int o) {
    int first = u16(b, o), last = u16(b, o + 2), file = u16(b, o + 4), arc = u16(b, o + 6);
    return first >= 1000 && first <= last && last < 20000 && file < 1000 && arc == 3;
  }

  // --- ROM plumbing -----------------------------------------------------------------------------

  static byte[] overlay(int want) {
    int ovt = u32(rom, 0x50), ovs = u32(rom, 0x54), fat = u32(rom, 0x48);
    for (int p = ovt; p + 32 <= ovt + ovs; p += 32) {
      if (u32(rom, p) != want) continue;
      int fileId = u32(rom, p + 24), flags = u32(rom, p + 28);
      byte[] b = Arrays.copyOfRange(rom, u32(rom, fat + fileId * 8), u32(rom, fat + fileId * 8 + 4));
      return (flags & 0x1000000) != 0 ? blz(b) : b;
    }
    throw new IllegalStateException("overlay " + want + " missing");
  }

  /** Backwards LZ (the overlay compression). */
  static byte[] blz(byte[] in) {
    int end = u32(in, in.length - 8), extra = u32(in, in.length - 4);
    int hdr = (end >>> 24) & 0xFF, cmpLen = end & 0xFFFFFF;
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

  /** Gen 5 message archive (key base 0x7C89, +0x2983 per entry, rotl 3 per char; tools/nds/README.md). */
  static List<String> decodeText(byte[] f) {
    List<String> o = new ArrayList<>();
    int entries = u16(f, 2), so = u32(f, 0x0C);
    for (int e = 0; e < entries; e++) {
      int off = so + u32(f, so + 4 + e * 8), len = u16(f, so + 4 + e * 8 + 4);
      int key = (0x7C89 + e * 0x2983) & 0xFFFF;
      StringBuilder sb = new StringBuilder();
      for (int j = 0; j < len; j++) {
        int c = u16(f, off + j * 2) ^ key;
        key = ((key << 3) | (key >>> 13)) & 0xFFFF;
        if (c == 0xFFFF) break;
        if (c >= 0x20 && c < 0xE000) sb.append((char) c);
      }
      o.add(sb.toString());
    }
    return o;
  }

  static int[][] narcIndex(String path) {
    int fat = u32(rom, 0x48);
    int s = u32(rom, fat + paths.get(path) * 8);
    int p = s + 0x10, img = 0, count = 0;
    int[] st = null, en = null;
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

  static byte[] narcFile(String path, int i) {
    int[][] x = narcIndex(path);
    return Arrays.copyOfRange(rom, x[i][0], x[i][1]);
  }

  static void walk(int fnt, int dir, String pre) {
    int e = fnt + dir * 8, sub = fnt + u32(rom, e), fid = u16(rom, e + 4), p = sub;
    while (true) {
      int t = rom[p] & 0xFF;
      if (t == 0) break;
      if (t < 0x80) { paths.put(pre + "/" + new String(rom, p + 1, t, java.nio.charset.StandardCharsets.US_ASCII), fid++); p += 1 + t; }
      else {
        int l = t & 0x7F;
        String nm = new String(rom, p + 1, l, java.nio.charset.StandardCharsets.US_ASCII);
        int sd = u16(rom, p + 1 + l) & 0x0FFF;
        p += 1 + l + 2;
        walk(fnt, sd, pre + "/" + nm);
      }
    }
  }
}
