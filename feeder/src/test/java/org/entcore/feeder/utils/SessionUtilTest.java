package org.entcore.feeder.utils;

import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.RunTestOnContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.entcore.common.user.UserUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@RunWith(VertxUnitRunner.class)
public class SessionUtilTest {

    @Rule
    public RunTestOnContext rule = new RunTestOnContext();

    @Test
    public void testRevokeUsersDropsPermanentThenCacheSessionsOfEachUser(final TestContext context) {
        final Async async = context.async();
        final EventBus eb = rule.vertx().eventBus();
        final List<String> calls = new ArrayList<>();
        eb.<JsonObject>consumer(UserUtils.SESSION_ADDRESS, message -> {
            final JsonObject body = message.body();
            calls.add(body.getString("action") + ":" + body.getString("userId"));
            if ("dropPermanentSessions".equals(body.getString("action"))) {
                context.assertNull(body.getString("currentSessionId"), "No session should be kept alive");
                context.assertNull(body.getString("currentTokenId"), "No OAuth2 token should be kept alive");
                context.assertTrue(body.getBoolean("immediate"), "Tokens should be deleted immediately");
            }
            replyOk(message);
            if (calls.size() == 4) {
                context.assertEquals(Arrays.asList(
                        "dropPermanentSessions:user1", "dropCacheSession:user1",
                        "dropPermanentSessions:user2", "dropCacheSession:user2"), calls);
                async.complete();
            }
        });
        SessionUtil.revokeUsers(eb, new JsonArray().add("user1").add("user2"));
    }

    @Test
    public void testRevokeUsersProcessesUsersOneAfterTheOther(final TestContext context) {
        final Async async = context.async();
        final EventBus eb = rule.vertx().eventBus();
        final AtomicInteger inFlight = new AtomicInteger();
        final AtomicInteger maxInFlight = new AtomicInteger();
        final AtomicInteger received = new AtomicInteger();
        eb.<JsonObject>consumer(UserUtils.SESSION_ADDRESS, message -> {
            maxInFlight.accumulateAndGet(inFlight.incrementAndGet(), Math::max);
            received.incrementAndGet();
            // Slow session module : a parallel revocation would send the next request before this reply
            rule.vertx().setTimer(50L, t -> {
                inFlight.decrementAndGet();
                replyOk(message);
                if (received.get() == 6) {
                    context.assertEquals(1, maxInFlight.get(), "Session module should receive one request at a time");
                    async.complete();
                }
            });
        });
        SessionUtil.revokeUsers(eb, new JsonArray().add("user1").add("user2").add("user3"));
    }

    @Test
    public void testRevokeUsersGoesOnWhenSessionModuleFails(final TestContext context) {
        final Async async = context.async();
        final EventBus eb = rule.vertx().eventBus();
        final List<String> calls = new ArrayList<>();
        eb.<JsonObject>consumer(UserUtils.SESSION_ADDRESS, message -> {
            final JsonObject body = message.body();
            calls.add(body.getString("action") + ":" + body.getString("userId"));
            if ("user1".equals(body.getString("userId"))) {
                message.fail(500, "session module failure");
            } else {
                replyOk(message);
            }
            if (calls.size() == 4) {
                context.assertEquals(Arrays.asList(
                        "dropPermanentSessions:user1", "dropCacheSession:user1",
                        "dropPermanentSessions:user2", "dropCacheSession:user2"), calls);
                async.complete();
            }
        });
        SessionUtil.revokeUsers(eb, new JsonArray().add("user1").add("user2"));
    }

    private static void replyOk(final Message<JsonObject> message) {
        message.reply(new JsonObject().put("status", "ok").put("dropped", new JsonArray()));
    }

}
