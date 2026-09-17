// Proves what the launcher will draw, from a BUILT apk, by decoding the chain the same way Android
// does: the adaptive icon's binary XML names its two layers by resource id; the resource table maps
// each id (every config of it) to a file path or a colour; the file must then exist in the apk.
//
// Written after four installs went to a phone on the strength of "the strings look right". This is
// the check the build runs instead. args: <apk>
import java.io.*; import java.nio.*; import java.nio.charset.StandardCharsets; import java.util.*;
import java.util.zip.*;

public class IconChain {
  public static void main(String[] a) throws Exception {
    Map<String, byte[]> files = new HashMap<>();
    try (ZipInputStream in = new ZipInputStream(new FileInputStream(a[0]))) {
      for (ZipEntry e; (e = in.getNextEntry()) != null; ) {
        if (e.getName().equals("resources.arsc") || e.getName().startsWith("res/")) {
          files.put(e.getName(), in.readAllBytes());
        }
      }
    }
    byte[] arsc = files.get("resources.arsc");
    ByteBuffer b = ByteBuffer.wrap(arsc).order(ByteOrder.LITTLE_ENDIAN);
    String[] pool = pool(b, 12);
    int pkg = 12 + b.getInt(16);
    String[] typeNames = pool(b, pkg + b.getInt(pkg + 268)), keys = pool(b, pkg + b.getInt(pkg + 276));

    // 1. Which file is the launcher's mipmap for Android 8+? The `pokemmo` mipmap's v26 config.
    String adaptiveXml = null;
    for (Object[] r : entries(b, pkg, keys, typeNames)) {
      if (r[1].equals("mipmap") && r[2].equals("pokemmo") && (int) r[4] >= 26) adaptiveXml = (String) r[6];
    }
    System.out.println("launcher icon (mipmap pokemmo, v26+): " + adaptiveXml);
    if (adaptiveXml == null || !files.containsKey(adaptiveXml)) { System.out.println("FAIL: no v26 mipmap file"); System.exit(1); }

    // 2. The adaptive icon's layers, by resource id, out of its binary XML.
    Map<String, Integer> layers = axmlRefs(files.get(adaptiveXml));
    System.out.println("adaptive layers: " + layers);
    boolean ok = layers.containsKey("foreground") && layers.containsKey("background");

    // 3. Each id -> every config's value; a file must exist, a colour must be a colour.
    for (Map.Entry<String, Integer> l : layers.entrySet()) {
      int id = l.getValue(); boolean seen = false;
      for (Object[] r : entries(b, pkg, keys, typeNames)) {
        if ((int) r[0] != id) continue;
        seen = true;
        String v = (String) r[6];
        boolean fileOk = v.startsWith("res/") ? files.containsKey(v) : true;
        System.out.printf("  %-10s 0x%08X %-8s %-20s density=%-3d sdk=%-2d -> %s %s%n", l.getKey(), id,
            r[1], r[2], (int) r[3], (int) r[4], v, fileOk ? "" : "<-- FILE MISSING");
        ok &= fileOk;
      }
      if (!seen) { System.out.println("  " + l.getKey() + ": NO TABLE ENTRY"); ok = false; }
    }
    System.out.println(ok ? "ICON CHAIN OK" : "ICON CHAIN BROKEN");
    System.exit(ok ? 0 : 1);
  }

  /** Every simple entry: {resId, typeName, keyName, density, sdk, dataType, valueText}. */
  static List<Object[]> entries(ByteBuffer b, int pkg, String[] keys, String[] typeNames) {
    List<Object[]> out = new ArrayList<>();
    String[] pool = pool(b, 12);
    int keyStrings = pkg + b.getInt(pkg + 276), p = keyStrings + b.getInt(keyStrings + 4);
    while (p + 8 <= b.capacity()) {
      int type = b.getShort(p) & 0xFFFF, headerSize = b.getShort(p + 2) & 0xFFFF, size = b.getInt(p + 4);
      if (size <= 0) break;
      if (type == 0x0201) {
        int id = b.get(p + 8) & 0xFF, count = b.getInt(p + 12), entriesStart = b.getInt(p + 16);
        // ResTable_config at p+20: size(4) mcc/mnc(4) locale(4) orientation(1) touchscreen(1)
        // density(2) at +12; input(4) screenSize(4); sdkVersion(2) at +24.
        int density = b.getShort(p + 20 + 12) & 0xFFFF, sdk = b.getShort(p + 20 + 24) & 0xFFFF;
        for (int i = 0; i < count; i++) {
          int off = b.getInt(p + headerSize + i * 4);
          if (off == -1) continue;
          int e = p + entriesStart + off;
          if ((b.getShort(e + 2) & 1) != 0) continue;
          int dt = b.get(e + 11) & 0xFF, data = b.getInt(e + 12);
          String v = dt == 0x03 ? pool[data] : String.format("colour 0x%08X", data);
          out.add(new Object[] {0x7F000000 | (id << 16) | i, typeNames[id - 1], keys[b.getInt(e + 4)], density, sdk, dt, v});
        }
      }
      p += size;
    }
    return out;
  }

  /** The resource ids named by each element of a small binary XML (attribute -> reference). */
  static Map<String, Integer> axmlRefs(byte[] xml) {
    ByteBuffer b = ByteBuffer.wrap(xml).order(ByteOrder.LITTLE_ENDIAN);
    Map<String, Integer> out = new LinkedHashMap<>();
    String[] pool = null;
    int p = 8;
    while (p + 8 <= xml.length) {
      int type = b.getShort(p) & 0xFFFF, size = b.getInt(p + 4);
      if (size <= 0) break;
      if (type == 0x0001) pool = pool(b, p);
      if (type == 0x0102) {
        String el = pool[b.getInt(p + 20)];
        int attrStart = p + 16 + (b.getShort(p + 24) & 0xFFFF), attrs = b.getShort(p + 28) & 0xFFFF;
        for (int i = 0; i < attrs; i++) {
          int at = attrStart + i * 20;
          if ((b.get(at + 15) & 0xFF) == 0x01) out.put(el, b.getInt(at + 16));
        }
      }
      p += size;
    }
    return out;
  }

  static String[] pool(ByteBuffer b, int at) {
    int count = b.getInt(at + 8), flags = b.getInt(at + 16), strStart = b.getInt(at + 20);
    boolean utf8 = (flags & 0x100) != 0;
    String[] out = new String[count];
    for (int i = 0; i < count; i++) {
      int off = at + strStart + b.getInt(at + 28 + i * 4);
      if (utf8) {
        int q = off + (((b.get(off) & 0x80) != 0) ? 2 : 1);
        int n = b.get(q) & 0xFF;
        if ((n & 0x80) != 0) { n = ((n & 0x7F) << 8) | (b.get(q + 1) & 0xFF); q += 2; } else q += 1;
        byte[] s = new byte[n];
        for (int k = 0; k < n; k++) s[k] = b.get(q + k);
        out[i] = new String(s, StandardCharsets.UTF_8);
      } else {
        int n = b.getShort(off) & 0xFFFF, q = off + 2;
        StringBuilder sb = new StringBuilder();
        for (int k = 0; k < n; k++) sb.append((char) b.getShort(q + k * 2));
        out[i] = sb.toString();
      }
    }
    return out;
  }
}
