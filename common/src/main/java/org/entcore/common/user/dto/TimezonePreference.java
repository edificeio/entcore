package org.entcore.common.user.dto;

import java.time.ZoneId;

/**
 * User timezone preference.
 * Used to determine the user's local timezone independently of other preferences.
 */
public class TimezonePreference implements Preference {

    private String timezone;

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    @Override
    public String encode() {
        return timezone;
    }

    @Override
    public boolean validate() {
        if (timezone == null) return true;
        try {
            ZoneId.of(timezone);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
