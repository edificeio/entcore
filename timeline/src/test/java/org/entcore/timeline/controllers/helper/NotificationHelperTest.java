package org.entcore.timeline.controllers.helper;

import io.vertx.core.json.JsonObject;
import org.entcore.common.user.dto.QuietHoursPreference;
import org.entcore.common.user.dto.TimezonePreference;
import org.junit.Test;
import java.time.*;
import static org.junit.Assert.*;

public class NotificationHelperTest {

    private static ZonedDateTime dt(int year, int month, int day, int hour, int minute) {
        return ZonedDateTime.of(year, month, day, hour, minute, 0, 0, ZoneId.of("Europe/Paris"));
    }

    private static TimezonePreference tz(String timezone) {
        TimezonePreference pref = new TimezonePreference();
        pref.setTimezone(timezone);
        return pref;
    }

    // --- isQuietHour(ZonedDateTime, int[][]) ---

    @Test
    public void testIsQuietHour_HourInSchedule_Quiet() {
        // Monday 10:00 is in schedule -> quiet
        int[][] schedule = new int[7][];
        schedule[0] = new int[]{10, 11, 12};
        for (int i = 1; i < 7; i++) schedule[i] = new int[]{};
        assertTrue(NotificationHelper.isQuietHour(dt(2026, 3, 30, 10, 0), schedule));
    }

    @Test
    public void testIsQuietHour_HourNotInSchedule_NotQuiet() {
        // Monday 09:00 not in schedule -> not quiet
        int[][] schedule = new int[7][];
        schedule[0] = new int[]{10, 11, 12};
        for (int i = 1; i < 7; i++) schedule[i] = new int[]{};
        assertFalse(NotificationHelper.isQuietHour(dt(2026, 3, 30, 9, 0), schedule));
    }

    @Test
    public void testIsQuietHour_EmptyDayArray_NotQuiet() {
        // Wednesday has empty array -> not quiet
        int[][] schedule = new int[7][];
        for (int i = 0; i < 7; i++) schedule[i] = new int[]{};
        assertFalse(NotificationHelper.isQuietHour(dt(2026, 4, 1, 10, 0), schedule));
    }

    @Test
    public void testIsQuietHour_NullSchedule_NotQuiet() {
        // null schedule -> not quiet (no preference = no filtering)
        assertFalse(NotificationHelper.isQuietHour(dt(2026, 4, 1, 3, 0), null));
    }

    @Test
    public void testIsQuietHour_TooShortSchedule_NotQuiet() {
        // schedule has 3 days, Sunday (dayIndex=6) out of bounds -> not quiet
        int[][] schedule = new int[3][];
        for (int i = 0; i < 3; i++) schedule[i] = new int[]{};
        assertFalse(NotificationHelper.isQuietHour(dt(2026, 3, 29, 12, 0), schedule));
    }

    // --- getTimezoneFromUai tests ---

    @Test
    public void testGetTz_Null() {
        // null UAI (foreign structure) -> no timezone -> quiet hours do not apply
        assertNull(NotificationHelper.getTimezoneFromUai(null));
    }

    @Test
    public void testGetTz_TooShort() {
        assertEquals(ZoneId.of("Europe/Paris"), NotificationHelper.getTimezoneFromUai("07"));
    }

    @Test
    public void testGetTz_CorseDuSud() {
        // Corse-du-Sud UAIs start with 620 -> default -> Europe/Paris
        assertEquals(ZoneId.of("Europe/Paris"), NotificationHelper.getTimezoneFromUai("6200060W"));
    }

    @Test
    public void testGetTz_HauteCorse() {
        // Haute-Corse UAIs start with 720 -> default -> Europe/Paris
        assertEquals(ZoneId.of("Europe/Paris"), NotificationHelper.getTimezoneFromUai("7200062E"));
    }

    @Test
    public void testGetTz_Metropole() {
        assertEquals(ZoneId.of("Europe/Paris"), NotificationHelper.getTimezoneFromUai("0750010E"));
    }

    @Test
    public void testGetTz_Guadeloupe() {
        assertEquals(ZoneId.of("America/Guadeloupe"), NotificationHelper.getTimezoneFromUai("9710001A"));
    }

    @Test
    public void testGetTz_Martinique() {
        assertEquals(ZoneId.of("America/Martinique"), NotificationHelper.getTimezoneFromUai("9720001A"));
    }

    @Test
    public void testGetTz_Guyane() {
        assertEquals(ZoneId.of("America/Cayenne"), NotificationHelper.getTimezoneFromUai("9730001A"));
    }

    @Test
    public void testGetTz_Reunion() {
        assertEquals(ZoneId.of("Indian/Reunion"), NotificationHelper.getTimezoneFromUai("9740001A"));
    }

    @Test
    public void testGetTz_Miquelon() {
        assertEquals(ZoneId.of("America/Miquelon"), NotificationHelper.getTimezoneFromUai("9750001A"));
    }

    @Test
    public void testGetTz_Mayotte() {
        assertEquals(ZoneId.of("Indian/Mayotte"), NotificationHelper.getTimezoneFromUai("9760001A"));
    }

    @Test
    public void testGetTz_SaintBarthelemy() {
        assertEquals(ZoneId.of("America/St_Barthelemy"), NotificationHelper.getTimezoneFromUai("9770001A"));
    }

    @Test
    public void testGetTz_SaintMartin() {
        assertEquals(ZoneId.of("America/Marigot"), NotificationHelper.getTimezoneFromUai("9780001A"));
    }

