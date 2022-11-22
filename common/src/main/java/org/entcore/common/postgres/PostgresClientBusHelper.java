package org.entcore.common.postgres;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.pgclient.impl.RowImpl;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowIterator;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import io.vertx.sqlclient.impl.RowDesc;
import io.vertx.sqlclient.impl.SqlResultBase;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

public class PostgresClientBusHelper  {
    public static final String ADDRESS = "local:postgres.client.bus";

    private static final Logger log = LoggerFactory.getLogger(PostgresClientBusHelper.class);

    public static JsonArray tupleToJson(final Tuple tuple){
        final JsonArray json = new JsonArray();
        for(int i = 0 ; i < tuple.size(); i++){
            final Object val = tuple.getValue(i);
            if (val == null ||
                    val == Tuple.JSON_NULL ||
                    val instanceof String ||
                    val instanceof Boolean ||
                    val instanceof Number ||
                    val instanceof JsonObject ||
                    val instanceof JsonArray) {
                json.add(val);
            } else if(val instanceof LocalDateTime){
                json.add("DATE$"+val.toString());
            }  else if(val instanceof UUID){
                json.add("UUID$"+val.toString());
            } else {
                json.add(val.toString());
            }
        }
        return json;
    }

    public static Tuple jsonToTuple(final JsonArray jsonTuple){
        final Tuple tuple = Tuple.tuple();
        for(int i = 0 ; i < jsonTuple.size(); i++){
            final Object val = jsonTuple.getValue(i);
            if(val instanceof  String){
                final String tmp = ((String) val);
                if(tmp.startsWith("DATE$")){
                    tuple.addValue(LocalDateTime.parse(tmp.replace("DATE$", "")));
                }else if(tmp.startsWith("UUID$")){
                    tuple.addValue(UUID.fromString(tmp.replace("UUID$", "")));
                }else{
                    tuple.addValue(val);
                }
            }else{
                tuple.addValue(val);
            }
        }
        return tuple;
    }

    public static JsonObject notifyToJson(final String channel, final String message){
        final JsonObject json = new JsonObject();
        json.put("channel", channel);
        json.put("message", message);
        json.put("action", "notify");
        return json;
    }

    public static String jsonToNotifyChannel(final JsonObject json){
        return json.getString("channel");
    }

    public static String jsonToNotifyMessage(final JsonObject json){
        return json.getString("message");
    }

    public static boolean isNotify(final JsonObject json){
        return "notify".equals(json.getString("action"));
    }

    public static JsonObject queryToJson(final String query, final Tuple tuple){
        final JsonObject json = new JsonObject();
        json.put("query", query);
        json.put("tuple", tupleToJson(tuple));
        json.put("action", "query");
        return json;
    }

    public static boolean isQuery(final JsonObject json){
        return "query".equals(json.getString("action"));
    }

    public static String jsonToQuery(final JsonObject json){
        return json.getString("query");
    }

    public static Tuple jsonToQueryTuple(final JsonObject json){
        final JsonArray tupleJson = json.getJsonArray("tuple");
        return jsonToTuple(tupleJson);
    }

    public static Row jsonToRow(final JsonObject json){
        final List<String> desc = new ArrayList<>(json.getMap().keySet());
        final Row row = new RowImpl(new RowDesc(desc));
        for(final String key : desc){
            final Object val = json.getValue(key);
            row.addValue(val);
        }
        return row;
    }

    public static JsonObject rowToJson(final Row row){
        final JsonObject json = new JsonObject();
        for(int i = 0 ; i < row.size(); i++){
            final String column = row.getColumnName(i);
            final Object val = row.getValue(column);
            if (val == null ||
                    val == Tuple.JSON_NULL ||
                    val instanceof String ||
                    val instanceof Boolean ||
                    val instanceof Number ||
                    val instanceof JsonObject ||
                    val instanceof JsonArray) {
                json.put(column, val);
            } else {
                json.put(column, val.toString());
            }
        }
        return json;
    }

    public static RowSet<Row> jsonToRowSet(final JsonArray json){
        final RowSetImpl<Row> rowset = new RowSetImpl<>();
        for(int i = 0 ; i < json.size(); i++){
            final JsonObject rowJson = json.getJsonObject(i);
            rowset.list.add(jsonToRow(rowJson));
        }
        return rowset;
    }

    public static JsonArray rowsetToJson(final RowSet<Row> rowset){
        final JsonArray array = new JsonArray();
        for(final Row row : rowset){
            array.add(rowToJson(row));
        }
        return array;
    }
    public static JsonObject transactionToJson(final JsonArray params){
        final JsonObject json = new JsonObject();
        json.put("transaction", params);
        json.put("action", "transaction");
        return json;
    }
    public static JsonArray jsonToTransactionParams(final JsonObject json){
        return json.getJsonArray("transaction");
    }

    public static boolean isTransaction(final JsonObject json){
        return "transaction".equals(json.getString("action"));
    }
    public static JsonObject resultToJson(final RowSet<Row> rowset){
        final JsonObject json = new JsonObject();
        json.put("rowset", rowsetToJson(rowset));
        json.put("type", "rowset");
        return json;
    }

    public static RowSet<Row> resultToRowset(final JsonObject json){
        final JsonArray rowset = json.getJsonArray("rowset");
        return jsonToRowSet(rowset);
    }

    public static boolean isRowsetResult(final JsonObject json){
        return "rowset".equals(json.getString("type"));
    }

    public static JsonObject resultNotifyToJson(){
        final JsonObject json = new JsonObject();
        json.put("type", "notify");
        return json;
    }

    public static boolean isNotifyResult(final JsonObject json){
        return "notify".equals(json.getString("type"));
    }

    public static class PostgresTransactionBus implements IPostgresTransaction {
        private final JsonArray params = new JsonArray();
        private final List<Promise> promises = new ArrayList<>();
        private final Function<PostgresTransactionBus, Future<Void>> onFinish;
        private boolean commit;

        PostgresTransactionBus(final Function<PostgresTransactionBus, Future<Void>> onFinish){
            this.onFinish = onFinish;
        }

        public JsonArray getParams() {
            return params;
        }

        public List<Promise> getPromises() {
            return promises;
        }

        public boolean isCommit() {
            return commit;
        }

        @Override
        public Future<RowSet<Row>> addPreparedQuery(String query, Tuple tuple) {
            final Promise<RowSet<Row>> promise = Promise.promise();
            params.add(queryToJson(query, tuple));
            promises.add(promise);
            return promise.future();
        }

        @Override
        public Future<Void> notify(final String channel, final String message) {
            final Promise<Void> promise = Promise.promise();
            params.add(notifyToJson(channel, message));
            promises.add(promise);
            return promise.future();
        }

        @Override
        public Future<Void> commit() {
            this.commit = true;
            return onFinish.apply(this);
        }

        @Override
        public Future<Void> rollback() {
            this.commit = false;
            return onFinish.apply(this);
        }
    }

    public static class RowSetImpl<R> extends SqlResultBase<RowSet<R>, RowSetImpl<R>> implements RowSet<R> {

        private final List<R> list = new ArrayList<>();

        @Override
        public RowSet<R> value() {
            return this;
        }

        @Override
        public RowIterator<R> iterator() {
            Iterator<R> i = list.iterator();
            return new RowIterator<R>() {
                @Override
                public boolean hasNext() {
                    return i.hasNext();
                }
                @Override
                public R next() {
                    return i.next();
                }
            };
        }
    }

}
