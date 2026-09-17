import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.Adler32;
import java.util.zip.CRC32;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

/**
 * Builds the MonMMO-EX Android client from a retail PokeMMO APK, in two passes around jarsigner.
 *
 * <pre>
 *   prepare &lt;retail.apk&gt; &lt;unsigned.apk&gt; &lt;host&gt; &lt;mod.zip|-&gt; &lt;game.public.pem&gt; &lt;chat.public.pem&gt;
 *   finish  &lt;v1-signed.apk&gt; &lt;out.apk&gt; &lt;keystore.p12&gt; &lt;password-file&gt; &lt;alias&gt;
 * </pre>
 *
 * prepare mirrors what the Windows package does to the desktop client:
 *
 * <ul>
 *   <li>The two server keys the retail client trusts (desktop f/h8: game, then chat) are swapped for
 *       ours inside classes.dex. The APK carries the exact same key strings as desktop 31914, and ours
 *       are the same length, so this is an in-place swap followed by the dex checksum and SHA-1
 *       repair.
 *   <li>assets/config/zzz-monmmo-ex-server.properties: AndroidLauncher.onCreate loads every
 *       properties file under the APK's assets/config/ into the client's config, the Android
 *       counterpart of the desktop config/ folder. Same keys as the Windows package.
 *   <li>assets/data/mods/monmmo-lost-knights.zip, beside the retail example mod.
 *   <li>The retail v1 signature files are dropped; the old v2 block goes with the rewrite.
 * </ul>
 *
 * finish zip-aligns the jarsigner output (stored entries on 4-byte boundaries, which targetSdk 30+
 * requires for resources.arsc) and adds an APK Signature Scheme v2 block with the same key, then
 * re-parses the result and checks the v2 signature, its digest and the alignment.
 */
public class ApkPackager {
  static final String RETAIL_GAME_KEY =
      "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEtqx2myJz3ftlYWgd7cbNqf2t208itQMY7ouPNBDpQetbi7eXbEDxDDZy4Q9fMnI6mF5/D0qMdRd40SRXf0OS7Q==";
  static final String RETAIL_CHAT_KEY =
      "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEh4Vqgnd+8Fqebu0H40v+FgwhE6RwgAYxJMihb8mJmcHDy8r/rPz3kLHH1oabyKIRUa5Y2cK0TsxZky+mp7DKWA==";
  static final String CONFIG_ENTRY = "assets/config/zzz-monmmo-ex-server.properties";
  static final String MOD_DIR = "assets/data/mods/";
  /**
   * The adaptive icon's two layers, by the names the retail resource table gives them. Resolved
   * through the table at build time (never by guessed offsets: an earlier parser with a hardcoded
   * chunk header size pointed at the wrong drawable and cost four installs). The overlay file
   * below is the art for the foreground; it is shipped under whatever path the table used.
   */
  static final String ICON_FOREGROUND_NAME = "pokemmo_foreground";
  static final String ICON_BACKGROUND_NAME = "pokemmo_background";
  static final String ICON_FOREGROUND_OVERLAY = "res/adaptive-foreground.png";
  static final int ALIGN = 4;
  static final int V2_ID = 0x7109871a;
  static final int RSA_PKCS1_SHA256 = 0x0103;
  static final int CHUNK = 1 << 20;
  // 1981-01-01 01:01, the timestamp every retail entry carries.
  static final int DOS_TIME = 0x0820, DOS_DATE = 0x0221;

  record Entry(String name, int method, long crc, long csize, long usize, byte[] data, int dataOff) {}

  public static void main(String[] args) throws Exception {
    if (args.length >= 7 && args[0].equals("prepare")) {
      prepare(
          Path.of(args[1]), Path.of(args[2]), args[3], args[4], Path.of(args[5]), Path.of(args[6]),
          args.length > 7 ? args[7] : "-", args.length > 8 ? args[8] : "-",
          args.length > 9 ? args[9] : "-", args.length > 10 ? args[10] : "-");
    } else if (args.length >= 6 && args[0].equals("finish")) {
      finish(Path.of(args[1]), Path.of(args[2]), Path.of(args[3]), Path.of(args[4]), args[5]);
    } else {
      System.err.println("usage: prepare <retail.apk> <unsigned.apk> <host> <mod.zip|-> <game.pem> <chat.pem>");
      System.err.println("       finish <v1-signed.apk> <out.apk> <keystore.p12> <password-file> <alias>");
      System.exit(2);
    }
  }

  // ---------------------------------------------------------------- prepare

