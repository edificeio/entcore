package org.entcore.directory.services.impl.mapper;

import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import org.entcore.directory.pojo.dto.HomePagePreference;
import org.entcore.directory.pojo.dto.UserPreferenceDto;

import static org.entcore.directory.pojo.dto.UserPreferenceDto.Application.HOME_PAGE;

public class UserPreferenceDtoMapper {

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
        return dto;
    }

}
