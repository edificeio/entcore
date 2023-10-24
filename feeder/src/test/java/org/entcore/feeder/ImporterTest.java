package org.entcore.feeder;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.feeder.aaf.AafFeeder;
import org.entcore.feeder.dictionary.structures.Importer;
import org.entcore.feeder.utils.TransactionManager;
import org.entcore.feeder.utils.Validator;
import org.entcore.test.TestHelper;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.testcontainers.containers.Neo4jContainer;

@RunWith(VertxUnitRunner.class)
public class ImporterTest {
    private static final TestHelper test = TestHelper.helper();
    @ClassRule
    public static Neo4jContainer<?> neo4jContainer = test.database().createNeo4jContainer();

    @BeforeClass
    public static void setUp(TestContext context) throws Exception {
        test.database().initNeo4j(context, neo4jContainer);
        final String base = neo4jContainer.getHttpUrl() + "/db/data/";
        final JsonObject neo4jConfig = new JsonObject()
                .put("server-uri", base).put("poolSize", 1);
        final Neo4j neo4j = Neo4j.getInstance();
        neo4j.init(test.vertx(), neo4jConfig
                .put("server-uri", base)
                .put("ignore-empty-statements-error", false));
        Validator.initLogin(neo4j, test.vertx());
        TransactionManager.getInstance().setNeo4j(neo4j);
    }

    @Test
    public void testShouldImportAAFWithMergeINE(TestContext context) throws Exception {
        final Async async = context.async();
        final String path = getClass().getResource("/aaf").getFile();
        final AafFeeder feeder = new AafFeeder(test.vertx(), path);
        final Importer importer = Importer.getInstance();
        importer.init(Neo4j.getInstance(), test.vertx(), "AAF", "fr", false, false, false, resInit -> {
            context.assertEquals("ok", resInit.body().getString("status"));
            try {
                feeder.launch(importer, res -> {
                    context.assertEquals("ok", res.body().getString("status"));
                    final Async async1 = context.async();
                    final Async async2 = context.async();
                    test.database().executeNeo4j("MATCH (s:Structure) RETURN s", new JsonObject()).onComplete(s->{
                        context.assertTrue(s.succeeded());
                        context.assertEquals(1, s.result().size());
                        System.out.println(s.result().encodePrettily());
                        async1.complete();
                    });
                    test.database().executeNeo4j("MATCH (u:User) RETURN u", new JsonObject()).onComplete(u -> {
                        context.assertTrue(u.succeeded());
                        context.assertEquals(1, u.result().size());
                        System.out.println(u.result().encodePrettily());
                        async2.complete();
                    });
                    async.complete();
                });
            }catch(Exception e){
                context.fail(e);
                async.complete();
            }
        });
    }
}