  static final String DATA_PAK_ENTRY = "assets/data/data.pak";
  static final String STRINGS_EN_ENTRY = "assets/data/strings/strings_en.xml";

  /**
   * [dataPak] and [stringsEn] replace the APK's own copies when not "-". They come from staging the
   * Expansion content against this APK's r32645 data - the species the character select resolves,
   * learnsets, details and names. Without them the stock client knows only dex 1-649 and crashes the
   * moment a party holds anything newer. Each keeps the original entry's compression method, since
   * the asset loader may memory-map a stored entry.
   *
   * [classesDex], when not "-", is a classes.dex rebuilt with smali from the APK's own code plus
   * MonMMO-EX edits (Fairy in the type enum, badge tables, Pokedex regions). It replaces the APK's
   * classes.dex BEFORE the server-key swap, so the same key patch and checksum repair apply to it.
   *
   * [overlayDir], when not "-", holds files laid out by APK path (assets/...): each replaces the
   * entry of the same name or is added - e.g. the sprite atlas with the hand-drawn Fairy badge page.
   */
  static void prepare(
      Path in, Path out, String host, String mod, Path gamePem, Path chatPem,
      String dataPak, String stringsEn, String classesDex, String overlayDir)
      throws Exception {
    byte[] file = Files.readAllBytes(in);
    // The mod keeps its own file name inside the APK, and its info.xml names the theme we select,
    // so nothing here goes stale when the theme is replaced or renamed.
    String modEntry = mod.equals("-") ? "-" : MOD_DIR + Path.of(mod).getFileName();
    String themeName = mod.equals("-") ? "-" : mobileThemeName(Files.readAllBytes(Path.of(mod)));
    Map<String, String> keys = new LinkedHashMap<>();
    keys.put(RETAIL_GAME_KEY, pemBody(gamePem));
    keys.put(RETAIL_CHAT_KEY, pemBody(chatPem));

    List<Entry> result = new ArrayList<>();
    List<String> replaced = new ArrayList<>();
    boolean dexPatched = false;
    int dropped = 0;
    for (Entry e : read(file)) {
      String n = e.name();
      if (isV1SignatureFile(n)) {
        dropped++;
        continue;
      }
      if (n.startsWith("assets/config/")) {
        throw new IllegalStateException("retail APK unexpectedly ships " + n);
      }
      if (n.equals(modEntry)) continue;
      String replacement =
          n.equals(DATA_PAK_ENTRY) ? dataPak : n.equals(STRINGS_EN_ENTRY) ? stringsEn : "-";
      if (!replacement.equals("-")) {
        byte[] bytes = Files.readAllBytes(Path.of(replacement));
        result.add(e.method() == 0 ? stored(n, bytes) : deflated(n, bytes));
        replaced.add(n);
        continue;
      }
      // Android 8+ draws the launcher icon from the adaptive icon (the `pokemmo` mipmap's v26
      // variant), not the legacy mipmaps - so the icon is only ours on modern phones once the
      // adaptive layers are. The foreground ships as a vector we cannot author here, so its table
      // entry is repointed at a PNG of the same path length (an in-place swap like the server keys),
      // and the background colour's entry is rewritten in place. Both are found by NAME.
      if (n.equals("resources.arsc") && overlayHas(overlayDir, ICON_FOREGROUND_OVERLAY)) {
        byte[] arsc = content(e);
        AdaptiveIcon icon = AdaptiveIcon.locate(arsc);
        String target = icon.foregroundPath.replaceAll("\\.xml$", ".png");
        if (target.length() != icon.foregroundPath.length()) {
          throw new IllegalStateException("foreground path is not an .xml: " + icon.foregroundPath);
        }
        System.arraycopy(target.getBytes(StandardCharsets.US_ASCII), 0, arsc, icon.foregroundPathAt, target.length());
        int argb = Integer.parseUnsignedInt(System.getProperty("monmmo.iconBackground", "FF14161A"), 16);
        ByteBuffer.wrap(arsc).order(ByteOrder.LITTLE_ENDIAN).putInt(icon.backgroundValueAt, argb);
        result.add(stored(n, arsc));
        result.add(stored(target, Files.readAllBytes(Path.of(overlayDir, "res", "adaptive-foreground.png"))));
        replaced.add(n + " (adaptive icon: " + icon.foregroundPath + " -> " + target
            + String.format(", background 0x%08X -> 0x%08X)", icon.backgroundValue, argb));
        continue;
      }
      if (n.equals("classes.dex")) {
        byte[] dex = classesDex.equals("-") ? content(e) : Files.readAllBytes(Path.of(classesDex));
        if (!classesDex.equals("-")) replaced.add(n + " (rebuilt)");
        patchDex(dex, keys);
        result.add(deflated(n, dex));
        dexPatched = true;
        continue;
      }
      result.add(e);
    }
    if (!dexPatched) throw new IllegalStateException("no classes.dex in " + in);
    for (String[] want : new String[][] {{dataPak, DATA_PAK_ENTRY}, {stringsEn, STRINGS_EN_ENTRY}}) {
      if (!want[0].equals("-") && !replaced.contains(want[1])) {
        throw new IllegalStateException("asked to replace " + want[1] + " but the APK has no such entry");
      }
    }
    if (!overlayDir.equals("-")) {
      Path root = Path.of(overlayDir);
      List<Path> files;
      try (var walk = Files.walk(root)) {
        files = walk.filter(Files::isRegularFile).sorted().toList();
      }
      for (Path p : files) {
        String name = root.relativize(p).toString().replace('\\', '/');
        if (name.equals(ICON_FOREGROUND_OVERLAY)) continue;   // shipped under the table's own path above
        // assets/ is the client's own data; res/ is only there for the launcher icon's mipmaps,
        // whose obfuscated names the build reads out of resources.arsc. Everything else - the dex,
        // the manifest, the resource table, the signatures - stays off limits to the overlay.
        if (!name.startsWith("assets/") && !name.startsWith("res/")) {
          throw new IllegalStateException("overlay may only touch assets/ or res/, not " + name);
        }
        byte[] bytes = Files.readAllBytes(p);
        boolean existed = result.removeIf(x -> x.name().equals(name));
        result.add(name.endsWith(".png") ? stored(name, bytes) : deflated(name, bytes));
        replaced.add(name + (existed ? " (overlay)" : " (added)"));
      }
    }
    if (!replaced.isEmpty()) System.out.println("[apk] replaced " + replaced);

    String props =
        "# MonMMO-EX: points the client at our server instead of the official one.\n"
            + "force.ls.host=" + host + "\n"
            + "force.ls.port=2106\n"
            + "force.gs.host=" + host + "\n"
            + "force.gs.port=7777\n"
            + "client.misc.ignore_feed=true\n"
            + "client.misc.testserver_feed_signature=false\n"
            + (mod.equals("-")
                ? ""
                : "client.mods.enabled_mods=" + Path.of(mod).getFileName() + "\n"
                    + (themeName.equals("-") ? "" : "client.ui.theme=" + themeName + "\n"));
    result.add(deflated(CONFIG_ENTRY, props.getBytes(StandardCharsets.ISO_8859_1)));
    if (!mod.equals("-")) result.add(stored(modEntry, Files.readAllBytes(Path.of(mod))));

    Files.write(out, write(result));
    System.out.println(
        "[apk] prepared " + out + ": " + result.size() + " entries, dropped " + dropped
            + " retail signature files, dex keys swapped, config for " + host
            + (mod.equals("-") ? ", no mod" : ", mod added"));
  }

