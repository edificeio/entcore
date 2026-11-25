package org.entcore.conversation;

import fr.wseduc.transformer.IContentTransformerClient;
import org.entcore.common.editor.IContentTransformerEventRecorder;
import org.entcore.common.sql.Sql;
import org.entcore.common.utils.Config;
import org.entcore.conversation.service.ConversationService;
import org.entcore.conversation.service.impl.SqlConversationService;
import org.entcore.test.TestHelper;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.testcontainers.containers.PostgreSQLContainer;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

/**
 * Messages sent by external applications without sender (POST /externe with an empty "from"): since usermessages_v2,
 * user_id is a UUID, so the message must only be linked to its recipients.
 */
@RunWith(VertxUnitRunner.class)
public class SendWithoutSenderTest {

    private static final TestHelper test = TestHelper.helper();
    private static final String SCHEMA = "conversation";
    private static final String R1 = "aaaaaaaa-0000-0000-0000-000000000001";
    private static final String R2 = "aaaaaaaa-0000-0000-0000-000000000002";

    @ClassRule
    public static PostgreSQLContainer<?> pgContainer = test.database().createPostgreSQLContainer();

    private static ConversationService conversationService;

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

    private static Future<JsonArray> rows(String query) {
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

    /** The payload as built by the controller: display names and Neo4j recipient details merged in. */
    private static JsonObject externalMessage(String subject) {
        return new JsonObject()
                .put("subject", subject)
                .put("body", "<p>Hello</p>")
                .put("to", new JsonArray().add(R1).add(R2))
                .put("cci", new JsonArray())
                .put("noReply", true)
                .put("from", "")
                .put("fromName", "External app")
                .put("displayNames", new JsonArray())
                .put("allUsers", new JsonArray().add(R1).add(R2))
                .put("inactives", new JsonArray())
                .put("undelivered", new JsonArray());
    }

    @Test
    public void messageShouldOnlyBeLinkedToItsRecipients(TestContext context) {
        final Async async = context.async();
        final JsonObject message = externalMessage("only recipients");
        conversationService.sendWithoutSender(message, message.getJsonArray("allUsers"), null)
            .compose(sent -> {
                context.assertNotNull(sent.getString("id"));
                context.assertEquals(sent.getString("id"), sent.getString("thread_id"));
                context.assertEquals("only recipients", sent.getString("subject"));
                return rows("SELECT \"from\", state, \"fromName\" FROM conversation.messages WHERE id = '" + sent.getString("id") + "'")
                    .compose(messages -> {
                        context.assertEquals(new JsonArray().add(new JsonArray().add("").add("SENT").add("External app")), messages);
                        return rows("SELECT user_id::text, is_sender, is_recipient, unread, msg_state FROM conversation.usermessages_v2 "
                                + "WHERE message_id = '" + sent.getString("id") + "' ORDER BY user_id");
                    })
                    .compose(links -> {
                        context.assertEquals(new JsonArray()
                                .add(new JsonArray().add(R1).add(false).add(true).add(true).add("SENT"))
                                .add(new JsonArray().add(R2).add(false).add(true).add(true).add("SENT")), links,
                            "Only the recipients are linked: there is no sender link");
                        return rows("SELECT user_id, nb_unread FROM conversation.userthreads "
                                + "WHERE thread_id = '" + sent.getString("id") + "' ORDER BY user_id");
                    })
                    .compose(userThreads -> {
                        context.assertEquals(new JsonArray()
                                .add(new JsonArray().add(R1).add(1))
                                .add(new JsonArray().add(R2).add(1)), userThreads);
                        return rows("SELECT \"from\" FROM conversation.threads WHERE id = '" + sent.getString("id") + "'");
                    });
            })
            .onSuccess(threads -> {
                context.assertEquals(new JsonArray().add(new JsonArray().add("")), threads);
                async.complete();
            })
            .onFailure(context::fail);
    }

    @Test
    public void senderGivenInThePayloadShouldBeIgnored(TestContext context) {
        final Async async = context.async();
        final JsonObject message = externalMessage("forged sender").put("from", R1).put("state", "DRAFT");
        conversationService.sendWithoutSender(message, new JsonArray().add(R2), null)
            .compose(sent -> rows("SELECT \"from\", state FROM conversation.messages WHERE id = '" + sent.getString("id") + "'"))
            .onSuccess(messages -> {
                context.assertEquals(new JsonArray().add(new JsonArray().add("").add("SENT")), messages);
                async.complete();
            })
            .onFailure(context::fail);
    }
}
