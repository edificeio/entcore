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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

/**
 * Folder listing on usermessages_v2: selection per folder, page order, total `count` on every row,
 * and the `response` / `hasAttachment` flags. Fixtures are inserted in SQL so that the test does not
 * depend on the sending path.
 */
@RunWith(VertxUnitRunner.class)
public class ListMessagesTest {
    private static final TestHelper test = TestHelper.helper();
    private static final String SCHEMA = "conversation";
    private static final String USER = "aaaaaaaa-0000-0000-0000-000000000001";
    private static final String OTHER = "aaaaaaaa-0000-0000-0000-000000000002";
    private static final String FOLDER = "ffffffff-0000-0000-0000-000000000001";
    private static final String THREAD = "thread-1";

    private static final String INBOX_1 = "11111111-0000-0000-0000-000000000001";
    private static final String INBOX_2 = "11111111-0000-0000-0000-000000000002";
    private static final String INBOX_3 = "11111111-0000-0000-0000-000000000003";
    private static final String SENT = "22222222-0000-0000-0000-000000000001";
    private static final String DRAFT = "33333333-0000-0000-0000-000000000001";
    private static final String TRASHED = "44444444-0000-0000-0000-000000000001";
    private static final String IN_FOLDER = "55555555-0000-0000-0000-000000000001";
    private static final String REPLY = "66666666-0000-0000-0000-000000000001";

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
        if (!fixturesInserted) {
            fixturesInserted = true;
            fixtures().onComplete(context.asyncAssertSuccess());
        }
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

    /**
     * USER receives three messages (INBOX_3 the most recent, INBOX_1 already read), sends one, keeps a draft,
     * trashed one and filed one in a folder. USER answered INBOX_3, and INBOX_2 carries an attachment.
     * INBOX_2 and INBOX_3 belong to THREAD.
     */
    private static Future<JsonArray> fixtures() {
        return raw("INSERT INTO conversation.messages (id, body, \"from\", \"to\", state, date, parent_id, thread_id) VALUES "
                + "('" + INBOX_1 + "', 'b', '" + OTHER + "', '[\"" + USER + "\"]', 'SENT', 1000, NULL, NULL),"
                + "('" + INBOX_2 + "', 'b', '" + OTHER + "', '[\"" + USER + "\"]', 'SENT', 2000, NULL, '" + THREAD + "'),"
                + "('" + INBOX_3 + "', 'b', '" + OTHER + "', '[\"" + USER + "\"]', 'SENT', 3000, NULL, '" + THREAD + "'),"
                + "('" + SENT + "', 'b', '" + USER + "', '[\"" + OTHER + "\"]', 'SENT', 4000, NULL, NULL),"
                + "('" + DRAFT + "', 'b', '" + USER + "', '[]', 'DRAFT', 5000, NULL, NULL),"
                + "('" + TRASHED + "', 'b', '" + OTHER + "', '[\"" + USER + "\"]', 'SENT', 6000, NULL, NULL),"
                + "('" + IN_FOLDER + "', 'b', '" + OTHER + "', '[\"" + USER + "\"]', 'SENT', 7000, NULL, NULL),"
                + "('" + REPLY + "', 'b', '" + USER + "', '[\"" + OTHER + "\"]', 'SENT', 8000, '" + INBOX_3 + "', NULL)")
            .compose(v -> raw("INSERT INTO conversation.folders (id, user_id, name) VALUES ('" + FOLDER + "', '" + USER + "', 'kept')"))
            .compose(v -> raw("INSERT INTO conversation.usermessages_v2 "
                + "(user_id, message_id, folder_id, trashed, unread, is_recipient, is_sender, msg_state, msg_date) VALUES "
                + "('" + USER + "', '" + INBOX_1 + "', NULL, false, false, true, false, 'SENT', 1000),"
                + "('" + USER + "', '" + INBOX_2 + "', NULL, false, true, true, false, 'SENT', 2000),"
                + "('" + USER + "', '" + INBOX_3 + "', NULL, false, true, true, false, 'SENT', 3000),"
                + "('" + USER + "', '" + SENT + "', NULL, false, false, false, true, 'SENT', 4000),"
                + "('" + USER + "', '" + DRAFT + "', NULL, false, false, false, true, 'DRAFT', 5000),"
                + "('" + USER + "', '" + TRASHED + "', NULL, true, true, true, false, 'SENT', 6000),"
                + "('" + USER + "', '" + IN_FOLDER + "', '" + FOLDER + "', false, true, true, false, 'SENT', 7000),"
                + "('" + OTHER + "', '" + INBOX_1 + "', NULL, false, false, false, true, 'SENT', 1000)"))
            .compose(v -> raw("INSERT INTO conversation.attachments (id, name, charset, filename, \"contentType\", "
                + "\"contentTransferEncoding\", size) VALUES ('att-1', 'a', 'utf-8', 'a.txt', 'text/plain', '7bit', 1)"))
            .compose(v -> raw("INSERT INTO conversation.usermessagesattachments (user_id, message_id, attachment_id) "
                + "VALUES ('" + USER + "', '" + INBOX_2 + "', 'att-1')"));
    }

