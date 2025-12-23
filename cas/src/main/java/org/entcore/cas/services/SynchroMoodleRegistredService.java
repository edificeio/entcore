package org.entcore.cas.services;

import fr.wseduc.cas.async.Handler;
import fr.wseduc.cas.entities.AuthCas;
import fr.wseduc.cas.entities.User;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import org.entcore.common.user.UserUtils;

import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;

public class SynchroMoodleRegistredService extends DefaultRegisteredService {


    private static final Logger log = LoggerFactory.getLogger(SynchroMoodleRegistredService.class);

    private static final String ID = "id";
    private static final String LOGIN = "login";
    private static final String NAME = "name";
    private static final String FIRSTNAME = "firstName";
    private static final String LASTNAME = "lastName";
    private static final String TYPE = "type";
    private static final String PROFIL = "profil";
    private static final String EMAIL = "email";
    private static final String GROUPS = "groups";
    private static final String GROUPS_KEY = "functionalGroups";
    private static final String GROUP_SUBTYPE = "subType";
    private static final String ACTION = "action";
    private static final String OK = "ok";
    private static final String RESULT = "result";
    private static final String STATUS = "status";
    private static final String USERID = "userId";

    private Future<JsonObject> getUserInfos(String userId) {
        JsonObject jo = new JsonObject();
        jo.put(ACTION, "getUserInfos").put(USERID, userId);
        Promise<JsonObject> promise = Promise.promise();
        eb.request("directory", jo, handlerToAsyncHandler(event -> {
            JsonObject res = event.body().getJsonObject(RESULT);
            if (OK.equals(event.body().getString(STATUS)) && res != null) {
                promise.complete(res);
            } else {
                promise.fail("user.not.found");
            }
        }));
        return promise.future();
    }

    @Override
    public void getUser(final AuthCas authCas, final String service, final Handler<User> userHandler) {
        final String userId = authCas.getUser();

        Future<JsonObject> getUserFuture = getUserInfos(userId);

        getUserFuture.onSuccess(data -> {
            User user = new User();
            prepareUser(user, userId, service, data);
            userHandler.handle(user);
            createStatsEvent(authCas, data, service);
        }).onFailure(err -> {
            log.error(String.format("[entcoreCAS@%s::getUserInfos] " +
                    "Failed to get User for SynchroMoodle. %s", this.getClass().getName(), err.getMessage()));
            userHandler.handle(null);
        });
    }

    @Override
    protected void prepareUser(final User user, final String userId, String service, final JsonObject data) {
        if (principalAttributeName != null) {
            user.setUser(data.getString(principalAttributeName));
            data.remove(principalAttributeName);
        } else {
            user.setUser(userId);
        }
        data.remove("password");

        Map<String, String> attributes = new HashMap<>();

        if (data.containsKey(ID)) {
            attributes.put(ID, data.getString(ID));
        }

        if (data.containsKey(LOGIN)) {
            attributes.put(LOGIN, data.getString(LOGIN));
        }

        if (data.containsKey(NAME)) {
            attributes.put(NAME, data.getString(NAME));
        }

        if (data.containsKey(FIRSTNAME)) {
            attributes.put(FIRSTNAME, data.getString(FIRSTNAME));
        }

        if (data.containsKey(LASTNAME)) {
            attributes.put(LASTNAME, data.getString(LASTNAME));
        }

        if (data.containsKey(TYPE)) {
            attributes.put(PROFIL, data.getString(TYPE));
        }

        if (data.containsKey(EMAIL)) {
            attributes.put(EMAIL, data.getString(EMAIL));
        }

        if (data.containsKey(GROUPS)) {
            JsonArray groupsData = data.getJsonArray(GROUPS);
            JsonArray groups = new JsonArray();
            for (Object o : groupsData) {
                JsonObject groupNode = (JsonObject) o;
                log.info("groupNode Test name: " + groupNode.getString("name") + " id: " + groupNode.getString("id") + " subtype: " + groupNode.getString(GROUP_SUBTYPE));
                if (groupNode != null && !"BroadcastGroup".equals(groupNode.getString(GROUP_SUBTYPE))) {
                    UserUtils.groupDisplayName(groupNode, "fr");
                    groups.add(new JsonObject().put(ID, groupNode.getString(ID))
                            .put(NAME, groupNode.getString(NAME)));
                }
            }
            attributes.put(GROUPS_KEY, groups.toString());
        }

        user.setAttributes(attributes);

    }
}


