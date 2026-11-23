package org.entcore.timeline.util;

import fr.wseduc.mongodb.MongoDb;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Checks that a failing getMore fails {@link MongoUtil#aggregateAll} instead of returning a partial result.
 * Uses a fake persistor because a real MongoDB cannot be made to fail mid-cursor deterministically.
 */
@RunWith(VertxUnitRunner.class)
public class MongoUtilGetMoreFailureTest {

	private static final String FAKE_ADDRESS = "test.fake.mongodb.persistor";

	private Vertx vertx;
	private final AtomicInteger getMoreCalls = new AtomicInteger();

	@Before
	public void setUp() {
		vertx = Vertx.vertx();
		vertx.eventBus().<JsonObject>consumer(FAKE_ADDRESS, message -> {
			final JsonObject command = new JsonObject(message.body().getString("command"));
			if (command.containsKey("aggregate")) {
				message.reply(ok(new JsonObject().put("id", 42L).put("firstBatch", new JsonArray().add(new JsonObject()))));
			} else if (getMoreCalls.incrementAndGet() == 1) {
				message.reply(ok(new JsonObject().put("id", 42L).put("nextBatch", new JsonArray().add(new JsonObject()))));
			} else {
				message.reply(new JsonObject().put("status", "error").put("message", "cursor not found"));
			}
		});
		MongoDb.getInstance().init(vertx.eventBus(), FAKE_ADDRESS);
	}

	@After
	public void tearDown(TestContext context) {
		vertx.close(context.asyncAssertSuccess());
	}

	private static JsonObject ok(JsonObject cursor) {
		return new JsonObject().put("status", "ok").put("result", new JsonObject().put("cursor", cursor));
	}

	@Test
	public void testAggregateAll_GetMoreFails_FailsWithoutPartialResult(TestContext context) {
		final JsonObject aggregation = new JsonObject().put("aggregate", "timeline").put("pipeline", new JsonArray());
		MongoUtil.aggregateAll(MongoDb.getInstance(), aggregation, null)
				.onComplete(context.asyncAssertFailure(t -> {
					context.assertEquals(2, getMoreCalls.get());
					context.assertTrue(t.getMessage().contains("after 2 documents"));
				}));
	}
}
