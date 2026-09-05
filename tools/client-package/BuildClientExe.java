import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Builds a double-clickable client exe: the launch4j stub taken from the retail PokeMMO.exe,
 * followed by one jar holding every entry of the patched client jar with the classpath overlay
 * (patch-classes.jar) merged over it. The result is what "Play MonMMO Local.cmd" assembles on the
 * command line, baked into one file the stub launches with the bundled jre.
 *
 * Usage: java BuildClientExe.java <stubExe> <baseJar> <overlayJar> <out.exe>
 */
public class BuildClientExe {
  public static void main(String[] args) throws Exception {
    if (args.length != 4) {
      System.err.println("usage: BuildClientExe <stubExe> <baseJar> <overlayJar> <out.exe>");
      System.exit(2);
    }
    Path stubExe = Path.of(args[0]);
    Path baseJar = Path.of(args[1]);
    Path overlayJar = Path.of(args[2]);
    Path out = Path.of(args[3]);

    long stubSize = zipStart(stubExe);
    Map<String, byte[]> overlay = new LinkedHashMap<>();
    try (ZipInputStream in = new ZipInputStream(Files.newInputStream(overlayJar))) {
      for (ZipEntry e; (e = in.getNextEntry()) != null; ) {
        if (!e.isDirectory()) overlay.put(e.getName(), in.readAllBytes());
      }
    }
    int overlayCount = overlay.size();

    Path parent = out.toAbsolutePath().getParent();
    if (parent != null) Files.createDirectories(parent);
    int copied = 0, replaced = 0, added = 0;
    try (OutputStream raw = new BufferedOutputStream(Files.newOutputStream(out), 1 << 20)) {
      try (InputStream stub = Files.newInputStream(stubExe)) {
        raw.write(stub.readNBytes((int) stubSize));
      }
      ZipOutputStream zip = new ZipOutputStream(raw);
      Set<String> written = new HashSet<>();
      try (ZipFile base = new ZipFile(baseJar.toFile())) {
        for (Enumeration<? extends ZipEntry> en = base.entries(); en.hasMoreElements(); ) {
          ZipEntry e = en.nextElement();
          if (!written.add(e.getName())) continue;
          zip.putNextEntry(new ZipEntry(e.getName()));
          if (!e.isDirectory()) {
            byte[] patched = overlay.remove(e.getName());
            if (patched != null) {
              zip.write(patched);
              replaced++;
            } else {
              try (InputStream in = base.getInputStream(e)) {
                in.transferTo(zip);
              }
              copied++;
            }
          }
          zip.closeEntry();
        }
      }
      for (Map.Entry<String, byte[]> e : overlay.entrySet()) {
        zip.putNextEntry(new ZipEntry(e.getKey()));
        zip.write(e.getValue());
        zip.closeEntry();
        added++;
      }
      zip.finish();
      zip.flush();
    }
    System.out.printf(
        "%s: stub %d bytes, %d entries copied, %d replaced, %d added (%d in overlay), %d bytes%n",
        out, stubSize, copied, replaced, added, overlayCount, Files.size(out));
  }

  /** Offset of the first zip local file header, i.e. the size of the launcher stub in front. */
  private static long zipStart(Path exe) throws Exception {
    byte[] bytes = Files.readAllBytes(exe);
    for (int i = 0; i + 4 <= bytes.length; i++) {
      if (bytes[i] == 'P' && bytes[i + 1] == 'K' && bytes[i + 2] == 3 && bytes[i + 3] == 4) return i;
    }
    throw new IllegalStateException(exe + " holds no zip");
  }
}
