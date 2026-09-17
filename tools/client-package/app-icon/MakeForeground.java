// Builds the adaptive icon's foreground layer from the supplied artwork.
//
// Android 8+ draws the launcher icon from res/wL.xml (the adaptive icon), not from the legacy
// mipmaps, which is why replacing those alone leaves modern phones showing the retail icon. Its
// foreground ships as a vector we cannot author here, so ApkPackager repoints the resource table at
// this PNG instead (res/df.xml -> res/df.png, the same length, an in-place swap).
//
// The canvas is 108dp: only the middle 72dp is guaranteed visible once a launcher applies its mask,
// and the outer ring can be cropped to a circle, a squircle or a rounded square depending on the
// phone. So the art is fitted into the inner two thirds, which is why this cannot just reuse the
// mipmap sizes.
import java.awt.*; import java.awt.image.BufferedImage; import java.io.File; import javax.imageio.ImageIO;

public class MakeForeground {
  static final int SIZE = Integer.getInteger("monmmo.iconCanvas", 108);   // a no-density bitmap is 1px = 1dp
  static double safe = 0.66;              // the guaranteed-visible middle of an adaptive canvas

  public static void main(String[] a) throws Exception {   // <art.png> <out.png>
    BufferedImage src = ImageIO.read(new File(a[0]));
    int x0 = src.getWidth(), y0 = src.getHeight(), x1 = -1, y1 = -1;
    for (int y = 0; y < src.getHeight(); y++) for (int x = 0; x < src.getWidth(); x++) {
      if ((src.getRGB(x, y) >>> 24) < 12) continue;
      if (x < x0) x0 = x; if (y < y0) y0 = y; if (x > x1) x1 = x; if (y > y1) y1 = y;
    }
    BufferedImage art = src.getSubimage(x0, y0, x1 - x0 + 1, y1 - y0 + 1);

    BufferedImage out = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g = out.createGraphics();
    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
    g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    if (a.length > 2) safe = Double.parseDouble(a[2]);
    if (a.length > 3) {                    // a plate baked in, for the v26 slot: the launcher masks
      g.setColor(new Color(Integer.parseInt(a[3].substring(1), 16)));   // this canvas and would
      g.fillRect(0, 0, SIZE, SIZE);                                     // otherwise show white
    }
    double box = SIZE * safe;
    double s = Math.min(box / art.getWidth(), box / art.getHeight());
    int w = (int) Math.round(art.getWidth() * s), h = (int) Math.round(art.getHeight() * s);
    g.drawImage(art, (SIZE - w) / 2, (SIZE - h) / 2, w, h, null);
    g.dispose();
    ImageIO.write(out, "png", new File(a[1]));
    System.out.printf("adaptive foreground %dx%d, art %dx%d inside the safe zone%n", SIZE, SIZE, w, h);
  }
}
