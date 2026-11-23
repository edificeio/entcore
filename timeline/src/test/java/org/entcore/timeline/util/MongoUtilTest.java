package org.entcore.timeline.util;

import fr.wseduc.mongodb.MongoDb;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.entcore.test.TestHelper;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.testcontainers.containers.MongoDBContainer;

import java.util.Collections;

/**
 * Runs {@link MongoUtil#aggregateAll} against a real MongoDB to reproduce the 16 MB reply cap.
 */
@RunWith(VertxUnitRunner.class)
public class MongoUtilTest {

	private static final TestHelper test = TestHelper.helper();
	private static final String BIG_COLLECTION = "aggregate_big";
	private static final String SMALL_COLLECTION = "aggregate_small";
	/** ~35 MB in total: the result needs firstBatch plus at least two getMore. */
	private static final int BIG_DOCS = 35;
	private static final String ONE_MB = String.join("", Collections.nCopies(1024 * 1024, "x"));

	@ClassRule
	public static MongoDBContainer mongoContainer = test.database().createMongoContainer();

	@BeforeClass
	public static void setUp(TestContext context) {
		test.database().initMongo(context, mongoContainer);
	}

	private static JsonObject aggregation(String collection, JsonArray pipeline) {
		return new JsonObject()
				.put("aggregate", collection)
				.put("allowDiskUse", true)
				.put("pipeline", pipeline)
				.put("cursor", new JsonObject().put("batchSize", Integer.MAX_VALUE));
	}

	private static Future<Void> insertBigDocs(int index) {
		if (index >= BIG_DOCS) {
			return Future.succeededFuture();
		}
		final Promise<Void> promise = Promise.promise();
		MongoDb.getInstance().insert(BIG_COLLECTION, new JsonObject().put("rank", index).put("payload", ONE_MB), reply -> {
			if (MongoDb.isOk(reply.body())) {
				promise.complete();
			} else {
				promise.fail(reply.body().encode());
			}
		});
		return promise.future().compose(v -> insertBigDocs(index + 1));
	}

	@Test
	public void testAggregateAll_ResultOver16MB_ReturnsEveryDocument(TestContext context) {
		final Async async = context.async();
		final JsonObject aggregation = aggregation(BIG_COLLECTION,
				new JsonArray().add(new JsonObject().put("$match", new JsonObject())));
		insertBigDocs(0).onComplete(context.asyncAssertSuccess(v ->
			// Raw command: proves the reply is truncated and a cursor is left open.
			MongoDb.getInstance().command(aggregation.encode(), reply -> {
				final JsonObject cursor = reply.body().getJsonObject("result").getJsonObject("cursor");
				context.assertTrue(cursor.getJsonArray("firstBatch").size() < BIG_DOCS);
				context.assertNotEquals(0L, cursor.getLong("id"));
				MongoUtil.aggregateAll(MongoDb.getInstance(), aggregation, null)
						.onComplete(context.asyncAssertSuccess(results -> {
							context.assertEquals(BIG_DOCS, results.size());
							async.complete();
						}));
			})));
	}

	@Test
	public void testAggregateAll_SmallResult_ReturnsFirstBatch(TestContext context) {
		final JsonArray docs = new JsonArray().add(new JsonObject().put("v", 1)).add(new JsonObject().put("v", 2));
		MongoDb.getInstance().insert(SMALL_COLLECTION, docs, inserted ->
			MongoUtil.aggregateAll(MongoDb.getInstance(),
					aggregation(SMALL_COLLECTION, new JsonArray().add(new JsonObject().put("$sort", new JsonObject().put("v", 1)))), null)
					.onComplete(context.asyncAssertSuccess(results -> {
						context.assertEquals(2, results.size());
						context.assertEquals(1, results.getJsonObject(0).getInteger("v"));
					})));
	}

	@Test
	public void testAggregateAll_InvalidPipeline_Fails(TestContext context) {
		MongoUtil.aggregateAll(MongoDb.getInstance(),
				aggregation(SMALL_COLLECTION, new JsonArray().add(new JsonObject().put("$unknownStage", 1))), null)
				.onComplete(context.asyncAssertFailure());
	}
}
