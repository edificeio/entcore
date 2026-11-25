package org.entcore.workspace.service.impl;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.notification.TimelineHelper;
import org.entcore.test.TestHelper;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.testcontainers.containers.Neo4jContainer;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Storage update of several users at once, against a real Neo4j so that the query itself is covered.
 */
@RunWith(VertxUnitRunner.class)
public class DefaultQuotaServiceTest {

    private static final TestHelper test = TestHelper.helper();
    private static final int THRESHOLD = 80;

    @ClassRule
    public static Neo4jContainer<?> neo4jContainer = test.database().createNeo4jContainer();

    private static DefaultQuotaService quotaService;

    @BeforeClass
    public static void setUp(TestContext context) {
        test.database().initNeo4j(context, neo4jContainer);
        quotaService = new DefaultQuotaService(new TimelineHelper(test.vertx(), test.vertx().eventBus(), new JsonObject()));
    }

    private static Future<JsonArray> cypher(String query, JsonObject params) {
        final Promise<JsonArray> promise = Promise.promise();
        Neo4j.getInstance().execute(query, params, event -> {
            if ("ok".equals(event.body().getString("status"))) {
                promise.complete(event.body().getJsonArray("result", new JsonArray()));
            } else {
                promise.fail(event.body().getString("message", "neo4j error"));
            }
        });
        return promise.future();
    }

    private static JsonObject user(String userId, long size) {
        return new JsonObject().put("userId", userId).put("size", size);
    }

    private static Map<String, JsonObject> byUserId(JsonArray rows) {
        return rows.stream().map(JsonObject.class::cast).collect(Collectors.toMap(r -> r.getString("userId"), r -> r));
    }

    @Test
    public void incrementStoragesShouldSumSizesPerUserAndFlagCrossedThreshold(TestContext context) {
        final Async async = context.async();
        cypher("CREATE (:UserBook {userid: 'q-a', storage: 100, quota: 1000, alertSize: false}), "
                + "(:UserBook {userid: 'q-b', storage: 700, quota: 1000, alertSize: false}), "
                + "(:UserBook {userid: 'q-c', storage: 900, quota: 1000, alertSize: true})", new JsonObject())
            .compose(v -> quotaService.incrementStorages(new JsonArray()
                    .add(user("q-a", 10)).add(user("q-a", 5))   // same user twice : summed
                    .add(user("q-b", 200))                      // crosses the threshold
                    .add(user("q-c", -300))                     // goes back under the threshold
                    .add(user("q-unknown", 50))                 // no UserBook : ignored
                    .add(new JsonObject().put("size", 1L)),     // no userId : ignored
                THRESHOLD))
            .onSuccess(rows -> {
                final Map<String, JsonObject> result = byUserId(rows);
                context.assertEquals(3, result.size(), "Only existing users are returned");
                context.assertEquals(115L, result.get("q-a").getLong("storage"));
                context.assertFalse(result.get("q-a").getBoolean("notify"));
                context.assertEquals(900L, result.get("q-b").getLong("storage"));
                context.assertTrue(result.get("q-b").getBoolean("notify"), "Crossing the threshold must notify");
                context.assertEquals(600L, result.get("q-c").getLong("storage"));
                context.assertFalse(result.get("q-c").getBoolean("notify"), "Going back under the threshold must not notify");
                async.complete();
            })
            .onFailure(context::fail);
    }

    @Test
    public void incrementStoragesShouldUpdateEveryUserBeyondOneBatch(TestContext context) {
        final Async async = context.async();
        final int count = DefaultQuotaService.STORAGE_BATCH_SIZE * 2 + 1;
        final JsonArray users = new JsonArray();
        for (int i = 0; i < count; i++) {
            users.add(user("b-" + i, 7));
        }
        cypher("UNWIND {users} AS row CREATE (:UserBook {userid: row.userId, storage: 0, quota: 1000})",
                new JsonObject().put("users", users))
            .compose(v -> quotaService.incrementStorages(users, THRESHOLD))
            .compose(rows -> {
                context.assertEquals(count, rows.size(), "Every batch must be applied and returned");
                return cypher("MATCH (u:UserBook) WHERE u.userid STARTS WITH 'b-' AND u.storage = 7 "
                        + "AND u.__lock__ = 0 RETURN count(u) AS updated", new JsonObject());
            })
            .onSuccess(result -> {
                context.assertEquals((long) count, result.getJsonObject(0).getLong("updated"));
                async.complete();
            })
            .onFailure(context::fail);
    }
}
