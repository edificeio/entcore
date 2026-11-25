package org.entcore.conversation.util;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Batched storage update : one request to the workspace quota service, then one session refresh per updated user.
 */
@RunWith(VertxUnitRunner.class)
public class QuotaUtilTest {

    private Vertx vertx;
    private final List<JsonObject> quotaRequests = new CopyOnWriteArrayList<>();
    private final List<JsonObject> sessionUpdates = new CopyOnWriteArrayList<>();

    @Before
    public void setUp() {
        vertx = Vertx.vertx();
        vertx.eventBus().<JsonObject>consumer("wse.session", message -> {
            sessionUpdates.add(message.body());
            message.reply(new JsonObject().put("status", "ok"));
        });
    }

    @After
    public void tearDown(TestContext context) {
        vertx.close(context.asyncAssertSuccess());
    }

    private void quotaServiceAnswers(JsonObject reply) {
        vertx.eventBus().<JsonObject>consumer(QuotaUtil.QUOTA_BUS_ADDRESS, message -> {
            quotaRequests.add(message.body());
            message.reply(reply);
        });
    }

    @Test
    public void shouldSendOneRequestWithoutZeroSizesAndRefreshSessions(TestContext context) {
        final Async async = context.async();
        quotaServiceAnswers(new JsonObject().put("status", "ok").put("results", new JsonArray()
                .add(new JsonObject().put("userId", "u1").put("storage", 110L).put("notify", true))
                .add(new JsonObject().put("userId", "u2").put("storage", 20L).put("notify", false))));
        final Map<String, Long> sizes = new HashMap<>();
        sizes.put("u1", 10L);
        sizes.put("u2", -5L);
        sizes.put("u3", 0L);
        QuotaUtil.updateUsersQuota(vertx.eventBus(), sizes, 80).onComplete(context.asyncAssertSuccess(rows -> {
            context.assertEquals(2, rows.size(), "Rows of the quota service are handed to the caller");
            context.assertEquals(1, quotaRequests.size(), "A single request for every user");
            final JsonObject request = quotaRequests.get(0);
            context.assertEquals("updateUsersQuota", request.getString("action"));
            context.assertEquals(80, request.getInteger("threshold"));
            context.assertEquals(2, request.getJsonArray("users").size(), "Zero sizes are not sent");
            // Session updates are fire and forget : wait for them.
            vertx.setPeriodic(20, id -> {
                if (sessionUpdates.size() == 2) {
                    vertx.cancelTimer(id);
                    sessionUpdates.forEach(update -> context.assertEquals("storage", update.getString("key")));
                    async.complete();
                }
            });
        }));
    }

    @Test
    public void shouldNotCallTheQuotaServiceWhenNothingChanges(TestContext context) {
        quotaServiceAnswers(new JsonObject().put("status", "ok"));
        final Map<String, Long> sizes = new HashMap<>();
        sizes.put("u1", 0L);
        QuotaUtil.updateUsersQuota(vertx.eventBus(), sizes, 80).onComplete(context.asyncAssertSuccess(rows -> {
            context.assertTrue(rows.isEmpty());
            context.assertTrue(quotaRequests.isEmpty());
        }));
    }

    @Test
    public void errorShouldAnswerNoRowsInsteadOfFailing(TestContext context) {
        quotaServiceAnswers(new JsonObject().put("status", "error").put("message", "boom"));
        final Map<String, Long> sizes = new HashMap<>();
        sizes.put("u1", 10L);
        QuotaUtil.updateUsersQuota(vertx.eventBus(), sizes, 80).onComplete(context.asyncAssertSuccess(rows -> {
            context.assertTrue(rows.isEmpty());
            context.assertTrue(sessionUpdates.isEmpty(), "No session refresh without a storage");
        }));
    }
}
