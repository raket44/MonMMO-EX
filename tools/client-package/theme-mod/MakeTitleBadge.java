// Draws the version line under the login-screen wordmark, in the client's own dialogue font.
//
// The wordmark is themes/<theme>/res/bg.png (area "background-image", drawn by the `logo` theme in
// main-widgets.xml). Each theme ships its own recolour of it, so the badge is drawn onto whichever
// bg.png that theme has, at build time, in that theme's accent colour - the stock yellow for the
// baked theme, the dark theme's cyan for the theme mod. Generated in both places because disabling
// the mod must not take the badge away.
import java.awt.*; import java.awt.image.BufferedImage; import java.io.File; import javax.imageio.ImageIO;
public class MakeTitleBadge {
  // args: <bg.png> <battle.ttf> <text> <#RRGGBB fill> <out>
  public static void main(String[] a) throws Exception {
    BufferedImage logo = ImageIO.read(new File(a[0]));
    Font font = Font.createFont(Font.TRUETYPE_FONT, new File(a[1])).deriveFont(Font.PLAIN, 30f);
    String text = a[2];
    Color fill = new Color(Integer.parseInt(a[3].substring(1), 16));
    int band = 46, W = logo.getWidth(), H = logo.getHeight() + band;
    BufferedImage out = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g = out.createGraphics();
    g.drawImage(logo, 0, 0, null);
    g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    g.setFont(font);
    FontMetrics fm = g.getFontMetrics();
    int x = (W - fm.stringWidth(text)) / 2, y = logo.getHeight() + fm.getAscent() + (band - fm.getHeight()) / 2 - 8;
    g.setColor(new Color(0x30, 0x30, 0x34));
    for (int dx = -1; dx <= 1; dx++) for (int dy = -1; dy <= 1; dy++) if (dx != 0 || dy != 0) g.drawString(text, x + dx, y + dy);
    g.setColor(fill);
    g.drawString(text, x, y);
    g.dispose();
    ImageIO.write(out, "png", new File(a[4]));
  }
}
