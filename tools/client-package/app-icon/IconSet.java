// Fits supplied artwork to the client's launcher-icon sizes: trims to content, scales with a margin
// so a circle/squircle mask cannot clip it, and writes one PNG per density.
import java.awt.*; import java.awt.image.BufferedImage; import java.io.File; import javax.imageio.ImageIO;
public class IconSet {
  public static void main(String[] a) throws Exception {          // <art.png> <outDir> [margin%]
    BufferedImage src = ImageIO.read(new File(a[0]));
    double margin = a.length > 2 ? Double.parseDouble(a[2]) : 0.08;
    int x0 = src.getWidth(), y0 = src.getHeight(), x1 = -1, y1 = -1;
    for (int y = 0; y < src.getHeight(); y++) for (int x = 0; x < src.getWidth(); x++) {
      if ((src.getRGB(x, y) >>> 24) < 12) continue;
      if (x < x0) x0 = x; if (y < y0) y0 = y; if (x > x1) x1 = x; if (y > y1) y1 = y;
    }
    BufferedImage art = src.getSubimage(x0, y0, x1 - x0 + 1, y1 - y0 + 1);
    System.out.printf("content %dx%d at %d,%d%n", art.getWidth(), art.getHeight(), x0, y0);
    int[] sizes = {16, 32, 48, 72, 96, 128, 144, 192, 512};
    for (int n : sizes) {
      BufferedImage out = new BufferedImage(n, n, BufferedImage.TYPE_INT_ARGB);
      Graphics2D g = out.createGraphics();
      g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
      g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
      double box = n * (1 - 2 * margin);
      double s = Math.min(box / art.getWidth(), box / art.getHeight());
      int w = (int) Math.round(art.getWidth() * s), h = (int) Math.round(art.getHeight() * s);
      g.drawImage(art, (n - w) / 2, (n - h) / 2, w, h, null);
      g.dispose();
      ImageIO.write(out, "png", new File(a[1] + "/icon-" + n + ".png"));
    }
    System.out.println("wrote the icon at every size the client uses");
  }
}
