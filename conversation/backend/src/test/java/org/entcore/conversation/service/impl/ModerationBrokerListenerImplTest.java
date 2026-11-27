package org.entcore.conversation.service.impl;

import io.edifice.moderation.events.ModerationEventBrokerListener;
import fr.wseduc.transformer.IContentTransformerClient;
import fr.wseduc.transformer.to.ContentTransformerResponse;
import io.edifice.moderation.v1.CopyResourceRequest;
import io.edifice.moderation.v1.DeleteResourceRequest;
import io.edifice.moderation.v1.ModerationResponseStatus;
import io.edifice.moderation.v1.Resource;
import io.edifice.moderation.v1.ResourcesVersionsRequest;
import io.edifice.moderation.v1.ResourcesVersionsResponse;
import io.edifice.moderation.v1.RightsOnResourceRequest;
import org.entcore.common.editor.IContentTransformerEventRecorder;
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
    private static final String COPIED_MESSAGE = "eeeeeeee-0000-0000-0000-000000000006";
    private static final String UNTRANSFORMABLE_MESSAGE = "eeeeeeee-0000-0000-0000-000000000007";
    private static final String COPIED_BODY = "<p>Reported content</p>";
    private static final String SENDERLESS_MESSAGE = "eeeeeeee-0000-0000-0000-000000000008";
    private static final String HIDDEN_RECIPIENT = "dddddddd-0000-0000-0000-000000000003";
    private static final String GROUP = "group-1";
    /** Body the fake content transformer refuses. */
    private static final String UNTRANSFORMABLE_BODY = "<p>boom</p>";
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
        // The directory completes users and groups : faked with the details the formatting reads.
        test.vertx().eventBus().<JsonObject>consumer("directory", message -> {
            final JsonArray result = new JsonArray();
            if ("list-users".equals(message.body().getString("action"))) {
                result.add(new JsonObject().put("id", STRANGER).put("type", "Student"));
            } else if ("getGroupsInfos".equals(message.body().getString("action"))) {
                result.add(new JsonObject().put("id", GROUP).put("nbUsers", 12).put("type", "ProfileGroup").put("subType", "StructureGroup"));
            }
            message.reply(new JsonObject().put("status", "ok").put("result", result));
        });
        // The transformer is external : faked, it hands back the HTML it received as the tiptap JSON.
        final IContentTransformerClient transformer = request -> UNTRANSFORMABLE_BODY.equals(request.getHtmlContent())
                ? Future.failedFuture("transformer down")
                : Future.succeededFuture(new ContentTransformerResponse(1, null,
                        new JsonObject().put("type", "doc").put("from", request.getHtmlContent()).getMap(), null, request.getHtmlContent(), null));
        final SqlConversationService conversationService = new SqlConversationService(test.vertx(), SCHEMA, transformer,
                IContentTransformerEventRecorder.noop);
        listener = new ModerationBrokerListenerImpl(Sql.getInstance(), conversationService, test.vertx(),
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

    private static CopyResourceRequest copy(String path) {
        return new CopyResourceRequest(new Resource(path, null));
    }

    @Test
    public void copyShouldHandOverTheMessageFormattedAsSeenByItsSender(TestContext context) throws Exception {
        final Async async = context.async();
        final String expectedVersion = sha256Hex(COPIED_BODY);
        // Display names as stored at sending time : id$displayName$groupName$groupDisplayName, blanks as spaces.
        final String displayNames = new JsonArray()
                .add(OWNER + "$Owner Name$ $ ")
                .add(STRANGER + "$Stranger Name$ $ ")
                .add(HIDDEN_RECIPIENT + "$Hidden Name$ $ ")
                .add(GROUP + "$ $Teachers group$ ").encode();
        // No usermessages row : the copy must not depend on the box of whoever reported the message.
        raw("INSERT INTO conversation.messages (id, subject, body, \"from\", \"fromName\", \"to\", cc, cci, \"displayNames\", language, state, date) VALUES "
                + "('" + COPIED_MESSAGE + "', 'Reported', '" + COPIED_BODY + "', '" + OWNER + "', 'Owner Name', "
                + "'[\"" + STRANGER + "\", \"" + GROUP + "\"]', '[]', '[\"" + HIDDEN_RECIPIENT + "\"]', '" + displayNames + "', 'fr', 'SENT', 1)")
            .compose(v -> listener.onCopyResource(copy("/conversation/messages/" + COPIED_MESSAGE)))
            .compose(response -> {
                context.assertEquals("/conversation/messages/" + COPIED_MESSAGE, response.getResource().getPath());
                context.assertEquals(expectedVersion, response.getResource().getVersion());
                context.assertEquals("Reported", response.getTitle());
                context.assertEquals(COPIED_BODY, response.getBody().get("from"), "The body is the transformed stored body");
                context.assertTrue(response.getAttachments().isEmpty(), "Attachments are not handed over in this version");

                final JsonObject from = (JsonObject) response.getMetadata().get("from");
                context.assertEquals(OWNER, from.getString("id"));
                context.assertEquals("Owner Name", from.getString("displayName"));
                final JsonObject to = (JsonObject) response.getMetadata().get("to");
                context.assertEquals(1, to.getJsonArray("users").size());
                final JsonObject user = to.getJsonArray("users").getJsonObject(0);
                context.assertEquals(STRANGER, user.getString("id"));
                context.assertEquals("Stranger Name", user.getString("displayName"));
                context.assertEquals("Student", user.getString("profile"), "Users are completed by the directory");
                context.assertEquals(1, to.getJsonArray("groups").size());
                final JsonObject group = to.getJsonArray("groups").getJsonObject(0);
                context.assertEquals(GROUP, group.getString("id"));
                context.assertNotNull(group.getString("displayName"), "Groups get their formatted name");
                context.assertEquals(12, group.getInteger("size"), "Groups are completed by the directory");
                final JsonObject cc = (JsonObject) response.getMetadata().get("cc");
                context.assertTrue(cc.getJsonArray("users").isEmpty() && cc.getJsonArray("groups").isEmpty());
                final JsonObject cci = (JsonObject) response.getMetadata().get("cci");
                context.assertEquals(HIDDEN_RECIPIENT, cci.getJsonArray("users").getJsonObject(0).getString("id"),
                        "Read as its sender, the copy keeps the cci recipients");
                // Same version as the one listed to moderation, so that it can tell whether the message changed.
                return listener.onResourcesVersions(versions("/conversation/messages/" + COPIED_MESSAGE))
                        .map(versions -> byPath(versions).get("/conversation/messages/" + COPIED_MESSAGE));
            })
            .onSuccess(listedVersion -> {
                context.assertEquals(expectedVersion, listedVersion);
                async.complete();
            })
            .onFailure(context::fail);
    }

    /** POST /externe without from, or a deleted sender : the message is stored with an empty from. */
    @Test
    public void copyOfSenderlessMessageShouldKeepTheExternalNameAndEveryRecipient(TestContext context) {
        final Async async = context.async();
        final String displayNames = new JsonArray().add(STRANGER + "$Stranger Name$ $ ").add(HIDDEN_RECIPIENT + "$Hidden Name$ $ ").encode();
        raw("INSERT INTO conversation.messages (id, subject, body, \"from\", \"fromName\", \"to\", cc, cci, \"displayNames\", state, date) VALUES "
                + "('" + SENDERLESS_MESSAGE + "', 'External', '" + COPIED_BODY + "', '', 'External application', "
                + "'[\"" + STRANGER + "\"]', '[]', '[\"" + HIDDEN_RECIPIENT + "\"]', '" + displayNames + "', 'SENT', 1)")
            .compose(v -> listener.onCopyResource(copy("/conversation/messages/" + SENDERLESS_MESSAGE)))
            .onSuccess(response -> {
                final JsonObject from = (JsonObject) response.getMetadata().get("from");
                context.assertEquals("", from.getString("id"), "No sender account");
                context.assertEquals("External application", from.getString("displayName"));
                context.assertNull(from.getValue("displayStructure"), "No establishment without a sender account");
                final JsonObject to = (JsonObject) response.getMetadata().get("to");
                context.assertEquals(STRANGER, to.getJsonArray("users").getJsonObject(0).getString("id"));
                final JsonObject cci = (JsonObject) response.getMetadata().get("cci");
                context.assertEquals(HIDDEN_RECIPIENT, cci.getJsonArray("users").getJsonObject(0).getString("id"),
                        "Without sender, the copy still keeps the cci recipients");
                async.complete();
            })
            .onFailure(context::fail);
    }

    @Test
    public void copyOfUnknownMessageShouldFail(TestContext context) {
        listener.onCopyResource(copy("/conversation/messages/" + UNKNOWN_MESSAGE))
            .onComplete(context.asyncAssertFailure(th -> context.assertEquals("moderation.resource.not.found", th.getMessage())));
    }

    @Test
    public void copyShouldFailWhenTheTransformationFails(TestContext context) {
        final Async async = context.async();
        raw("INSERT INTO conversation.messages (id, subject, body, \"from\", \"to\", state, date) VALUES "
                + "('" + UNTRANSFORMABLE_MESSAGE + "', 's', '" + UNTRANSFORMABLE_BODY + "', '" + OWNER + "', '[]', 'SENT', 1) "
                + "ON CONFLICT (id) DO NOTHING")
            .compose(v -> listener.onCopyResource(copy("/conversation/messages/" + UNTRANSFORMABLE_MESSAGE)))
            .onSuccess(response -> context.fail("The copy must fail without a body to hand over"))
            .onFailure(th -> {
                context.assertEquals("moderation.copy.error", th.getMessage());
                async.complete();
            });
    }

    @Test
    public void invalidOrUnsupportedCopyShouldFail(TestContext context) {
        listener.onCopyResource(null).onComplete(context.asyncAssertFailure());
        listener.onCopyResource(new CopyResourceRequest(null)).onComplete(context.asyncAssertFailure());
        listener.onCopyResource(copy("folder/" + MESSAGE)).onComplete(context.asyncAssertFailure());
    }
}