    @Test
    public void testGetTz_WallisFutuna() {
        assertEquals(ZoneId.of("Pacific/Wallis"), NotificationHelper.getTimezoneFromUai("9860001A"));
    }

    @Test
    public void testGetTz_Polynesie() {
        assertEquals(ZoneId.of("Pacific/Tahiti"), NotificationHelper.getTimezoneFromUai("9870001A"));
    }

    @Test
    public void testGetTz_NouvelleCaledonie() {
        assertEquals(ZoneId.of("Pacific/Noumea"), NotificationHelper.getTimezoneFromUai("9880001A"));
    }

    @Test
    public void testGetTz_NonNumeric() {
        // Non-numeric UAI should fall back to Europe/Paris via NumberFormatException catch
        assertEquals(ZoneId.of("Europe/Paris"), NotificationHelper.getTimezoneFromUai("ABCDEFG"));
    }

    // --- isQuietHour(Instant, QuietHoursPreference, String) ---

    @Test
    public void testIsQuietHour_NoPreference_NoUai_NotQuiet() {
        // no preference, no UAI -> null zone -> not quiet (foreign structure)
        assertFalse(NotificationHelper.isQuietHour(Instant.now(), null, null, null));
    }

    @Test
    public void testIsQuietHour_NoPreference_WithUai_NotQuiet() {
        // no preference -> no schedule -> not quiet regardless of UAI or time
        ZonedDateTime wednesday10h = dt(2026, 4, 1, 10, 0);
        assertFalse(NotificationHelper.isQuietHour(wednesday10h.toInstant(), null, null, "0750010E"));
    }

    @Test
    public void testIsQuietHour_EnabledFalse_NotQuiet() {
        // enabled = false (default) -> not quiet regardless of schedule
        int[][] schedule = new int[7][];
        schedule[0] = new int[]{10};
        for (int i = 1; i < 7; i++) schedule[i] = new int[]{};
        QuietHoursPreference userPrefQuietHours = new QuietHoursPreference();
        userPrefQuietHours.setSchedule(schedule);
        // enabled stays false (primitive default)
        ZonedDateTime monday10h = dt(2026, 3, 30, 10, 0);
        assertFalse(NotificationHelper.isQuietHour(monday10h.toInstant(), userPrefQuietHours, tz("Europe/Paris"), "0750010E"));
    }

    @Test
    public void testIsQuietHour_WithSchedulePreference_UsesSchedule() {
        // enabled = true + schedule with Monday 10h -> quiet
        int[][] schedule = new int[7][];
        schedule[0] = new int[]{10};
        for (int i = 1; i < 7; i++) schedule[i] = new int[]{};
        QuietHoursPreference userPrefQuietHours = new QuietHoursPreference();
        userPrefQuietHours.setEnabled(true);
        userPrefQuietHours.setSchedule(schedule);
        ZonedDateTime monday10h = dt(2026, 3, 30, 10, 0);
        assertTrue(NotificationHelper.isQuietHour(monday10h.toInstant(), userPrefQuietHours, tz("Europe/Paris"), "0750010E"));
    }

    @Test
    public void testIsQuietHour_PreferenceTzOverridesUai() {
        // enabled = true + preference tz (Reunion) overrides Guadeloupe UAI
        // Wednesday 10:00 Paris = 14:00 Reunion -> 14h not in schedule -> not quiet
        int[][] schedule = new int[7][];
        schedule[2] = new int[]{22, 23, 0, 1}; // Wednesday quiet hours (not 14h)
        for (int i = 0; i < 7; i++) if (schedule[i] == null) schedule[i] = new int[]{};
        QuietHoursPreference userPrefQuietHours = new QuietHoursPreference();
        userPrefQuietHours.setEnabled(true);
        userPrefQuietHours.setSchedule(schedule);
        ZonedDateTime wednesday10hParis = dt(2026, 4, 1, 10, 0);
        assertFalse(NotificationHelper.isQuietHour(wednesday10hParis.toInstant(), userPrefQuietHours, tz("Indian/Reunion"), "9710001A"));
    }

    // --- resolveTimezone priority tests ---

    // --- resolveTimezone(TimezonePreference, String) ---

    @Test
    public void testResolveTimezone_PreferenceOverridesUai() {
        // explicit tz in preference wins -> UAI never resolved
        assertEquals(ZoneId.of("Indian/Reunion"), NotificationHelper.resolveTimezone(tz("Indian/Reunion"), "9710001A"));
    }

    @Test
    public void testResolveTimezone_FallsBackToUai_WhenNoPref() {
        // no preference -> resolves UAI
        assertEquals(ZoneId.of("America/Guadeloupe"), NotificationHelper.resolveTimezone(null, "9710001A"));
    }

    @Test
    public void testResolveTimezone_FallsBackToUai_WhenNoTzInPref() {
        // preference without timezone -> resolves UAI
        assertEquals(ZoneId.of("Indian/Reunion"), NotificationHelper.resolveTimezone(new TimezonePreference(), "9740001A"));
    }

    @Test
    public void testResolveTimezone_NullUai_ReturnsNull() {
        // no preference, null UAI (foreign structure) -> null
        assertNull(NotificationHelper.resolveTimezone(null, (String) null));
    }

    @Test
    public void testResolveTimezone_InvalidTzInPref_FallsBackToUai() {
        // invalid tz string in preference -> falls back to UAI resolution
        assertEquals(ZoneId.of("Europe/Paris"), NotificationHelper.resolveTimezone(tz("Invalid/Zone"), "0750010E"));
    }
}
