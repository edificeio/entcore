package org.entcore.feeder.harness;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.Timeout;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.feeder.aaf.AafFeeder;
import org.entcore.feeder.dictionary.structures.Importer;
import org.entcore.feeder.utils.TransactionManager;
import org.entcore.feeder.utils.Validator;
import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Import AAF du harnais de comparaison feeder / referential (POC referential, étape 2).
 * Importe le dossier {@code harness.aaf.path} dans le Neo4j {@code harness.neo4j.uri}, après le schéma de l'infra
 * (contraintes, DeleteGroup) sauf si {@code harness.neo4j.schema=false} : un import successif rejoue sur la même base.
 * Ignoré sans ces deux propriétés : seul le script du dépôt referential (harness/aaf2d-snapshot.sh) le lance.
 */
@RunWith(VertxUnitRunner.class)
public class AafHarnessImportTest {

    private static final String NEO4J_URI = "harness.neo4j.uri";
    private static final String AAF_PATH = "harness.aaf.path";
    private static final String APPLY_SCHEMA = "harness.neo4j.schema";
    private static final List<String> SCHEMA_FILES = Arrays.asList("001-schema-index.cypher", "002-schema.cypher");
    private static final String SCHEMA_DIR = "../infra/src/main/resources/neo4j";

    /** L'import complet du jeu AAF dépasse le délai par défaut de vertx-unit (2 min). */
    @Rule
    public Timeout timeout = Timeout.seconds(1800);

    @Test
    public void importAafIntoHarnessNeo4j(TestContext context) throws IOException {
        final String uri = System.getProperty(NEO4J_URI);
        final String path = System.getProperty(AAF_PATH);
        Assume.assumeTrue("harness properties not set", uri != null && path != null);

        final Vertx vertx = Vertx.vertx();
        final Neo4j neo4j = Neo4j.getInstance();
        neo4j.init(vertx, new JsonObject().put("server-uri", uri + "/db/data/").put("poolSize", 1)
            .put("ignore-empty-statements-error", false));
        TransactionManager.getInstance().setNeo4j(neo4j);

        final Async async = context.async();
        final List<String> schema = Boolean.parseBoolean(System.getProperty(APPLY_SCHEMA, "true"))
            ? schemaStatements() : new ArrayList<>();
        applySchema(context, neo4j, schema, 0, () ->
            Validator.initLogin(neo4j, vertx, v -> {
                final Importer importer = Importer.getInstance();
                importer.init(neo4j, vertx, "AAF", "fr", false, false, false, init -> {
                    context.assertEquals("ok", init.body().getString("status"), init.body().encode());
                    try {
                        new AafFeeder(vertx, path).launch(importer, res -> {
                            context.assertEquals("ok", res.body().getString("status"), res.body().encode());
                            async.complete();
                        });
                    } catch (Exception e) {
                        context.fail(e);
                    }
                });
            }));
    }

    /** Une instruction par ligne non vide des fichiers de schéma de l'infra, sans le « ; » final. */
    private static List<String> schemaStatements() throws IOException {
        final List<String> statements = new ArrayList<>();
        for (String file : SCHEMA_FILES) {
            final Path schema = Paths.get(SCHEMA_DIR, file);
            for (String line : Files.readAllLines(schema, StandardCharsets.UTF_8)) {
                final String statement = line.trim().replaceAll(";$", "");
                if (!statement.isEmpty()) {
                    statements.add(statement);
                }
            }
        }
        return statements;
    }

    /** Une transaction par instruction : Neo4j refuse de mêler schéma et données dans une même transaction. */
    private static void applySchema(TestContext context, Neo4j neo4j, List<String> statements, int index,
            Runnable next) {
        if (index == statements.size()) {
            next.run();
            return;
        }
        neo4j.execute(statements.get(index), new JsonObject(), res -> {
            context.assertEquals("ok", res.body().getString("status"), res.body().encode());
            applySchema(context, neo4j, statements, index + 1, next);
        });
    }
}
