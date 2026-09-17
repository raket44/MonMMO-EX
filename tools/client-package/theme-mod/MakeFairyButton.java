// Builds the Fairy (type 19) move button in the Dark Theme's own language: the Psychic button's
// wedge recoloured to Fairy pink, its glyph replaced by the theme's own star.
import java.awt.*; import java.awt.image.BufferedImage; import java.io.File; import javax.imageio.ImageIO;
public class MakeFairyButton {
  static final int SRC_X = 27, SRC_Y = 13, W = 320, H = 130;     // their type14 (Psychic) cell
  static final int OLD_R = 0xFF, OLD_G = 0x72, OLD_B = 0x7A;      // its flat wedge colour
  static final int NEW_R = 0xFF, NEW_G = 0x9F, NEW_B = 0xC7;      // Fairy pink
  static final int GX0 = 43, GY0 = 64, GX1 = 85, GY1 = 109;       // the glyph's box in that cell
  public static void main(String[] a) throws Exception {
    BufferedImage sheet = ImageIO.read(new File(a[0]));
    BufferedImage star = ImageIO.read(new File(a[1]));
    BufferedImage out = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
    for (int y = 0; y < H; y++) for (int x = 0; x < W; x++) {
      int p = sheet.getRGB(SRC_X + x, SRC_Y + y);
      int al = p >>> 24, r = (p >> 16) & 255, g = (p >> 8) & 255, b = p & 255;
      // Scale each channel by the wedge's own shift, so blends to black and to white survive.
      r = Math.min(255, r * NEW_R / OLD_R); g = Math.min(255, g * NEW_G / OLD_G); b = Math.min(255, b * NEW_B / OLD_B);
      out.setRGB(x, y, (al << 24) | (r << 16) | (g << 8) | b);
    }
    Graphics2D gg = out.createGraphics();
    gg.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
    gg.setComposite(AlphaComposite.Src);
    gg.setColor(new Color(NEW_R, NEW_G, NEW_B));
    gg.fillRect(GX0, GY0, GX1 - GX0, GY1 - GY0);                  // wipe the Psychic spiral
    gg.setComposite(AlphaComposite.SrcOver);
    int size = 40, cx = (GX0 + GX1) / 2, cy = (GY0 + GY1) / 2;
    gg.drawImage(star, cx - size / 2, cy - size / 2, size, size, null);
    gg.dispose();
    ImageIO.write(out, "png", new File(a[2]));
    System.out.println("wrote " + a[2] + " " + W + "x" + H);
  }
}
