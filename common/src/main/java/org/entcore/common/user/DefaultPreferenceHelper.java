package org.entcore.common.user;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import org.entcore.common.http.request.JsonHttpServerRequest;
import org.entcore.common.user.dto.UserPreferenceDto;

public class DefaultPreferenceHelper implements PreferenceHelper {

    private final EventBus eventBus;
    private final String USER_BOOK_PREF = "userbook.preferences";

    public DefaultPreferenceHelper(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    @Override
    public Future<UserPreferenceDto> updatePreferences(UserPreferenceDto preference, HttpServerRequest request) {
        Promise<UserPreferenceDto> promise = Promise.promise();
        if(preference.getPreferences().isEmpty()) {
            promise.complete(preference);
            return promise.future();
        }
        JsonObject params = new JsonObject();
        params.put("action", "v1.set.currentuser");
        params.put("request", new JsonObject().put("headers", JsonHttpServerRequest.toJson(request)));
        params.put("message", JsonObject.mapFrom(preference));

        eventBus.request(USER_BOOK_PREF, params, messageAsyncResult -> {
            if (messageAsyncResult.failed()) {
                promise.fail(messageAsyncResult.cause());
                return;
            }
            JsonObject jsonPreference = ((JsonObject)messageAsyncResult.result().body()).getJsonObject("message", new JsonObject());
            promise.complete(jsonPreference.mapTo(UserPreferenceDto.class));
        });
        return promise.future();
    }

    @Override
    public Future<UserPreferenceDto> getPreferences(HttpServerRequest request) {
        Promise<UserPreferenceDto> promise = Promise.promise();

        JsonObject params = new JsonObject();
        params.put("action", "v1.get.currentuser");
        params.put("request", JsonHttpServerRequest.toJson(request));

        eventBus.request(USER_BOOK_PREF, params, asyncResult -> {
            if (asyncResult.failed()) {
                promise.fail(asyncResult.cause());
                return;
            }
            JsonObject jsonPreference = ((JsonObject)asyncResult.result().body()).getJsonObject("message", new JsonObject());
            promise.complete(jsonPreference.mapTo(UserPreferenceDto.class));
        });
        return promise.future();
    }
}