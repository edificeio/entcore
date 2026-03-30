package org.entcore.common.user.mapper;

import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import org.entcore.common.user.dto.*;

import static org.entcore.common.user.dto.UserPreferenceDto.Application.*;

public final class UserPreferenceDtoMapper {

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
           dto.setHomePage(Json.decodeValue(pref.getString(HOME_PAGE.getMappingName()), HomePagePreference.class));
        }
        if (pref.containsKey(THEME.getMappingName())) {
            dto.getPreferences().add(THEME);
            dto.setTheme(new ThemePreference(pref.getString(THEME.getMappingName())));
        }
        if (pref.containsKey(LANGUAGE.getMappingName())) {
            dto.getPreferences().add(LANGUAGE);
            dto.setLanguage(Json.decodeValue(pref.getString(LANGUAGE.getMappingName()), LanguagePreference.class));
        }
        if (pref.containsKey(APPLICATION.getMappingName())) {
            dto.getPreferences().add(APPLICATION);
            dto.setApps(Json.decodeValue(pref.getString(APPLICATION.getMappingName()), ApplicationPreference.class));
        }
        return dto;
    }

}
