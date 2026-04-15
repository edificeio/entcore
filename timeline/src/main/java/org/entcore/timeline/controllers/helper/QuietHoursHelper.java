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
    static boolean isQuietHour(Instant now, QuietHoursPreference userPrefQuietHours, TimezonePreference userPrefTimezone, String userPrefUai) {
        if (userPrefQuietHours == null || !userPrefQuietHours.isEnabled() || userPrefQuietHours.getSchedule() == null) return false;
        ZoneId zone = resolveTimezone(userPrefTimezone, userPrefUai);
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

    /** Returns the next send instant based on quiet hours. Returns notificationTime if no preference is active. */
    static Instant computeNextSendTime(Instant notificationTime, QuietHoursPreference userPrefQuietHours, TimezonePreference userPrefTimezone, String userPrefUai) {
        if (userPrefQuietHours == null || !userPrefQuietHours.isEnabled() || userPrefQuietHours.getSchedule() == null) {
            return notificationTime;
        }
        ZoneId zone = resolveTimezone(userPrefTimezone, userPrefUai);
        return computeNextSendTime(notificationTime, userPrefQuietHours, zone);
    }

    /** Returns the next send instant using an already-resolved ZoneId. Returns notificationTime if zone is null. */
    static Instant computeNextSendTime(Instant notificationTime, QuietHoursPreference userPrefQuietHours, ZoneId zone) {
        if (zone == null) return notificationTime;
        if (userPrefQuietHours == null || !userPrefQuietHours.isEnabled() || userPrefQuietHours.getSchedule() == null) {
            return notificationTime;
        }
        return computeNextSendTime(notificationTime.atZone(zone), userPrefQuietHours.getSchedule());
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
     * Resolves the ZoneId to use for quiet hours:
     * 1. Explicit timezone from user preference
     * 2. Fallback from UAI (structure code)
     * 3. null if no fallback (foreign structure -> no quiet hours applied)
     */
    static ZoneId resolveTimezone(TimezonePreference userPrefTimezone, String userPrefUai) {
        if (userPrefTimezone != null && userPrefTimezone.getTimezone() != null) {
            try {
                return ZoneId.of(userPrefTimezone.getTimezone());
            } catch (Exception e) {
                log.warn("[QuietHoursHelper] Invalid timezone in preference: " + userPrefTimezone.getTimezone());
            }
        }
        return getTimezoneFromUai(userPrefUai);
    }

    /**
     * Returns the ZoneId for a French school UAI code.
     * Extracts the department from the first 2-3 characters to handle DOM-TOM.
     * Falls back to Europe/Paris for metropolitan France or unknown UAI.
     */
    static ZoneId getTimezoneFromUai(String uai) {
        if (uai == null) return null;
        if (uai.length() < 3) return ZoneId.of("Europe/Paris");
        try {
            int dept = Integer.parseInt(uai.substring(0, 3));
            switch (dept) {
                case 971: return ZoneId.of("America/Guadeloupe");
                case 972: return ZoneId.of("America/Martinique");
                case 973: return ZoneId.of("America/Cayenne");
                case 974: return ZoneId.of("Indian/Reunion");
                case 975: return ZoneId.of("America/Miquelon");
                case 976: return ZoneId.of("Indian/Mayotte");
                case 977: return ZoneId.of("America/St_Barthelemy");
                case 978: return ZoneId.of("America/Marigot");
                case 986: return ZoneId.of("Pacific/Wallis");
                case 987: return ZoneId.of("Pacific/Tahiti");
                case 988: return ZoneId.of("Pacific/Noumea");
                default:  return ZoneId.of("Europe/Paris");
            }
        } catch (NumberFormatException e) {
            return ZoneId.of("Europe/Paris");
        }
    }
}
