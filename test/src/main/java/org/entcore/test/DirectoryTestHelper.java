package org.entcore.test;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.user.UserInfos;
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

    public Async createActiveUser(TestContext context, UserInfos infos) {
        final Async async = context.async();
        final JsonObject params = new JsonObject().put("userid", infos.getUserId());
        final String query = "CREATE (u:User:Visible {id : {userid} }) RETURN u";
        Neo4j.getInstance().execute(query, params, message -> {
            if ("ok".equals(message.body().getString("status"))) {
                async.complete();
            } else {
                context.fail(message.body().getString("message"));
            }
        });
        return async;
    }

    public Async createGroup(TestContext context, String id, String name) {
        final Async async = context.async();
        final JsonObject params = new JsonObject().put("id", id).put("name", name);
        final String query = "CREATE (u:Group:Visible {id : {id}, name: {name} }) RETURN u";
        Neo4j.getInstance().execute(query, params, message -> {
            if ("ok".equals(message.body().getString("status"))) {
                async.complete();
            } else {
                context.fail(message.body().getString("message"));
            }
        });
        return async;
    }
}