    private Future<JsonArray> list(String folder, Boolean unread, int page, int pageSize) {
        final Promise<JsonArray> promise = Promise.promise();
        final UserInfos user = new UserInfos();
        user.setUserId(USER);
        conversationService.list(folder, unread, user, page, pageSize, null, EnumSet.of(ConversationService.State.SENT,
                ConversationService.State.RECALL), res -> {
            if (res.isRight()) {
                promise.complete(res.right().getValue());
            } else {
                promise.fail(res.left().getValue());
            }
        });
        return promise.future();
    }

    private static List<String> ids(JsonArray messages) {
        final List<String> ids = new ArrayList<>();
        messages.forEach(m -> ids.add(((JsonObject) m).getString("id")));
        return ids;
    }

    @Test
    public void inbox_shouldPageByDateDescAndCarryTheTotalOnEachRow(TestContext context) {
        list("INBOX", null, 0, 2)
            .compose(first -> list("INBOX", null, 1, 2).map(second -> new JsonArray().add(first).add(second)))
            .onComplete(context.asyncAssertSuccess(pages -> {
                final JsonArray first = pages.getJsonArray(0);
                final JsonArray second = pages.getJsonArray(1);
                context.assertEquals(Arrays.asList(INBOX_3, INBOX_2), ids(first));
                context.assertEquals(Arrays.asList(INBOX_1), ids(second));
                first.forEach(m -> context.assertEquals(3L, ((JsonObject) m).getLong("count")));
                second.forEach(m -> context.assertEquals(3L, ((JsonObject) m).getLong("count")));
            }));
    }

    @Test
    public void inbox_shouldKeepTheFieldNamesOfTheFormerQuery(TestContext context) {
        list("INBOX", null, 0, 10).onComplete(context.asyncAssertSuccess(messages -> {
            final JsonObject inbox3 = messages.getJsonObject(0);
            context.assertEquals(INBOX_3, inbox3.getString("id"));
            context.assertEquals("SENT", inbox3.getString("state"));
            context.assertEquals(3000L, inbox3.getLong("date"));
            context.assertTrue(inbox3.getBoolean("unread"));
            context.assertTrue(inbox3.getBoolean("response"));
            context.assertFalse(inbox3.getBoolean("hasAttachment"));
            context.assertEquals(new JsonArray().add(USER).encode(), inbox3.getJsonArray("to").encode());

            final JsonObject inbox2 = messages.getJsonObject(1);
            context.assertFalse(inbox2.getBoolean("response"));
            context.assertTrue(inbox2.getBoolean("hasAttachment"));
        }));
    }

    @Test
    public void inbox_unreadFilter_shouldExcludeReadMessages(TestContext context) {
        list("INBOX", true, 0, 10).onComplete(context.asyncAssertSuccess(messages -> {
            context.assertEquals(Arrays.asList(INBOX_3, INBOX_2), ids(messages));
            context.assertEquals(2L, messages.getJsonObject(0).getLong("count"));
        }));
    }

