import java.nio.file.*;
import java.util.*;

/**
 * Checks how a Gen 5 script's jump offsets resolve to targets. 37 of the 82 entries that stop
 * decoding stop at an ODD offset, which real code never sits at, so at least some targets are
 * computed wrongly - and a wrong branch target is worse than a stop: the script runs real commands
 * from the wrong place.
 *
 * Every convention is scored the same way, against the ROM: decode each file from its entry points
 * with the known table, collecting the offsets that really are command starts; then resolve every
 * jump under each convention and count how many land on one. The right convention lands on a
 * command start nearly always; a wrong one lands mid-command about half the time.
 *
 *   Jumps5 <rom> <table>
 */
public class Jumps5 {
  static byte[] rom;
  static Map<String, Integer> paths = new LinkedHashMap<>();
  static Map<Integer, String[]> cmds = new HashMap<>();
  static final Set<Integer> TERMINAL = new HashSet<>(Arrays.asList(0x02, 0x05, 0x1D, 0x1E, 0x1C));
  /** Commands whose last argument is a 4-byte relative offset. */
  static final Map<Integer, String> JUMPS = Map.of(0x04, "CallRoutine", 0x1E, "Jump", 0x1F, "When", 0x20, "If");

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
    int[][] idx = narcIndex("/a/0/5/7");

    // name -> hits, total
    String[] names = {"afterField+off (current)", "commandStart+off", "afterOpcode+off", "entryBase+off"};
    int[] hits = new int[names.length], odd = new int[names.length];
    int total = 0;

    for (int fi = 0; fi < idx.length; fi++) {
      byte[] f = Arrays.copyOfRange(rom, idx[fi][0], idx[fi][1]);
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
      // Command starts reachable by straight-line decoding from every entry.
      Set<Integer> starts = new HashSet<>();
      List<int[]> jumps = new ArrayList<>(); // {pc, fieldPos}
      for (int e : entries) {
        int pc = e;
        for (int step = 0; step < 4096 && pc >= 0 && pc + 2 <= f.length; step++) {
          if (!starts.add(pc)) break;
          int op = u16(f, pc);
          String[] c = cmds.get(op);
          if (c == null) break;
          int q = pc + 2;
          boolean bad = false;
          for (char s : c[1].toCharArray()) {
            int n = s == 'B' ? 1 : s == 'H' ? 2 : 4;
            if (q + n > f.length) { bad = true; break; }
            if (n == 4 && JUMPS.containsKey(op)) jumps.add(new int[] {pc, q, e});
            q += n;
          }
          if (bad || TERMINAL.contains(op)) break;
          pc = q;
        }
      }
      for (int[] j : jumps) {
        int pc = j[0], q = j[1], entry = j[2];
        int off = u32(f, q);
        int[] cand = {q + 4 + off, pc + off, pc + 2 + off, entry + off};
        total++;
        for (int i = 0; i < cand.length; i++) {
          if (cand[i] % 2 != 0) odd[i]++;
          if (cand[i] >= 0 && cand[i] < f.length && starts.contains(cand[i])) hits[i]++;
        }
      }
    }
    System.out.println("jump-like commands found: " + total);
    for (int i = 0; i < names.length; i++)
      System.out.printf("  %-26s lands on a command start %6d (%4.1f%%)   odd targets %6d%n",
          names[i], hits[i], 100.0 * hits[i] / Math.max(1, total), odd[i]);
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
