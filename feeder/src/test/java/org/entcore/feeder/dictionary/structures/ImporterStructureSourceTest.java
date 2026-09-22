package org.entcore.feeder.dictionary.structures;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.feeder.csv.CsvFeeder;
import org.entcore.feeder.utils.TransactionManager;
import org.entcore.feeder.utils.Validator;
import org.entcore.test.TestHelper;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.testcontainers.containers.Neo4jContainer;

/**
 * Verify thath strcuture's source from AAF survive a CSV import.
 */
@RunWith(VertxUnitRunner.class)
public class ImporterStructureSourceTest {

    private static final TestHelper test = TestHelper.helper();

    @ClassRule
    public static Neo4jContainer<?> neo4jContainer = test.database().createNeo4jContainer();

    @BeforeClass
    public static void setUp(TestContext context) {
        test.database().initNeo4j(context, neo4jContainer);
        Validator.initLogin(Neo4j.getInstance(), test.vertx());
        TransactionManager.getInstance().setNeo4j(Neo4j.getInstance());
    }

    @Before
    public void resetGraphData() {
        GraphData.clear();
    }

    /** Importer and GraphData are singletons : keep them in memory impact other tests. */
    @AfterClass
    public static void releaseImporter() {
        Importer.getInstance().clear();
        GraphData.clear();
    }

    @Test
    public void csvImportOnAafStructureShouldKeepAafSource(TestContext context) {
        final String externalId = "STRUCT-AAF";
        createStructure(externalId, "Lycee AAF", "AAF")
                .compose(v -> initImporter("CSV"))
                .compose(v -> updateStructure(externalId, "Lycee AAF renomme"))
                .compose(v -> readStructure(externalId))
                .onComplete(context.asyncAssertSuccess(s -> {
                    context.assertEquals("AAF", s.getString("source"));
                    context.assertEquals("Lycee AAF renomme", s.getString("feederName"));
                }));
    }

    @Test
    public void csvImportOnAaf1dStructureShouldKeepAaf1dSource(TestContext context) {
        final String externalId = "STRUCT-AAF1D";
        createStructure(externalId, "Ecole AAF1D", "AAF1D")
                .compose(v -> initImporter("CSV"))
                .compose(v -> updateStructure(externalId, "Ecole AAF1D renommee"))
                .compose(v -> readStructure(externalId))
                .onComplete(context.asyncAssertSuccess(s -> {
                    context.assertEquals("AAF1D", s.getString("source"));
                    context.assertEquals("Ecole AAF1D renommee", s.getString("feederName"));
                }));
    }

    @Test
    public void csvImportOnCsvStructureShouldKeepCsvSource(TestContext context) {
        final String externalId = "STRUCT-CSV";
        createStructure(externalId, "Lycee CSV", "CSV")
                .compose(v -> initImporter("CSV"))
                .compose(v -> updateStructure(externalId, "Lycee CSV renomme"))
                .compose(v -> readStructure(externalId))
                .onComplete(context.asyncAssertSuccess(s -> context.assertEquals("CSV", s.getString("source"))));
    }

    @Test
    public void aafImportOnCsvStructureShouldSetAafSource(TestContext context) {
        final String externalId = "STRUCT-CSV-TO-AAF";
        createStructure(externalId, "Lycee CSV", "CSV")
                .compose(v -> initImporter("AAF"))
                .compose(v -> updateStructure(externalId, "Lycee repris par l'AAF"))
                .compose(v -> readStructure(externalId))
                .onComplete(context.asyncAssertSuccess(s -> context.assertEquals("AAF", s.getString("source"))));
    }

    @Test
    public void csvImportShouldCreateCsvSourcedClassOnAafStructure(TestContext context) {
        final String externalId = "STRUCT-AAF-CSV";
        createStructure(externalId, "LyceeAAF", "AAF")
                .compose(v -> initImporter("CSV"))
                .compose(v -> launchCsvImport(getClass().getResource("/csv-aaf").getFile()))
                .compose(v -> readStructure(externalId))
                .compose(s -> {
                    context.assertEquals("AAF", s.getString("source"));
                    return test.database().executeNeo4jWithUniqueResult(
                            "MATCH (c:Class {externalId: {externalId}}) RETURN c.source as source ",
                            new JsonObject().put("externalId", externalId + "$6emeA"));
                })
                .onComplete(context.asyncAssertSuccess(c -> context.assertEquals("CSV", c.getString("source"))));
    }

    private Future<Void> createStructure(String externalId, String name, String source) {
        final JsonObject params = new JsonObject()
                .put("id", externalId)
                .put("externalId", externalId)
                .put("name", name)
                .put("source", source)
                .put("checksum", "checksum-" + source);
        return test.database().executeNeo4j(
                "CREATE (s:Structure {id: {id}, externalId: {externalId}, name: {name}, feederName: {name}, " +
                        "source: {source}, checksum: {checksum}}) ", params).mapEmpty();
    }

    private Future<Void> initImporter(String source) {
        final Promise<Void> promise = Promise.promise();
        Importer.getInstance().init(Neo4j.getInstance(), test.vertx(), source, "fr", false, false, false, res -> {
            if ("ok".equals(res.body().getString("status"))) {
                promise.complete();
            } else {
                promise.fail(res.body().encode());
            }
        });
        return promise.future();
    }

    private Future<Void> updateStructure(String externalId, String name) {
        final Importer importer = Importer.getInstance();
        importer.createOrUpdateStructure(new JsonObject().put("externalId", externalId).put("name", name));
        final Promise<Void> promise = Promise.promise();
        importer.persist(res -> {
            if ("ok".equals(res.body().getString("status"))) {
                promise.complete();
            } else {
                promise.fail(res.body().encode());
            }
        });
        return promise.future();
    }

    private Future<Void> launchCsvImport(String path) {
        final Promise<Void> promise = Promise.promise();
        try {
            new CsvFeeder(test.vertx()).launch(Importer.getInstance(), path, new JsonObject(), res -> {
                if (res != null && "ok".equals(res.body().getString("status"))) {
                    promise.complete();
                } else {
                    promise.fail(res == null ? "csv import failed" : res.body().encode());
                }
            });
        } catch (Exception e) {
            promise.fail(e);
        }
        return promise.future();
    }

    private Future<JsonObject> readStructure(String externalId) {
        return test.database().executeNeo4jWithUniqueResult(
                "MATCH (s:Structure {externalId: {externalId}}) RETURN s.source as source, s.feederName as feederName ",
                new JsonObject().put("externalId", externalId));
    }
}
