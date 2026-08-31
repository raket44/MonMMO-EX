import java.io.*;
import java.nio.*;
import java.nio.file.*;
import java.util.*;

/** Reads an NDS ROM's filesystem and reports the NARC entry counts for the paths the client uses. */
public class NdsProbe {
  static byte[] rom;
  static ByteBuffer buf;

  static int u16(int off) { return ((rom[off] & 0xFF) | ((rom[off + 1] & 0xFF) << 8)); }
  static int u32(int off) {
    return (rom[off] & 0xFF) | ((rom[off+1] & 0xFF) << 8) | ((rom[off+2] & 0xFF) << 16) | ((rom[off+3] & 0xFF) << 24);
  }

  public static void main(String[] args) throws Exception {
    rom = Files.readAllBytes(Paths.get(args[0]));
    String code = new String(rom, 0x0C, 4, java.nio.charset.StandardCharsets.US_ASCII);
    System.out.println("game code: " + code + "  size: " + rom.length);

    int fntOff = u32(0x40), fatOff = u32(0x48), fatSize = u32(0x4C);
    int fileCount = fatSize / 8;
    System.out.println("files in FAT: " + fileCount);

    // Walk the FNT to resolve named paths.
    Map<String, Integer> paths = new LinkedHashMap<>();
    walk(fntOff, 0, "", paths);

    String[] wanted = args.length > 1
        ? Arrays.copyOfRange(args, 1, args.length)
        : new String[]{"/a/0/0/8", "/a/0/1/2", "/a/0/0/2", "/a/0/0/3", "/a/0/1/4", "/a/0/6/1", "/a/0/0/9"};
    for (String p : wanted) {
      Integer id = paths.get(p);
      if (id == null) { System.out.println(p + " -> not found"); continue; }
      int start = u32(fatOff + id * 8), end = u32(fatOff + id * 8 + 4);
      String magic = new String(rom, start, 4, java.nio.charset.StandardCharsets.US_ASCII);
      int count = magic.equals("NARC") ? narcCount(start) : -1;
      System.out.printf("%s -> fileId=%d size=%d magic=%s entries=%s%n",
          p, id, end - start, magic, count < 0 ? "n/a" : count);
    }
  }

  /** NARC: header, then BTAF whose entry count is at +0x18 from the NARC start. */
  static int narcCount(int narcStart) {
    return u16(narcStart + 0x18);
  }

  static void walk(int fntOff, int dirId, String prefix, Map<String, Integer> out) {
    int entry = fntOff + dirId * 8;
    int subTableOff = fntOff + u32(entry);
    int firstFileId = u16(entry + 4);
    int p = subTableOff;
    int fileId = firstFileId;
    while (true) {
      int type = rom[p] & 0xFF;
      if (type == 0) break;
      if (type < 0x80) {
        String name = new String(rom, p + 1, type, java.nio.charset.StandardCharsets.US_ASCII);
        out.put(prefix + "/" + name, fileId);
        fileId++;
        p += 1 + type;
      } else {
        int len = type & 0x7F;
        String name = new String(rom, p + 1, len, java.nio.charset.StandardCharsets.US_ASCII);
        int subDirId = u16(p + 1 + len) & 0x0FFF;
        p += 1 + len + 2;
        walk(fntOff, subDirId, prefix + "/" + name, out);
      }
    }
  }
}
