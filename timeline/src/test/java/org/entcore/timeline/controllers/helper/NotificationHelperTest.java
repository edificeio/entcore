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
        assertTrue(QuietHoursHelper.isQuietHour(dt(2026, 3, 30, 10, 0), schedule));
    }

    @Test
    public void testIsQuietHour_HourNotInSchedule_NotQuiet() {
        // Monday 09:00 not in schedule -> not quiet
        int[][] schedule = new int[7][];
        schedule[0] = new int[]{10, 11, 12};
        for (int i = 1; i < 7; i++) schedule[i] = new int[]{};
        assertFalse(QuietHoursHelper.isQuietHour(dt(2026, 3, 30, 9, 0), schedule));
    }

    @Test
    public void testIsQuietHour_EmptyDayArray_NotQuiet() {
        // Wednesday has empty array -> not quiet
        int[][] schedule = new int[7][];
        for (int i = 0; i < 7; i++) schedule[i] = new int[]{};
        assertFalse(QuietHoursHelper.isQuietHour(dt(2026, 4, 1, 10, 0), schedule));
    }

    @Test
    public void testIsQuietHour_NullSchedule_NotQuiet() {
        // null schedule -> not quiet (no preference = no filtering)
        assertFalse(QuietHoursHelper.isQuietHour(dt(2026, 4, 1, 3, 0), null));
    }

    @Test
    public void testIsQuietHour_TooShortSchedule_NotQuiet() {
        // schedule has 3 days, Sunday (dayIndex=6) out of bounds -> not quiet
        int[][] schedule = new int[3][];
        for (int i = 0; i < 3; i++) schedule[i] = new int[]{};
        assertFalse(QuietHoursHelper.isQuietHour(dt(2026, 3, 29, 12, 0), schedule));
    }

    // --- isQuietHour(Instant, QuietHoursPreference, TimezonePreference) : user timezone preference only ---

    @Test
    public void testIsQuietHour_NoPreference_NotQuiet() {
        // no quiet-hours preference -> not quiet
        assertFalse(QuietHoursHelper.isQuietHour(Instant.now(), null, null));
    }

    @Test
    public void testIsQuietHour_NoTimezone_NotQuiet() {
        // quiet hours enabled but no timezone preference -> null zone -> not quiet (quiet hours not applied)
        int[][] schedule = new int[7][];
        schedule[0] = new int[]{10};
        for (int i = 1; i < 7; i++) schedule[i] = new int[]{};
        QuietHoursPreference userPrefQuietHours = new QuietHoursPreference();
        userPrefQuietHours.setEnabled(true);
        userPrefQuietHours.setSchedule(schedule);
        ZonedDateTime monday10h = dt(2026, 3, 30, 10, 0);
        assertFalse(QuietHoursHelper.isQuietHour(monday10h.toInstant(), userPrefQuietHours, null));
    }

    @Test
    public void testIsQuietHour_EnabledFalse_NotQuiet() {
        // enabled = false (default) -> not quiet regardless of schedule
        int[][] schedule = new int[7][];
        schedule[0] = new int[]{10};
        for (int i = 1; i < 7; i++) schedule[i] = new int[]{};
        QuietHoursPreference userPrefQuietHours = new QuietHoursPreference();
        userPrefQuietHours.setSchedule(schedule);
        ZonedDateTime monday10h = dt(2026, 3, 30, 10, 0);
        assertFalse(QuietHoursHelper.isQuietHour(monday10h.toInstant(), userPrefQuietHours, tz("Europe/Paris")));
    }

    @Test
    public void testIsQuietHour_WithSchedulePreference_UsesSchedule() {
        // enabled = true + schedule with Monday 10h + Paris tz -> quiet
        int[][] schedule = new int[7][];
        schedule[0] = new int[]{10};
        for (int i = 1; i < 7; i++) schedule[i] = new int[]{};
        QuietHoursPreference userPrefQuietHours = new QuietHoursPreference();
        userPrefQuietHours.setEnabled(true);
        userPrefQuietHours.setSchedule(schedule);
        ZonedDateTime monday10h = dt(2026, 3, 30, 10, 0);
        assertTrue(QuietHoursHelper.isQuietHour(monday10h.toInstant(), userPrefQuietHours, tz("Europe/Paris")));
    }

    @Test
    public void testIsQuietHour_UsesPreferenceTimezone() {
        // tz preference (Reunion): Wednesday 10:00 Paris = 14:00 Reunion -> 14h not in schedule -> not quiet
        int[][] schedule = new int[7][];
        schedule[2] = new int[]{22, 23, 0, 1}; // Wednesday quiet hours (not 14h)
        for (int i = 0; i < 7; i++) if (schedule[i] == null) schedule[i] = new int[]{};
        QuietHoursPreference userPrefQuietHours = new QuietHoursPreference();
        userPrefQuietHours.setEnabled(true);
        userPrefQuietHours.setSchedule(schedule);
        ZonedDateTime wednesday10hParis = dt(2026, 4, 1, 10, 0);
        assertFalse(QuietHoursHelper.isQuietHour(wednesday10hParis.toInstant(), userPrefQuietHours, tz("Indian/Reunion")));
    }

    // --- resolveTimezone(TimezonePreference) : user preference only, null when absent/invalid ---

    @Test
    public void testResolveTimezone_ValidPreference() {
        assertEquals(ZoneId.of("Indian/Reunion"), QuietHoursHelper.resolveTimezone(tz("Indian/Reunion")));
    }

    @Test
    public void testResolveTimezone_NullPreference_ReturnsNull() {
        assertNull(QuietHoursHelper.resolveTimezone(null));
    }

    @Test
    public void testResolveTimezone_EmptyPreference_ReturnsNull() {
        // preference without timezone -> null (no UAI fallback anymore)
        assertNull(QuietHoursHelper.resolveTimezone(new TimezonePreference()));
    }

    @Test
    public void testResolveTimezone_InvalidTzInPref_ReturnsNull() {
        // invalid tz string -> no fallback -> null
        assertNull(QuietHoursHelper.resolveTimezone(tz("Invalid/Zone")));
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
        assertEquals(t, QuietHoursHelper.computeNextSendTime(t, null, (ZoneId) null));
    }

    @Test
    public void testComputeNextSendTime_NotEnabled_ReturnsOriginal() {
        Instant t = dt(2026, 3, 30, 22, 30).toInstant();
        QuietHoursPreference pref = new QuietHoursPreference(); // enabled=false
        pref.setSchedule(fullSchedule(22, 23));
        assertEquals(t, QuietHoursHelper.computeNextSendTime(t, pref, ZoneId.of("Europe/Paris")));
    }

    @Test
    public void testComputeNextSendTime_NullSchedule_ReturnsOriginal() {
        Instant t = dt(2026, 3, 30, 22, 30).toInstant();
        QuietHoursPreference pref = new QuietHoursPreference();
        pref.setEnabled(true);
        // schedule stays null
        assertEquals(t, QuietHoursHelper.computeNextSendTime(t, pref, ZoneId.of("Europe/Paris")));
    }

    @Test
    public void testComputeNextSendTime_NullZone_ReturnsOriginal() {
        Instant t = dt(2026, 3, 30, 22, 30).toInstant();
        QuietHoursPreference pref = new QuietHoursPreference();
        pref.setEnabled(true);
        pref.setSchedule(fullSchedule(22, 23));
        assertEquals(t, QuietHoursHelper.computeNextSendTime(t, pref, (ZoneId) null));
    }

    // --- computeNextSendTime (moteur pur ZonedDateTime, int[][]) ---

    @Test
    public void testComputeNextSendTime_HourNotQuiet_ReturnsOriginal() {
        // Monday 10:37, hour 10 not in schedule -> original instant unchanged (not truncated)
        ZonedDateTime localTime = dt(2026, 3, 30, 10, 37);
        int[][] schedule = new int[7][];
        schedule[0] = new int[]{22, 23}; // Monday: only 22h-23h quiet
        for (int i = 1; i < 7; i++) schedule[i] = new int[]{};
        Instant result = QuietHoursHelper.computeNextSendTime(localTime, schedule);
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
        assertEquals(expected.toInstant(), QuietHoursHelper.computeNextSendTime(localTime, schedule));
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
        assertEquals(expected.toInstant(), QuietHoursHelper.computeNextSendTime(localTime, schedule));
    }

    @Test
    public void testComputeNextSendTime_FullDayQuiet_SkipsToNextDay() {
        // Monday entirely quiet -> first non-quiet hour is Tuesday 00:00
        ZonedDateTime localTime = dt(2026, 3, 30, 14, 0);
        int[][] schedule = new int[7][];
        schedule[0] = new int[]{0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23}; // Monday full
        for (int i = 1; i < 7; i++) schedule[i] = new int[]{};
        ZonedDateTime expected = dt(2026, 3, 31, 0, 0); // Tuesday 00:00
        assertEquals(expected.toInstant(), QuietHoursHelper.computeNextSendTime(localTime, schedule));
    }

    @Test
    public void testComputeNextSendTime_AllQuiet168Slots_ReturnsNull() {
        // Every hour of every day is quiet -> no slot found -> null
        ZonedDateTime localTime = dt(2026, 3, 30, 10, 0);
        int[] allHours = new int[]{0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23};
        int[][] schedule = new int[7][];
        for (int i = 0; i < 7; i++) schedule[i] = allHours;
        assertNull(QuietHoursHelper.computeNextSendTime(localTime, schedule));
    }

    @Test
    public void testComputeNextSendTime_ExactlyAtQuietBoundary_NotQuiet() {
        // Monday 08:00 exactly, hour 8 not in schedule -> original instant unchanged
        ZonedDateTime localTime = dt(2026, 3, 30, 8, 0);
        int[][] schedule = new int[7][];
        schedule[0] = new int[]{0, 1, 2, 3, 4, 5, 6, 7}; // 0h-7h quiet, 8h not
        for (int i = 1; i < 7; i++) schedule[i] = new int[]{};
        assertEquals(localTime.toInstant(), QuietHoursHelper.computeNextSendTime(localTime, schedule));
    }

    @Test
    public void testComputeNextSendTime_ExactlyAtQuietHour_Reports() {
        // Monday 07:59 is in quiet hour 7 -> report to 8h
        ZonedDateTime localTime = dt(2026, 3, 30, 7, 59);
        int[][] schedule = new int[7][];
        schedule[0] = new int[]{0, 1, 2, 3, 4, 5, 6, 7}; // 0h-7h quiet
        for (int i = 1; i < 7; i++) schedule[i] = new int[]{};
        ZonedDateTime expected = dt(2026, 3, 30, 8, 0);
        assertEquals(expected.toInstant(), QuietHoursHelper.computeNextSendTime(localTime, schedule));
    }

    @Test
    public void testComputeNextSendTime_NullDayInSchedule_TreatedAsNotQuiet() {
        // schedule[0] = null for Monday -> not quiet, return original
        ZonedDateTime localTime = dt(2026, 3, 30, 10, 0);
        int[][] schedule = new int[7][];
        schedule[0] = null; // null treated as not quiet
        for (int i = 1; i < 7; i++) schedule[i] = new int[]{};
        assertEquals(localTime.toInstant(), QuietHoursHelper.computeNextSendTime(localTime, schedule));
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
        assertEquals(expected.toInstant(), QuietHoursHelper.computeNextSendTime(localTime, schedule));
    }

    // --- computeDailyMailScheduleAt (daily mail par timezone) ---
    // Run = Monday 2026-06-01 06:00 Europe/Paris unless stated otherwise.

    private static final ZoneId PARIS = ZoneId.of("Europe/Paris");

    private static Instant mondayRun() {
        return ZonedDateTime.of(2026, 6, 1, 6, 0, 0, 0, PARIS).toInstant();
    }

    @Test
    public void testComputeDailyMailScheduleAt_NoQuietHours_SendsAt7Local() {
        // no quiet hours preference -> mail scheduled at 07:00 local
        Instant expected = ZonedDateTime.of(2026, 6, 1, 7, 0, 0, 0, PARIS).toInstant();
        assertEquals(expected, QuietHoursHelper.computeDailyMailScheduleAt(mondayRun(), null, PARIS));
    }

    @Test
    public void testComputeDailyMailScheduleAt_RunWithJitter_StillSendsAt7Local() {
        // cron fired at 06:00:42 -> truncation keeps the 07:00 local target
        Instant jittered = ZonedDateTime.of(2026, 6, 1, 6, 0, 42, 0, PARIS).toInstant();
        Instant expected = ZonedDateTime.of(2026, 6, 1, 7, 0, 0, 0, PARIS).toInstant();
        assertEquals(expected, QuietHoursHelper.computeDailyMailScheduleAt(jittered, null, PARIS));
    }

    @Test
    public void testComputeDailyMailScheduleAt_QuietUntil7_SendsAt7Local() {
        // default quiet hours 20h-7h: hour 7 is free -> mail at 07:00 local
        QuietHoursPreference pref = enabledPref(fullSchedule(20, 21, 22, 23, 0, 1, 2, 3, 4, 5, 6));
        Instant expected = ZonedDateTime.of(2026, 6, 1, 7, 0, 0, 0, PARIS).toInstant();
        assertEquals(expected, QuietHoursHelper.computeDailyMailScheduleAt(mondayRun(), pref, PARIS));
    }

    @Test
    public void testComputeDailyMailScheduleAt_QuietUntil9_PushedTo9Local() {
        // quiet hours covering 7h and 8h -> mail pushed to 09:00 local
        QuietHoursPreference pref = enabledPref(fullSchedule(0, 1, 2, 3, 4, 5, 6, 7, 8));
        Instant expected = ZonedDateTime.of(2026, 6, 1, 9, 0, 0, 0, PARIS).toInstant();
        assertEquals(expected, QuietHoursHelper.computeDailyMailScheduleAt(mondayRun(), pref, PARIS));
    }

    @Test
    public void testComputeDailyMailScheduleAt_SlotJustBeforeNextRun_Sent() {
        // Monday quiet from 7h, Tuesday quiet 0h-4h -> first free slot Tuesday 05:00, before next run (06:00) -> sent
        int[][] schedule = new int[7][];
        schedule[0] = new int[]{7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23};
        schedule[1] = new int[]{0, 1, 2, 3, 4};
        for (int i = 2; i < 7; i++) schedule[i] = new int[]{};
        Instant expected = ZonedDateTime.of(2026, 6, 2, 5, 0, 0, 0, PARIS).toInstant();
        assertEquals(expected, QuietHoursHelper.computeDailyMailScheduleAt(mondayRun(), enabledPref(schedule), PARIS));
    }

    @Test
    public void testComputeDailyMailScheduleAt_SlotAtNextRun_Skipped() {
        // Monday quiet from 7h, Tuesday quiet 0h-5h -> first free slot Tuesday 06:00 = next run -> mail skipped (null)
        int[][] schedule = new int[7][];
        schedule[0] = new int[]{7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23};
        schedule[1] = new int[]{0, 1, 2, 3, 4, 5};
        for (int i = 2; i < 7; i++) schedule[i] = new int[]{};
        assertNull(QuietHoursHelper.computeDailyMailScheduleAt(mondayRun(), enabledPref(schedule), PARIS));
    }

    @Test
    public void testComputeDailyMailScheduleAt_DegenerateFullQuiet_Skipped() {
        // quiet 24/7 -> no slot in 168h -> mail skipped (null)
        int[] allDay = new int[24];
        for (int h = 0; h < 24; h++) allDay[h] = h;
        QuietHoursPreference pref = enabledPref(fullSchedule(allDay));
        assertNull(QuietHoursHelper.computeDailyMailScheduleAt(mondayRun(), pref, PARIS));
    }

    @Test
    public void testComputeDailyMailScheduleAt_HalfHourOffset_India() {
        // Asia/Kolkata is UTC+5:30 -> run at 01:00 UTC = 06:30 local, truncated to 06:00 local -> mail at 07:00 local
        ZoneId kolkata = ZoneId.of("Asia/Kolkata");
        Instant runTime = Instant.parse("2026-06-01T01:00:00Z");
        Instant expected = ZonedDateTime.of(2026, 6, 1, 7, 0, 0, 0, kolkata).toInstant();
        assertEquals(expected, QuietHoursHelper.computeDailyMailScheduleAt(runTime, null, kolkata));
    }

    @Test
    public void testComputeDailyMailScheduleAt_NullZone_Skipped() {
        assertNull(QuietHoursHelper.computeDailyMailScheduleAt(mondayRun(), null, null));
    }

    // --- computeWeeklyMailScheduleAt : immediate if not in quiet, else resumption, drop if no slot before next run ---

    private static final Instant WEEKLY_NOW = dt(2026, 6, 1, 22, 30).toInstant();        // Monday 22:30 Paris
    private static final Instant WEEKLY_NEXT_RUN = WEEKLY_NOW.plus(Duration.ofDays(7));

    @Test
    public void testComputeWeeklyMailScheduleAt_NoZone_Immediate() {
        // no timezone -> no quiet hours -> immediate (returns now)
        assertEquals(WEEKLY_NOW, QuietHoursHelper.computeWeeklyMailScheduleAt(WEEKLY_NOW, enabledPref(fullSchedule(22, 23)), null, WEEKLY_NEXT_RUN));
    }

    @Test
    public void testComputeWeeklyMailScheduleAt_NoQuietHours_Immediate() {
        assertEquals(WEEKLY_NOW, QuietHoursHelper.computeWeeklyMailScheduleAt(WEEKLY_NOW, null, PARIS, WEEKLY_NEXT_RUN));
    }

    @Test
    public void testComputeWeeklyMailScheduleAt_NotQuietNow_Immediate() {
        // quiet 02h-04h, now is 22:30 -> not in a quiet slot -> immediate
        assertEquals(WEEKLY_NOW, QuietHoursHelper.computeWeeklyMailScheduleAt(WEEKLY_NOW, enabledPref(fullSchedule(2, 3, 4)), PARIS, WEEKLY_NEXT_RUN));
    }

    @Test
    public void testComputeWeeklyMailScheduleAt_InQuiet_ReturnsResumption() {
        // Monday quiet 22h,23h ; now Monday 22:30 -> resumption = Tuesday 00:00
        int[][] schedule = new int[7][];
        schedule[0] = new int[]{22, 23};
        for (int i = 1; i < 7; i++) schedule[i] = new int[]{};
        Instant expected = dt(2026, 6, 2, 0, 0).toInstant();
        assertEquals(expected, QuietHoursHelper.computeWeeklyMailScheduleAt(WEEKLY_NOW, enabledPref(schedule), PARIS, WEEKLY_NEXT_RUN));
    }

    @Test
    public void testComputeWeeklyMailScheduleAt_DegenerateFullQuiet_Dropped() {
        int[] allDay = new int[24];
        for (int h = 0; h < 24; h++) allDay[h] = h;
        assertNull(QuietHoursHelper.computeWeeklyMailScheduleAt(WEEKLY_NOW, enabledPref(fullSchedule(allDay)), PARIS, WEEKLY_NEXT_RUN));
    }

    @Test
    public void testComputeWeeklyMailScheduleAt_ResumptionAfterNextRun_Dropped() {
        // resumption Tuesday 00:00, but next run only 1h after now -> dropped
        int[][] schedule = new int[7][];
        schedule[0] = new int[]{22, 23};
        for (int i = 1; i < 7; i++) schedule[i] = new int[]{};
        Instant nextRun = WEEKLY_NOW.plus(Duration.ofHours(1));
        assertNull(QuietHoursHelper.computeWeeklyMailScheduleAt(WEEKLY_NOW, enabledPref(schedule), PARIS, nextRun));
    }
}
