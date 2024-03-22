package org.entcore.feeder.dictionary.structures;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.entcore.common.events.EventStoreFactory;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.neo4j.TransactionHelper;
import org.entcore.feeder.exceptions.TransactionException;
import org.entcore.feeder.utils.TransactionManager;
import org.entcore.feeder.utils.Validator;
import org.entcore.test.TestHelper;
import org.entcore.test.noop.NoopEventStoreFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.testcontainers.containers.Neo4jContainer;

@RunWith(VertxUnitRunner.class)
public class PostImportFixIncorrectStoragesTest {
    private static final TestHelper test = TestHelper.helper();

    @ClassRule
    public static Neo4jContainer<?> neo4jContainer = test.database().createNeo4jContainer();

    private static Neo4j neo4j;

    private static DuplicateUsers mockDuplicateUsers;

    private static MockedStatic<EventStoreFactory> staticMockEventStoreFactory;


    @BeforeClass
    public static void setUp(TestContext context) throws Exception {
        staticMockEventStoreFactory = Mockito.mockStatic(EventStoreFactory.class);
        staticMockEventStoreFactory.when(EventStoreFactory::getFactory).thenReturn(NoopEventStoreFactory.INSTANCE);

        test.database().initNeo4j(context, neo4jContainer);
        final String base = neo4jContainer.getHttpUrl() + "/db/data/";
        final JsonObject neo4jConfig = new JsonObject()
                .put("server-uri", base).put("poolSize", 1);
        neo4j = Neo4j.getInstance();
        neo4j.init(test.vertx(), neo4jConfig
                .put("server-uri", base)
                .put("ignore-empty-statements-error", false));
        Validator.initLogin(neo4j, test.vertx());
        TransactionManager.getInstance().setNeo4j(neo4j);

        mockDuplicateUsers = new DuplicateUsers(false, true, test.vertx().eventBus());
    }

    @AfterClass
    public static void tearDown() {
        if(!staticMockEventStoreFactory.isClosed()) {
            staticMockEventStoreFactory.close();
        }
    }

    /**
     * <h2>Goal</h2>
     * <p>
     *     Check that after an import from a source which is supposed to fix users storages by default, the fix
     *     is indeed executed.
     * </p>
     * <p>
     *     Note that we just want to test that fixing storages is triggered, not that it does what it is supposed to do.
     * </p>
     */
    @Test
    public void shouldFixIncorrectStoragesIffDefaultSource(final TestContext testContext) {
        final JsonObject config = getPostImportConfigForSource("AAF");
        final PostImport postImport = new PostImport(test.vertx(), mockDuplicateUsers, config);
        final Async async = testContext.async();
        withOneUserBookWithoutQuota().onComplete(testContext.asyncAssertSuccess(h -> {
            postImport.execute("AAF");
            test.vertx().setTimer(500L, e -> {
                checkHasExpectedNumberOfUserBooksWithoutQuota(0)
                        .onSuccess(ev -> async.complete())
                        .onFailure(ev -> testContext.fail("It seems like some storages have not been fixed : " + ev.getMessage()));
            });
        }));
    }


    /**
     * <h2>Goal</h2>
     * <p>
     *     Check that after an import from a source which is supposed to fix users storages by default, the fix
     *     is indeed executed.
     * </p>
     * <p>
     *     Note that we just want to test that fixing storages is triggered, not that it does what it is supposed to do.
     * </p>
     */
    @Test
    public void shouldFixIncorrectStoragesIffSourceSpecified(final TestContext testContext) {
        final JsonObject config = withFixIncorrectStorages(getPostImportConfigForSource("MANUAL_2"), "MANUAL_2");
        final PostImport postImport = new PostImport(test.vertx(), mockDuplicateUsers, config);
        final Async async = testContext.async();
        withOneUserBookWithoutQuota().onComplete(testContext.asyncAssertSuccess(h -> {
            postImport.execute("MANUAL_2");
            test.vertx().setTimer(500L, e -> {
                checkHasExpectedNumberOfUserBooksWithoutQuota(0)
                        .onSuccess(ev -> async.complete())
                        .onFailure(ev -> testContext.fail("It seems like some storages have not been fixed : " + ev.getMessage()));
            });
        }));
    }

    /**
     * <h2>Goal</h2>
     * <p>
     *     Check that after an import from a source which is not supposed to fix users storages by default, the fix
     *     is not executed.
     * </p>
     * <p>
     *     Note that we just want to test that fixing storages is not triggered.
     * </p>
     */
    @Test
    public void shouldNotFixIncorrectStoragesIffDefaultSource(final TestContext testContext) {
        final JsonObject config = getPostImportConfigForSource("MANUAL");
        final PostImport postImport = new PostImport(test.vertx(), mockDuplicateUsers, config);
        final Async async = testContext.async();
        withOneUserBookWithoutQuota().onComplete(testContext.asyncAssertSuccess(h -> {
            postImport.execute("MANUAL");
            test.vertx().setTimer(500L, e -> {
                checkHasExpectedNumberOfUserBooksWithoutQuota(1)
                        .onSuccess(ev -> async.complete())
                        .onFailure(ev -> testContext.fail("It seems like some storages have been fixed but should not have : " + ev.getMessage()));
            });
        }));
    }

    private Future<Void> checkHasExpectedNumberOfUserBooksWithoutQuota(final int expected) {
        final Promise<Void> promise = Promise.promise();
        neo4j.execute("MATCH (ub:UserBook) WHERE NOT(HAS(ub.storage)) return count(ub) as count", new JsonObject(),
        result -> {
            if ("ok".equals(result.body().getString("status"))) {
                final int count = result.body().getJsonArray("result").getJsonObject(0).getInteger("count");
                if(count == expected) {
                    promise.complete();
                } else {
                    promise.fail("Expected " + expected + " userbooks without quota but found " + count);
                }
            } else {
                promise.fail("Could not fetch userbooks from db");
            }
        });
        return promise.future();
    }

    private JsonObject withFixIncorrectStorages(final JsonObject config, final String source) {
        return config.put("fix-incorrect-storages-sources", new JsonArray().add(source));
    }

    private Future<Void> withOneUserBookWithoutQuota() {
        final Promise<Void> promise = Promise.promise();
        TransactionHelper txl = null;
        try {
            txl = TransactionManager.getTransaction();
            txl.add("match (n) detach delete n", new JsonObject());
            txl.add("create (:UserBook{tag: 'unitTest', userid: 'noUserId'})", new JsonObject());
            txl.commit(event -> {
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


    public static  JsonObject getPostImportConfigForSource(final String source) {
        return new JsonObject()
            .put("notify-apps-after-import", false)
            .put("manual-group-link-users-auto", false)
            .put("tenant-link-structure", false)
            .put("fix-incorrect-storages", true)
            .put("exclude-mark-duplicates-by-source", new JsonArray().add(source));
    }

}
