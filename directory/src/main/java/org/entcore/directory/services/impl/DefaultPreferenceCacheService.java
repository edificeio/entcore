package org.entcore.directory.services.impl;

import io.vertx.core.eventbus.EventBus;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonObject;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;
import org.entcore.common.user.dto.UserPreferenceDto;
import org.entcore.directory.services.PreferenceCacheService;

public class DefaultPreferenceCacheService implements PreferenceCacheService {

    private EventBus eb;

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultPreferenceCacheService.class);
    private static final String PREFERENCES_ATTRIBUTE_NAME = "preferences";

    public DefaultPreferenceCacheService(EventBus eb){
        this.eb = eb;
    }

    @Override
    public void refreshPreferences(UserInfos userInfos, UserPreferenceDto preferences) {
        UserUtils.addSessionAttribute(eb, userInfos.getUserId(), PREFERENCES_ATTRIBUTE_NAME, preferences.getLegacyPreferences(), event -> {
            if(Boolean.FALSE.equals(event)) {
                LOGGER.error("Could not add preferences attribute to session.");
            }
        });
    }

    @Override
    public void addPreferences(UserInfos userInfos, JsonObject session, UserPreferenceDto preference) {
        final JsonObject cache = session.getJsonObject("cache");
        if (cache.containsKey(PREFERENCES_ATTRIBUTE_NAME)) {
            JsonObject prefs = cache.getJsonObject(PREFERENCES_ATTRIBUTE_NAME);
            for (UserPreferenceDto.Application app : preference.getPreferences()) {
                prefs.put(app.getMappingName(), preference.getPreference(app).encode());
            }
            UserUtils.addSessionAttribute(eb, userInfos.getUserId(), PREFERENCES_ATTRIBUTE_NAME, prefs, event -> {
                if (!event) {
                    LOGGER.error("Could not add preferences attribute to session.");
                }
            });
        }
    }

}
