import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Dumps the cosmetic-addon index from the client's data/sprites/addons.pak, following the
 * client's own parser (f.oF1.L2, verified against bytecode):
 *
 * <pre>
 * [whole file, LE] ... [20-byte SHA1 of everything before it]
 * pos 8: u16 (version), u8 baseCount
 * base bodies: baseCount x { 3 resolutions (f.AX1.ue) x { u8 frameCount, frameCount x { s32 len, len bytes (PNG) } } }
 * s32 addonCount
 * addon: s16 id, u8 slot (f.ne0 ordinal), u8 nameLen, name, s32 flags,
 *        u8 rows x { s16 len, len x s16 } (frame tables),
 *        u8 variants, u8 anims x { u8 kind, u16 durationMs, u8 n, n bytes } (f.Se0),
 *        variants x { 3 resolutions x { u8 frameCount, frameCount x { s32 len, skip } } }
 * </pre>
 *
 * The linked bag item id is the client's own J61.ZQ1 formula: HAT ids 256..495 -> id+4320;
 * otherwise 2000 + slot*256 + id. An item exists only for item-backed slots (all but
 * FOREHEAD/HAIR/EYES/FACIAL_HAIR) or when flags bit 2 marks an innate-slot addon as item-backed.
 * flags bit 4 (J61.q4) marks a default addon - usable without any item.
 *
 * Output: slot|id|itemId|flags|variants|name  (itemId -1 = no item, always free)
 */
public class AddonIndex {
  private static final String[] SLOTS = {
    "FOREHEAD", "HAT", "HAIR", "EYES", "FACIAL_HAIR", "BACK", "TOP", "GLOVES", "FOOTWEAR",
    "LEGGINGS", "FISHING_ROD", "BIKE"
  };
  private static final int RESOLUTIONS = 3; // f.AX1.ue: So0/E90/uj

  public static void main(String[] args) throws IOException {
    ByteBuffer b = ByteBuffer.wrap(Files.readAllBytes(Path.of(args[0])));
    b.order(ByteOrder.LITTLE_ENDIAN);
    b.limit(b.limit() - 20); // trailing SHA1

    b.position(8);
    b.getShort();
    int baseCount = b.get() & 0xFF;
    for (int i = 0; i < baseCount; i++) skipFrames(b, 1);

    int addonCount = b.getInt();
    for (int a = 0; a < addonCount; a++) {
      int id = b.getShort() & 0xFFFF;
      int slot = b.get() & 0xFF;
      byte[] name = new byte[b.get() & 0xFF];
      b.get(name);
      int flags = b.getInt();
      int rows = b.get() & 0xFF;
      for (int r = 0; r < rows; r++) {
        int len = b.getShort() & 0xFFFF;
        for (int k = 0; k < len; k++) b.getShort();
      }
      int variants = b.get() & 0xFF;
      int anims = b.get() & 0xFF;
      for (int s = 0; s < anims; s++) {
        b.get();
        b.getShort();
        int n = b.get() & 0xFF;
        b.position(b.position() + n);
      }
      skipAddonFrames(b, variants);

      String label = new String(name).trim();
      int itemId = itemIdFor(slot, id, flags);
      if (!"UNUSED".equals(label)) {
        System.out.println(
            SLOTS[slot] + "|" + id + "|" + itemId + "|" + flags + "|" + variants + "|" + label);
      }
    }
  }

  /** J61 ctor + ZQ1: item-backed slots always link an item; innate slots only with flags bit 2. */
  private static int itemIdFor(int slot, int id, int flags) {
    boolean innate = slot == 0 || slot == 2 || slot == 3 || slot == 4;
    if (innate && (flags & 4) == 0) return -1;
    if (slot == 1 && id > 255) {
      if (id < 496) return id + 4320;
      throw new IllegalStateException("HAT id out of allocated range: " + id);
    }
    return 2000 + slot * 256 + id;
  }

  private static void skipFrames(ByteBuffer b, int variants) {
    for (int v = 0; v < variants; v++) {
      for (int r = 0; r < RESOLUTIONS; r++) {
        int frames = b.get() & 0xFF;
        for (int i = 0; i < frames; i++) {
          int len = b.getInt();
          b.position(b.position() + len);
        }
      }
    }
  }

  /**
   * Addon frames are pose -> frame -> layered entries: resolution 0 carries a mirror byte per
   * frame; a zero layer byte prefixes an inline length-prefixed PNG, a nonzero one a 3-byte
   * reference plus two s32s (format version 9).
   */
  private static void skipAddonFrames(ByteBuffer b, int variants) {
    for (int v = 0; v < variants; v++) {
      for (int r = 0; r < RESOLUTIONS; r++) {
        int poses = b.get() & 0xFF;
        for (int p = 0; p < poses; p++) {
          int frames = b.get() & 0xFF;
          for (int i = 0; i < frames; i++) {
            int mirror = r == 0 ? b.get() & 0xFF : 0;
            b.get(); // layer
            if (mirror == 0) {
              int len = b.getInt();
              b.position(b.position() + len);
            } else {
              // Mirrored frame: a backref to its unmirrored source, no inline image.
              b.position(b.position() + 4);
              b.getInt();
              b.getInt();
            }
          }
        }
      }
    }
  }
}
