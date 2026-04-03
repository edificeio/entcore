package org.entcore.common.user.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.vertx.core.json.JsonObject;

import java.util.*;

public class UserPreferenceDto {

    private HomePagePreference homePage;
    private ThemePreference theme;
    private LanguagePreference language;
    private ApplicationPreference apps;

    @JsonIgnore
    private JsonObject legacyPreferences;
    @JsonIgnore
    private final List<Application> preferences = new ArrayList<>();

    public JsonObject getLegacyPreferences() {
        return legacyPreferences;
    }

    public void setLegacyPreferences(JsonObject legacyPreferences) {
        this.legacyPreferences = legacyPreferences;
    }

    public List<Application> getPreferences() {
        return preferences;
    }

    public HomePagePreference getHomePage() {
        return homePage;
    }

    public void setHomePage(HomePagePreference homePage) {
        this.homePage = homePage;
    }

    public ThemePreference getTheme() {
        return theme;
    }

    public void setTheme(ThemePreference theme) {
        this.theme = theme;
    }

    public LanguagePreference getLanguage() {
        return language;
    }

    public void setLanguage(LanguagePreference language) {
        this.language = language;
    }

    public ApplicationPreference getApps() {
        return apps;
    }

    public void setApps(ApplicationPreference apps) {
        this.apps = apps;
    }

    public Preference getPreference(Application appName) {
        switch (appName) {
            case HOME_PAGE: return homePage;
            case THEME: return theme;
            case LANGUAGE: return language;
            case APPLICATION: return apps;
        }
        return new Preference() {
            @Override
            public String encode() {
                return null;
            }
        };
    }

    public void populateApplicationPreferences(Collection<String> appCollection) {
        Arrays.stream(Application.values())
              .filter(a -> appCollection.contains(a.getMappingName()))
                .forEach( a -> this.getPreferences().add(a));
    }

    public enum Application {

        HOME_PAGE("homePage"),
        THEME("theme"),
        LANGUAGE("language"),
        APPLICATION("apps");

        private String mappingName;

        Application(String mappingName) {
            this.mappingName = mappingName;
        }

        public String getMappingName() {
            return mappingName;
        }

    }
}