  static boolean isV1SignatureFile(String n) {
    if (!n.startsWith("META-INF/") || n.indexOf('/', 9) >= 0) return false;
    return n.equals("META-INF/MANIFEST.MF") || n.endsWith(".SF") || n.endsWith(".RSA")
        || n.endsWith(".DSA") || n.endsWith(".EC");
  }

  static void patchDex(byte[] dex, Map<String, String> replacements) throws Exception {
    if (dex[0] != 'd' || dex[1] != 'e' || dex[2] != 'x' || dex[3] != '\n') {
      throw new IllegalStateException("classes.dex has no dex magic");
    }
    for (Map.Entry<String, String> r : replacements.entrySet()) {
      byte[] from = r.getKey().getBytes(StandardCharsets.US_ASCII);
      byte[] to = r.getValue().getBytes(StandardCharsets.US_ASCII);
      if (from.length != to.length) {
        throw new IllegalStateException("key lengths differ: " + from.length + " vs " + to.length);
      }
      int at = indexOf(dex, from, 0);
      if (at < 0) throw new IllegalStateException("retail key not found: " + r.getKey().substring(36, 48));
      if (indexOf(dex, from, at + 1) >= 0) throw new IllegalStateException("retail key found twice");
      System.arraycopy(to, 0, dex, at, to.length);
    }
    fixDexHeader(dex);
  }

