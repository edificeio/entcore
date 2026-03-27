package org.entcore.directory.services;

import io.vertx.core.json.JsonObject;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.dto.UserPreferenceDto;

public interface PreferenceCacheService {

    /**
     * Update preferences in cache (managed in the session of the user). Use the full preferences from neo4j (legacy)
     *
     * @param userInfos
     * @param preferences
     */
    void refreshPreferences(UserInfos userInfos, UserPreferenceDto preferences);

    /**
     * Add preferences defined in the dto to the cache (session)
     *
      * @param userInfos
     * @param session
     * @param preference
     */
    void addPreferences(UserInfos userInfos, JsonObject session, UserPreferenceDto preference);
}
