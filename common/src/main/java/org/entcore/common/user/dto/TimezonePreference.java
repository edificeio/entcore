package org.entcore.common.user.dto;

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
}
