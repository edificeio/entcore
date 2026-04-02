package org.entcore.common.user.mapper;

import fr.wseduc.webutils.I18n;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import org.entcore.common.user.dto.*;

import static org.entcore.common.user.dto.UserPreferenceDto.Application.*;

public final class UserPreferenceDtoMapper {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserPreferenceDtoMapper.class);

    private UserPreferenceDtoMapper() {
        //private
    }

    public static UserPreferenceDto map(JsonObject pref) {
        UserPreferenceDto dto = new UserPreferenceDto();
        dto.setLegacyPreferences(pref);
        if(pref == null) {
            return dto;
        }
        if (pref.containsKey(HOME_PAGE.getMappingName())) {
           dto.getPreferences().add(HOME_PAGE);
           String encoded =  pref.getString(HOME_PAGE.getMappingName());
           dto.setHomePage(decodeSafely(encoded, HomePagePreference.class));
        }
        if (pref.containsKey(THEME.getMappingName())) {
            dto.getPreferences().add(THEME);
            dto.setTheme(new ThemePreference(pref.getString(THEME.getMappingName())));
        }
        if (pref.containsKey(LANGUAGE.getMappingName())) {
            dto.getPreferences().add(LANGUAGE);
            String encoded =  pref.getString(LANGUAGE.getMappingName());
            dto.setLanguage(decodeSafely(encoded, LanguagePreference.class));
        }
        if (pref.containsKey(APPLICATION.getMappingName())) {
            dto.getPreferences().add(APPLICATION);
            String encoded = pref.getString(APPLICATION.getMappingName());
            dto.setApps(decodeSafely(encoded, ApplicationPreference.class));
        }
        if (pref.containsKey("lastDomain") && pref.getString("lastDomain") != null) {
            dto.setLastDomain(pref.getString("lastDomain"));
        }
       	else {
           dto.setLastDomain(I18n.DEFAULT_DOMAIN);
        }
        if (pref.containsKey(TIMEZONE.getMappingName())) {
           dto.getPreferences().add(TIMEZONE);
           dto.setTimezone(new TimezonePreference());
           dto.getTimezone().setTimezone(pref.getString(TIMEZONE.getMappingName()));
        }
        if (pref.containsKey(QUIET_HOURS.getMappingName())) {
           dto.getPreferences().add(QUIET_HOURS);
           String encoded = pref.getString(QUIET_HOURS.getMappingName());
           dto.setQuietHours(decodeSafely(encoded, QuietHoursPreference.class));
        }
        return dto;
    }

    private static <T> T decodeSafely(String codedValue, Class<T> clazz) {
        try {
            return Json.decodeValue(codedValue, clazz);
        } catch (Exception e) {
            LOGGER.error("Could not encode safe string " + codedValue + " " +  e.getMessage());
            return null;
        }
    }

}
