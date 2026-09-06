package monmmo;

/**
 * Appends the Gen 5 season to the menu header's day-and-time line ("Sunday, 03:37, Winter").
 * Reached from a one-instruction hook the overlay adds after the header formats that line
 * (f/gz.NZ1, string 1155). The season is the client's own global one (f/u2.MM0: the value the
 * server's season packet set, else the client's month-based fallback), so the label agrees with
 * the seasonal map art. Any failure leaves the stock line untouched.
 */
public final class HudSeason {
  private static final String[] NAMES = {"Spring", "Summer", "Autumn", "Winter"};

  private HudSeason() {}

  public static String withSeason(String dayAndTime) {
    try {
      int season = f.u2.r02.MM0();
      if (season < 0 || season >= NAMES.length) return dayAndTime;
      return dayAndTime + ", " + NAMES[season];
    } catch (Throwable t) {
      return dayAndTime;
    }
  }
}
