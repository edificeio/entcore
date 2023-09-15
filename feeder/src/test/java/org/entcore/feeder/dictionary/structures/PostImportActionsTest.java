package org.entcore.feeder.dictionary.structures;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.entcore.common.events.EventStoreFactory;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.feeder.utils.TransactionManager;
import org.entcore.test.TestHelper;
import org.entcore.test.noop.NoopEventStoreFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import java.util.concurrent.atomic.AtomicInteger;

@RunWith(VertxUnitRunner.class)
public class PostImportActionsTest {
    private static final TestHelper test = TestHelper.helper();

    private static DuplicateUsers mockDuplicateUsers;

    private static MockedStatic<EventStoreFactory> staticMockEventStoreFactory;
    private static MockedStatic<TransactionManager> staticMockTransactionManager;


    @BeforeClass
    public static void setUp(TestContext context) throws Exception {
        staticMockEventStoreFactory = Mockito.mockStatic(EventStoreFactory.class);
        staticMockEventStoreFactory.when(EventStoreFactory::getFactory).thenReturn(NoopEventStoreFactory.INSTANCE);


        final Neo4j mockNeo4J = Mockito.mock(Neo4j.class);
        staticMockTransactionManager = Mockito.mockStatic(TransactionManager.class);
        staticMockTransactionManager.when(TransactionManager::getNeo4jHelper).thenReturn(mockNeo4J);

        mockDuplicateUsers = new DuplicateUsers(false, true, test.vertx().eventBus());
    }

    @AfterClass
    public static void tearDown() {
        if(!staticMockEventStoreFactory.isClosed()) {
            staticMockEventStoreFactory.close();
        }
        if(!staticMockTransactionManager.isClosed()) {
            staticMockTransactionManager.close();
        }
    }

    /**
     * <h2>Goal</h2>
     * <p>Check that after an import from a source which is supposed to trigger a linkStructure by default, the linkStructure
     * is indeed executed</p>
     */
    @Test
    public void shouldLinkStructuresOnlyDefaultSource(final TestContext testContext) {
        final JsonObject config = getPostImportConfigForSource("AAF");
        final PostImport postImport = new PostImport(test.vertx(), mockDuplicateUsers, config);
        try(MockedStatic<Tenant> tenant = Mockito.mockStatic(Tenant.class)) {
            AtomicInteger nbTimesCalled = new AtomicInteger(0);
            tenant.when(() -> Tenant.linkStructures(any())).then((Answer<Void>) invocation -> {
                nbTimesCalled.incrementAndGet();
                return null;
            });

            final Async async = testContext.async();
            // Do things
            postImport.execute("AAF");
            test.vertx().setTimer(500L, e -> {
                testContext.assertEquals(1, nbTimesCalled.get(), "Post import execution should have triggered a call to Tenant::linkStructures. Has something changed in default values of \"tenant-link-structure-sources\" or maybe postImport::execute did not complete.");
                async.complete();
            });
        }
    }

    /**
     * <h2>Goal</h2>
     * <p>Check that after an import from a source which has been added to the sources supposed to trigger a linkStructure, the linkStructure
     * is indeed executed</p>
     */
    @Test
    public void shouldLinkStructuresOnlyIfSourceInSpecifiedList(final TestContext testContext) {
        final JsonObject config = withAllowedSourceForLinkStructure(
                        getPostImportConfigForSource("MY_SOURCE"), "MY_SOURCE");
        final PostImport postImport = new PostImport(test.vertx(), mockDuplicateUsers, config);
        try(MockedStatic<Tenant> tenant = Mockito.mockStatic(Tenant.class)) {
            AtomicInteger nbTimesCalled = new AtomicInteger(0);
            tenant.when(() -> Tenant.linkStructures(any())).then((Answer<Void>) invocation -> {
                nbTimesCalled.incrementAndGet();
                return null;
            });

            final Async async = testContext.async();
            // Do things
            postImport.execute("MY_SOURCE");
            test.vertx().setTimer(500L, e -> {
                testContext.assertEquals(1, nbTimesCalled.get(), "Post import execution should have triggered a call to Tenant::linkStructures for a source which has been manually specified. Has something changed in default values of \"tenant-link-structure-sources\" or maybe postImport::execute did not complete.");
                async.complete();
            });
        }
    }

    /**
     * <h2>Goal</h2>
     * <p>Check that after an import from a source which is not in the default list of sources to trigger a linkStructure by default,
     * the linkStructure is not executed</p>
     */
    @Test
    public void shouldNotLinkStructuresForANonDefaultSource(final TestContext testContext) {
        final JsonObject config = getPostImportConfigForSource("MANUAL");
        final PostImport postImport = new PostImport(test.vertx(), mockDuplicateUsers, config);
        try(MockedStatic<Tenant> tenant = Mockito.mockStatic(Tenant.class)) {
            AtomicInteger nbTimesCalled = new AtomicInteger(0);
            tenant.when(() -> Tenant.linkStructures(any()))
                    .then((Answer<Void>) invocation -> {
                        nbTimesCalled.incrementAndGet();
                        return null;
                    });

            final Async async = testContext.async();
            // Do things
            postImport.execute("MANUAL");
            test.vertx().setTimer(500L, e -> {
                testContext.assertEquals(0, nbTimesCalled.get(), "Post import execution should " +
                        "not have triggered a call to Tenant::linkStructures. " +
                        "Has something changed in default values of \"tenant-link-structure-sources\" or maybe " +
                        "postImport::execute did not complete.");
                async.complete();
            });
        }
    }













