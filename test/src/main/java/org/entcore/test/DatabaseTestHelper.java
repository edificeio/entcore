package org.entcore.test;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.sql.DB;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.containers.PostgreSQLContainer;

public class DatabaseTestHelper {
    private final Vertx vertx;

    public DatabaseTestHelper(Vertx v) {
        this.vertx = v;
    }

    public Async initPostgreSQL(TestContext context, PostgreSQLContainer postgreSQLContainer, String schema) {
        final Async async = context.async();
        final JsonObject postgresConfig = new JsonObject().put("address", "sql.persistor")
                .put("url", postgreSQLContainer.getJdbcUrl()).put("username", postgreSQLContainer.getUsername())
                .put("password", postgreSQLContainer.getPassword());

        final DeploymentOptions deploymentOptions = new DeploymentOptions().setConfig(postgresConfig).setWorker(true)
                .setInstances(1).setMultiThreaded(true);

        vertx.deployVerticle(fr.wseduc.sql.SqlPersistor.class.getName(), deploymentOptions, ar -> {
            if (ar.succeeded()) {
                Sql sql = Sql.getInstance();
                sql.init(vertx.eventBus(), "sql.persistor");
                DB.loadScripts(schema, vertx, "sql");
                vertx.setTimer(2000L, t -> async.complete());
            } else {
                context.fail();
            }
        });
        return async;
    }

    public void initNeo4j(TestContext context, Neo4jContainer neo4jContainer) {
        final String base = neo4jContainer.getHttpUrl() + "/db/data/";
        final JsonObject config = new JsonObject().put("server-uri", base).put("poolSize", 1);
        final Neo4j neo4j = Neo4j.getInstance();
        neo4j.init(vertx, config);
    }

    public PostgreSQLContainer createPostgreSQLContainer() {
        return new PostgreSQLContainer("postgres:9.5");
    }

    public PostgreSQLContainer createPostgreSQL96Container() {
        return new PostgreSQLContainer("postgres:9.6");
    }

    public Neo4jContainer createNeo4jContainer() {
        return new Neo4jContainer("neo4j:3.1").withoutAuthentication()//
                .withNeo4jConfig("cypher.default_language_version", "2.3");
    }

    public Future<JsonObject> executeSqlWithUniqueResult(String query, JsonArray values) {
        Sql sql = Sql.getInstance();
        Future<JsonObject> future = Future.future();
        sql.prepared(query, values, SqlResult.validUniqueResultHandler(res -> {
            if (res.isRight()) {
                future.complete(res.right().getValue());
            } else {
                future.fail(res.left().getValue());
            }
        }));
        return future;

    }
}