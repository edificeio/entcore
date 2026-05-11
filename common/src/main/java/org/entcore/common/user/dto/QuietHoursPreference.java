package org.entcore.common.user.dto;

import io.vertx.core.json.Json;

import java.util.HashSet;
import java.util.Set;

/**
 * Represents a user's quiet hours preference for notification deferral.
 */
public class QuietHoursPreference implements Preference {

    /**
     * Indicates who manages the quiet hours configuration.
     */
    public enum ManagedBy {
        /** Set by the user themselves */
        USER,
        /** Set by the structure (school/organization) */
        STRUCTURE
    }

    /**
     * Weekly schedule of quiet hours.
     * Array of 7 days (0=Monday, 6=Sunday), each containing the hours (0-23) that are quiet.
     * Null means no schedule is defined.
     */
    private int[][] schedule;

    /**
     * Indicates who manages this quiet hours configuration.
     * See {@link ManagedBy} for possible values.
     */
    private ManagedBy managedBy;

    /**
     * Whether quiet hours filtering is active.
     * When false, notifications are sent immediately regardless of the schedule.
     */
    private boolean enabled;

    public int[][] getSchedule() {
        return schedule;
    }

    public void setSchedule(int[][] schedule) {
        this.schedule = schedule;
    }

    public ManagedBy getManagedBy() {
        return managedBy;
    }

    public void setManagedBy(ManagedBy managedBy) {
        this.managedBy = managedBy;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public String encode() {
        return Json.encode(this);
    }

    @Override
    public boolean validate() {
        if (schedule == null) return true;
        if (schedule.length != 7) return false;
        for (int[] day : schedule) {
            if (day == null) return false;
            Set<Integer> seen = new HashSet<>();
            for (int hour : day) {
                if (hour < 0 || hour > 23) return false;
                if (!seen.add(hour)) return false; // duplicate
            }
        }
        return true;
    }
}
