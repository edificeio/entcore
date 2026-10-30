package org.entcore.feeder.dictionary.structures;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.entcore.common.events.EventStoreFactory;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.neo4j.TransactionHelper;
import org.entcore.common.user.UserUtils;
import org.entcore.feeder.ManualFeeder;
import org.entcore.feeder.exceptions.TransactionException;
import org.entcore.feeder.utils.TransactionManager;
import org.entcore.feeder.utils.Validator;
import org.entcore.test.TestHelper;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.testcontainers.containers.Neo4jContainer;

import java.util.HashSet;
import java.util.Set;

/**
 * A pre-deleted user must be logged out of every client : their sessions and OAuth2 tokens are revoked and their
 * FCM tokens are removed, whichever path pre-deleted them (manual deletion or pre-delete task).
 */
@RunWith(VertxUnitRunner.class)
public class UserPreDeleteTest {
    private static final TestHelper test = TestHelper.helper();
    private static final String MANUAL_DELETE_ADDRESS = "test.manual-delete-user";
    private static final long REVOCATION_TIMEOUT_MS = 20000L;
    @ClassRule
    public static Neo4jContainer<?> neo4jContainer = test.database().createNeo4jContainer();

    private static Neo4j neo4j;

    @BeforeClass
    public static void setUp(TestContext context) throws Exception {
        EventStoreFactory.getFactory().setVertx(test.vertx());
        test.database().initNeo4j(context, neo4jContainer);
        final String base = neo4jContainer.getHttpUrl() + "/db/data/";
        neo4j = Neo4j.getInstance();
        neo4j.init(test.vertx(), new JsonObject()
                .put("server-uri", base)
                .put("poolSize", 1)
                .put("ignore-empty-statements-error", false));
        Validator.initLogin(neo4j, test.vertx());
        TransactionManager.getInstance().setNeo4j(neo4j);
        final ManualFeeder manualFeeder = new ManualFeeder(neo4j, test.vertx().eventBus(), null);
        test.vertx().eventBus().<JsonObject>consumer(MANUAL_DELETE_ADDRESS, manualFeeder::deleteUser);
        prepareData().onComplete(context.asyncAssertSuccess());
    }

    @Test
    public void testManualPreDeleteRevokesSessionsAndFcmTokens(final TestContext context) {
        final Async async = context.async(2);
        expectRevocation(context, "manualUser", async);
        test.vertx().eventBus().<JsonObject>request(MANUAL_DELETE_ADDRESS,
                new JsonObject().put("users", new JsonArray().add("manualUser")), context.asyncAssertSuccess(reply -> {
            context.assertEquals("ok", reply.body().getString("status"), reply.body().encode());
            async.countDown();
        }));
    }

    @Test
    public void testPreDeleteTaskRevokesSessionsAndFcmTokens(final TestContext context) {
        final Async async = context.async(2);
        expectRevocation(context, "aafUser", async);
        new User.PreDeleteTask(0).preDeleteMissingUsersInStructure("STRUCT_PREDELETE", "AAF", new JsonArray(),
                new JsonArray().add("Student"), m -> {
            context.assertEquals("ok", m.body().getString("status"), m.body().encode());
            async.countDown();
        });
    }

    /**
     * Fakes the session module : once both sessions drops have been received for the user, checks that they have
     * been pre-deleted with their FCM tokens removed, then counts down {@code async}.
     */
    private void expectRevocation(final TestContext context, final String userId, final Async async) {
        final EventBus eb = test.vertx().eventBus();
        final Set<String> actions = new HashSet<>();
        final MessageConsumer<JsonObject> sessionModule = eb.consumer(UserUtils.SESSION_ADDRESS);
        final long timeoutId = test.vertx().setTimer(REVOCATION_TIMEOUT_MS,
                t -> context.fail("Sessions and OAuth2 tokens of " + userId + " have not been revoked"));
        sessionModule.handler(message -> {
            final JsonObject body = message.body();
            context.assertEquals(userId, body.getString("userId"), "Only the pre-deleted user should be revoked");
            actions.add(body.getString("action"));
            message.reply(new JsonObject().put("status", "ok").put("dropped", new JsonArray()));
            if (actions.contains("dropPermanentSessions") && actions.contains("dropCacheSession")) {
                test.vertx().cancelTimer(timeoutId);
                sessionModule.unregister();
                checkPreDeletedWithoutFcmTokens(context, userId, async);
            }
        });
    }

    private void checkPreDeletedWithoutFcmTokens(final TestContext context, final String userId, final Async async) {
        final String query =
                "MATCH (u:User {id : {userId}})-[:PREFERS]->(uac:UserAppConf) " +
                "OPTIONAL MATCH (u)-[:IN]->(dg:DeleteGroup) " +
                "RETURN HAS(u.deleteDate) as preDeleted, uac.fcmTokens as fcmTokens, count(dg) as deleteGroups";
        neo4j.execute(query, new JsonObject().put("userId", userId), res -> {
            final JsonArray result = res.body().getJsonArray("result");
            context.assertEquals("ok", res.body().getString("status"), res.body().encode());
            context.assertEquals(1, result.size(), "User should still exist with their preferences");
            final JsonObject user = result.getJsonObject(0);
            context.assertTrue(user.getBoolean("preDeleted"), "User should be pre-deleted");
            context.assertTrue(user.getInteger("deleteGroups") > 0, "User should be in the delete group");
            context.assertNull(user.getValue("fcmTokens"), "FCM tokens should be removed");
            async.countDown();
        });
    }

    private static Future<Void> prepareData() {
        final Promise<Void> promise = Promise.promise();
        try {
            final TransactionHelper tx = TransactionManager.getTransaction();
            tx.add("MERGE (:DeleteGroup)", new JsonObject());
            // Manually created user, pre-deleted from the admin console
            tx.add("CREATE (:User {id: 'manualUser', login: 'manual.user', source: 'MANUAL', profiles: ['Student']})" +
                    "-[:PREFERS]->(:UserAppConf {fcmTokens: ['fcm1', 'fcm2']})", new JsonObject());
            // AAF user who disappeared from their structure, pre-deleted by the task
            tx.add("CREATE (:Structure {id: 'structPreDelete', externalId: 'STRUCT_PREDELETE'})" +
                    "<-[:DEPENDS]-(:Group:ProfileGroup {id: 'pgPreDelete'})" +
                    "<-[:IN]-(:User {id: 'aafUser', externalId: 'aafUserExt', login: 'aaf.user', source: 'AAF', profiles: ['Student']})" +
                    "-[:PREFERS]->(:UserAppConf {fcmTokens: ['fcm3']})", new JsonObject());
            tx.commit(event -> {
                if ("ok".equals(event.body().getString("status"))) {
                    promise.complete();
                } else {
                    promise.fail(event.body().encodePrettily());
                }
            });
        } catch (TransactionException e) {
            promise.fail(e);
        }
        return promise.future();
    }

}
