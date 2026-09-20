import java.nio.file.*;
import java.util.*;

/**
 * Sizes the Gen 5 script opcodes that tools/nds/bw1-script-commands.txt does not know, from the
 * ROM itself. STRICT on purpose: a wrong size silently corrupts every command after it (it eats
 * the next opcode, or leaves an argument behind to decode as one), while an unknown opcode only
 * ends that one script early - so a size is only proposed when the evidence is decisive.
 *
 * For each occurrence of an unknown opcode, a candidate size is CLEAN when decoding forward from
 * the byte after it reaches a real terminal (End, Return, Jump...) through known opcodes only,
 * inside the file. The clean sets of every occurrence are intersected: exactly one survivor is
 * reported as confident, anything else is reported as ambiguous and left unknown.
 *
 *   Sizes5 <rom> <table>            - report
 *   Sizes5 <rom> <table> --apply    - report and append the confident ones to the table
 */
public class Sizes5 {
  static byte[] rom;
  static Map<String, Integer> paths = new LinkedHashMap<>();
  static Map<Integer, String[]> cmds = new HashMap<>();
  /** End, Return, EndRoutine, ReturnStd, Jump, ReturnAfterDelay's cousins - a script really stops here. */
  static final Set<Integer> TERMINAL = new HashSet<>(Arrays.asList(0x02, 0x05, 0x1D, 0x1E, 0x1C));
  /** Sizes to try, in bytes: every Gen 5 argument seen so far is a 2-byte H or a 4-byte L. */
  static final int[] CANDIDATES = {0, 2, 4, 6, 8, 10, 12, 14, 16, 18, 20, 24};
  static final int MAX_STEPS = 80;
  /**
   * Offsets that hold DATA, per file: a movement table (an ApplyMovement target, terminated by
   * 0xFE) and the file's own entry-offset header. Code never starts inside either, so a candidate
   * size whose decode lands there is disproved - the single strongest check available, since most
   * unknown opcodes appear once and "reaches a terminal" alone leaves several sizes standing.
   */
  static Map<Integer, Set<Integer>> dataBytes = new HashMap<>();

  static int u16(byte[] b, int o) { return (b[o] & 0xFF) | ((b[o + 1] & 0xFF) << 8); }
  static int u32(byte[] b, int o) { return (b[o] & 0xFF) | ((b[o+1] & 0xFF) << 8) | ((b[o+2] & 0xFF) << 16) | ((b[o+3] & 0xFF) << 24); }

