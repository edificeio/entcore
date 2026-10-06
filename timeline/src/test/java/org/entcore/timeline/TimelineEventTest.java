/*
 * Copyright © "Open Digital Education", 2019
 *
 * This program is published by "Open Digital Education".
 * You must indicate the name of the software and the company in any production /contribution
 * using the software and indicate on the home page of the software industry in question,
 * "powered by Open Digital Education" with a reference to the website: https://opendigitaleducation.com/.
 *
 * This program is free software, licensed under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation, version 3 of the License.
 *
 * You can redistribute this application and/or modify it since you respect the terms of the GNU Affero General Public License.
 * If you modify the source code and then use this modified source code in your creation, you must make available the source code of your modifications.
 *
 * You should have received a copy of the GNU Affero General Public License along with the software.
 * If not, please see : <http://www.gnu.org/licenses/>. Full compliance requires reading the terms of this license and following its directives.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */
package org.entcore.timeline;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.entcore.common.user.UserInfos;
import org.entcore.test.TestHelper;
import org.entcore.timeline.events.DefaultTimelineEventStore;
import org.entcore.timeline.events.SplitTimelineEventStore;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.testcontainers.containers.MongoDBContainer;

import fr.wseduc.mongodb.MongoDb;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

@RunWith(VertxUnitRunner.class)
public class TimelineEventTest {
    private static final TestHelper test = TestHelper.helper();
    private static final String ME = "receivedOnlyMe";
    private static final String OTHER = "receivedOnlyOther";
    @ClassRule
    public static MongoDBContainer mongoContainer = test.database().createMongoContainer();
    static SplitTimelineEventStore splitStore;
    static DefaultTimelineEventStore store;

    @BeforeClass
    public static void setUp(TestContext context) throws Exception {
        test.database().initMongo(context, mongoContainer);
        store = new DefaultTimelineEventStore();
        splitStore = new SplitTimelineEventStore(store, 5);
    }

    private JsonArray recipients(String prefix, int count) {
        final JsonArray all = new JsonArray();
        for (int i = 0; i < count; i++) {
            all.add(recipient(prefix + i));
        }
        return all;
    }

    private JsonObject recipient(String userId) {
        return recipient(userId, true);
    }

    private JsonObject recipient(String userId, boolean unread) {
        return new JsonObject().put("userId", userId).put("unread", unread ? 1 : 0);
    }

    private JsonObject event(String type, String eventType, JsonArray recipients) {
        final List<String> recipientsIds = recipients.stream().map(JsonObject.class::cast)
                .map(e -> e.getString("userId")).collect(Collectors.toList());
        return new JsonObject().put("type", type).put("event-type", eventType).put("recipients", recipients)
                .put("recipientsIds", recipientsIds).put("params", new JsonObject());
    }

    @Test
    public void testSplitStoreShouldNotSplit(TestContext context) {
        final Async async = context.async();
        final JsonObject event = event("WORKSPACE", "CONTRIB-FOLDER", recipients("user", 4));
        splitStore.setCombineResult(false);
        splitStore.add(event, res0 -> {
            final String id = res0.getString("_id");
            test.database().executeMongoWithUniqueResultById("timeline", id).onComplete(res -> {
                context.assertTrue(res.succeeded());
                final JsonObject json = res.result();
                context.assertEquals(4, json.getJsonArray("recipients").size());
                async.complete();
            });
        });
    }

    @Test
    public void testSplitStoreShouldNotSplitOnLimit(TestContext context) {
        final Async async = context.async();
        final JsonObject event = event("WORKSPACE", "CONTRIB-FOLDER", recipients("user", 5));
        splitStore.setCombineResult(false);
        splitStore.add(event, res0 -> {
            final String id = res0.getString("_id");
            test.database().executeMongoWithUniqueResultById("timeline", id).onComplete(res -> {
                context.assertTrue(res.succeeded());
                final JsonObject json = res.result();
                context.assertEquals(5, json.getJsonArray("recipients").size());
                async.complete();
            });
        });
    }

    @Test
    public void testSplitStoreShouldSplitIn2(TestContext context) {
        final Async async = context.async();
        final JsonObject event = event("WORKSPACE", "CONTRIB-FOLDER", recipients("user", 10));
        final AtomicInteger count = new AtomicInteger();
        splitStore.setCombineResult(false);
        splitStore.add(event, res0 -> {
            final String id = res0.getString("_id");
            test.database().executeMongoWithUniqueResultById("timeline", id).onComplete(res -> {
                context.assertTrue(res.succeeded());
                final JsonObject json = res.result();
                context.assertEquals(5, json.getJsonArray("recipients").size());
                final int value = count.incrementAndGet();
                if (value == 2) {
                    async.complete();
                }
            });
        });
    }

