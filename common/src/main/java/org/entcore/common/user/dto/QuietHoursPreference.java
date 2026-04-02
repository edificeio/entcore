package org.entcore.common.user.dto;

import io.vertx.core.json.Json;

import java.util.HashSet;
import java.util.Set;

public class QuietHoursPreference implements Preference {

    private String timezone;
    private int[][] schedule;
    private boolean managed;
    private boolean enabled;

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public int[][] getSchedule() {
        return schedule;
    }

    public void setSchedule(int[][] schedule) {
        this.schedule = schedule;
    }

    public boolean isManaged() {
        return managed;
    }

    public void setManaged(boolean managed) {
        this.managed = managed;
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
