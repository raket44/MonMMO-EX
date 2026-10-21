import java.nio.file.*;
import java.util.*;

/**
 * Pokemon White's mart shelves, from overlay 21 (the shop app). Read 2026-10-21 from the code at
 * 0x21B3900: a shelf id of 255 is the common shelf, index = u8 table 0x21D9484[badges]; any id
 * below 247 is shelf id + 6. Shelf N's items are u16 indexes at pointer table 0x21DE1C8[N], count
 * u8 table 0x21D95F6[N]. The script side: CallStd 10110 (file 871) with the id in VAR_0x8000.
 *
 *   Marts5 <white.nds>   - prints the badge tiers and all 26 shelves (server: UnovaMarts.kt)
 */
public class Marts5 {
  static int u16(byte[] b, int o) { return (b[o] & 0xFF) | ((b[o + 1] & 0xFF) << 8); }
  static int u32(byte[] b, int o) { return (b[o] & 0xFF) | ((b[o+1] & 0xFF) << 8) | ((b[o+2] & 0xFF) << 16) | ((b[o+3] & 0xFF) << 24); }

  static byte[] blz(byte[] in) {
    int n = in.length, foot = u32(in, n - 8), hdr = foot >>> 24, csize = foot & 0xFFFFFF, extra = u32(in, n - 4);
    if (hdr < 8 || csize > n) return in;
    byte[] out = new byte[n + extra];
    int start = n - csize;
    System.arraycopy(in, 0, out, 0, start);
    int src = n - hdr, dst = out.length;
    while (dst > start && src > start) {
      int fl = in[--src] & 0xFF;
      for (int b = 0; b < 8 && dst > start && src > start; b++, fl <<= 1) {
        if ((fl & 0x80) == 0) out[--dst] = in[--src];
        else {
          int b1 = in[--src] & 0xFF, b2 = in[--src] & 0xFF, len = (b1 >> 4) + 3, disp = ((b1 & 0xF) << 8 | b2) + 3;
          for (int k = 0; k < len && dst > start; k++) { out[dst - 1] = out[dst - 1 + disp]; dst--; }
        }
      }
    }
    return out;
  }

  public static void main(String[] a) throws Exception {
    byte[] rom = Files.readAllBytes(Paths.get(a[0]));
    int ovt = u32(rom, 0x50), fat = u32(rom, 0x48);
    byte[] f = null;
    int ram = 0;
    for (int i = 0; i < u32(rom, 0x54) / 32; i++) {
      int e = ovt + i * 32;
      if (u32(rom, e) != 21) continue;
      ram = u32(rom, e + 4);
      int fid = u32(rom, e + 24), fl = u32(rom, e + 28);
      f = Arrays.copyOfRange(rom, u32(rom, fat + fid * 8), u32(rom, fat + fid * 8 + 4));
      if ((fl >>> 24 & 1) != 0) f = blz(f);
    }
    int tiers = 0x21D9484 - ram, ptrs = 0x21DE1C8 - ram, counts = 0x21D95F6 - ram;
    StringBuilder t = new StringBuilder("tiers by badges 0-8:");
    for (int b = 0; b <= 8; b++) t.append(' ').append(f[tiers + b] & 0xFF);
    System.out.println(t);
    for (int s = 0; s < 26; s++) {
      int p = u32(f, ptrs + 4 * s) - ram, n = f[counts + s] & 0xFF;
      StringBuilder sb = new StringBuilder(s < 6 ? "common " + s + ":" : "shelf id " + (s - 6) + ":");
      for (int k = 0; k < n; k++) sb.append(' ').append(u16(f, p + 2 * k));
      System.out.println(sb);
    }
  }
}
