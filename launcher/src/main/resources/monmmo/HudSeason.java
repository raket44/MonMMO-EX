package monmmo;

/**
 * The menu header's day-and-time line, corrected in two places by the overlay's hooks into
 * f/gz.NZ1:
 * - the weekday: the stock header derives it from the in-game clock, which runs four in-game
 *   days per real day, so it drifted one day every six hours; the calendar weekday replaces it;
 * - the season: the stock line never names one; it is appended from the client's own global
 *   season (f/u2.MM0: the server's season packet, else the client's fallback), so the label
 *   agrees with the seasonal map art.
 * Any failure leaves the stock behaviour in place.
 */
public final class HudSeason {
  private static final String[] NAMES = {"Spring", "Summer", "Autumn", "Winter"};

  private HudSeason() {}

  /** Today's weekday in the header's numbering: 0 Sunday .. 6 Saturday. */
  public static int realWeekday() {
    try {
      return java.time.LocalDate.now().getDayOfWeek().getValue() % 7;
    } catch (Throwable t) {
      return 0;
    }
  }

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
