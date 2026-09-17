// Draws the version line under the login-screen wordmark, in the client's own dialogue font.
//
// The wordmark is themes/<theme>/res/bg.png (area "background-image", drawn by the `logo` theme in
// main-widgets.xml). Each theme ships its own recolour of it, so the badge is drawn onto whichever
// bg.png that theme has, at build time, in that theme's accent colour - the stock yellow for the
// baked theme, the dark theme's cyan for the theme mod. Generated in both places because disabling
// the mod must not take the badge away.
import java.awt.*; import java.awt.image.BufferedImage; import java.io.File; import javax.imageio.ImageIO;
public class MakeTitleBadge {
  // args: <bg.png> <battle.ttf> <text> <#RRGGBB fill> <out> [#from #to]
  // The optional pair recolours the wordmark's accent - the client's own logo, hue-swapped here, so
  // a themed title screen needs no third-party file in the package.
  public static void main(String[] a) throws Exception {
    BufferedImage logo = ImageIO.read(new File(a[0]));
    if (a.length > 6) logo = recolour(logo, Integer.parseInt(a[5].substring(1), 16),
        Integer.parseInt(a[6].substring(1), 16));
    Font font = Font.createFont(Font.TRUETYPE_FONT, new File(a[1])).deriveFont(Font.PLAIN, 34f);
    // (recolour lives below main)
    String text = a[2];
    Color fill = new Color(Integer.parseInt(a[3].substring(1), 16));
    int band = 38, W = logo.getWidth(), H = logo.getHeight() + band;
    BufferedImage out = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g = out.createGraphics();
    g.drawImage(logo, 0, 0, null);
    g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    g.setFont(font);
    FontMetrics fm = g.getFontMetrics();
    int x = (W - fm.stringWidth(text)) / 2, y = logo.getHeight() + fm.getAscent() + (band - fm.getHeight()) / 2 - 18;
    g.setColor(new Color(0x30, 0x30, 0x34));
    for (int dx = -1; dx <= 1; dx++) for (int dy = -1; dy <= 1; dy++) if (dx != 0 || dy != 0) g.drawString(text, x + dx, y + dy);
    g.setColor(fill);
    g.drawString(text, x, y);
    g.dispose();
    ImageIO.write(out, "png", new File(a[4]));
  }

  /**
   * Shifts pixels near the source hue onto the target hue, keeping their brightness and alpha, so
   * antialiased edges come through cleanly instead of banding.
   */
  static BufferedImage recolour(BufferedImage src, int from, int to) {
    float[] f = Color.RGBtoHSB((from >> 16) & 255, (from >> 8) & 255, from & 255, null);
    float[] t = Color.RGBtoHSB((to >> 16) & 255, (to >> 8) & 255, to & 255, null);
    BufferedImage out = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
    for (int y = 0; y < src.getHeight(); y++) for (int x = 0; x < src.getWidth(); x++) {
      int p = src.getRGB(x, y), al = p >>> 24;
      float[] h = Color.RGBtoHSB((p >> 16) & 255, (p >> 8) & 255, p & 255, null);
      double dist = Math.abs(h[0] - f[0]);
      if (dist > 0.5) dist = 1 - dist;                      // hue is a circle
      if (al == 0 || h[1] < 0.15 || dist > 0.08) { out.setRGB(x, y, p); continue; }
      int rgb = Color.HSBtoRGB(t[0], h[1] * (t[1] / Math.max(0.001f, f[1])), h[2]);
      out.setRGB(x, y, (al << 24) | (rgb & 0xFFFFFF));
    }
    return out;
  }
}
