import java.nio.file.*;
import java.util.*;

/**
 * Finds Black/White's SCRIPT COMMAND TABLE in the ARM9 and its overlays - the game's own array of
 * handler pointers, one per opcode. That table is the authoritative answer to "how many arguments
 * does this opcode take", which the ROM's script bytes alone cannot settle for an opcode used once
 * (tools/nds/Sizes5.java leaves those unknown on purpose).
 *
 * The signature: a long run of consecutive u32 values in DS RAM range with bit 0 set (THUMB
 * function pointers). Reported with the RAM address, the file it lives in, and the entry count, so
 * the count can be checked against the highest opcode the scripts actually use.
 *
 *   CmdTable5 <rom>                 - find candidate tables
 *   CmdTable5 <rom> <opcode-hex>... - also print each opcode's handler address
 */
public class CmdTable5 {
  static byte[] rom;

  static int u16(byte[] b, int o) { return (b[o] & 0xFF) | ((b[o + 1] & 0xFF) << 8); }
  static int u32(byte[] b, int o) { return (b[o] & 0xFF) | ((b[o+1] & 0xFF) << 8) | ((b[o+2] & 0xFF) << 16) | ((b[o+3] & 0xFF) << 24); }

  static byte[] blz(byte[] in) {
    try {
      int end = u32(in, in.length - 8), extra = u32(in, in.length - 4);
      int hdr = (end >>> 24) & 0xFF, cmpLen = end & 0xFFFFFF;
      if (hdr < 8 || hdr > 11 || cmpLen > in.length || extra <= 0 || extra > in.length * 8) return in;
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
    } catch (Exception e) { return in; }
  }

  record Bin(String name, byte[] data, int ram) {}

  public static void main(String[] a) throws Exception {
    rom = Files.readAllBytes(Paths.get(a[0]));
    List<Bin> bins = new ArrayList<>();
    int arm9Off = u32(rom, 0x20), arm9Size = u32(rom, 0x2C), arm9Ram = u32(rom, 0x28);
    bins.add(new Bin("arm9", blz(Arrays.copyOfRange(rom, arm9Off, arm9Off + arm9Size)), arm9Ram));
    int ovt = u32(rom, 0x50), ovs = u32(rom, 0x54), fat = u32(rom, 0x48);
    for (int p = ovt; p + 32 <= ovt + ovs; p += 32) {
      int id = u32(rom, p), ram = u32(rom, p + 4), fileId = u32(rom, p + 24), comp = u32(rom, p + 28);
      int s = u32(rom, fat + fileId * 8), e = u32(rom, fat + fileId * 8 + 4);
      if (s < 0 || e > rom.length || e <= s) continue;
      byte[] b = Arrays.copyOfRange(rom, s, e);
      if ((comp & 0x1000000) != 0) b = blz(b);
      bins.add(new Bin("ov" + id, b, ram));
    }
    System.out.println("binaries: " + bins.size());

    // A handler table: consecutive u32 in RAM range, bit 0 set (THUMB).
    List<int[]> found = new ArrayList<>();
    for (int bi = 0; bi < bins.size(); bi++) {
      byte[] b = bins.get(bi).data();
      int run = 0;
      for (int o = 0; o + 4 <= b.length; o += 4) {
        int v = u32(b, o);
        boolean ptr = (v & 1) == 1 && (v & 0xFF000000) == 0x02000000;
        if (ptr) run++;
        else {
          if (run >= 200) found.add(new int[] {bi, o - run * 4, run});
          run = 0;
        }
      }
      if (run >= 200) found.add(new int[] {bi, b.length - run * 4, run});
    }
    found.sort((x, y) -> y[2] - x[2]);
    for (int[] f : found) {
      Bin bin = bins.get(f[0]);
      System.out.printf("%-6s file offset 0x%06X  ram 0x%08X  %d entries%n", bin.name(), f[1], bin.ram() + f[1], f[2]);
    }
    if (found.isEmpty()) { System.out.println("no candidate table found"); return; }

    for (int i = 1; i < a.length; i++) {
      int op = Integer.parseInt(a[i], 16);
      for (int[] f : found) {
        Bin bin = bins.get(f[0]);
        if (op >= f[2]) continue;
        System.out.printf("  0x%03X -> %-6s handler 0x%08X%n", op, bin.name(), u32(bin.data(), f[1] + op * 4));
      }
    }
  }
}
