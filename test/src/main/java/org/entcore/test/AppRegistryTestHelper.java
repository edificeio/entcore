package org.entcore.test;

import java.util.UUID;

import io.vertx.core.Promise;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.neo4j.StatementsBuilder;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class AppRegistryTestHelper {
    private final TestHelper test;

    public AppRegistryTestHelper(TestHelper t) {
        this.test = t;
    }

    public Future<Void> createApplicationSystem(String name, JsonArray scopes) {
        final Promise<Void> future = Promise.promise();
        final JsonObject app = new JsonObject().put("displayNameSearchField", name.toLowerCase()).put("name", name)
                .put("displayName", name).put("appType", "SYSTEM").put("scope", scopes);
        final String query = "CREATE (m:Application {props}) RETURN m.id as id";
        final StatementsBuilder b = new StatementsBuilder().add(query, new JsonObject().put("props", app));
        Neo4j.getInstance().executeTransaction(b.build(), null, true, m -> {
            JsonArray results = m.body().getJsonArray("results");
            if ("ok".equals(m.body().getString("status")) && results != null) {
                future.complete();
            } else {
                future.fail("Failed to create application: " + m.body());
            }
        });
        return future.future();
    }

    public Future<String> createRole(String name, String structureId) {
        final String id = UUID.randomUUID().toString();
        final JsonObject app = new JsonObject().put("id", id).put("name", name);
        if (structureId != null) {
            app.put("structureId", structureId);
        }
        final String query = "CREATE (m:Role {props}) RETURN m.id as id";
        final StatementsBuilder b = new StatementsBuilder().add(query, new JsonObject().put("props", app));
        final Promise<String> future = Promise.promise();
        Neo4j.getInstance().executeTransaction(b.build(), null, true, m -> {
            JsonArray results = m.body().getJsonArray("results");
            if ("ok".equals(m.body().getString("status")) && results != null) {
                future.complete(id);
            } else {
                future.fail("Failed to create role: " + m.body());
            }
        });
        return future.future();
    }

    public Future<String> createActionWorkflow(String name, String applicationid) {
        return createAction(name, applicationid, "SECURED_ACTION_WORKFLOW");
    }

    public Future<String> createAction(String name, String applicationid, String type) {
        final String id = UUID.randomUUID().toString();
        final JsonObject app = new JsonObject().put("id", id).put("name", name).put("type", type).put("appId",
                applicationid);
        final String query = "MATCH (app:Application {id:{appId}}) MERGE (a:Action {id:{id},name:{name},type:{type}})<-[:PROVIDE]-(app) RETURN a,app";
        final StatementsBuilder b = new StatementsBuilder().add(query, app);
        final Promise<String> future = Promise.promise();
        Neo4j.getInstance().executeTransaction(b.build(), null, true, m -> {
            JsonArray results = m.body().getJsonArray("results");
            if ("ok".equals(m.body().getString("status")) && results != null) {
                future.complete(id);
            } else {
                future.fail("Failed to create role: " + m.body());
            }
        });
        return future.future();
    }

    public Future<Void> attachActionToRole(String actionId, String roleId) {
        final JsonObject app = new JsonObject().put("actionId", actionId).put("roleId", roleId);
        final String query = "MATCH (a:Action {id:{actionId}}),(r:Role {id:{roleId}}) MERGE (a)<-[:AUTHORIZE]-(r) RETURN a,r";
        final StatementsBuilder b = new StatementsBuilder().add(query, app);
        final Promise<Void> future = Promise.promise();
        Neo4j.getInstance().executeTransaction(b.build(), null, true, m -> {
            JsonArray results = m.body().getJsonArray("results");
            if ("ok".equals(m.body().getString("status")) && results != null) {
                future.complete();
            } else {
                future.fail("Failed to create role: " + m.body());
            }
        });
        return future.future();
    }

    public Future<String> createApplicationUser(String name, String address, JsonArray scopes) {
        return createApplicationUser(name, address, scopes, null);
    }

    public Future<String> createApplicationUser(String name, String address, JsonArray scopes, String structureId) {
        final String id = UUID.randomUUID().toString();
        final Promise<String> future = Promise.promise();
        final JsonObject app = new JsonObject().put("id", id).put("displayNameSearchField", name.toLowerCase())
                .put("name", name).put("displayName", name).put("appType", "END_USER").put("scope", scopes)
                .put("address", address);
        if (structureId != null) {
            app.put("structureId", structureId);
        }
        final String query = "CREATE (m:Application:External {props}) RETURN m.id as id";
        final StatementsBuilder b = new StatementsBuilder().add(query, new JsonObject().put("props", app));
        Neo4j.getInstance().executeTransaction(b.build(), null, true, m -> {
            JsonArray results = m.body().getJsonArray("results");
            if ("ok".equals(m.body().getString("status")) && results != null) {
                future.complete(id);
            } else {
                future.fail("Failed to create application: " + m.body());
            }
        });
        return future.future();
    }
}