  /** SHA-1 of everything after the signature field, then Adler-32 of everything after the checksum. */
  static void fixDexHeader(byte[] dex) throws Exception {
    MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
    sha1.update(dex, 32, dex.length - 32);
    System.arraycopy(sha1.digest(), 0, dex, 12, 20);
    Adler32 adler = new Adler32();
    adler.update(dex, 12, dex.length - 12);
    putLe32(dex, 8, adler.getValue());
  }

  static String pemBody(Path pem) throws IOException {
    StringBuilder sb = new StringBuilder();
    for (String line : Files.readAllLines(pem)) {
      if (!line.startsWith("-----")) sb.append(line.trim());
    }
    return sb.toString();
  }

  // ---------------------------------------------------------------- finish

  static void finish(Path in, Path out, Path keystore, Path passwordFile, String alias) throws Exception {
    char[] password = Files.readString(passwordFile).trim().toCharArray();
    KeyStore ks = KeyStore.getInstance("PKCS12");
    try (InputStream s = Files.newInputStream(keystore)) {
      ks.load(s, password);
    }
    PrivateKey key = (PrivateKey) ks.getKey(alias, password);
    X509Certificate cert = (X509Certificate) ks.getCertificate(alias);
    if (key == null || cert == null) throw new IllegalStateException("alias " + alias + " not in " + keystore);

    byte[] aligned = write(read(Files.readAllBytes(in)));
    int eocd = findEocd(aligned);
    int cdOff = (int) u32(aligned, eocd + 16);

    byte[] digest = v2Digest(aligned, cdOff, cdOff, eocd, cdOff);
    byte[] block = signingBlock(digest, key, cert);

    ByteArrayOutputStream o = new ByteArrayOutputStream(aligned.length + block.length);
    o.write(aligned, 0, cdOff);
    o.write(block);
    o.write(aligned, cdOff, eocd - cdOff);
    byte[] tail = Arrays.copyOfRange(aligned, eocd, aligned.length);
    putLe32(tail, 16, cdOff + block.length);
    o.write(tail);
    byte[] apk = o.toByteArray();

    verify(apk);
    Files.write(out, apk);
    System.out.println("[apk] signed " + out + " (" + apk.length + " bytes), v2 block " + block.length + " bytes");
  }

  /** APK Signature Scheme v2: 1 MiB chunks of contents, central directory and EOCD. */
  static byte[] v2Digest(byte[] f, int contentsEnd, int cdStart, int eocd, int cdOffsetInEocd)
      throws Exception {
    byte[] eocdCopy = Arrays.copyOfRange(f, eocd, f.length);
    putLe32(eocdCopy, 16, cdOffsetInEocd);
    MessageDigest md = MessageDigest.getInstance("SHA-256");
    ByteArrayOutputStream chunkDigests = new ByteArrayOutputStream();
    int chunks = 0;
    for (int[] section : new int[][] {{0, 0, contentsEnd}, {0, cdStart, eocd}, {1, 0, eocdCopy.length}}) {
      byte[] src = section[0] == 0 ? f : eocdCopy;
      for (int p = section[1]; p < section[2]; p += CHUNK) {
        int len = Math.min(CHUNK, section[2] - p);
        md.reset();
        md.update((byte) 0xa5);
        md.update(le32(len));
        md.update(src, p, len);
        chunkDigests.write(md.digest());
        chunks++;
      }
    }
    md.reset();
    md.update((byte) 0x5a);
    md.update(le32(chunks));
    md.update(chunkDigests.toByteArray());
    return md.digest();
  }

  static byte[] signingBlock(byte[] digest, PrivateKey key, X509Certificate cert) throws Exception {
    byte[] digests = lp(lp(concat(le32(RSA_PKCS1_SHA256), lp(digest))));
    byte[] certificates = lp(lp(cert.getEncoded()));
    byte[] attributes = lp(new byte[0]);
    byte[] signedData = concat(digests, certificates, attributes);

    Signature s = Signature.getInstance("SHA256withRSA");
    s.initSign(key);
    s.update(signedData);
    byte[] signatures = lp(lp(concat(le32(RSA_PKCS1_SHA256), lp(s.sign()))));

    byte[] signer = concat(lp(signedData), signatures, lp(cert.getPublicKey().getEncoded()));
    byte[] value = lp(lp(signer));
    byte[] pair = concat(le64(4 + value.length), le32(V2_ID), value);
    long size = pair.length + 8 + 16;
    return concat(le64(size), pair, le64(size), "APK Sig Block 42".getBytes(StandardCharsets.US_ASCII));
  }