    @Test
    public void systemFolders_shouldSelectOnTheUserMessageFlags(TestContext context) {
        list("OUTBOX", null, 0, 10)
            .compose(outbox -> list("DRAFT", null, 0, 10).map(draft -> new JsonArray().add(outbox).add(draft)))
            .compose(acc -> list("TRASH", null, 0, 10).map(trash -> acc.add(trash)))
            .compose(acc -> list(FOLDER, null, 0, 10).map(folder -> acc.add(folder)))
            .onComplete(context.asyncAssertSuccess(lists -> {
                context.assertEquals(Arrays.asList(SENT), ids(lists.getJsonArray(0)));
                context.assertEquals(Arrays.asList(DRAFT), ids(lists.getJsonArray(1)));
                context.assertEquals(Arrays.asList(TRASHED), ids(lists.getJsonArray(2)));
                context.assertEquals(Arrays.asList(IN_FOLDER), ids(lists.getJsonArray(3)));
            }));
    }

    @Test
    public void emptyFolder_shouldReturnAnEmptyPage(TestContext context) {
        list("INBOX", null, 5, 10).onComplete(context.asyncAssertSuccess(messages ->
            context.assertTrue(messages.isEmpty())));
    }

    private UserInfos user() {
        final UserInfos user = new UserInfos();
        user.setUserId(USER);
        return user;
    }

    private static <T> io.vertx.core.Handler<fr.wseduc.webutils.Either<String, T>> complete(Promise<T> promise) {
        return res -> {
            if (res.isRight()) {
                promise.complete(res.right().getValue());
            } else {
                promise.fail(res.left().getValue());
            }
        };
    }

    @Test
    public void count_shouldMatchTheListedFolders(TestContext context) {
        final Promise<JsonObject> inbox = Promise.promise();
        final Promise<JsonObject> folderUnread = Promise.promise();
        conversationService.count("INBOX", null, null, user(), complete(inbox));
        // User folder + unread: the unread condition adds a parameter, which must follow the user id.
        conversationService.count(FOLDER, "", true, user(), complete(folderUnread));
        Future.all(inbox.future(), folderUnread.future()).onComplete(context.asyncAssertSuccess(done -> {
            context.assertEquals(3L, inbox.future().result().getLong("count"));
            context.assertEquals(1L, folderUnread.future().result().getLong("count"));
        }));
    }

    @Test
    public void listThreads_shouldSumTheUnreadMessagesOfEachThread(TestContext context) {
        final Promise<JsonArray> threads = Promise.promise();
        conversationService.listThreads(user(), 0, complete(threads));
        threads.future().onComplete(context.asyncAssertSuccess(rows -> {
            context.assertEquals(1, rows.size());
            context.assertEquals(THREAD, rows.getJsonObject(0).getString("id"));
            context.assertEquals(2L, rows.getJsonObject(0).getLong("unread"));
        }));
    }

    @Test
    public void listThreadMessages_shouldReturnTheThreadWithItsAttachments(TestContext context) {
        final Promise<JsonArray> messages = Promise.promise();
        conversationService.listThreadMessages(THREAD, 0, user(), complete(messages));
        messages.future().onComplete(context.asyncAssertSuccess(rows -> {
            context.assertEquals(Arrays.asList(INBOX_3, INBOX_2), ids(rows));
            context.assertEquals(0, rows.getJsonObject(0).getJsonArray("attachments").size());
            context.assertEquals(1, rows.getJsonObject(1).getJsonArray("attachments").size());
        }));
    }

    @Test
    public void listThreadMessagesNavigation_shouldReturnTheOlderMessages(TestContext context) {
        final Promise<JsonArray> messages = Promise.promise();
        conversationService.listThreadMessagesNavigation(INBOX_3, true, user(), complete(messages));
        messages.future().onComplete(context.asyncAssertSuccess(rows ->
            context.assertEquals(Arrays.asList(INBOX_2), ids(rows))));
    }
}
