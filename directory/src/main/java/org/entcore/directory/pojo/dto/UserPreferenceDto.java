package org.entcore.directory.pojo.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.vertx.core.json.JsonObject;

import java.util.*;

public class UserPreferenceDto {


    private HomePagePreference homePage;
    @JsonIgnore
    private JsonObject legacyPreferences;
    @JsonIgnore
    private final List<Application> preferences = new ArrayList<>();

    public JsonObject getLegacyPreferences() {
        return legacyPreferences;
    }

    public UserPreferenceDto setLegacyPreferences(JsonObject legacyPreferences) {
        this.legacyPreferences = legacyPreferences;
        return this;
    }

    public List<Application> getPreferences() {
        return preferences;
    }

    public HomePagePreference getHomePage() {
        return homePage;
    }

    public UserPreferenceDto setHomePage(HomePagePreference homePage) {
        this.homePage = homePage;
        return this;
    }

    public ApplicationPreference getApplicationPreference(Application appName) {
        switch (appName) {
            case HOME_PAGE: return homePage;
        }
        return null;
    }

    public void populateApplicationPreferences(Collection<String> apps) {
        Arrays.stream(Application.values())
              .filter(a -> apps.contains(a.getMappingName()))
                .forEach( a -> this.getPreferences().add(a));

    }


    public enum Application {

        HOME_PAGE("homePage"),
        THEME("theme"),
        LANGUAGE("language");

        private String mappingName;

        Application(String mappingName) {
            this.mappingName = mappingName;
        }

        public String getMappingName() {
            return mappingName;
        }
    }
}

