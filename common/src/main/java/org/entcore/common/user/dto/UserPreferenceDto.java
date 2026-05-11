package org.entcore.common.user.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import fr.wseduc.webutils.I18n;
import io.vertx.core.json.JsonObject;

import java.util.*;

public class UserPreferenceDto {

    private HomePagePreference homePage;
    private ThemePreference theme;
    private LanguagePreference language;
    private ApplicationPreference apps;
    private String lastDomain;
    private TimezonePreference timezone;
    private QuietHoursPreference quietHours;

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

    public String getLastDomain() {
        return lastDomain;
    }

    public void setLastDomain(String lastDomain) {
        this.lastDomain = lastDomain;
    }

    @JsonIgnore
    public String getCurrentLanguage() {
        if (this.language == null) {
            return "fr";
        }
        String domain = this.lastDomain;
        if(domain == null) {
            domain = I18n.DEFAULT_DOMAIN;
        }
        if(language.getLanguages().containsKey(domain)) {
            return language.getLanguages().get(domain);
        }
        return language.getLanguages().get(I18n.DEFAULT_DOMAIN);
    }

    public TimezonePreference getTimezone() {
        return timezone;
    }

    public void setTimezone(TimezonePreference timezone) {
        this.timezone = timezone;
    }

    public QuietHoursPreference getQuietHours() {
        return quietHours;
    }

    public void setQuietHours(QuietHoursPreference quietHours) {
        this.quietHours = quietHours;
    }

    public Preference getPreference(Application appName) {
        switch (appName) {
            case HOME_PAGE: return homePage;
            case THEME: return theme;
            case LANGUAGE: return language;
            case APPLICATION: return apps;
            case TIMEZONE: return timezone;
            case QUIET_HOURS: return quietHours;
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
        APPLICATION("apps"),
        TIMEZONE("timezone"),
        QUIET_HOURS("quietHours");

        private String mappingName;

        Application(String mappingName) {
            this.mappingName = mappingName;
        }

        public String getMappingName() {
            return mappingName;
        }

    }
}

