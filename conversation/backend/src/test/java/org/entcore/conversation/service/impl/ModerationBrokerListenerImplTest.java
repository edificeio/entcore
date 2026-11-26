package org.entcore.conversation.service.impl;

import io.edifice.moderation.events.ModerationEventBrokerListener;
import io.edifice.moderation.v1.DeleteResourceRequest;
import io.edifice.moderation.v1.ModerationResponseStatus;
import io.edifice.moderation.v1.Resource;
import io.edifice.moderation.v1.ResourcesVersionsRequest;
import io.edifice.moderation.v1.ResourcesVersionsResponse;
import io.edifice.moderation.v1.RightsOnResourceRequest;
import org.entcore.common.sql.Sql;
import org.entcore.common.utils.Config;
import org.entcore.test.TestHelper;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.testcontainers.containers.PostgreSQLContainer;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

/**
 * Moderation rights check on conversation messages, against the real schema so that the query itself is covered.
 */
@RunWith(VertxUnitRunner.class)
public class ModerationBrokerListenerImplTest {

    private static final TestHelper test = TestHelper.helper();
    private static final String SCHEMA = "conversation";
    private static final String OWNER = "dddddddd-0000-0000-0000-000000000001";
    private static final String STRANGER = "dddddddd-0000-0000-0000-000000000002";
    private static final String MESSAGE = "eeeeeeee-0000-0000-0000-000000000001";
    private static final String OTHER_MESSAGE = "eeeeeeee-0000-0000-0000-000000000002";
    private static final String UNKNOWN_MESSAGE = "eeeeeeee-0000-0000-0000-000000000003";
    private static final String DELETED_MESSAGE = "eeeeeeee-0000-0000-0000-000000000004";
    private static final String KEPT_MESSAGE = "eeeeeeee-0000-0000-0000-000000000005";
    private static final String BODY = "<p>Bonjour é</p>";
    private static final String OTHER_BODY = "<p>Other</p>";

    @ClassRule
    public static PostgreSQLContainer<?> pgContainer = test.database().createPostgreSQLContainer();

    private static ModerationBrokerListenerImpl listener;
    /** Storage given back per user, as received by the fake workspace quota service. */
    private static final Map<String, Long> released = new ConcurrentHashMap<>();

    @BeforeClass
    public static void setUp(TestContext context) throws Exception {
        // 027-fix-app-rights.sql grants default privileges for this role, which the test container does not have.
        pgContainer.execInContainer("psql", "-U", pgContainer.getUsername(), "-d", pgContainer.getDatabaseName(),
                "-c", "CREATE ROLE \"web-education\"");
        test.database().initPostgreSQL(context, pgContainer, SCHEMA);
        Config.getInstance().setConfig(new JsonObject());
        test.vertx().eventBus().<JsonObject>consumer("org.entcore.workspace.quota", message -> {
            final JsonArray rows = new JsonArray();
            for (Object o : message.body().getJsonArray("users")) {
                final JsonObject user = (JsonObject) o;
                released.merge(user.getString("userId"), user.getLong("size"), Long::sum);
                rows.add(new JsonObject().put("userId", user.getString("userId")).put("storage", 0L).put("notify", false));
            }
            message.reply(new JsonObject().put("status", "ok").put("results", rows));
        });
        // Not started : the tests call the handler directly, without the event bus nor the broker.
        listener = new ModerationBrokerListenerImpl(Sql.getInstance(), test.vertx(),
                ModerationEventBrokerListener.Parameters.builder().application("conversation").build());
    }