  // ---------------------------------------------------------------- verification

  static void verify(byte[] apk) throws Exception {
    int eocd = findEocd(apk);
    int cdOff = (int) u32(apk, eocd + 16);
    String magic = new String(apk, cdOff - 16, 16, StandardCharsets.US_ASCII);
    if (!magic.equals("APK Sig Block 42")) throw new IllegalStateException("no signing block before CD");
    long size = u64(apk, cdOff - 24);
    int blockStart = (int) (cdOff - size - 8);
    if (u64(apk, blockStart) != size) throw new IllegalStateException("signing block sizes disagree");

    byte[] v2 = null;
    for (int p = blockStart + 8; p < cdOff - 24; ) {
      long len = u64(apk, p);
      int id = (int) u32(apk, p + 8);
      if (id == V2_ID) v2 = Arrays.copyOfRange(apk, p + 12, (int) (p + 8 + len));
      p += 8 + (int) len;
    }
    if (v2 == null) throw new IllegalStateException("no v2 signature in block");

    int[] cur = {0};
    byte[] signers = take(v2, cur);
    int[] sc = {0};
    byte[] signer = take(signers, sc);
    int[] c = {0};
    byte[] signedData = take(signer, c);
    byte[] signatures = take(signer, c);
    byte[] publicKeyDer = take(signer, c);

    int[] d = {0};
    byte[] digests = take(signedData, d);
    byte[] certs = take(signedData, d);
    int[] dd = {0};
    byte[] firstDigest = take(digests, dd);
    int algo = (int) u32(firstDigest, 0);
    int[] fd = {4};
    byte[] digest = take(firstDigest, fd);
    int[] cc = {0};
    X509Certificate cert =
        (X509Certificate)
            CertificateFactory.getInstance("X.509")
                .generateCertificate(new ByteArrayInputStream(take(certs, cc)));
    if (!Arrays.equals(cert.getPublicKey().getEncoded(), publicKeyDer)) {
      throw new IllegalStateException("certificate key differs from signer key");
    }

    int[] ss = {0};
    byte[] firstSignature = take(signatures, ss);
    if ((int) u32(firstSignature, 0) != algo || algo != RSA_PKCS1_SHA256) {
      throw new IllegalStateException("unexpected signature algorithm " + Integer.toHexString(algo));
    }
    int[] fs = {4};
    byte[] sigBytes = take(firstSignature, fs);
    PublicKey pub = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(publicKeyDer));
    Signature s = Signature.getInstance("SHA256withRSA");
    s.initVerify(pub);
    s.update(signedData);
    if (!s.verify(sigBytes)) throw new IllegalStateException("v2 signature does not verify");

    byte[] expected = v2Digest(apk, blockStart, cdOff, eocd, blockStart);
    if (!Arrays.equals(expected, digest)) throw new IllegalStateException("v2 digest mismatch");

    int misaligned = 0;
    for (Entry e : read(apk)) {
      if (e.method() == 0 && e.dataOff() % ALIGN != 0) misaligned++;
    }
    if (misaligned > 0) throw new IllegalStateException(misaligned + " stored entries misaligned");

