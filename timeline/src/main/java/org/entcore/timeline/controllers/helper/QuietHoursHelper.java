package org.entcore.timeline.controllers.helper;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.user.dto.QuietHoursPreference;
import org.entcore.common.user.dto.TimezonePreference;

import java.time.*;
import java.time.temporal.ChronoUnit;

/**
 * Utility class for quiet hours logic: schedule evaluation, timezone resolution, UAI mapping.
 */
public final class QuietHoursHelper {

    private static final Logger log = LoggerFactory.getLogger(QuietHoursHelper.class);

    private QuietHoursHelper() {}

    /** Returns true if the current instant falls within the user's quiet hours. Returns false if no preference is active. */
    static boolean isQuietHour(Instant now, QuietHoursPreference userPrefQuietHours, TimezonePreference userPrefTimezone) {
        if (userPrefQuietHours == null || !userPrefQuietHours.isEnabled() || userPrefQuietHours.getSchedule() == null) return false;
        ZoneId zone = resolveTimezone(userPrefTimezone);
        if (zone == null) return false;
        return isQuietHour(now.atZone(zone), userPrefQuietHours.getSchedule());
    }

    /**
     * Returns true if the given datetime falls within the provided schedule.
     * Returns false if schedule is null or the hour is not listed for that day.
     * schedule[dayIndex] = array of quiet hours (0=Monday, 6=Sunday).
     */
    static boolean isQuietHour(ZonedDateTime now, int[][] schedule) {
        if (schedule == null) return false;
        int dayIndex = now.getDayOfWeek().getValue() - 1;
        int hour = now.getHour();
        if (dayIndex >= schedule.length || schedule[dayIndex] == null) return false;
        for (int quietHour : schedule[dayIndex]) {
            if (quietHour == hour) return true;
        }
        return false;
    }

    /** Returns the next send instant using an already-resolved ZoneId. Returns notificationTime if zone is null. */
    public static Instant computeNextSendTime(Instant notificationTime, QuietHoursPreference userPrefQuietHours, ZoneId zone) {
        if (zone == null) return notificationTime;
        if (userPrefQuietHours == null || !userPrefQuietHours.isEnabled() || userPrefQuietHours.getSchedule() == null) {
            return notificationTime;
        }
        return computeNextSendTime(notificationTime.atZone(zone), userPrefQuietHours.getSchedule());
    }

    /**
     * Computes the scheduleAt instant for the daily mail of a user processed at runTime.
     *
     * <p>Base send time is the next local hour after runTime (run at 06:00 local -> mail at 07:00 local).
     * If quiet hours are active at the base time, the send time is pushed to the first non-quiet slot.
     * The mail must be sent before the next daily run of the user's timezone (next local 06:00, i.e. +1 calendar
     * day — calendar arithmetic so DST transition days stay correct): past that point a fresher digest takes over,
     * so the mail is dropped instead.</p>
     *
     * @return the send instant, or null if no valid slot exists before the next run (mail must be skipped)
     */
    public static Instant computeDailyMailScheduleAt(Instant runTime, QuietHoursPreference userPrefQuietHours, ZoneId zone) {
        if (zone == null) return null;
        final ZonedDateTime localRun = runTime.atZone(zone).truncatedTo(ChronoUnit.HOURS);
        final Instant base = localRun.plusHours(1).toInstant();
        final Instant nextRun = localRun.plusDays(1).toInstant();
        final Instant scheduleAt = computeNextSendTime(base, userPrefQuietHours, zone);
        if (scheduleAt == null || !scheduleAt.isBefore(nextRun)) return null;
        return scheduleAt;
    }

    /**
     * Pure engine: returns the original instant if the current hour is not quiet.
     * Otherwise advances hour by hour (max 168 slots) to find the first non-quiet hour.
     * Returns null if no slot found (degenerate schedule). DST handled by ZonedDateTime.plusHours.
     */
    static Instant computeNextSendTime(ZonedDateTime localTime, int[][] schedule) {
        if (schedule == null) return localTime.toInstant();
        int currentDayIndex = localTime.getDayOfWeek().getValue() - 1;
        int currentHour = localTime.getHour();
        if (!isHourInSchedule(schedule, currentDayIndex, currentHour)) return localTime.toInstant();
        ZonedDateTime cursor = localTime.truncatedTo(ChronoUnit.HOURS).plusHours(1);
        for (int i = 0; i < 168; i++) {
            if (!isHourInSchedule(schedule, cursor.getDayOfWeek().getValue() - 1, cursor.getHour())) {
                return cursor.toInstant();
            }
            cursor = cursor.plusHours(1);
        }
        return null;
    }

    /** Returns true if the given hour is in the schedule for the given dayIndex. */
    static boolean isHourInSchedule(int[][] schedule, int dayIndex, int hour) {
        if (dayIndex < 0 || dayIndex >= schedule.length || schedule[dayIndex] == null) return false;
        for (int quietHour : schedule[dayIndex]) {
            if (quietHour == hour) return true;
        }
        return false;
    }

    /**
     * Resolves the ZoneId from the user's explicit timezone preference only.
     * Returns null when the user has no (valid) timezone preference: callers treat that as "no quiet hours"
     * (and fall back to a default zone solely for daily-mail scheduling).
     */
    public static ZoneId resolveTimezone(TimezonePreference userPrefTimezone) {
        if (userPrefTimezone != null && userPrefTimezone.getTimezone() != null) {
            try {
                return ZoneId.of(userPrefTimezone.getTimezone());
            } catch (Exception e) {
                log.warn("[QuietHoursHelper] Invalid timezone in preference: " + userPrefTimezone.getTimezone());
            }
        }
        return null;
    }
}
