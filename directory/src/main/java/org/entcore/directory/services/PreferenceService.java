package org.entcore.directory.services;

import fr.wseduc.webutils.Either;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import org.entcore.common.user.UserInfos;
import org.entcore.directory.pojo.dto.UserPreferenceDto;

public interface PreferenceService {

    /**
     * Update user preferences, doesn't affect preferences not defined in the UserPreferenceDto
     * @param preference updates preferences
     * @param userInfos userInfo of the connected user
     * @param session session of the user
     * @return result of the update of preferences
     */
    Future<UserPreferenceDto> updatePreferences(UserPreferenceDto preference, UserInfos userInfos, JsonObject session);

    /**
     * Retreive user preferences, map only those defined in the actual DTO
     * @param userInfos connected user
     * @param session session of the user
     * @return preferences actually mapped
     */
    Future<UserPreferenceDto> getPreferences(UserInfos userInfos, JsonObject session);
}
