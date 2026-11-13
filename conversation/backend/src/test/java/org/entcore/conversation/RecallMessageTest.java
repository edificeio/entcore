package org.entcore.conversation;

import fr.wseduc.transformer.IContentTransformerClient;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.entcore.common.editor.IContentTransformerEventRecorder;
import org.entcore.common.sql.Sql;
import org.entcore.common.user.UserInfos;
import org.entcore.common.utils.Config;
import org.entcore.conversation.service.ConversationService;
import org.entcore.conversation.service.impl.SqlConversationService;
import org.entcore.test.TestHelper;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Recalling a message must update the state copied on every usermessages_v2 link,
 * since folder listings read msg_state instead of messages.state.
 */
@RunWith(VertxUnitRunner.class)
public class RecallMessageTest {
    private static final TestHelper test = TestHelper.helper();
    private static final String SCHEMA = "conversation";
    private static final String SENDER = "dddddddd-0000-0000-0000-000000000001";
    private static final String RECIPIENT = "dddddddd-0000-0000-0000-000000000002";
    /** Sent by SENDER to RECIPIENT, recalled by SENDER. */
    private static final String RECALLED = "eeeeeeee-0000-0000-0000-000000000001";
    /** Sent by SENDER to RECIPIENT, recall attempted by RECIPIENT. */
    private static final String KEPT = "eeeeeeee-0000-0000-0000-000000000002";

    @ClassRule
    public static PostgreSQLContainer<?> pgContainer = test.database().createPostgreSQLContainer();

    private static ConversationService conversationService;
    private static boolean fixturesInserted = false;

    @BeforeClass
    public static void setUp(TestContext context) throws Exception {
        Config.getInstance().setConfig(new JsonObject());
        conversationService = new SqlConversationService(test.vertx(), SCHEMA, IContentTransformerClient.noop,
                IContentTransformerEventRecorder.noop);
        // 027-fix-app-rights.sql grants default privileges for this role, which the test container does not have.
        pgContainer.execInContainer("psql", "-U", pgContainer.getUsername(), "-d", pgContainer.getDatabaseName(),
                "-c", "CREATE ROLE \"web-education\"");
        test.database().initPostgreSQL(context, pgContainer, SCHEMA);
    }

    /** Inserted once, after the asynchronous schema initialisation of setUp has completed. */
    @Before
    public void insertFixturesOnce(TestContext context) {
        if (fixturesInserted) {
            return;
        }
        fixturesInserted = true;
        // Sent now, so that the messages are still within the recall delay.
        final long now = System.currentTimeMillis();
        raw("INSERT INTO conversation.messages (id, body, \"from\", \"to\", state, date) VALUES "
                + "('" + RECALLED + "', 'b', '" + SENDER + "', '[\"" + RECIPIENT + "\"]', 'SENT', " + now + "),"
                + "('" + KEPT + "', 'b', '" + SENDER + "', '[\"" + RECIPIENT + "\"]', 'SENT', " + now + ")")
            .compose(v -> raw("INSERT INTO conversation.usermessages_v2 (user_id, message_id, is_recipient, is_sender, msg_state, msg_date) VALUES "
                + "('" + SENDER + "', '" + RECALLED + "', false, true, 'SENT', " + now + "),"
                + "('" + RECIPIENT + "', '" + RECALLED + "', true, false, 'SENT', " + now + "),"
                + "('" + SENDER + "', '" + KEPT + "', false, true, 'SENT', " + now + "),"
                + "('" + RECIPIENT + "', '" + KEPT + "', true, false, 'SENT', " + now + ")"))
            .onComplete(context.asyncAssertSuccess());
    }

    private static Future<JsonArray> raw(String query) {
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

    private static UserInfos user(String id) {
        final UserInfos user = new UserInfos();
        user.setUserId(id);
        return user;
    }

    /** States of the message, then of its links, in a stable order. */
    private static Future<String> states(String messageId) {
        return raw("SELECT (SELECT state FROM conversation.messages WHERE id = '" + messageId + "') || ':' || "
                + "string_agg(msg_state, ',' ORDER BY user_id) FROM conversation.usermessages_v2 "
                + "WHERE message_id = '" + messageId + "'")
            .map(rows -> rows.getJsonArray(0).getString(0));
    }

    @Test
    public void recall_shouldUpdateTheStateOfEveryLink(TestContext context) {
        conversationService.recallMessage(RECALLED, user(SENDER))
            .compose(v -> states(RECALLED))
            .onComplete(context.asyncAssertSuccess(states ->
                context.assertEquals("RECALL:RECALL,RECALL", states)));
    }

    @Test
    public void recall_byANonSender_shouldFailAndChangeNothing(TestContext context) {
        final io.vertx.ext.unit.Async async = context.async();
        conversationService.recallMessage(KEPT, user(RECIPIENT)).onComplete(recall -> {
            context.assertTrue(recall.failed());
            states(KEPT).onComplete(context.asyncAssertSuccess(states -> {
                context.assertEquals("SENT:SENT,SENT", states);
                async.complete();
            }));
        });
    }
}
