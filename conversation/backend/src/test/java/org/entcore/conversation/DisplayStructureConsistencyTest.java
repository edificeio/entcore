package org.entcore.conversation;

import fr.wseduc.webutils.Either;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.user.UserInfos;
import org.entcore.common.utils.Config;
import org.entcore.conversation.service.ConversationService;
import org.entcore.conversation.service.impl.SqlConversationService;
import org.entcore.test.TestHelper;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.testcontainers.containers.PostgreSQLContainer;

import fr.wseduc.transformer.IContentTransformerClient;
import org.entcore.common.editor.IContentTransformerEventRecorder;

/**
 * Checks that the establishment displayed for a message sender does not depend on the surface it is read
 * from: the message list and the message detail must resolve the very same one for a given reader.
 * <p>
 * The scenario is built so that a surface forgetting to compare the sender's structures with the reader's
 * would return a different, and wrong, establishment — the assertion could not pass by luck.
 */
@RunWith(VertxUnitRunner.class)
public class DisplayStructureConsistencyTest {

    private static final TestHelper test = TestHelper.helper();
    private static final String schema = "conversation";

    @ClassRule
    public static PostgreSQLContainer<?> pgContainer = test.database().createPostgreSQLContainer();

    private static ConversationService conversationService;

    private static final String READER_ID = "reader-consistency";
    private static final String SENDER_ID = "sender-consistency";
    private static final String MESSAGE_ID = "11111111-2222-3333-4444-555555555555";

    /** Shared with the reader, so it must win — even though it is not the sender's preferred one. */
    private static final String SHARED_STRUCTURE_ID = "structure-shared";
    private static final String SHARED_STRUCTURE_NAME = "Collège Jean Moulin";
    /** The sender's preferred structure, which the reader does not belong to. */
    private static final String PREFERRED_STRUCTURE_ID = "structure-preferred";
    private static final String PREFERRED_STRUCTURE_NAME = "Académie de Lyon";

    private static final UserInfos reader = test.directory().generateUser(READER_ID);

    @BeforeClass
    public static void setUp(TestContext context) throws Exception {
        Config.getInstance().setConfig(new JsonObject());
        conversationService = new SqlConversationService(test.vertx(), schema,
                IContentTransformerClient.noop, IContentTransformerEventRecorder.noop);
        stubDirectory();
        // Asynchronous: the Sql instance is only usable once this class setup has completed, hence the
        // message being inserted lazily by the first test rather than here.
        test.database().initPostgreSQL(context, pgContainer, schema);
    }

    private static Future<Void> messageInserted;

    /** Inserts the message once, on first use, and lets every test share that same outcome. */
    private static synchronized Future<Void> messageInserted() {
        if (messageInserted == null) {
            messageInserted = insertMessage();
        }
        return messageInserted;
    }

    /**
     * Minimal stand-in for the directory module. It must answer every action the formatting pipeline
     * issues, not only the new one: an unanswered request would leave the pipeline waiting forever.
     */
    private static void stubDirectory() {
        test.vertx().eventBus().consumer("directory", message -> {
            final JsonObject body = (JsonObject) message.body();
            switch (body.getString("action", "")) {
                case "list-users":
                    message.reply(new JsonObject().put("status", "ok").put("result", new JsonArray()
                            .add(new JsonObject().put("id", SENDER_ID).put("type", "Teacher"))
                            .add(new JsonObject().put("id", READER_ID).put("type", "Relative"))));
                    break;
                case "getGroupsInfos":
                    message.reply(new JsonObject().put("status", "ok").put("result", new JsonArray()));
                    break;
                case "list-users-structures":
                    message.reply(new JsonObject().put("status", "ok").put("result", new JsonArray()
                            // Sender: two structures, prefers the one the reader is NOT part of.
                            .add(new JsonObject()
                                    .put("id", SENDER_ID)
                                    .put("structures", new JsonArray()
                                            .add(structure(PREFERRED_STRUCTURE_ID, PREFERRED_STRUCTURE_NAME))
                                            .add(structure(SHARED_STRUCTURE_ID, SHARED_STRUCTURE_NAME)))
                                    .put("preferredStructureId", PREFERRED_STRUCTURE_ID))
                            .add(new JsonObject()
                                    .put("id", READER_ID)
                                    .put("structures", new JsonArray()
                                            .add(structure(SHARED_STRUCTURE_ID, SHARED_STRUCTURE_NAME))))));
                    break;
                default:
                    message.reply(new JsonObject().put("status", "error").put("message", "Invalid action."));
            }
        });
    }

    private static JsonObject structure(final String id, final String name) {
        return new JsonObject().put("id", id).put("name", name);
    }

