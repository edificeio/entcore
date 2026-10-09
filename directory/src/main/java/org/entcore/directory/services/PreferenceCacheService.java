package org.entcore.directory.services;

import io.vertx.core.Future;
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

    void putLastDomain(UserInfos userInfos, String lastDomain);

    /**
     * Add preferences defined in the dto to the cache (session)
     *
      * @param userInfos
     * @param session
     * @param preference
     * @return a future completed once the session store has acknowledged the update.
     * It never fails : a session update failure is only logged, since preferences are already persisted.
     */
    Future<Void> addPreferences(UserInfos userInfos, JsonObject session, UserPreferenceDto preference);
}
