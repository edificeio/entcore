package org.entcore.test;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
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
    private final TestHelper test;

    DirectoryTestHelper(TestHelper t, Vertx v) {
        this.vertx = v;
        this.test = t;
    }

    public DirectoryTestHelper createMock(Handler<Message<JsonObject>> handler) {
        vertx.eventBus().consumer("directory", handler);
        return this;
    }

    public DirectoryTestHelper createMockCommunication(Handler<Message<JsonObject>> handler) {
        vertx.eventBus().consumer("wse.communication.users", handler);
        return this;
    }


    public DirectoryTestHelper mockUserPreferences(final JsonObject pref) {
        vertx.eventBus().consumer("userbook.preferences", (Message<JsonObject> e) ->{
            switch (e.body().getString("action")){
                case "get.currentuser":
                    e.reply(new JsonObject().put("status", "ok").put("value", pref));
                    break;
                default:
                    e.fail(500, "notimplemented");
                    break;
            }
        });
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

    public Future<String> createManualGroup(String name) {
        String id = UUID.randomUUID().toString();
        return createGroup(id, name, "ManualGroup").map(id);
    }

    public Future<String> createProfileGroup(String name) {
        String id = UUID.randomUUID().toString();
        return createGroup(id, name, "ProfileGroup").compose(r -> createProfile(id, name).map(id));
    }

    public Future<String> createFunctionGroup(String name) {
        String id = UUID.randomUUID().toString();
        return createGroup(id, name, "FunctionGroup").map(id);
    }

    public Future<String> createProfile(String profileGroup, String name) {
        final String id = UUID.randomUUID().toString();
        final JsonObject props = new JsonObject().put("pgId", profileGroup).put("name", name).put("id", id);
        final String query = "MATCH (pg:ProfileGroup {id:{pgId}}) MERGE (p:Profile {id:{id},name:{name}}) MERGE (pg)-[:HAS_PROFILE]->(p)";
        final Future<String> future = Future.future();
        Neo4j.getInstance().execute(query, props, message -> {
            if ("ok".equals(message.body().getString("status"))) {
                future.complete(id);
            } else {
                future.fail(message.body().getString("message"));
            }
        });
        return future;
    }

    public Future<String> createStructure(String name, String UAI) {
        return createStructure(name, UAI, "");
    }

    public Future<String> createStructure(String name, String UAI, String externalId) {
        return createStructure(name, UAI, externalId, "AAF");
    }

    public Future<String> createStructure(String name, String UAI, String externalId, String source) {
        final String id = UUID.randomUUID().toString();
        final Future<String> async = Future.future();
        final JsonObject props = new JsonObject().put("name", name).put("UAI", UAI).put("externalId", externalId)
                .put("id", id).put("source", source);
        final JsonObject params = new JsonObject().put("props", props);
        final String query = "CREATE (u:Structure {props}) RETURN u";
        Neo4j neo = Neo4j.getInstance();
        Neo4j.getInstance().execute(query, params, message -> {
            if ("ok".equals(message.body().getString("status"))) {
                async.complete(id);
            } else {
                async.fail(message.body().getString("message"));
            }
        });
        return async;
    }

    public Future<Void> attachUserToGroup(String userId, String groupId) {
        final Future<Void> async = Future.future();
        final String query = "MATCH (u:User{id:{userId}}) WITH u MATCH (g:Group {id:{groupId}}) MERGE (u)-[i:IN]->(g) RETURN u,i,g";
        final JsonObject params = new JsonObject().put("userId", userId).put("groupId", groupId);
        Neo4j.getInstance().execute(query, params, message -> {
            if ("ok".equals(message.body().getString("status"))) {
                async.complete();
            } else {
                async.fail(message.body().getString("message"));
            }
        });
        return async;
    }

    public Future<Void> enableAppForStruct(String id) {
        final Future<Void> async = Future.future();
        final String query = "MATCH (s:Structure {id:{structureId}}) SET s.hasApp=TRUE RETURN s";
        final JsonObject params = new JsonObject().put("structureId", id);
        Neo4j.getInstance().execute(query, params, message -> {
            if ("ok".equals(message.body().getString("status"))) {
                async.complete();
            } else {
                async.fail(message.body().getString("message"));
            }
        });
        return async;
    }

    public Future<Void> attachGroupToStruct(String groupId, String structureId) {
        final Future<Void> async = Future.future();
        final String query = "MATCH (g:Group{id:{groupId}}), (s:Structure {id:{structureId}}) MERGE (g)-[d:DEPENDS]->(s) RETURN g,d,s";
        final JsonObject params = new JsonObject().put("groupId", groupId).put("structureId", structureId);
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
        return createGroup(id, name, null);
    }

    public Future<Void> createGroup(String id, String name, String type) {
        final Future<Void> async = Future.future();
        final String safeType = type == null ? "" : ":" + type;
        final JsonObject params = new JsonObject().put("id", id).put("name", name).put("filter", name)
                .put("displayNameSearchField", name.toLowerCase()).put("users", "BOTH");
        final String query = String.format("CREATE (u:Group:Visible%s {id : {id}, name: {name} }) RETURN u", safeType);
        Neo4j.getInstance().execute(query, params, message -> {
            if ("ok".equals(message.body().getString("status"))) {
                async.complete();
            } else {
                async.fail(message.body().getString("message"));
            }
        });
        return async;
    }

    public Future<String> addAdminLocalFunctionToStructure(String structureId) {
        final String query = "MATCH (s:Structure {id:{structureId}}) MERGE (s)<-[:DEPENDS]-(fg:FunctionGroup {name:'AdminLocal', id: {id}}) ";
        final Future<String> async = Future.future();
        final String id = UUID.randomUUID().toString();
        final JsonObject params = new JsonObject().put("structureId", structureId).put("id", id);
        Neo4j.getInstance().execute(query, params, message -> {
            if ("ok".equals(message.body().getString("status"))) {
                async.complete(id);
            } else {
                async.fail(message.body().getString("message"));
            }
        });
        return async;
    }

    public Future<Void> attachSubstructure(String structureId, String subStructureId) {
        final String query = "MATCH (s:Structure {id:{structureId}}),(subStructure:Structure {id:{subStructureId}}) MERGE (s)<-[:HAS_ATTACHMENT]-(subStructure) ";
        final Future<Void> async = Future.future();
        final JsonObject params = new JsonObject().put("structureId", structureId).put("subStructureId",
                subStructureId);
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

    public Future<Boolean> groupHasRole(String groupId, String roleId) {
        Future<Boolean> future = Future.future();
        test.database().executeNeo4jWithUniqueResult(
                "MATCH (pg:ProfileGroup)-[rel:AUTHORIZED]->(r:Role) WHERE pg.id={groupId} AND r.id={roleId} RETURN COUNT(rel) as nb",
                new JsonObject().put("roleId", roleId).put("groupId", groupId)).setHandler(resCount -> {
                    if (resCount.succeeded()) {
                        future.complete(resCount.result().getInteger("nb").intValue() > 0);
                    } else {
                        future.fail(resCount.cause());
                    }
                });
        return future;
    }

    public Future<Boolean> groupHasRoles(String groupId, String groupType, List<String> roleIds) {
        Promise<Boolean> promise = Promise.promise();
        String query = "MATCH (g:" + groupType + ")-[rel:AUTHORIZED]->(r:Role) WHERE g.id={groupId} AND r.id IN {roleIds} RETURN COUNT(rel) as nb";
        JsonObject params = new JsonObject()
                .put("groupId", groupId)
                .put("roleIds", roleIds);
        test.database().executeNeo4jWithUniqueResult(query, params)
            .onComplete(res -> {
                if (res.succeeded()) {
                    promise.complete(res.result().getInteger("nb").intValue() > 0);
                } else {
                    promise.fail(res.cause());
                }
            });
        return promise.future();
    }

    public Future<Boolean> functionGroupHasRole(String groupId, String roleId) {
        Future<Boolean> future = Future.future();
        test.database().executeNeo4jWithUniqueResult(
                "MATCH (pg:FunctionGroup)-[rel:AUTHORIZED]->(r:Role) WHERE pg.id={groupId} AND r.id={roleId} RETURN COUNT(rel) as nb",
                new JsonObject().put("roleId", roleId).put("groupId", groupId)).setHandler(resCount -> {
                    if (resCount.succeeded()) {
                        future.complete(resCount.result().getInteger("nb").intValue() > 0);
                    } else {
                        future.fail(resCount.cause());
                    }
                });
        return future;
    }

    public Future<String> createRole(String name, String nodeType) {
        final String id = UUID.randomUUID().toString();

        final Promise<String> async = Promise.promise();
        final JsonObject props = new JsonObject()
                .put("id", id)
                .put("name", name);
        final JsonObject params = new JsonObject()
                .put("props", props);
        final String query = "CREATE (r:"+nodeType+" {props}) RETURN r";
        Neo4j.getInstance().execute(query, params, message -> {
            if ("ok".equals(message.body().getString("status"))) {
                async.complete(id);
            } else {
                async.fail(message.body().getString("message"));
            }
        });
        return async.future();
    }

    public Future<Void> attachRoleToGroup(String roleId, String groupId) {
        final Promise<Void> async = Promise.promise();

        final String query = "MATCH (r:Role {id:{roleId}}), (g:Group {id:{groupId}}) MERGE (g)-[a:AUTHORIZED]->(r) RETURN g,a,r";
        final JsonObject params = new JsonObject().put("roleId", roleId).put("groupId", groupId);
        Neo4j.getInstance().execute(query, params, message -> {
            if ("ok".equals(message.body().getString("status"))) {
                async.complete();
            } else {
                async.fail(message.body().getString("message"));
            }
        });
        return async.future();
    }

    public Future<Void> setDistributions(String structureId, JsonArray distributions) {
        final Promise<Void> async = Promise.promise();

        final String query = "MATCH (s:Structure {id: {structureId}}) SET s.distributions = {distributions} RETURN s.id as id, s.distributions as distributions";
        final JsonObject params = new JsonObject()
                .put("structureId", structureId)
                .put("distributions", distributions);

        Neo4j.getInstance().execute(query, params, message -> {
            if ("ok".equals(message.body().getString("status"))) {
                async.complete();
            } else {
                async.fail(message.body().getString("message"));
            }
        });
        return async.future();
    }

    public Future<Void> setLevelsOfEducation(String structureId, JsonArray levelsOfEducation) {
        final Promise<Void> async = Promise.promise();

        final String query = "MATCH (s:Structure {id: {structureId}}) SET s.levelsOfEducation = {levelsOfEducation} RETURN s.id as id, s.levelsOfEducation as levelsOfEducation";
        final JsonObject params = new JsonObject()
                .put("structureId", structureId)
                .put("levelsOfEducation", levelsOfEducation);

        Neo4j.getInstance().execute(query, params, message -> {
            if ("ok".equals(message.body().getString("status"))) {
                async.complete();
            } else {
                async.fail(message.body().getString("message"));
            }
        });
        return async.future();
    }

    public Future<Void> setHasApp(String structureId, Boolean hasApp) {
        final Promise<Void> async = Promise.promise();

        final String query = "MATCH (s:Structure {id: {structureId}}) SET s.hasApp = {hasApp} RETURN s.id as id, s.hasApp as hasApp";
        final JsonObject params = new JsonObject()
                .put("structureId", structureId)
                .put("hasApp", hasApp);

        Neo4j.getInstance().execute(query, params, message -> {
            if ("ok".equals(message.body().getString("status"))) {
                async.complete();
            } else {
                async.fail(message.body().getString("message"));
            }
        });
        return async.future();
    }

    public Future<Boolean> structureHasApp(String structureId, Boolean hasApp) {
        Promise<Boolean> async = Promise.promise();
        final String query = "MATCH (s:Structure {id: {structureId}}) WHERE s.hasApp={hasApp} RETURN COUNT(s) as nb";
        final JsonObject params = new JsonObject()
                .put("structureId", structureId)
                .put("hasApp", hasApp);

        test.database().executeNeo4jWithUniqueResult(query, params)
                .onComplete(resCount -> {
                    if (resCount.succeeded()) {
                        async.complete(resCount.result().getInteger("nb").intValue() > 0);
                    } else {
                        async.fail(resCount.cause());
                    }
                });
        return async.future();
    }

    public Future<Boolean> structureHasDistributions(String structureId, JsonArray distributions) {
        Promise<Boolean> async = Promise.promise();
        final String query = "MATCH (s:Structure {id: {structureId}}) WHERE s.distributions={distributions} RETURN COUNT(s) as nb";
        final JsonObject params = new JsonObject()
                .put("structureId", structureId)
                .put("distributions", distributions);

        test.database().executeNeo4jWithUniqueResult(query, params)
                .onComplete(resCount -> {
                    if (resCount.succeeded()) {
                        async.complete(resCount.result().getInteger("nb").intValue() > 0);
                    } else {
                        async.fail(resCount.cause());
                    }
                });
        return async.future();
    }

    public Future<Boolean> structureHasLevelsOfEducation(String structureId, JsonArray levelsOfEducation) {
        Promise<Boolean> async = Promise.promise();
        final String query = "MATCH (s:Structure {id: {structureId}}) WHERE s.levelsOfEducation={levelsOfEducation} RETURN COUNT(s) as nb";
        final JsonObject params = new JsonObject()
                .put("structureId", structureId)
                .put("levelsOfEducation", levelsOfEducation);

        test.database().executeNeo4jWithUniqueResult(query, params)
                .onComplete(resCount -> {
                    if (resCount.succeeded()) {
                        async.complete(resCount.result().getInteger("nb").intValue() > 0);
                    } else {
                        async.fail(resCount.cause());
                    }
                });
        return async.future();
    }
}