    /** Sql is only ready once the class setup has completed, hence an idempotent seed before each test. */
    @Before
    public void seed(TestContext context) {
        final Async async = context.async();
        raw("INSERT INTO conversation.usermessages_v2 (user_id, message_id) SELECT '" + OWNER + "', '" + MESSAGE + "' "
                + "WHERE NOT EXISTS (SELECT 1 FROM conversation.usermessages_v2 WHERE user_id = '" + OWNER + "')")
            .compose(v -> raw("INSERT INTO conversation.messages (id, body, \"from\", \"to\", state, date) VALUES "
                + "('" + MESSAGE + "', '" + BODY + "', '" + OWNER + "', '[]', 'SENT', 1), "
                + "('" + OTHER_MESSAGE + "', '" + OTHER_BODY + "', '" + OWNER + "', '[]', 'SENT', 1) "
                + "ON CONFLICT (id) DO NOTHING"))
            .onSuccess(v -> async.complete())
            .onFailure(context::fail);
    }

    private static Future<Void> raw(String query) {
        final Promise<Void> promise = Promise.promise();
        Sql.getInstance().raw(query, result -> {
            if ("ok".equals(result.body().getString("status"))) {
                promise.complete();
            } else {
                promise.fail(result.body().getString("message", "sql error"));
            }
        });
        return promise.future();
    }

    private static RightsOnResourceRequest request(String userId, String path) {
        return new RightsOnResourceRequest(userId, new Resource(path, null));
    }

    @Test
    public void userWithTheMessageShouldHaveRight(TestContext context) {
        listener.onHasRight(request(OWNER, "/conversation/messages/" + MESSAGE))
            .onComplete(context.asyncAssertSuccess(response -> context.assertTrue(response.isHasRight())));
    }

    @Test
    public void userWithoutTheMessageShouldNotHaveRight(TestContext context) {
        listener.onHasRight(request(STRANGER, "/conversation/messages/" + MESSAGE))
            .onComplete(context.asyncAssertSuccess(response -> context.assertFalse(response.isHasRight())));
    }

    @Test
    public void sqlErrorShouldAnswerNoRightInsteadOfHanging(TestContext context) {
        // A message id that is not a UUID makes the cast fail in the database.
        listener.onHasRight(request(OWNER, "/conversation/messages/not-a-uuid"))
            .onComplete(context.asyncAssertSuccess(response -> context.assertFalse(response.isHasRight())));
    }

    @Test
    public void unsupportedPathShouldFail(TestContext context) {
        listener.onHasRight(request(OWNER, "folder/" + MESSAGE))
            .onComplete(context.asyncAssertFailure());
    }

    @Test
    public void invalidRequestShouldFail(TestContext context) {
        listener.onHasRight(null).onComplete(context.asyncAssertFailure());
        listener.onHasRight(new RightsOnResourceRequest(OWNER, null)).onComplete(context.asyncAssertFailure());
        listener.onHasRight(request(null, "/conversation/messages/" + MESSAGE)).onComplete(context.asyncAssertFailure());
    }

    private static ResourcesVersionsRequest versions(String... paths) {
        return new ResourcesVersionsRequest(Arrays.stream(paths).map(path -> new Resource(path, null)).collect(Collectors.toList()));
    }

    private static Map<String, String> byPath(ResourcesVersionsResponse response) {
        return response.getResources().stream().collect(Collectors.toMap(Resource::getPath, Resource::getVersion));
    }

    private static String sha256Hex(String value) throws Exception {
        final byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        return String.format("%064x", new BigInteger(1, digest));
    }

    @Test
    public void versionsShouldBeTheSha256OfEachBody(TestContext context) throws Exception {
        final String expected = sha256Hex(BODY);
        final String otherExpected = sha256Hex(OTHER_BODY);
        listener.onResourcesVersions(versions("/conversation/messages/" + MESSAGE, "/conversation/messages/" + OTHER_MESSAGE))
            .onComplete(context.asyncAssertSuccess(response -> {
                final Map<String, String> versions = byPath(response);
                context.assertEquals(2, versions.size());
                context.assertEquals(expected, versions.get("/conversation/messages/" + MESSAGE));
                context.assertEquals(otherExpected, versions.get("/conversation/messages/" + OTHER_MESSAGE));
            }));
    }

