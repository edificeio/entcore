package org.entcore.test;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.user.UserInfos;

import fr.wseduc.webutils.security.BCrypt;

import java.util.*;

public class DirectoryTestHelper {
    private final Vertx vertx;

    DirectoryTestHelper(Vertx v) {
        this.vertx = v;
    }

    public DirectoryTestHelper createMock(Handler<Message<JsonObject>> handler) {
        vertx.eventBus().consumer("directory", handler);
        return this;
    }

    public DirectoryTestHelper createMockCommunication(Handler<Message<JsonObject>> handler) {
        vertx.eventBus().consumer("wse.communication.users", handler);
        return this;
    }

    public UserInfos generateUser(String id, String... groupIds) {
        final UserInfos user = new UserInfos();
        user.setChildrenIds(new ArrayList<>());
        user.setUserId(id);
        user.setUsername("Test Test");
        user.setGroupsIds(new ArrayList<>());
        for (String gr : groupIds) {
            user.getGroupsIds().add(gr);
        }
        return user;
    }

    public Future<Void> createActiveUser(UserInfos infos) {
        final Future<Void> async = Future.future();
        final JsonObject params = new JsonObject().put("userid", infos.getUserId());
        final String query = "CREATE (u:User:Visible {id : {userid} }) RETURN u";
        Neo4j.getInstance().execute(query, params, message -> {
            if ("ok".equals(message.body().getString("status"))) {
                async.complete();
            } else {
                async.fail(message.body().getString("message"));
            }
        });
        return async;
    }

    public Future<String> createActiveUser(String login, String password, String email) {
        return createActiveUser(login, null, password, email);
    }

    public Future<String> createActiveUser(String login, String loginAlias, String password, String email) {
        final String id = UUID.randomUUID().toString();
        final Future<String> async = Future.future();
        final JsonObject props = new JsonObject().put("login", login)
                .put("password", BCrypt.hashpw(password, BCrypt.gensalt())).put("email", email).put("id", id);
        if (loginAlias != null) {
            props.put("loginAlias", loginAlias);
        }
        final JsonObject params = new JsonObject().put("props", props);
        final String query = "CREATE (u:User:Visible {props}) RETURN u";
        Neo4j.getInstance().execute(query, params, message -> {
            if ("ok".equals(message.body().getString("status"))) {
                async.complete(id);
            } else {
                async.fail(message.body().getString("message"));
            }
        });
        return async;
    }

    public Future<String> createInactiveUser(String login, String activationCode, String email) {
        return createInactiveUser(login, null, activationCode, email);
    }

    public Future<String> createInactiveUser(String login, String loginAlias, String activationCode, String email) {
        final Future<String> async = Future.future();
        final String id = UUID.randomUUID().toString();
        final JsonObject props = new JsonObject().put("login", login).put("activationCode", activationCode)
                .put("email", email).put("id", id);
        if (loginAlias != null) {
            props.put("loginAlias", loginAlias);
        }
        final JsonObject params = new JsonObject().put("props", props);
        final String query = "CREATE (u:User:Visible {props}) RETURN u";
        Neo4j.getInstance().execute(query, params, message -> {
            if ("ok".equals(message.body().getString("status"))) {
                async.complete(id);
            } else {
                async.fail(message.body().getString("message"));
            }
        });
        return async;
    }

    public Future<Void> createGroup(String id, String name) {
        final Future<Void> async = Future.future();
        final JsonObject params = new JsonObject().put("id", id).put("name", name);
        final String query = "CREATE (u:Group:Visible {id : {id}, name: {name} }) RETURN u";
        Neo4j.getInstance().execute(query, params, message -> {
            if ("ok".equals(message.body().getString("status"))) {
                async.complete();
            } else {
                async.fail(message.body().getString("message"));
            }
        });
        return async;
    }

    public Future<JsonObject> fetchOneUser(String id) {
        final Future<JsonObject> async = Future.future();
        final JsonObject params = new JsonObject().put("userid", id);
        final String query = "MATCH (u:User {id : {userid} }) RETURN u";
        Neo4j.getInstance().execute(query, params, message -> {
            if ("ok".equals(message.body().getString("status"))) {
                final JsonArray r = message.body().getJsonArray("result");
                async.complete(r.getJsonObject(0).getJsonObject("u").getJsonObject("data"));
            } else {
                async.fail(message.body().getString("message"));
            }
        });
        return async;
    }

    public Future<Void> resetUser(String id, String resetCode) {
        final Future<Void> async = Future.future();
        final String query = "MATCH (u:User {id : {id}}) SET u.resetCode={resetCode} RETURN u";
        final JsonObject params = new JsonObject().put("id", id).put("resetCode", resetCode);
        Neo4j.getInstance().execute(query, params, message -> {
            if ("ok".equals(message.body().getString("status"))) {
                async.complete();
            } else {
                async.fail(message.body().getString("message"));
            }
        });
        return async;
    }
}