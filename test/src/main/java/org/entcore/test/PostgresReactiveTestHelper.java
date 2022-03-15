package org.entcore.test;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgConnection;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;
import org.testcontainers.containers.PostgreSQLContainer;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

public class PostgresReactiveTestHelper {
    private final Vertx vertx;
    private final Future<PgConnection> pgConnection;

    public PostgresReactiveTestHelper(Vertx v, PostgreSQLContainer<?> postgres) {
        this.vertx = v;
        this.pgConnection = Future.future();
        final PgConnectOptions options = new PgConnectOptions();
        options.setDatabase(postgres.getDatabaseName());
        options.setHost(postgres.getHost());
        options.setPassword(postgres.getPassword());
        options.setPort(postgres.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT));
        options.setUser(postgres.getUsername());
        PgConnection.connect(vertx, options, this.pgConnection.completer());
    }


    public Future<List<JsonObject>> execute(String query, JsonArray values) {
        final Tuple tuple = Tuple.tuple();
        for (Object o : values) {
            tuple.addValue(o);
        }
        return this.pgConnection.compose(conn -> {
            final Future<List<JsonObject>> future = Future.future();
            conn.preparedQuery(query).execute(tuple, event -> {
                try {
                    if (event.succeeded()) {
                        final List<JsonObject> all = new ArrayList<>();
                        final List<String> columnNames = event.result().columnsNames();
                        final Iterator<Row> it = event.result().iterator();
                        while (it.hasNext()) {
                            final Row row = it.next();
                            final JsonObject object = new JsonObject();
                            for (final String col : columnNames) {
                                Object value = row.getValue(col);
                                if (value instanceof UUID) {
                                    value = value.toString();
                                }
                                if (value instanceof LocalDateTime) {
                                    value = ((LocalDateTime) value).toInstant(ZoneOffset.UTC).toEpochMilli();
                                }
                                object.put(col, value);
                            }
                            all.add(object);
                        }
                        future.complete(all);
                    } else {
                        future.fail(event.cause());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    future.fail(e);
                }
            });
            return future;
        });
    }
}