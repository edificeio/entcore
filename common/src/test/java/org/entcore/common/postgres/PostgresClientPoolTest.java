package org.entcore.common.postgres;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgConnection;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.Tuple;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;

@RunWith(VertxUnitRunner.class)
public class PostgresClientPoolTest {

  @ClassRule
  public static PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer("postgres:14.3");

  public static PostgresClientPool postgresClientPool;

  public static final int maxDummies = 1000;

  @BeforeClass
  public static void beforeAll(TestContext context) {
    final Vertx vertx = Vertx.vertx();
    final JsonObject postresql = new JsonObject().put("database", postgreSQLContainer.getDatabaseName())
        .put("port", postgreSQLContainer.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT))
        .put("host", postgreSQLContainer.getContainerIpAddress())
        .put("password", postgreSQLContainer.getPassword()).put("user", postgreSQLContainer.getUsername());
    final PoolOptions poolOptions = new PoolOptions().setMaxSize(2);
    final PgPool pgPool = PgPool.pool(vertx, IPostgresClient.getConnectOption(postresql),poolOptions);
    postgresClientPool = new PostgresClientPool(vertx, pgPool, new JsonObject());

    final Async async = context.async(maxDummies);
    final PgConnectOptions options = new PgConnectOptions().setPort(postresql.getInteger("port", 5432))
        .setHost(postresql.getString("host")).setDatabase(postresql.getString("database"))
        .setUser(postresql.getString("user")).setPassword(postresql.getString("password"));
    PgConnection.connect(Vertx.vertx(), options, res -> {
      context.assertTrue(res.succeeded());
      res.result().query("CREATE SCHEMA test").execute(resSch -> {
        context.assertTrue(resSch.succeeded());
        res.result().query("CREATE TABLE test.dummy (id VARCHAR(36) PRIMARY KEY, dummy_content VARCHAR(36))").execute(resSql -> {
          for(int i =0; i < maxDummies; i ++) {
            final Tuple values = Tuple.of(UUID.randomUUID().toString(), "dummy " + i);
            res.result().preparedQuery("INSERT INTO test.dummy VALUES ($1, $2)").execute(values, resInsert -> {
              if(resInsert.succeeded()) {
                async.countDown();
              } else {
                context.fail(resInsert.cause());
              }
            });
          }
        });
      });
    });
  }

  /**
   * <h1>Goal</h1>
   * <p>
   *   Check that a queryStream can browse through all the content of a query without omitting any element nor
   *   sending back duplicates.
   * </p>
   * <h1>Steps</h1>
   * <ul>
   *   <li>Launch the query</li>
   *   <li>For each row, assert that the row was not already retrieved</li>
   *   <li>At the end of the stream, ensure that we have fetched all the content that was inserted</li>
   * </ul>
   * <h1>Remarks</h1>
   * <ul>
   *   <li>The batch size is voluntarily low so we force the stream to fetch multiple times</li>
   * </ul>
   * @param context Test context
   */
  @Test
  public void testBrowseAllElements(final TestContext context) {
    final Async async = context.async();
    final Set<String> dummyIds = new HashSet<>();
    postgresClientPool.queryStream("SELECT id as id, dummy_content as content from test.dummy", Tuple.tuple(), 5)
        .onSuccess(handler -> {
          try {
            handler.handler(row -> {
                  final String id = row.getString("id");
                  context.assertTrue(dummyIds.add(id), "The content returned by the queryStream has already been returned");
                })
                .exceptionHandler(context::fail)
                .endHandler(e -> {
                  context.assertEquals(maxDummies, dummyIds.size(), "Some rows were not returned");
                  async.complete();
                });
          } catch (Exception e) {
            context.fail(e);
          }
        }).onFailure(context::fail);
  }

  /**
   * <h1>Goal</h1>
   * <p>
   *   Check that a queryStream releases all its connection after having performed its treatment.
   * </p>
   * <h1>Steps</h1>
   * <ul>
   *   <li>Launch n queries (with n >> poolSize)</li>
   *   <li>Assert that all queries browse through each of its elements</li>
   * </ul>
   * @param context Test context
   */
  @Test
  public void testConnectionsAreDuelyReleased(final TestContext context) {
    final int nbParallelStreaming = 100;
    final int nbElementsPerProcess = 50;
    final Async async = context.async(nbParallelStreaming);
    IntStream.range(0, nbParallelStreaming).forEach(index -> {
      final Set<String> dummyIds = new HashSet<>();
      final int offset = (int) Math.floor(Math.random() * (maxDummies - nbElementsPerProcess));
      final String query = "SELECT id as id, dummy_content as content from test.dummy LIMIT " + nbElementsPerProcess + " OFFSET " + offset;
      postgresClientPool.queryStream(query, Tuple.tuple(), 2)
          .onSuccess(handler -> {
            try {
              handler.handler(row -> {
                    final String id = row.getString("id");
                    context.assertTrue(dummyIds.add(id), "The content returned by the queryStream has already been returned");
                  })
                  .exceptionHandler(context::fail)
                  .endHandler(e -> {
                    context.assertEquals(nbElementsPerProcess, dummyIds.size(), "Some rows were not returned");
                    async.countDown();
                  });
            } catch (Exception e) {
              context.fail(e);
            }
          }).onFailure(context::fail);
    });
  }

}
