import java.nio.file.*;
import java.util.Arrays;

/**
 * Retargets f/HQ's character-customization open from preview mode (f.r4.XO0 - the floppy only
 * applies locally and sends nothing) to the immediate-send mode (f.r4.Qc0 - the floppy emits
 * c2s 0x29 and closes the dialog). The class's constant pool holds the field name "XO0" as a
 * single-use Utf8 of the same length as "Qc0", so the patch is an in-place 3-byte rename.
 */
public class PatchHQ {
  public static void main(String[] args) throws Exception {
    byte[] data = Files.readAllBytes(Path.of(args[0]));
    byte[] needle = {1, 0, 3, 'X', 'O', '0'}; // Utf8 tag, u16 len 3, "XO0"
    int found = -1;
    int count = 0;
    for (int i = 0; i <= data.length - needle.length; i++) {
      boolean match = true;
      for (int j = 0; j < needle.length; j++) {
        if (data[i + j] != needle[j]) { match = false; break; }
      }
      if (match) { found = i; count++; }
    }
    if (count != 1) throw new IllegalStateException("Expected exactly 1 tagged 'XO0' Utf8, found " + count);
    data[found + 3] = 'Q';
    data[found + 4] = 'c';
    data[found + 5] = '0';
    Files.write(Path.of(args[1]), data);
    System.out.println("Patched Utf8 at " + found + " -> Qc0, wrote " + args[1]);
  }
}