    for (Entry e : read(apk)) {
      if (!e.name().equals("classes.dex")) continue;
      byte[] dex = content(e);
      byte[] check = dex.clone();
      fixDexHeader(check);
      if (!Arrays.equals(check, dex)) throw new IllegalStateException("classes.dex header checksum stale");
      if (indexOf(dex, RETAIL_GAME_KEY.getBytes(StandardCharsets.US_ASCII), 0) >= 0
          || indexOf(dex, RETAIL_CHAT_KEY.getBytes(StandardCharsets.US_ASCII), 0) >= 0) {
        throw new IllegalStateException("retail server key still present in classes.dex");
      }
    }
    System.out.println("[apk] verified: v2 signature + digest, alignment, dex header, retail keys gone");
  }

  static byte[] take(byte[] buf, int[] cursor) {
    int len = (int) u32(buf, cursor[0]);
    byte[] out = Arrays.copyOfRange(buf, cursor[0] + 4, cursor[0] + 4 + len);
    cursor[0] += 4 + len;
    return out;
  }

  // ---------------------------------------------------------------- zip

  static List<Entry> read(byte[] f) {
    int eocd = findEocd(f);
    int count = u16(f, eocd + 10);
    int p = (int) u32(f, eocd + 16);
    List<Entry> out = new ArrayList<>(count);
    for (int i = 0; i < count; i++) {
      if (u32(f, p) != 0x02014b50L) throw new IllegalStateException("bad central directory at " + p);
      int method = u16(f, p + 10);
      long crc = u32(f, p + 16), csize = u32(f, p + 20), usize = u32(f, p + 24);
      int nl = u16(f, p + 28), el = u16(f, p + 30), cl = u16(f, p + 32);
      long localOff = u32(f, p + 42);
      if (csize == 0xFFFFFFFFL || usize == 0xFFFFFFFFL || localOff == 0xFFFFFFFFL) {
        throw new IllegalStateException("zip64 entries are not supported");
      }
      String name = new String(f, p + 46, nl, StandardCharsets.UTF_8);
      int lo = (int) localOff;
      if (u32(f, lo) != 0x04034b50L) throw new IllegalStateException("bad local header for " + name);
      int dataOff = lo + 30 + u16(f, lo + 26) + u16(f, lo + 28);
      out.add(new Entry(name, method, crc, csize, usize, f, dataOff));
      p += 46 + nl + el + cl;
    }
    return out;
  }

  static int findEocd(byte[] f) {
    for (int i = f.length - 22; i >= Math.max(0, f.length - 22 - 65535); i--) {
      if (u32(f, i) == 0x06054b50L) return i;
    }
    throw new IllegalStateException("no end of central directory");
  }

  /** Raw-copies entries with fresh headers: no data descriptors, stored data 4-byte aligned. */
  static byte[] write(List<Entry> entries) throws IOException {
    long total = 0;
    for (Entry e : entries) total += e.csize() + 128 + e.name().length() * 2L;
    ByteArrayOutputStream out = new ByteArrayOutputStream((int) Math.min(Integer.MAX_VALUE - 8, total + 65536));
    int[] offsets = new int[entries.size()];
    for (int i = 0; i < entries.size(); i++) {
      Entry e = entries.get(i);
      byte[] name = e.name().getBytes(StandardCharsets.UTF_8);
      int off = out.size();
      offsets[i] = off;
      int extra = 0;
      if (e.method() == 0) {
        int base = off + 30 + name.length;
        extra = 6;
        int rem = (base + extra) % ALIGN;
        if (rem != 0) extra += ALIGN - rem;
      }
      out.write(le32(0x04034b50));
      out.write(le16(e.method() == 0 ? 10 : 20));
      out.write(le16(0));
      out.write(le16(e.method()));
      out.write(le16(DOS_TIME));
      out.write(le16(DOS_DATE));
      out.write(le32(e.crc()));
      out.write(le32(e.csize()));
      out.write(le32(e.usize()));
      out.write(le16(name.length));
      out.write(le16(extra));
      out.write(name);
      if (extra > 0) {
        // zipalign's own alignment extra field (0xd935): header, then the alignment, then padding.
        out.write(le16(0xd935));
        out.write(le16(extra - 4));
        out.write(le16(ALIGN));
        out.write(new byte[extra - 6]);
      }
      out.write(e.data(), e.dataOff(), (int) e.csize());
    }
    int cdStart = out.size();
    for (int i = 0; i < entries.size(); i++) {
      Entry e = entries.get(i);
      byte[] name = e.name().getBytes(StandardCharsets.UTF_8);
      out.write(le32(0x02014b50));
      out.write(le16(20));
      out.write(le16(e.method() == 0 ? 10 : 20));
      out.write(le16(0));
      out.write(le16(e.method()));
      out.write(le16(DOS_TIME));
      out.write(le16(DOS_DATE));
      out.write(le32(e.crc()));
      out.write(le32(e.csize()));
      out.write(le32(e.usize()));
      out.write(le16(name.length));
      out.write(le16(0));
      out.write(le16(0));
      out.write(le16(0));
      out.write(le16(0));
      out.write(le32(0));
      out.write(le32(offsets[i]));
      out.write(name);
    }
    int cdSize = out.size() - cdStart;
    out.write(le32(0x06054b50));
    out.write(le16(0));
    out.write(le16(0));
    out.write(le16(entries.size()));
    out.write(le16(entries.size()));
    out.write(le32(cdSize));
    out.write(le32(cdStart));
    out.write(le16(0));
    return out.toByteArray();
  }

  /**
   * The name of the mod's mobile theme, taken from its own info.xml, so the config can select it as
   * the client's theme. Returns "-" when the mod ships no mobile theme.
   */
  static String mobileThemeName(byte[] modZip) throws Exception {
    for (Entry e : read(modZip)) {
      if (!e.name().equals("info.xml")) continue;
      String xml = new String(content(e), StandardCharsets.UTF_8);
      Matcher m = Pattern.compile("<theme\\s+[^>]*>", Pattern.DOTALL).matcher(xml);
      while (m.find()) {
        String tag = m.group();
        if (!tag.contains("is_mobile=\"true\"")) continue;
        Matcher n = Pattern.compile("name=\"([^\"]+)\"").matcher(tag);
        if (n.find()) return n.group(1);
      }
    }
    return "-";
  }

  static boolean overlayHas(String overlayDir, String rel) {
    return !overlayDir.equals("-") && Files.exists(Path.of(overlayDir).resolve(rel));
  }

  /**
   * Where the adaptive icon's two layers live inside resources.arsc, found by walking the table
   * properly: the global string pool, the package's type and key pools, then every type chunk with
   * its REAL header size (it carries a variable-length config struct). Byte offsets are absolute in
   * the arsc so the caller can patch in place.
   */
  static final class AdaptiveIcon {
    String foregroundPath; int foregroundPathAt;   // the path's characters, patched .xml -> .png
    int backgroundValue, backgroundValueAt;         // the colour's Res_value data word

    static AdaptiveIcon locate(byte[] arsc) {
      ByteBuffer b = ByteBuffer.wrap(arsc).order(ByteOrder.LITTLE_ENDIAN);
      AdaptiveIcon out = new AdaptiveIcon();
      int poolStart = 12;                                              // after ResTable_header
      int poolCount = b.getInt(poolStart + 8), poolFlags = b.getInt(poolStart + 16),
          poolStrings = b.getInt(poolStart + 20);
      boolean utf8 = (poolFlags & 0x100) != 0;
      int pkg = poolStart + b.getInt(poolStart + 4);
      int typeStrings = pkg + b.getInt(pkg + 268), keyStrings = pkg + b.getInt(pkg + 276);
      String[] keys = poolStrings(b, keyStrings);
      int p = keyStrings + b.getInt(keyStrings + 4);
      while (p + 8 <= arsc.length) {
        int type = b.getShort(p) & 0xFFFF, headerSize = b.getShort(p + 2) & 0xFFFF, size = b.getInt(p + 4);
        if (size <= 0) break;
        if (type == 0x0201) {
          int count = b.getInt(p + 12), entriesStart = b.getInt(p + 16);
          for (int i = 0; i < count; i++) {
            int off = b.getInt(p + headerSize + i * 4);
            if (off == -1) continue;
            int e = p + entriesStart + off;
            if ((b.getShort(e + 2) & 1) != 0) continue;                // complex entries: not ours
            String key = keys[b.getInt(e + 4)];
            int dataType = b.get(e + 11) & 0xFF, data = b.getInt(e + 12);
            if (key.equals(ICON_FOREGROUND_NAME) && dataType == 0x03) {
              if (!utf8) throw new IllegalStateException("UTF-16 resource pool: extend the patch");
              if (out.foregroundPath != null) throw new IllegalStateException("two foreground entries; extend the patch to cover both");
              // UTF-8 pool entry: char count (1-2 bytes), byte count (1-2 bytes), then the bytes.
              int s = poolStart + poolStrings + b.getInt(poolStart + 28 + data * 4);
              s += (b.get(s) & 0x80) != 0 ? 2 : 1;
              s += (b.get(s) & 0x80) != 0 ? 2 : 1;
              out.foregroundPath = poolStrings(b, poolStart)[data];
              out.foregroundPathAt = s;
            } else if (key.equals(ICON_BACKGROUND_NAME) && (dataType == 0x1C || dataType == 0x1D)) {
              out.backgroundValue = data;
              out.backgroundValueAt = e + 12;
            }
          }
        }
        p += size;
      }
      if (out.foregroundPath == null || out.backgroundValueAt == 0) {
        throw new IllegalStateException("could not find " + ICON_FOREGROUND_NAME + " / " + ICON_BACKGROUND_NAME);
      }
      return out;
    }

    static String[] poolStrings(ByteBuffer b, int at) {
      int count = b.getInt(at + 8), flags = b.getInt(at + 16), strStart = b.getInt(at + 20);
      boolean utf8 = (flags & 0x100) != 0;
      String[] out = new String[count];
      for (int i = 0; i < count; i++) {
        int off = at + strStart + b.getInt(at + 28 + i * 4);
        if (utf8) {
          int p = off;
          if ((b.get(p) & 0x80) != 0) p += 2; else p += 1;             // char count
          int n = b.get(p) & 0xFF;
          if ((n & 0x80) != 0) { n = ((n & 0x7F) << 8) | (b.get(p + 1) & 0xFF); p += 2; } else p += 1;
          byte[] s = new byte[n];
          for (int k = 0; k < n; k++) s[k] = b.get(p + k);
          out[i] = new String(s, StandardCharsets.UTF_8);
        } else {
          int n = b.getShort(off) & 0xFFFF, p = off + 2;
          if ((n & 0x8000) != 0) { n = ((n & 0x7FFF) << 16) | (b.getShort(p) & 0xFFFF); p += 2; }
          StringBuilder sb = new StringBuilder();
          for (int k = 0; k < n; k++) sb.append((char) b.getShort(p + k * 2));
          out[i] = sb.toString();
        }
      }
      return out;
    }
  }

  static byte[] content(Entry e) throws Exception {
    int len = (int) e.csize();
    if (e.method() == 0) return Arrays.copyOfRange(e.data(), e.dataOff(), e.dataOff() + len);
    if (e.method() != 8) throw new IllegalStateException("unsupported method " + e.method() + " for " + e.name());
    byte[] input = Arrays.copyOfRange(e.data(), e.dataOff(), e.dataOff() + len + 1);
    input[len] = 0;
    Inflater inflater = new Inflater(true);
    inflater.setInput(input);
    byte[] out = new byte[(int) e.usize()];
    int n = 0;
    while (n < out.length) {
      int r = inflater.inflate(out, n, out.length - n);
      if (r == 0 && (inflater.finished() || inflater.needsInput())) break;
      n += r;
    }
    inflater.end();
    if (n != out.length) throw new IllegalStateException("short inflate for " + e.name());
    CRC32 crc = new CRC32();
    crc.update(out);
    if (crc.getValue() != e.crc()) throw new IllegalStateException("crc mismatch for " + e.name());
    return out;
  }

  static Entry deflated(String name, byte[] content) {
    Deflater deflater = new Deflater(9, true);
    deflater.setInput(content);
    deflater.finish();
    ByteArrayOutputStream b = new ByteArrayOutputStream(content.length / 2 + 64);
    byte[] buf = new byte[1 << 16];
    while (!deflater.finished()) b.write(buf, 0, deflater.deflate(buf));
    deflater.end();
    byte[] comp = b.toByteArray();
    return new Entry(name, 8, crc(content), comp.length, content.length, comp, 0);
  }

  static Entry stored(String name, byte[] content) {
    return new Entry(name, 0, crc(content), content.length, content.length, content, 0);
  }

  static long crc(byte[] b) {
    CRC32 c = new CRC32();
    c.update(b);
    return c.getValue();
  }

  // ---------------------------------------------------------------- bytes

  static int indexOf(byte[] hay, byte[] needle, int from) {
    outer:
    for (int i = from; i + needle.length <= hay.length; i++) {
      for (int j = 0; j < needle.length; j++) if (hay[i + j] != needle[j]) continue outer;
      return i;
    }
    return -1;
  }

  static int u16(byte[] b, int o) {
    return (b[o] & 0xFF) | (b[o + 1] & 0xFF) << 8;
  }

  static long u32(byte[] b, int o) {
    return (u16(b, o) | (long) u16(b, o + 2) << 16) & 0xFFFFFFFFL;
  }

  static long u64(byte[] b, int o) {
    return u32(b, o) | u32(b, o + 4) << 32;
  }

  static void putLe32(byte[] b, int o, long v) {
    for (int i = 0; i < 4; i++) b[o + i] = (byte) (v >>> (8 * i));
  }

  static byte[] le16(int v) {
    return new byte[] {(byte) v, (byte) (v >>> 8)};
  }

  static byte[] le32(long v) {
    byte[] b = new byte[4];
    putLe32(b, 0, v);
    return b;
  }

  static byte[] le64(long v) {
    byte[] b = new byte[8];
    for (int i = 0; i < 8; i++) b[i] = (byte) (v >>> (8 * i));
    return b;
  }

  static byte[] lp(byte[] body) {
    return concat(le32(body.length), body);
  }

  static byte[] concat(byte[]... parts) {
    int n = 0;
    for (byte[] p : parts) n += p.length;
    byte[] out = new byte[n];
    int o = 0;
    for (byte[] p : parts) {
      System.arraycopy(p, 0, out, o, p.length);
      o += p.length;
    }
    return out;
  }
}
