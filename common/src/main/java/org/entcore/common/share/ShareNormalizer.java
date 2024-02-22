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
/**
 * <p>
 * ShareNormalizer provide functions to transform historical shared model object to the new normalized model object
 * </p>
 * <p>
 * Historical model object structure: <b>[{userId: "USER_ID", "fqdn.java.method1": true},{groupId: "GROUPID", "fqdn.java.method2": true, "fqdn.java.method3": true}]</b>
 * </p>
 * <p>
 * Normalized model object structure: <b>["user:USER_ID:read", "group:GROUP_ID:contrib", "group:GROUP_ID:manage"]</b>
 * </p>
 *
 */
public class ShareNormalizer {
    private final Map<String, SecuredAction> securedActions;

    public ShareNormalizer(final Map<String, SecuredAction> securedActions) {
        this.securedActions = securedActions;
    }

    /**
     * <p>This method convert a list of user and groups into the historical shared array</p>
     *
     * @param json an object having this structure: <b>{"groups":{"checked":{"GROUP_ID": ["action1"]}}, "users":{"USER_ID": ["action2", "action3"]}}}</b>
     * @return a shared array having this structure: <b>[{groupId:"GROUP_ID", "action1":true},{userId:"USER_ID", "action2":true, action3: true }]</b>
     */
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

    /**
     * This method take an object which could be wether an object have shared rights or an object listing all users and groups with related authorized actions and add normalized rights to it
     *
     * @param json an object of type <b>{shared:[{userId:"USER_ID", action1: true}]}</b> or <b>{"groups":{"checked":{"GROUP_ID": ["action1"]}}, "users":{"USER_ID": ["action2", "action3"]}}}</b>
     * @param getOwner a function extracting the ownerId of the current json object (or return Optional.empty if missing)
     * @return the same <json> with a new field "rights" wich contains normalized rights
     */
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


    /**
     * This method take a list of json object and add a field "rights" wich contains normalized rights
     *
     * @param jsonArray a an array of JsonObject
     * @param getOwner a function extracting the ownerId of the current json object (or return Optional.empty if missing)
     * @return the same JsonArray with the new field "rights" added to each object
     */
    public JsonArray addNormalizedRights(final JsonArray jsonArray, final Function<JsonObject, Optional<String>> getOwner){
        for(final Object jsonObject : jsonArray){
            if(jsonObject instanceof JsonObject){
                addNormalizedRights((JsonObject)jsonObject, getOwner);
            }
        }
        return jsonArray;
    }

    /**
     * This method take an Either that return a JsonObject/JsonArray and add a field "rights" wich contains normalized rights
     *
     * @param either Either of a JsonArray OR JsonObject
     * @param getOwner a function extracting the ownerId of the current json object (or return Optional.empty if missing)
     * @return the same JsonArray or JsonObject with the new field "rights" added to each object
     */
    public <T> Either<String, T> addNormalizedRights(final Either<String, T> either, final Function<JsonObject, Optional<String>> getOwner){
        final Object result = ((Either)either).right().getValue();
        if(result instanceof JsonObject){
            addNormalizedRights(((JsonObject) result), getOwner);
        }else if(result instanceof JsonArray){
            addNormalizedRights(((JsonArray) result), getOwner);
        }
        return either;
    }

    /**
     * This method take an AsyncResult that return a JsonObject/JsonArray and add a field "rights" wich contains normalized rights
     *
     * @param asyncResult asyncResult of a JsonArray OR JsonObject
     * @param getOwner a function extracting the ownerId of the current json object (or return Optional.empty if missing)
     * @return the same JsonArray or JsonObject with the new field "rights" added to each object
     */
    public <T> AsyncResult<T> addNormalizedRights(final AsyncResult<T> asyncResult, final Function<JsonObject, Optional<String>> getOwner){
        final Object result = ((AsyncResult)asyncResult).result();
        if(result instanceof JsonObject){
            addNormalizedRights(((JsonObject) result), getOwner);
        }else if(result instanceof JsonArray){
            addNormalizedRights(((JsonArray) result), getOwner);
        }
        return asyncResult;
    }
    /**
     * This method take a Handler that return an Either/AsyncResult of JsonArray/JsonObject
     *
     * @param handler returning an Either<String,JsonObject> OR an Either<String,JsonArray> OR AsyncResult<JsonObject> OR a AsyncResult<JsonArray>
     * @param getOwner a function extracting the ownerId of the current json object (or return Optional.empty if missing)
     * @return the same JsonArray or JsonObject with the new field "rights" added to each object
     */
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
