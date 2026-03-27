package org.entcore.common.user;

import io.vertx.core.Future;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import org.entcore.common.user.dto.UserPreferenceDto;

public interface PreferenceHelper {

    /**
     * Update user preferences, doesn't affect preferences not defined in the UserPreferenceDto
     * @param preference updates preferences
     * @return result of the update of preferences
     */
    Future<UserPreferenceDto> updatePreferences(UserPreferenceDto preference, HttpServerRequest request);

    /**
     * Retrieve user preferences, map only those defined in the actual DTO
     *
     * @return preferences actually mapped
     */
    Future<UserPreferenceDto> getPreferences(HttpServerRequest request);
}
