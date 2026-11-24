package org.entcore.conversation.service.impl;

import io.edifice.moderation.events.ModerationEventBrokerListener;
import io.edifice.moderation.v1.Resource;
import io.edifice.moderation.v1.ResourcesVersionsRequest;
import io.edifice.moderation.v1.ResourcesVersionsResponse;
import io.edifice.moderation.v1.RightsOnResourceRequest;
import org.entcore.common.sql.Sql;
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
import java.util.stream.Collectors;

import io.vertx.core.Future;
import io.vertx.core.Promise;
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
    private static final String BODY = "<p>Bonjour é</p>";
    private static final String OTHER_BODY = "<p>Other</p>";

    @ClassRule
    public static PostgreSQLContainer<?> pgContainer = test.database().createPostgreSQLContainer();

    private static ModerationBrokerListenerImpl listener;

    @BeforeClass
    public static void setUp(TestContext context) throws Exception {
        // 027-fix-app-rights.sql grants default privileges for this role, which the test container does not have.
        pgContainer.execInContainer("psql", "-U", pgContainer.getUsername(), "-d", pgContainer.getDatabaseName(),
                "-c", "CREATE ROLE \"web-education\"");
        test.database().initPostgreSQL(context, pgContainer, SCHEMA);
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
        listener.onHasRight(request(OWNER, "message/" + MESSAGE))
            .onComplete(context.asyncAssertSuccess(response -> context.assertTrue(response.isHasRight())));
    }

    @Test
    public void userWithoutTheMessageShouldNotHaveRight(TestContext context) {
        listener.onHasRight(request(STRANGER, "message/" + MESSAGE))
            .onComplete(context.asyncAssertSuccess(response -> context.assertFalse(response.isHasRight())));
    }

    @Test
    public void sqlErrorShouldAnswerNoRightInsteadOfHanging(TestContext context) {
        // A message id that is not a UUID makes the cast fail in the database.
        listener.onHasRight(request(OWNER, "message/not-a-uuid"))
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
        listener.onHasRight(request(null, "message/" + MESSAGE)).onComplete(context.asyncAssertFailure());
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
        listener.onResourcesVersions(versions("message/" + MESSAGE, "message/" + OTHER_MESSAGE))
            .onComplete(context.asyncAssertSuccess(response -> {
                final Map<String, String> versions = byPath(response);
                context.assertEquals(2, versions.size());
                context.assertEquals(expected, versions.get("message/" + MESSAGE));
                context.assertEquals(otherExpected, versions.get("message/" + OTHER_MESSAGE));
            }));
    }

    @Test
    public void unknownMessagesShouldBeLeftOut(TestContext context) {
        // A message id that is not a UUID is just unknown: ids are compared as text.
        listener.onResourcesVersions(versions("message/" + MESSAGE, "message/" + UNKNOWN_MESSAGE, "message/not-a-uuid"))
            .onComplete(context.asyncAssertSuccess(response ->
                context.assertEquals(Collections.singleton("message/" + MESSAGE), byPath(response).keySet())));
    }

    @Test
    public void versionsOfUnsupportedPathShouldFail(TestContext context) {
        listener.onResourcesVersions(versions("message/" + MESSAGE, "folder/" + MESSAGE))
            .onComplete(context.asyncAssertFailure());
    }

    @Test
    public void invalidVersionsRequestShouldFail(TestContext context) {
        listener.onResourcesVersions(null).onComplete(context.asyncAssertFailure());
        listener.onResourcesVersions(new ResourcesVersionsRequest(null)).onComplete(context.asyncAssertFailure());
        listener.onResourcesVersions(versions()).onComplete(context.asyncAssertFailure());
    }
}