    @Test
    public void unknownMessagesShouldBeLeftOut(TestContext context) {
        // A message id that is not a UUID is just unknown: ids are compared as text.
        listener.onResourcesVersions(versions("/conversation/messages/" + MESSAGE, "/conversation/messages/" + UNKNOWN_MESSAGE, "/conversation/messages/not-a-uuid"))
            .onComplete(context.asyncAssertSuccess(response ->
                context.assertEquals(Collections.singleton("/conversation/messages/" + MESSAGE), byPath(response).keySet())));
    }

    @Test
    public void versionsOfUnsupportedPathShouldFail(TestContext context) {
        listener.onResourcesVersions(versions("/onversation/message/" + MESSAGE, "/onversation/message/" + MESSAGE))
            .onComplete(context.asyncAssertFailure());
    }

    @Test
    public void invalidVersionsRequestShouldFail(TestContext context) {
        listener.onResourcesVersions(null).onComplete(context.asyncAssertFailure());
        listener.onResourcesVersions(new ResourcesVersionsRequest(null)).onComplete(context.asyncAssertFailure());
        listener.onResourcesVersions(versions()).onComplete(context.asyncAssertFailure());
    }

    private static Future<JsonArray> select(String query) {
        final Promise<JsonArray> promise = Promise.promise();
        Sql.getInstance().raw(query, result -> {
            if ("ok".equals(result.body().getString("status"))) {
                promise.complete(result.body().getJsonArray("results", new JsonArray()));
            } else {
                promise.fail(result.body().getString("message", "sql error"));
            }
        });
        return promise.future();
    }

    private static DeleteResourceRequest delete(String path) {
        return new DeleteResourceRequest(new Resource(path, null));
    }

    @Test
    public void deleteShouldRemoveTheMessageFromEveryBoxAndGiveBackTheStorage(TestContext context) {
        final Async async = context.async();
        released.clear();
        raw("INSERT INTO conversation.usermessages_v2 (user_id, message_id, total_quota) VALUES "
                + "('" + OWNER + "', '" + DELETED_MESSAGE + "', 100), ('" + STRANGER + "', '" + DELETED_MESSAGE + "', 0), "
                + "('" + STRANGER + "', '" + KEPT_MESSAGE + "', 40)")
            .compose(v -> listener.onDeleteResource(delete("/conversation/messages/" + DELETED_MESSAGE)))
            .compose(response -> {
                context.assertEquals(ModerationResponseStatus.OK, response.getStatus());
                return select("SELECT user_id::text, message_id::text FROM conversation.usermessages_v2 "
                        + "WHERE message_id IN ('" + DELETED_MESSAGE + "', '" + KEPT_MESSAGE + "')");
            })
            .onSuccess(rows -> {
                context.assertEquals(new JsonArray().add(new JsonArray().add(STRANGER).add(KEPT_MESSAGE)).encode(), rows.encode(),
                        "Only the other message must stay");
                context.assertEquals(-100L, released.get(OWNER), "The owner gets back the storage of the deleted message");
                context.assertFalse(released.containsKey(STRANGER), "Nothing to give back when the row weighed nothing");
                async.complete();
            })
            .onFailure(context::fail);
    }

    @Test
    public void deleteSqlErrorShouldAnswerErrorInsteadOfHanging(TestContext context) {
        listener.onDeleteResource(delete("/conversation/messages/not-a-uuid"))
            .onComplete(context.asyncAssertSuccess(response -> context.assertEquals(ModerationResponseStatus.ERROR, response.getStatus())));
    }

    @Test
    public void invalidOrUnsupportedDeleteShouldFail(TestContext context) {
        listener.onDeleteResource(null).onComplete(context.asyncAssertFailure());
        listener.onDeleteResource(new DeleteResourceRequest(null)).onComplete(context.asyncAssertFailure());
        listener.onDeleteResource(delete("folder/" + MESSAGE)).onComplete(context.asyncAssertFailure());
    }
}