    /**
     * Inserts the message straight into the database: going through the draft then send machinery would
     * drag in unrelated dependencies without making the assertion any stronger.
     * {@code content_version} is set so that reading the message does not trigger a body transformation.
     */
    private static Future<Void> insertMessage() {
        final Promise<Void> promise = Promise.promise();
        final String insertMessage =
                "INSERT INTO conversation.messages " +
                "(id, subject, body, \"from\", \"fromName\", \"to\", \"toName\", cc, \"ccName\", cci, \"cciName\", " +
                "\"displayNames\", state, date, language, content_version, \"noReply\") " +
                "VALUES (?, ?, ?, ?, NULL, ?::jsonb, '[]'::jsonb, '[]'::jsonb, '[]'::jsonb, '[]'::jsonb, '[]'::jsonb, " +
                "?::jsonb, 'SENT', ?, 'fr', 1, false)";
        final JsonArray messageValues = new JsonArray()
                .add(MESSAGE_ID)
                .add("Réunion de rentrée")
                .add("<p>Bonjour</p>")
                .add(SENDER_ID)
                .add(new JsonArray().add(READER_ID).encode())
                // Encoded as "id$displayName$groupName$groupSubName"; an empty groupName means a user.
                // The trailing part must not be empty: DecodedDisplayName.decode splits on "$" and demands
                // exactly four parts, while String.split drops trailing empty ones — "id$name$$" would
                // yield two parts and be silently ignored. Its value is unused for users.
                .add(new JsonArray()
                        .add(SENDER_ID + "$Dupont Marie$$-")
                        .add(READER_ID + "$Martin Julien$$-").encode())
                .add(System.currentTimeMillis());

        final String insertUserMessage =
                "INSERT INTO conversation.usermessages (user_id, message_id, folder_id, trashed, unread) " +
                "VALUES (?, ?, NULL, false, true)";
        final JsonArray userMessageValues = new JsonArray().add(READER_ID).add(MESSAGE_ID);

        Sql.getInstance().prepared(insertMessage, messageValues, res -> {
            if (!"ok".equals(res.body().getString("status"))) {
                promise.fail("insert message: " + res.body().getString("message"));
                return;
            }
            Sql.getInstance().prepared(insertUserMessage, userMessageValues, res2 -> {
                if (!"ok".equals(res2.body().getString("status"))) {
                    promise.fail("insert usermessage: " + res2.body().getString("message"));
                    return;
                }
                promise.complete();
            });
        });
        return promise.future();
    }

    private static String displayedStructureName(final JsonObject message) {
        final JsonObject from = message.getJsonObject("from");
        if (from == null) {
            return null;
        }
        final JsonObject displayStructure = from.getJsonObject("displayStructure");
        return displayStructure == null ? null : displayStructure.getString("name");
    }

    private Future<JsonObject> messageFromList() {
        return messageInserted().compose(inserted -> conversationService.listAndFormat("INBOX", false, reader, 0, 25, null, "fr")
                .map(messages -> {
                    for (Object m : messages) {
                        final JsonObject message = (JsonObject) m;
                        if (MESSAGE_ID.equals(message.getString("id"))) {
                            return message;
                        }
                    }
                    return null;
                }));
    }

    private Future<JsonObject> messageFromDetail() {
        return messageInserted()
                .compose(inserted -> conversationService.getAndFormat(MESSAGE_ID, reader, "fr", false, null));
    }

    @Test
    public void listAndDetail_shouldDisplayTheSameStructure(final TestContext context) {
        messageFromList()
                .compose(fromList -> messageFromDetail().map(fromDetail -> new JsonObject()
                        .put("list", displayedStructureName(fromList))
                        .put("detail", displayedStructureName(fromDetail))))
                .onComplete(context.asyncAssertSuccess(names -> {
                    context.assertNotNull(names.getString("list"), "the list returned no establishment");
                    context.assertEquals(names.getString("list"), names.getString("detail"));
                }));
    }

    @Test
    public void bothSurfaces_shouldPreferTheStructureSharedWithTheReader(final TestContext context) {
        // Pins the value itself, not just the equality: two surfaces could agree on the wrong one.
        messageFromList()
                .compose(fromList -> messageFromDetail().map(fromDetail -> new JsonObject()
                        .put("list", displayedStructureName(fromList))
                        .put("detail", displayedStructureName(fromDetail))))
                .onComplete(context.asyncAssertSuccess(names -> {
                    context.assertEquals(SHARED_STRUCTURE_NAME, names.getString("list"));
                    context.assertEquals(SHARED_STRUCTURE_NAME, names.getString("detail"));
                    context.assertNotEquals(PREFERRED_STRUCTURE_NAME, names.getString("list"));
                }));
    }

    @Test
    public void detail_shouldNotDecorateRecipients(final TestContext context) {
        // The field belongs to `from` only; the reader appears in `to` and must stay untouched.
        messageFromDetail().onComplete(context.asyncAssertSuccess(message -> {
            final JsonArray recipients = message.getJsonObject("to").getJsonArray("users");
            for (Object r : recipients) {
                context.assertFalse(((JsonObject) r).containsKey("displayStructure"),
                        "a recipient carries displayStructure: " + r);
            }
        }));
    }
}
