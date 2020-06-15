package org.entcore.test;

import java.util.Map;

import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.neo4j.StatementsBuilder;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;

public class AppRegistryTestHelper {
    private final TestHelper test;

    public AppRegistryTestHelper(TestHelper t) {
        this.test = t;
    }

    public Future<Void> createApplicationSystem(String name, JsonArray scopes) {
        final Future<Void> future = Future.future();
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
        return future;
    }

    public Future<Void> createApplicationUser(String name, String address, JsonArray scopes) {
        final Future<Void> future = Future.future();
        final JsonObject app = new JsonObject().put("displayNameSearchField", name.toLowerCase()).put("name", name)
                .put("displayName", name).put("appType", "END_USER").put("scope", scopes).put("address", address);
        final String query = "CREATE (m:Application:External {props}) RETURN m.id as id";
        final StatementsBuilder b = new StatementsBuilder().add(query, new JsonObject().put("props", app));
        Neo4j.getInstance().executeTransaction(b.build(), null, true, m -> {
            JsonArray results = m.body().getJsonArray("results");
            if ("ok".equals(m.body().getString("status")) && results != null) {
                future.complete();
            } else {
                future.fail("Failed to create application: " + m.body());
            }
        });
        return future;
    }
}