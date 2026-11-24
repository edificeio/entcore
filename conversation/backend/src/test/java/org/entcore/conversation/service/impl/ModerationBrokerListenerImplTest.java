package org.entcore.conversation.service.impl;

import io.edifice.moderation.events.ModerationEventBrokerListener;
import io.edifice.moderation.v1.Resource;
import io.edifice.moderation.v1.RightsOnResourceRequest;
import org.entcore.common.sql.Sql;
import org.entcore.test.TestHelper;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.testcontainers.containers.PostgreSQLContainer;

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
}