    @Test
    public void testSplitStoreShouldSplitIn3(TestContext context) {
        final Async async = context.async();
        final JsonObject event = event("WORKSPACE", "CONTRIB-FOLDER", recipients("user", 11));
        final AtomicInteger count = new AtomicInteger();
        final List<Integer> results = new ArrayList<>();
        results.add(5);
        results.add(5);
        results.add(1);
        splitStore.setCombineResult(false);
        splitStore.add(event, res0 -> {
            final String id = res0.getString("_id");
            test.database().executeMongoWithUniqueResultById("timeline", id).onComplete(res -> {
                context.assertTrue(res.succeeded());
                final JsonObject json = res.result();
                final int value = count.incrementAndGet();
                final int size = json.getJsonArray("recipients").size();
                boolean removed = results.remove(Integer.valueOf(size));
                context.assertTrue(removed);
                System.out.println(res0);
                if (value == 3) {
                    context.assertEquals(0, results.size());
                    async.complete();
                }
            });
        });
    }

    @Test
    public void testSplitStoreShouldNotSplitAndCombine(TestContext context) {
        final Async async = context.async();
        final JsonObject event = event("WORKSPACE", "CONTRIB-FOLDER", recipients("user", 4));
        splitStore.setCombineResult(true);
        splitStore.add(event, res0 -> {
            final String _ids = res0.getString("_id");
            context.assertNotNull(_ids);
            async.complete();
        });
    }

    @Test
    public void testSplitStoreShouldSplitIn2AndCombine(TestContext context) {
        final Async async = context.async();
        final JsonObject event = event("WORKSPACE", "CONTRIB-FOLDER", recipients("user", 10));
        splitStore.setCombineResult(true);
        splitStore.add(event, res0 -> {
            final JsonArray _ids = res0.getJsonArray("_ids");
            context.assertEquals(2, _ids.size());
            async.complete();
        });
    }

    @Test
    public void testSplitStoreShouldSplitIn3AndCombine(TestContext context) {
        final Async async = context.async();
        final JsonObject event = event("WORKSPACE", "CONTRIB-FOLDER", recipients("user", 11));
        splitStore.setCombineResult(true);
        splitStore.add(event, res0 -> {
            final JsonArray _ids = res0.getJsonArray("_ids");
            context.assertEquals(3, _ids.size());
            async.complete();
        });
    }

    private JsonObject eventFrom(String type, String resource, String sender, JsonArray recipients, boolean preview) {
        final JsonObject event = event(type, "TEST", recipients).put("resource", resource)
                .put("date", MongoDb.offsetFromNow(-60));
        if (sender != null) {
            event.put("sender", sender);
        }
        if (preview) {
            event.put("preview", new JsonObject().put("text", "preview"));
        }
        return event;
    }

    private List<JsonObject> receivedOnlyDataset(String type) {
        return Arrays.asList(
                eventFrom(type, "A", OTHER, new JsonArray().add(recipient(ME)), true),
                eventFrom(type, "B", ME, new JsonArray().add(recipient(OTHER)), true),
                eventFrom(type, "C", ME, new JsonArray().add(recipient(ME)).add(recipient(OTHER)), true),
                eventFrom(type, "D", null, new JsonArray().add(recipient(ME)), false));
    }

    private Future<Void> addAll(List<JsonObject> events) {
        final List<Future<?>> futures = new ArrayList<>();
        for (JsonObject event : events) {
            final Promise<Void> promise = Promise.promise();
            store.add(event, res -> {
                if ("ok".equals(res.getString("status"))) {
                    promise.complete();
                } else {
                    promise.fail(res.encode());
                }
            });
            futures.add(promise.future());
        }
        return Future.all(futures).mapEmpty();
    }

    private Future<List<String>> getResources(String type, boolean receivedOnly) {
        final Promise<List<String>> promise = Promise.promise();
        final UserInfos user = new UserInfos();
        user.setUserId(ME);
        store.get(user, Collections.singletonList(type), 0, 25, null, false, false, receivedOnly, "3.0", res -> {
            if (!"ok".equals(res.getString("status"))) {
                promise.fail(res.encode());
                return;
            }
            promise.complete(res.getJsonArray("results").stream().map(JsonObject.class::cast)
                    .map(notif -> notif.getString("resource")).sorted().collect(Collectors.toList()));
        });
        return promise.future();
    }

    @Test
    public void testGetV3ShouldReturnReceivedAndSentWithPreview(TestContext context) {
        addAll(receivedOnlyDataset("RECEIVEDONLY_DEFAULT"))
                .compose(v -> getResources("RECEIVEDONLY_DEFAULT", false))
                .onComplete(context.asyncAssertSuccess(resources ->
                        context.assertEquals(Arrays.asList("A", "B", "C", "D"), resources)));
    }

    @Test
    public void testGetV3ReceivedOnlyShouldReturnOnlyReceived(TestContext context) {
        addAll(receivedOnlyDataset("RECEIVEDONLY_FILTER"))
                .compose(v -> getResources("RECEIVEDONLY_FILTER", true))
                .onComplete(context.asyncAssertSuccess(resources ->
                        context.assertEquals(Arrays.asList("A", "C", "D"), resources)));
    }
}