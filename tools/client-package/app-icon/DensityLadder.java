// The adaptive foreground at every density Android expects: 108dp per bucket, art in the 66% safe
// zone, transparent. Plus the legacy launcher mipmaps (48dp per bucket) for Android 7.
import java.awt.*; import java.awt.image.BufferedImage; import java.io.File; import javax.imageio.ImageIO;
public class DensityLadder { public static void main(String[] a) throws Exception {   // <art> <outDir>
  BufferedImage src = ImageIO.read(new File(a[0]));
  int x0 = src.getWidth(), y0 = src.getHeight(), x1 = -1, y1 = -1;
  for (int y = 0; y < src.getHeight(); y++) for (int x = 0; x < src.getWidth(); x++) {
    if ((src.getRGB(x, y) >>> 24) < 12) continue;
    if (x < x0) x0 = x; if (y < y0) y0 = y; if (x > x1) x1 = x; if (y > y1) y1 = y;
  }
  BufferedImage art = src.getSubimage(x0, y0, x1 - x0 + 1, y1 - y0 + 1);
  String[] buckets = {"mdpi", "hdpi", "xhdpi", "xxhdpi", "xxxhdpi"};
  double[] scale = {1, 1.5, 2, 3, 4};
  for (int i = 0; i < buckets.length; i++) {
    write(art, (int) (108 * scale[i]), 0.66, a[1] + "/drawable-" + buckets[i] + "/pokemmo_foreground.png");
    write(art, (int) (48 * scale[i]), 0.92, a[1] + "/mipmap-" + buckets[i] + "/pokemmo.png");
  }
  System.out.println("wrote foreground 108..432 and mipmap 48..192, art " + art.getWidth() + "x" + art.getHeight());
}
static void write(BufferedImage art, int n, double fill, String path) throws Exception {
  new File(path).getParentFile().mkdirs();
  BufferedImage out = new BufferedImage(n, n, BufferedImage.TYPE_INT_ARGB);
  Graphics2D g = out.createGraphics();
  g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
  g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
  double box = n * fill, s = Math.min(box / art.getWidth(), box / art.getHeight());
  int w = (int) Math.round(art.getWidth() * s), h = (int) Math.round(art.getHeight() * s);
  g.drawImage(art, (n - w) / 2, (n - h) / 2, w, h, null); g.dispose();
  ImageIO.write(out, "png", new File(path));
}}
