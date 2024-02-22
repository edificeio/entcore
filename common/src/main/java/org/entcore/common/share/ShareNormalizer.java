package org.entcore.common.share;

import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.security.SecuredAction;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public class ShareNormalizer {
    private final Map<String, SecuredAction> securedActions;

    public ShareNormalizer(final Map<String, SecuredAction> securedActions) {
        this.securedActions = securedActions;
    }

    public JsonArray toSharedArray(final JsonObject json){
        final JsonArray shared = new JsonArray();
        // set group shares
        final JsonObject groups = json.getJsonObject("groups", new JsonObject()).getJsonObject("checked", new JsonObject());
        for(final String key : groups.fieldNames()){
            final JsonArray actions = groups.getJsonArray(key, new JsonArray());
            final JsonObject currentShare = new JsonObject();
            currentShare.put("groupId", key);
            for(final Object action : actions){
                currentShare.put(action.toString(), true);
            }
            shared.add(currentShare);
        }
        // set user shares
        final JsonObject users = json.getJsonObject("users", new JsonObject()).getJsonObject("checked", new JsonObject());
        for(final String key : users.fieldNames()){
            final JsonArray actions = users.getJsonArray(key, new JsonArray());
            final JsonObject currentShare = new JsonObject();
            currentShare.put("userId", key);
            for(final Object action : actions){
                currentShare.put(action.toString(), true);
            }
            shared.add(currentShare);
        }
        return shared;
    }

    public JsonObject addNormalizedRights(final JsonObject json, final Function<JsonObject, Optional<String>> getOwner) {
        if(json.containsKey("shared")){
            final JsonArray previousShared = json.getJsonArray("shared", new JsonArray());
            final ShareModel model = new ShareModel(previousShared, this.securedActions, (getOwner.apply(json)));
            json.put("rights", new JsonArray(model.getSerializedRights()));
        }
        if(json.containsKey("actions") && json.containsKey("users") && json.containsKey("groups")){
            // normalize
            final JsonArray shared = toSharedArray(json);
            final ShareModel model = new ShareModel(shared, this.securedActions, (getOwner.apply(json)));
            json.put("rights", new JsonArray(model.getSerializedRights()));
        }
        return json;
    }

    public JsonArray addNormalizedRights(final JsonArray jsonArray, final Function<JsonObject, Optional<String>> getOwner){
        for(final Object jsonObject : jsonArray){
            if(jsonObject instanceof JsonObject){
                addNormalizedRights((JsonObject)jsonObject, getOwner);
            }
        }
        return jsonArray;
    }

    public <T> Either<String, T> addNormalizedRights(final Either<String, T> either, final Function<JsonObject, Optional<String>> getOwner){
        final Object result = ((Either)either).right().getValue();
        if(result instanceof JsonObject){
            addNormalizedRights(((JsonObject) result), getOwner);
        }else if(result instanceof JsonArray){
            addNormalizedRights(((JsonArray) result), getOwner);
        }
        return either;
    }

    public <T> AsyncResult<T> addNormalizedRights(final AsyncResult<T> asyncResult, final Function<JsonObject, Optional<String>> getOwner){
        final Object result = ((AsyncResult)asyncResult).result();
        if(result instanceof JsonObject){
            addNormalizedRights(((JsonObject) result), getOwner);
        }else if(result instanceof JsonArray){
            addNormalizedRights(((JsonArray) result), getOwner);
        }
        return asyncResult;
    }

    public <T> Handler<T> addNormalizedRights(final Handler<T> handler, final Function<JsonObject, Optional<String>> getOwner){
        return res -> {
            // get result and add normalized rights
            if(res instanceof Either){
                if(((Either)res).isRight()){
                    addNormalizedRights((Either)res, getOwner);
                }
            }else if (res instanceof AsyncResult){
                addNormalizedRights((AsyncResult)res, getOwner);
            }
            handler.handle(res);
        };
    }

}
