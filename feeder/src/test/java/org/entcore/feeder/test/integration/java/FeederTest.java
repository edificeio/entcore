package org.entcore.feeder.test.integration.java;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.entcore.feeder.Feeder;
import org.entcore.test.TestHelper;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.testcontainers.containers.Neo4jContainer;

@RunWith(VertxUnitRunner.class)
public class FeederTest {
    private static final TestHelper test = TestHelper.helper();
    @ClassRule
    public static Neo4jContainer<?> neo4jContainer = test.database().createNeo4jContainer();
    static {
        System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS %4$s %5$s%6$s%n");
    }

    @BeforeClass
    public static void setUp(TestContext context) throws Exception {
        test.database().initNeo4j(context, neo4jContainer);
        final Feeder feeder = new Feeder();
        final Context context1 = test.vertx().getOrCreateContext();
        final String path = FeederTest.class.getClassLoader().getResource("aaf").getPath();
        final JsonObject config = new JsonObject()
                .put("log-details", true)
                .put("postImport", true)
                .put("apply-communication-rules", true)
                .put("feeder", "AAF")
                .put("import-files", path)
                .put("delete-user-delay", 10000l)
                .put("pre-delete-user-delay", 1000l)
                .put("delete-cron", "0 0 0 * * ? 2099")
                .put("pre-delete-cron", "0 0 0 * * ? 2099");
        context1.config().mergeIn(config);
        feeder.init(test.vertx(), context1);
        feeder.start();
        test.vertx().eventBus().consumer(Feeder.FEEDER_ADDRESS, feeder);
    }

    @Test
    public void testShouldImportAaf(final TestContext context) {
        final Async async = context.async();
        final EventBus eb = test.vertx().eventBus();
        eb.send(Feeder.FEEDER_ADDRESS, new JsonObject().put("action", "import"), (AsyncResult<Message<JsonObject>> message) -> {
            context.assertEquals("ok", message.result().body().getString("status"));
            //wait post import
            test.vertx().setTimer(1000, e->{
                async.complete();
            });
        });
    }
}