    /**
     * <h2>Goal</h2>
     * <p>Check that after an import from a source which is supposed to trigger a Group::runLinkRules by default,
     * the Group::runLinkRules is indeed executed</p>
     */
    @Test
    public void shouldLinkUserAutoSourcesOnlyDefaultSource(final TestContext testContext) {
        final JsonObject config = withRunLinkRules(getPostImportConfigForSource("AAF"));
        final PostImport postImport = new PostImport(test.vertx(), mockDuplicateUsers, config);
        try(MockedStatic<Group> tenant = Mockito.mockStatic(Group.class)) {
            AtomicInteger nbTimesCalled = new AtomicInteger(0);
            tenant.when(Group::runLinkRules).then((Answer<Void>) invocation -> {
                nbTimesCalled.incrementAndGet();
                return null;
            });

            final Async async = testContext.async();
            // Do things
            postImport.execute("AAF");
            test.vertx().setTimer(500L, e -> {
                testContext.assertEquals(1, nbTimesCalled.get(), "Post import execution should have triggered a call to Group::runLinkRules. Has something changed in default values of \"manual-group-link-users-auto\" or maybe postImport::execute did not complete.");
                async.complete();
            });
        }
    }

    /**
     * <h2>Goal</h2>
     * <p>Check that after an import from a source which has been added to the sources supposed to trigger a Group::runLinkRules,
     * the Group::runLinkRules is indeed executed</p>
     */
    @Test
    public void shouldLinkUserAutoSourcesOnlyIfSourceInSpecifiedList(final TestContext testContext) {
        final JsonObject config = withRunLinkRules(
                withAllowedSourceForAutoSources(
                        getPostImportConfigForSource("MY_SOURCE"), "MY_SOURCE"));
        final PostImport postImport = new PostImport(test.vertx(), mockDuplicateUsers, config);
        try(MockedStatic<Group> tenant = Mockito.mockStatic(Group.class)) {
            AtomicInteger nbTimesCalled = new AtomicInteger(0);
            tenant.when(Group::runLinkRules).then((Answer<Void>) invocation -> {
                nbTimesCalled.incrementAndGet();
                return null;
            });

            final Async async = testContext.async();
            // Do things
            postImport.execute("MY_SOURCE");
            test.vertx().setTimer(500L, e -> {
                testContext.assertEquals(1, nbTimesCalled.get(), "Post import execution should " +
                        "have triggered a call to Group::runLinkRules for a source which has been " +
                        "manually specified. Has something changed in default values of " +
                        "\"manual-group-link-users-auto-sources\" or maybe postImport::execute did not complete.");
                async.complete();
            });
        }
    }

    /**
     * <h2>Goal</h2>
     * <p>Check that after an import from a source which is not in the default list of sources to trigger a Group::runLinkRules,
     * the Group.runLinkRules is not executed</p>
     */
    @Test
    public void shouldNotLinkUserAutoSourcesForANonDefaultSource(final TestContext testContext) {
        final JsonObject config = withRunLinkRules(getPostImportConfigForSource("MANUAL"));
        final PostImport postImport = new PostImport(test.vertx(), mockDuplicateUsers, config);
        try(MockedStatic<Group> tenant = Mockito.mockStatic(Group.class)) {
            AtomicInteger nbTimesCalled = new AtomicInteger(0);
            tenant.when(Group::runLinkRules).then((Answer<Void>) invocation -> {
                nbTimesCalled.incrementAndGet();
                return null;
            });

            final Async async = testContext.async();
            // Do things
            postImport.execute("MANUAL");
            test.vertx().setTimer(500L, e -> {
                testContext.assertEquals(0, nbTimesCalled.get(), "Post import execution should " +
                        "not have triggered a call to Group::runLinkRules. " +
                        "Has something changed in default values of \"manual-group-link-users-auto-sources\" or maybe " +
                        "postImport::execute did not complete.");
                async.complete();
            });
        }
    }


    public static  JsonObject getPostImportConfigForSource(final String source) {
        return new JsonObject()
                .put("notify-apps-after-import", false)
                .put("manual-group-link-users-auto", false)
                .put("fix-incorrect-storages", false)
                .put("exclude-mark-duplicates-by-source", new JsonArray().add(source));
    }

    private JsonObject withRunLinkRules(final JsonObject config) {
        return config.put("manual-group-link-users-auto", true);
    }

    public static  JsonObject withAllowedSourceForLinkStructure(final JsonObject config, final String source) {
        config.put("tenant-link-structure-sources", new JsonArray().add(source));
        return config;
    }

    public static JsonObject withAllowedSourceForAutoSources(final JsonObject config, final String source) {
        config.put("manual-group-link-users-auto-sources", new JsonArray().add(source));
        return config;
    }

}