  public static void main(String[] a) throws Exception {
    rom = Files.readAllBytes(Paths.get(a[0]));
    walk(u32(rom, 0x40), 0, "");
    for (String line : Files.readAllLines(Paths.get(a[1]))) {
      String[] p = line.split(";");
      if (p.length < 2 || p[0].startsWith("#")) continue;
      cmds.put(Integer.parseInt(p[0]), new String[] {p[1], p.length > 2 ? p[2] : ""});
    }
    boolean apply = a.length > 2 && a[2].equals("--apply");

    int[][] idx = narcIndex("/a/0/5/7");
    byte[][] files = new byte[idx.length][];
    for (int i = 0; i < idx.length; i++) files[i] = Arrays.copyOfRange(rom, idx[i][0], idx[i][1]);

    // Every place an unknown opcode stops a decode, by opcode.
    Map<Integer, List<int[]>> stops = new TreeMap<>();
    for (int fi = 0; fi < files.length; fi++) {
      byte[] f = files[fi];
      if (f.length < 4) continue;
      List<Integer> entries = new ArrayList<>();
      int p = 0;
      while (p + 4 <= f.length) {
        if (u16(f, p) == 0xFD13) break;
        int off = u32(f, p), tgt = p + 4 + off;
        if (tgt < 0 || tgt > f.length) break;
        entries.add(tgt);
        p += 4;
        if (entries.size() > 2048) break;
      }
      Set<Integer> data = new HashSet<>();
      for (int i = 0; i < p && i < f.length; i++) data.add(i); // the entry-offset header
      dataBytes.put(fi, data);
      for (int e : entries) {
        int pc = e;
        for (int step = 0; step < 4096 && pc >= 0 && pc + 2 <= f.length; step++) {
          int op = u16(f, pc);
          String[] c = cmds.get(op);
          if (c == null) { stops.computeIfAbsent(op, k -> new ArrayList<>()).add(new int[] {fi, pc}); break; }
          // ApplyMovement obj, offset: its target is a movement table, so those bytes are data.
          if (op == 0x64) {
            int tgt = pc + 4 + 4 + u32(f, pc + 4);
            for (int m = tgt; m + 4 <= f.length; m += 4) {
              for (int k = 0; k < 4; k++) data.add(m + k);
              if (u16(f, m) == 0xFE) break;
            }
          }
          int q = advance(f, pc, c[1]);
          if (q < 0 || TERMINAL.contains(op)) break;
          pc = q;
        }
      }
    }

    List<String> confident = new ArrayList<>();
    int ambiguous = 0, none = 0;
    for (Map.Entry<Integer, List<int[]>> en : stops.entrySet()) {
      int op = en.getKey();
      List<int[]> os = dedup(en.getValue());
      Set<Integer> agreed = null;
      for (int[] o : os) {
        Set<Integer> clean = new TreeSet<>();
        for (int size : CANDIDATES) if (decodesCleanly(files[o[0]], o[1] + 2 + size, dataBytes.get(o[0]))) clean.add(size);
        agreed = (agreed == null) ? clean : intersect(agreed, clean);
      }
      String where = os.size() + (os.size() == 1 ? " use" : " uses");
      if (agreed != null && agreed.size() == 1) {
        int size = agreed.iterator().next();
        confident.add(String.format("%d;CMD_%03X;%s", op, op, "H".repeat(size / 2)));
        System.out.printf("CONFIDENT 0x%03X = %d bytes (%s)%n", op, size, where);
      } else if (agreed != null && agreed.size() > 1) {
        ambiguous++;
        System.out.printf("ambiguous 0x%03X   sizes %s (%s) - left unknown%n", op, agreed, where);
      } else {
        none++;
        System.out.printf("no clean size 0x%03X (%s) - left unknown%n", op, where);
      }
    }
    System.out.printf("%n%d confident, %d ambiguous, %d with no clean size%n", confident.size(), ambiguous, none);

    if (apply && !confident.isEmpty()) {
      List<String> table = new ArrayList<>(Files.readAllLines(Paths.get(a[1])));
      table.addAll(confident);
      table.sort((x, y) -> {
        if (x.startsWith("#")) return -1;
        if (y.startsWith("#")) return 1;
        return Integer.compare(Integer.parseInt(x.split(";")[0]), Integer.parseInt(y.split(";")[0]));
      });
      Files.write(Paths.get(a[1]), String.join("\n", table).concat("\n").getBytes("UTF-8"));
      System.out.println("appended " + confident.size() + " to " + a[1]);
    }
  }

  /** One occurrence per (file, offset) - an entry reached twice must not count as agreement. */
  static List<int[]> dedup(List<int[]> in) {
    Map<String, int[]> m = new LinkedHashMap<>();
    for (int[] o : in) m.putIfAbsent(o[0] + ":" + o[1], o);
    return new ArrayList<>(m.values());
  }

  static Set<Integer> intersect(Set<Integer> a, Set<Integer> b) {
    Set<Integer> out = new TreeSet<>(a);
    out.retainAll(b);
    return out;
  }

  /** The offset after this command's arguments, or -1 when they run off the file. */
  static int advance(byte[] f, int pc, String sizes) {
    int q = pc + 2;
    for (char s : sizes.toCharArray()) {
      int n = s == 'B' ? 1 : s == 'H' ? 2 : 4;
      if (q + n > f.length) return -1;
      q += n;
    }
    return q;
  }

  /**
   * True when a script decodes from [pc] to a real terminal through known opcodes only. An unknown
   * opcode, a run off the end, or more than MAX_STEPS commands all count as unclean - the size
   * being tested is then not evidenced.
   */
  static boolean decodesCleanly(byte[] f, int pc, Set<Integer> data) {
    for (int step = 0; step < MAX_STEPS; step++) {
      if (pc < 0 || pc + 2 > f.length) return false;
      if (data != null && data.contains(pc)) return false;
      int op = u16(f, pc);
      String[] c = cmds.get(op);
      if (c == null) return false;
      if (TERMINAL.contains(op)) return true;
      int q = advance(f, pc, c[1]);
      if (q < 0) return false;
      pc = q;
    }
    return false;
  }

  static int[][] narcIndex(String path) {
    int fat = u32(rom, 0x48);
    int s = u32(rom, fat + paths.get(path) * 8);
    int p = s + 0x10;
    int[] st = null, en = null;
    int img = 0, count = 0;
    while (true) {
      String m = new String(rom, p, 4, java.nio.charset.StandardCharsets.US_ASCII);
      int cs = u32(rom, p + 4);
      if (m.equals("BTAF")) {
        count = u16(rom, p + 8);
        st = new int[count]; en = new int[count];
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
