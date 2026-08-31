package monmmo;

import java.io.FileWriter;
import java.io.PrintWriter;

/**
 * File-backed diagnostic sink for the LoadMap apply patch. The client runs under javaw, so
 * System.err goes nowhere; this appends to log/monmmo-map.log next to the client's own logs.
 */
public final class MapLog {
  private MapLog() {}

  public static synchronized void log(String line) {
    try (PrintWriter out = new PrintWriter(new FileWriter("log/monmmo-map.log", true))) {
      out.println(System.currentTimeMillis() + " " + line);
    } catch (Exception ignored) {
    }
  }

  public static synchronized void fail(Throwable t) {
    try (PrintWriter out = new PrintWriter(new FileWriter("log/monmmo-map.log", true))) {
      out.println(System.currentTimeMillis() + " LoadMap apply THREW:");
      t.printStackTrace(out);
    } catch (Exception ignored) {
    }
  }
}
