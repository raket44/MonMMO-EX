import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
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
  static final String MOD_ENTRY = "assets/data/mods/monmmo-lost-knights.zip";
  static final int ALIGN = 4;
  static final int V2_ID = 0x7109871a;
  static final int RSA_PKCS1_SHA256 = 0x0103;
  static final int CHUNK = 1 << 20;
  // 1981-01-01 01:01, the timestamp every retail entry carries.
  static final int DOS_TIME = 0x0820, DOS_DATE = 0x0221;

  record Entry(String name, int method, long crc, long csize, long usize, byte[] data, int dataOff) {}

  public static void main(String[] args) throws Exception {
    if (args.length >= 7 && args[0].equals("prepare")) {
      prepare(Path.of(args[1]), Path.of(args[2]), args[3], args[4], Path.of(args[5]), Path.of(args[6]));
    } else if (args.length >= 6 && args[0].equals("finish")) {
      finish(Path.of(args[1]), Path.of(args[2]), Path.of(args[3]), Path.of(args[4]), args[5]);
    } else {
      System.err.println("usage: prepare <retail.apk> <unsigned.apk> <host> <mod.zip|-> <game.pem> <chat.pem>");
      System.err.println("       finish <v1-signed.apk> <out.apk> <keystore.p12> <password-file> <alias>");
      System.exit(2);
    }
  }

  // ---------------------------------------------------------------- prepare

  static void prepare(Path in, Path out, String host, String mod, Path gamePem, Path chatPem)
      throws Exception {
    byte[] file = Files.readAllBytes(in);
    Map<String, String> keys = new LinkedHashMap<>();
    keys.put(RETAIL_GAME_KEY, pemBody(gamePem));
    keys.put(RETAIL_CHAT_KEY, pemBody(chatPem));

    List<Entry> result = new ArrayList<>();
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
      if (n.equals(MOD_ENTRY)) continue;
      if (n.equals("classes.dex")) {
        byte[] dex = content(e);
        patchDex(dex, keys);
        result.add(deflated(n, dex));
        dexPatched = true;
        continue;
      }
      result.add(e);
    }
    if (!dexPatched) throw new IllegalStateException("no classes.dex in " + in);

    String props =
        "# MonMMO-EX: points the client at our server instead of the official one.\n"
            + "force.ls.host=" + host + "\n"
            + "force.ls.port=2106\n"
            + "force.gs.host=" + host + "\n"
            + "force.gs.port=7777\n"
            + "client.misc.ignore_feed=true\n"
            + "client.misc.testserver_feed_signature=false\n"
            + (mod.equals("-") ? "" : "client.mods.enabled_mods=monmmo-lost-knights.zip\n");
    result.add(deflated(CONFIG_ENTRY, props.getBytes(StandardCharsets.ISO_8859_1)));
    if (!mod.equals("-")) result.add(stored(MOD_ENTRY, Files.readAllBytes(Path.of(mod))));

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
