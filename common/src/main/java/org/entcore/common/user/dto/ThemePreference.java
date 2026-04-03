package org.entcore.common.user.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public class ThemePreference implements Preference {

    private String theme;

    public ThemePreference() {
        //for jackson
    }

    public ThemePreference(String theme) {
        this.theme = theme;
    }

    @JsonValue
    public String getTheme() {
        return theme;
    }

    @JsonCreator
    public ThemePreference setTheme(String theme) {
        this.theme = theme;
        return this;
    }

    @Override
    public String encode() {
        return theme;
    }
}
