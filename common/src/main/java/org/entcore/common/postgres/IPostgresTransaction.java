package org.entcore.common.postgres;

import io.vertx.core.Future;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;

public interface IPostgresTransaction {
    Future<RowSet<Row>> addPreparedQuery(String query, Tuple tuple);

    Future<Void> notify(String channel, String message);

    Future<Void> commit();

    Future<Void> rollback();
}
