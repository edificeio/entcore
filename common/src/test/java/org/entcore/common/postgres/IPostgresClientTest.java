package org.entcore.common.postgres;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.sqlclient.Query;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.SqlConnection;
import org.junit.Test;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class IPostgresClientTest {

    @Test
    public void notifyShouldExecuteNotifyQueryAndCompleteFuture() {
        final SqlConnection connection = mock(SqlConnection.class);
        @SuppressWarnings("unchecked")
        final Query<RowSet<Row>> query = mock(Query.class);
        final String channel = "channel_name";
        final String message = "message_payload";
        final String sql = "NOTIFY " + channel + ", '" + message + "'";

        when(connection.query(sql)).thenReturn(query);
        doAnswer(invocation -> {
            final Handler<io.vertx.core.AsyncResult<RowSet<Row>>> handler = invocation.getArgument(0);
            handler.handle(Future.succeededFuture());
            return query;
        }).when(query).execute(any());

        final Future<Void> result = IPostgresClient.notify(connection, channel, message);

        assertTrue(result.succeeded());
        verify(connection).query(sql);
        verify(query).execute(any());
    }

    @Test
    public void notifyShouldFailFutureWhenQueryExecutionFails() {
        final SqlConnection connection = mock(SqlConnection.class);
        @SuppressWarnings("unchecked")
        final Query<RowSet<Row>> query = mock(Query.class);
        final RuntimeException error = new RuntimeException("notify failed");
        final String channel = "channel_name";
        final String message = "message_payload";
        final String sql = "NOTIFY " + channel + ", '" + message + "'";

        when(connection.query(sql)).thenReturn(query);
        doAnswer(invocation -> {
            final Handler<io.vertx.core.AsyncResult<RowSet<Row>>> handler = invocation.getArgument(0);
            handler.handle(Future.failedFuture(error));
            return query;
        }).when(query).execute(any());

        final Future<Void> result = IPostgresClient.notify(connection, channel, message);

        assertTrue(result.failed());
        assertSame(error, result.cause());
        verify(connection).query(sql);
        verify(query).execute(any());
    }
}