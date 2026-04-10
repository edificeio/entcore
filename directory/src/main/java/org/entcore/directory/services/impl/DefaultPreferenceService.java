package org.entcore.directory.services.impl;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.dto.UserPreferenceDto;
import org.entcore.common.user.mapper.UserPreferenceDtoMapper;
import org.entcore.directory.services.PreferenceCacheService;
import org.entcore.directory.services.PreferenceService;

import static org.entcore.common.neo4j.Neo4jResult.validUniqueResultHandler;

public class DefaultPreferenceService implements PreferenceService {

    private final Neo4j neo4j = Neo4j.getInstance();
    private final PreferenceCacheService preferenceCacheService;

    public DefaultPreferenceService(PreferenceCacheService preferenceCacheService) {
        this.preferenceCacheService = preferenceCacheService;
    }

    @Override
    public Future<UserPreferenceDto> updatePreferences(UserPreferenceDto preference, UserInfos userInfos, JsonObject session) {
        Promise<UserPreferenceDto> promise = Promise.promise();
        if(preference.getPreferences().isEmpty()) {
            promise.complete(preference);
            return promise.future();
        }
        JsonObject params = new JsonObject();
        params.put("userId", userInfos.getUserId());

        StringBuilder query = new StringBuilder("MATCH (u:User {id:{userId}}) MERGE (u)-[:PREFERS]->(uac:UserAppConf) ");
        StringBuilder create = new StringBuilder(" ON CREATE SET ");
        StringBuilder merge = new StringBuilder(" ON MATCH SET ");

        preference.getPreferences().forEach( ( appName ) -> {
            String partialQuery = " uac."+ appName.getMappingName() +" = {"+ appName.getMappingName() +"},";
            create.append(partialQuery);
            merge.append(partialQuery);
            params.put(appName.getMappingName(), preference.getPreference(appName).encode());
        });

        create.deleteCharAt(create.length() - 1);
        merge.deleteCharAt(merge.length() - 1);

        query.append(create).append(merge).append(" RETURN uac, u.lastDomain as lastDomain");

        neo4j.execute(query.toString(), params, validUniqueResultHandler( result -> {
            if(result.isRight()) {
                JsonObject prefs = result.right().getValue().getJsonObject("uac").getJsonObject("data", new JsonObject());
                prefs.put("lastDomain", result.right().getValue().getString("lastDomain"));
                UserPreferenceDto userPreferenceDto = UserPreferenceDtoMapper.map(prefs);
                promise.complete(userPreferenceDto);
                preferenceCacheService.addPreferences(userInfos, session, userPreferenceDto);
            } else {
                promise.fail(result.left().getValue());
            }
        }));
        return promise.future();
    }

    @Override
    public Future<UserPreferenceDto> getPreferences(UserInfos userInfos, JsonObject session) {
        Promise<UserPreferenceDto> promise = Promise.promise();
        JsonObject params = new JsonObject();
        params.put("userId", userInfos.getUserId());

        final JsonObject cache = session.getJsonObject("cache", new JsonObject());
        if(cache.containsKey("preferences")) {
            JsonObject prefs =  cache.getJsonObject("preferences").copy();
            neo4j.execute("MATCH (u:User {id:{userId}}) RETURN u.lastDomain as lastDomain", params, validUniqueResultHandler(result -> {
                if (result.isRight()) {
                    prefs.put("lastDomain", result.right().getValue().getString("lastDomain"));
                    promise.complete(UserPreferenceDtoMapper.map(prefs));
                } else {
                    promise.fail(result.left().getValue());
                }
            }));
            return promise.future();
        }

        neo4j.execute("MATCH (u:User {id:{userId}})-[:PREFERS]->(uac:UserAppConf) RETURN uac, u.lastDomain", params, validUniqueResultHandler(result -> {
            if (result.isRight()) {
                JsonObject prefs = result.right().getValue().getJsonObject("uac").getJsonObject("data", new JsonObject());
                prefs.put("lastDomain", result.right().getValue().getString("lastDomain"));
                UserPreferenceDto preferenceDto = UserPreferenceDtoMapper.map(prefs);
                preferenceCacheService.refreshPreferences(userInfos, preferenceDto);
                promise.complete(preferenceDto);
            } else {
                promise.fail(result.left().getValue());
            }
        }));
        return promise.future();
    }

    @Override
    public Future<UserPreferenceDto> getPreferences(String userId) {
        Promise<UserPreferenceDto> promise = Promise.promise();
        JsonObject params = new JsonObject();
        params.put("userId", userId);
        neo4j.execute("MATCH (u:User {id:{userId}})-[:PREFERS]->(uac:UserAppConf) RETURN uac, u.lastDomain as lastDomain", params, validUniqueResultHandler(result -> {
            if (result.isRight()) {
                JsonObject prefs = result.right().getValue().getJsonObject("uac").getJsonObject("data", new JsonObject());
                prefs.put("lastDomain", result.right().getValue().getString("lastDomain"));
                UserPreferenceDto preferenceDto = UserPreferenceDtoMapper.map(prefs);
                promise.complete(preferenceDto);
            } else {
                promise.fail(result.left().getValue());
            }
        }));
        return promise.future();
    }
}