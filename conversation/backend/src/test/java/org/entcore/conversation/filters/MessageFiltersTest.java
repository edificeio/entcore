package org.entcore.conversation.filters;

import fr.wseduc.webutils.http.Binding;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.entcore.common.http.filter.ResourcesProvider;
import org.entcore.common.sql.Sql;
import org.entcore.common.user.UserInfos;
import org.entcore.common.utils.Config;
import org.entcore.test.TestHelper;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Message access filters read usermessages_v2: a user may act only on the messages linked to him.
 * The positive cases prove the queries run; a SQL error would also deny, and pass the negative cases alone.
 * VisiblesFilter is only covered on its denial path: granting goes through the Neo4j visibility check.
 */
@RunWith(VertxUnitRunner.class)
public class MessageFiltersTest {
    private static final TestHelper test = TestHelper.helper();
    private static final String SCHEMA = "conversation";
    private static final String USER = "bbbbbbbb-0000-0000-0000-000000000001";
    private static final String OTHER = "bbbbbbbb-0000-0000-0000-000000000002";
    private static final String FOLDER = "folder-of-user";
    /** Sent by OTHER to USER. */
    private static final String RECEIVED = "cccccccc-0000-0000-0000-000000000001";
    /** Sent by USER. */
    private static final String SENT = "cccccccc-0000-0000-0000-000000000002";
    /** Between OTHER and someone else: USER has no link to it. */
    private static final String FOREIGN = "cccccccc-0000-0000-0000-000000000003";

    @ClassRule
    public static PostgreSQLContainer<?> pgContainer = test.database().createPostgreSQLContainer();

    private static boolean fixturesInserted = false;

    @BeforeClass
    public static void setUp(TestContext context) throws Exception {
        Config.getInstance().setConfig(new JsonObject());
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
        raw("INSERT INTO conversation.messages (id, body, \"from\", \"to\", state, date) VALUES "
                + "('" + RECEIVED + "', 'b', '" + OTHER + "', '[\"" + USER + "\"]', 'SENT', 1000),"
                + "('" + SENT + "', 'b', '" + USER + "', '[\"" + OTHER + "\"]', 'SENT', 2000),"
                + "('" + FOREIGN + "', 'b', '" + OTHER + "', '[]', 'SENT', 3000)")
            .compose(v -> raw("INSERT INTO conversation.folders (id, user_id, name) VALUES ('" + FOLDER + "', '" + USER + "', 'f')"))
            .compose(v -> raw("INSERT INTO conversation.usermessages_v2 (user_id, message_id, is_recipient, is_sender, msg_state, msg_date) VALUES "
                + "('" + USER + "', '" + RECEIVED + "', true, false, 'SENT', 1000),"
                + "('" + USER + "', '" + SENT + "', false, true, 'SENT', 2000),"
                + "('" + OTHER + "', '" + RECEIVED + "', false, true, 'SENT', 1000),"
                + "('" + OTHER + "', '" + FOREIGN + "', false, true, 'SENT', 3000)"))
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

    private static UserInfos user() {
        final UserInfos user = new UserInfos();
        user.setUserId(USER);
        return user;
    }

    private static void assertAuthorization(TestContext context, ResourcesProvider filter, HttpServerRequest request, boolean expected) {
        final io.vertx.ext.unit.Async async = context.async();
        filter.authorize(request, (Binding) null, user(), granted -> {
            context.assertEquals(expected, granted);
            async.complete();
        });
    }

    private static HttpServerRequest withParam(String name, String value) {
        return test.http().post("/conversation/test", new JsonObject().put(name, value));
    }

    private static HttpServerRequest withBodyIds(JsonObject params, String... ids) {
        return test.http().post("/conversation/test", params, new JsonObject().put("id", new JsonArray(java.util.Arrays.asList(ids))));
    }

    @Test
    public void messageUser_shouldGrantOnlyALinkedMessage(TestContext context) {
        assertAuthorization(context, new MessageUserFilter(), withParam("id", RECEIVED), true);
        assertAuthorization(context, new MessageUserFilter(), withParam("id", FOREIGN), false);
        assertAuthorization(context, new MessageUserFilter(), withParam("id", "not-a-uuid"), false);
    }

    @Test
    public void multipleMessageUser_shouldGrantOnlyWhenEveryMessageIsLinked(TestContext context) {
        assertAuthorization(context, new MultipleMessageUserFilter(), withBodyIds(new JsonObject(), RECEIVED, SENT), true);
        assertAuthorization(context, new MultipleMessageUserFilter(), withBodyIds(new JsonObject(), RECEIVED, FOREIGN), false);
    }

    @Test
    public void messageOwner_shouldGrantOnlyTheSender(TestContext context) {
        assertAuthorization(context, new MessageOwnerFilter(), withParam("id", SENT), true);
        assertAuthorization(context, new MessageOwnerFilter(), withParam("id", RECEIVED), false);
    }

    @Test
    public void foldersMessages_shouldGrantLinkedMessagesInOwnFolder(TestContext context) {
        assertAuthorization(context, new FoldersMessagesFilter(),
                withBodyIds(new JsonObject().put("folderId", FOLDER), RECEIVED, SENT), true);
        assertAuthorization(context, new FoldersMessagesFilter(),
                withBodyIds(new JsonObject().put("folderId", FOLDER), FOREIGN), false);
    }

    @Test
    public void visibles_shouldDenyAReplyToAnUnlinkedMessage(TestContext context) {
        assertAuthorization(context, new VisiblesFilter(), withParam("In-Reply-To", FOREIGN), false);
    }
}
