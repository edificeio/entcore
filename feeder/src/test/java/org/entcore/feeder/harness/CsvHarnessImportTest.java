package org.entcore.feeder.harness;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.Timeout;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.feeder.Feeder;
import org.entcore.feeder.csv.CsvImportsLauncher;
import org.entcore.feeder.dictionary.structures.DuplicateUsers;
import org.entcore.feeder.dictionary.structures.PostImport;
import org.entcore.feeder.utils.TransactionManager;
import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * CSV import of the feeder / referential comparison harness (referential POC, step 3), as close as possible to
 * production: Feeder verticle deployed (bus "import" action), CsvImportsLauncher on the {@code harness.csv.path}
 * archive directory with the Educasud configuration, then post-import. Skipped without {@code harness.neo4j.uri} and
 * {@code harness.csv.path}: only the referential repository script (harness/aaf2d-snapshot.sh) runs it.
 */
@RunWith(VertxUnitRunner.class)
public class CsvHarnessImportTest {

    private static final String NEO4J_URI = "harness.neo4j.uri";
    private static final String CSV_PATH = "harness.csv.path";
    private static final String APPLY_SCHEMA = "harness.neo4j.schema";
    /** csv configuration of the Educasud platform. */
    private static final JsonObject EDUCASUD = new JsonObject()
        .put("namePattern", ".*?/Educasud_[0-9]{8}_[0-9]{7}[A-Z]_(.*?).zip")
        .put("profiles", new JsonObject()
            .put("_Eleves_", "Student")
            .put("_Enseignants_", "Teacher")
            .put("_Personnels_", "Personnel")
            .put("_Parents_", "Relative"))
        .put("preDelete", true);

    @Rule
    public Timeout timeout = Timeout.seconds(1800);

    @Test
    public void importCsvIntoHarnessNeo4j(TestContext context) throws IOException {
        final String uri = System.getProperty(NEO4J_URI);
        final String path = System.getProperty(CSV_PATH);
        Assume.assumeTrue("harness properties not set", uri != null && path != null);

        final Vertx vertx = Vertx.vertx();
        final JsonObject neo4jConfig = new JsonObject().put("server-uri", uri + "/db/data/").put("poolSize", 1);
        final Neo4j neo4j = Neo4j.getInstance();
        neo4j.init(vertx, neo4jConfig.copy());
        TransactionManager.getInstance().setNeo4j(neo4j);
        vertx.sharedData().getLocalMap("server").put("neo4jConfig", neo4jConfig.encode());
        vertx.sharedData().getLocalMap("server").put("node", "");
        // import-files (AAF imports): unused here but required to start the verticle; file-system: storage where
        // the CSV validation copies the import directory (identical copy, no effect)
        final JsonObject feederConfig = new JsonObject().put("neo4jConfig", neo4jConfig).put("feeder", "AAF")
            .put("import-files", path)
            .put("file-system", new JsonObject().put("path", path));

        final Async async = context.async();
        final List<String> schema = Boolean.parseBoolean(System.getProperty(APPLY_SCHEMA, "true"))
            ? AafHarnessImportTest.schemaStatements() : new ArrayList<>();
        AafHarnessImportTest.applySchema(context, neo4j, schema, 0, () ->
            vertx.deployVerticle(new Feeder(), new DeploymentOptions().setConfig(feederConfig), deployed -> {
                if (deployed.failed()) {
                    deployed.cause().printStackTrace();
                }
                context.assertTrue(deployed.succeeded(), String.valueOf(deployed.cause()));
                final PostImport postImport = new PostImport(vertx,
                    new DuplicateUsers(true, true, vertx.eventBus()), feederConfig) {
                    @Override
                    public void execute() {
                        // dernier maillon du lanceur : le vrai post-import, puis la fin du test
                        execute(null, (Set<String>) null, v -> async.complete());
                    }
                };
                new CsvImportsLauncher(vertx, path, EDUCASUD, postImport).handle(0L);
            }));
    }
}
