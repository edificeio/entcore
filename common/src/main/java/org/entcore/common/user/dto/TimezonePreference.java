package org.entcore.common.user.dto;

import io.vertx.core.json.Json;

import java.time.ZoneId;

/**
 * User timezone preference.
 * Used to determine the user's local timezone independently of other preferences.
 */
public class TimezonePreference implements Preference {

    private String timezone;
    private ManagedBy managedBy;

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public ManagedBy getManagedBy() {
        return managedBy;
    }

    public void setManagedBy(ManagedBy managedBy) {
        this.managedBy = managedBy;
    }

    @Override
    public String encode() {
        return Json.encode(this);
    }

    @Override
    public boolean validate() {
        if (timezone == null) return true;
        try {
            ZoneId.of(timezone);
            return true;
        } catch (Exception invalidZone) {
            return false;
        }
    }
}
