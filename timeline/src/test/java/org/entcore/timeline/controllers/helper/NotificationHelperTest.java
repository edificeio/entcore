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

    // --- computeNextSendTime (guards — point d'entrée) ---

    private static QuietHoursPreference enabledPref(int[]... days) {
        QuietHoursPreference pref = new QuietHoursPreference();
        pref.setEnabled(true);
        if (days.length == 7) pref.setSchedule(days);
        return pref;
    }

    private static int[][] fullSchedule(int... hours) {
        int[][] s = new int[7][];
        for (int i = 0; i < 7; i++) s[i] = hours;
        return s;
    }

    @Test
    public void testComputeNextSendTime_NullPref_ReturnsOriginal() {
        Instant t = dt(2026, 3, 30, 22, 30).toInstant();
        assertEquals(t, NotificationHelper.computeNextSendTime(t, null, (ZoneId) null));
    }

    @Test
    public void testComputeNextSendTime_NotEnabled_ReturnsOriginal() {
        Instant t = dt(2026, 3, 30, 22, 30).toInstant();
        QuietHoursPreference pref = new QuietHoursPreference(); // enabled=false
        pref.setSchedule(fullSchedule(22, 23));
        assertEquals(t, NotificationHelper.computeNextSendTime(t, pref, ZoneId.of("Europe/Paris")));
    }

    @Test
    public void testComputeNextSendTime_NullSchedule_ReturnsOriginal() {
        Instant t = dt(2026, 3, 30, 22, 30).toInstant();
        QuietHoursPreference pref = new QuietHoursPreference();
        pref.setEnabled(true);
        // schedule stays null
        assertEquals(t, NotificationHelper.computeNextSendTime(t, pref, ZoneId.of("Europe/Paris")));
    }

    @Test
    public void testComputeNextSendTime_NullZone_ReturnsOriginal() {
        Instant t = dt(2026, 3, 30, 22, 30).toInstant();
        QuietHoursPreference pref = new QuietHoursPreference();
        pref.setEnabled(true);
        pref.setSchedule(fullSchedule(22, 23));
        assertEquals(t, NotificationHelper.computeNextSendTime(t, pref, (ZoneId) null));
    }

    // --- computeNextSendTime (moteur pur ZonedDateTime, int[][]) ---

    @Test
    public void testComputeNextSendTime_HourNotQuiet_ReturnsOriginal() {
        // Monday 10:37, hour 10 not in schedule -> original instant unchanged (not truncated)
        ZonedDateTime localTime = dt(2026, 3, 30, 10, 37);
        int[][] schedule = new int[7][];
        schedule[0] = new int[]{22, 23}; // Monday: only 22h-23h quiet
        for (int i = 1; i < 7; i++) schedule[i] = new int[]{};
        Instant result = NotificationHelper.computeNextSendTime(localTime, schedule);
        assertEquals(localTime.toInstant(), result);
    }

    @Test
    public void testComputeNextSendTime_HourQuiet_ReturnsNextHour() {
        // Monday 22:37 is quiet, 23h also quiet, next non-quiet = Tuesday 00:00 if not quiet
        ZonedDateTime localTime = dt(2026, 3, 30, 22, 37);
        int[][] schedule = new int[7][];
        schedule[0] = new int[]{22, 23}; // Monday: 22h-23h quiet
        for (int i = 1; i < 7; i++) schedule[i] = new int[]{};
        // Expected: Tuesday 00:00 Europe/Paris
        ZonedDateTime expected = dt(2026, 3, 31, 0, 0);
        assertEquals(expected.toInstant(), NotificationHelper.computeNextSendTime(localTime, schedule));
    }

    @Test
    public void testComputeNextSendTime_MidnightSpanning() {
        // Friday 23:30, schedule [22,23,0,1,2] -> next non-quiet hour = 3h Saturday
        ZonedDateTime localTime = dt(2026, 3, 27, 23, 30); // Friday
        int[][] schedule = new int[7][];
        schedule[4] = new int[]{22, 23}; // Friday
        schedule[5] = new int[]{0, 1, 2}; // Saturday
        for (int i = 0; i < 7; i++) if (schedule[i] == null) schedule[i] = new int[]{};
        ZonedDateTime expected = dt(2026, 3, 28, 3, 0); // Saturday 3h
        assertEquals(expected.toInstant(), NotificationHelper.computeNextSendTime(localTime, schedule));
    }

    @Test
    public void testComputeNextSendTime_FullDayQuiet_SkipsToNextDay() {
        // Monday entirely quiet -> first non-quiet hour is Tuesday 00:00
        ZonedDateTime localTime = dt(2026, 3, 30, 14, 0);
        int[][] schedule = new int[7][];
        schedule[0] = new int[]{0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23}; // Monday full
        for (int i = 1; i < 7; i++) schedule[i] = new int[]{};
        ZonedDateTime expected = dt(2026, 3, 31, 0, 0); // Tuesday 00:00
        assertEquals(expected.toInstant(), NotificationHelper.computeNextSendTime(localTime, schedule));
    }

    @Test
    public void testComputeNextSendTime_AllQuiet168Slots_ReturnsNull() {
        // Every hour of every day is quiet -> no slot found -> null
        ZonedDateTime localTime = dt(2026, 3, 30, 10, 0);
        int[] allHours = new int[]{0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23};
        int[][] schedule = new int[7][];
        for (int i = 0; i < 7; i++) schedule[i] = allHours;
        assertNull(NotificationHelper.computeNextSendTime(localTime, schedule));
    }

    @Test
    public void testComputeNextSendTime_ExactlyAtQuietBoundary_NotQuiet() {
        // Monday 08:00 exactly, hour 8 not in schedule -> original instant unchanged
        ZonedDateTime localTime = dt(2026, 3, 30, 8, 0);
        int[][] schedule = new int[7][];
        schedule[0] = new int[]{0, 1, 2, 3, 4, 5, 6, 7}; // 0h-7h quiet, 8h not
        for (int i = 1; i < 7; i++) schedule[i] = new int[]{};
        assertEquals(localTime.toInstant(), NotificationHelper.computeNextSendTime(localTime, schedule));
    }

    @Test
    public void testComputeNextSendTime_ExactlyAtQuietHour_Reports() {
        // Monday 07:59 is in quiet hour 7 -> report to 8h
        ZonedDateTime localTime = dt(2026, 3, 30, 7, 59);
        int[][] schedule = new int[7][];
        schedule[0] = new int[]{0, 1, 2, 3, 4, 5, 6, 7}; // 0h-7h quiet
        for (int i = 1; i < 7; i++) schedule[i] = new int[]{};
        ZonedDateTime expected = dt(2026, 3, 30, 8, 0);
        assertEquals(expected.toInstant(), NotificationHelper.computeNextSendTime(localTime, schedule));
    }

    @Test
    public void testComputeNextSendTime_NullDayInSchedule_TreatedAsNotQuiet() {
        // schedule[0] = null for Monday -> not quiet, return original
        ZonedDateTime localTime = dt(2026, 3, 30, 10, 0);
        int[][] schedule = new int[7][];
        schedule[0] = null; // null treated as not quiet
        for (int i = 1; i < 7; i++) schedule[i] = new int[]{};
        assertEquals(localTime.toInstant(), NotificationHelper.computeNextSendTime(localTime, schedule));
    }

    @Test
    public void testComputeNextSendTime_DST_SpringForward() {
        // Europe/Paris: clocks go forward at 2h->3h on last Sunday of March
        // Notif at 01:30 local, quiet hours [1,2], next non-quiet = 3h (but 2h doesn't exist that night)
        // ZonedDateTime.plusHours(1) from 1h30 gives 3h30 (2h skipped by DST)
        // schedule[6] = [1, 2] (Sunday) -> hour 1 is quiet, hour 2 doesn't exist (DST), cursor lands on 3h
        ZonedDateTime localTime = ZonedDateTime.of(2026, 3, 29, 1, 30, 0, 0, ZoneId.of("Europe/Paris")); // Sunday
        int[][] schedule = new int[7][];
        schedule[6] = new int[]{1, 2}; // Sunday: quiet 1h and 2h
        for (int i = 0; i < 6; i++) schedule[i] = new int[]{};
        // After DST, 2h doesn't exist: plusHours(1) from truncated 1h gives 3h directly
        ZonedDateTime expected = ZonedDateTime.of(2026, 3, 29, 3, 0, 0, 0, ZoneId.of("Europe/Paris"));
        assertEquals(expected.toInstant(), NotificationHelper.computeNextSendTime(localTime, schedule));
    }
}
