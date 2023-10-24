package org.entcore.common.redis;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.redis.client.RedisAPI;
import io.vertx.redis.client.Response;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public interface IRedisClient {
    RedisAPI getClient();

    default Future<List<JsonObject>> xreadGroup(String group, String consumer, String stream, boolean ack) {
        return xreadGroup(group, consumer, Arrays.asList(stream), ack, Optional.empty(), Optional.empty(), Optional.empty(), false);
    }

    default Future<List<JsonObject>> xreadGroup(String group, String consumer, String stream, boolean ack, Optional<Integer> count) {
        return xreadGroup(group, consumer, Arrays.asList(stream), ack, count, Optional.empty(), Optional.empty(), false);
    }

    default Future<List<JsonObject>> xreadGroup(String group, String consumer, String stream, boolean ack, Optional<Integer> count, Optional<Integer> blockMs) {
        return xreadGroup(group, consumer, Arrays.asList(stream), ack, count, blockMs, Optional.empty(), false);
    }

    default Future<List<JsonObject>> xreadGroup(String group, String consumer, String stream, boolean ack, Optional<Integer> count, Optional<Integer> blockMs, Optional<String> ids) {
        return xreadGroup(group, consumer, Arrays.asList(stream), ack, count, blockMs, ids, false);
    }

    default Future<List<JsonObject>> xreadGroup(String group, String consumer, String stream, boolean ack, Optional<Integer> count, Optional<Integer> blockMs, Optional<String> ids, boolean autoClean) {
        return xreadGroup(group, consumer, Arrays.asList(stream), ack, count, blockMs, ids, autoClean);
    }

    default Future<List<JsonObject>> xreadGroup(String group, String consumer, List<String> streams) {
        return xreadGroup(group, consumer, streams, false, Optional.empty(), Optional.empty(), Optional.empty(), false);
    }

    default Future<List<JsonObject>> xreadGroup(String group, String consumer, List<String> streams, boolean ack) {
        return xreadGroup(group, consumer, streams, ack, Optional.empty(), Optional.empty(), Optional.empty(), false);
    }

    default Future<List<JsonObject>> xreadGroup(String group, String consumer, List<String> streams, boolean ack, Optional<Integer> count) {
        return xreadGroup(group, consumer, streams, ack, count, Optional.empty(), Optional.empty(), false);
    }

    default Future<List<JsonObject>> xreadGroup(String group, String consumer, List<String> streams, boolean ack, Optional<Integer> count, Optional<String> ids) {
        return xreadGroup(group, consumer, streams, ack, count, Optional.empty(), ids, false);
    }

    default Future<List<JsonObject>> xreadGroup(String group, String consumer, List<String> streams, boolean ack, Optional<Integer> count, Optional<Integer> blockMs, Optional<String> ids, boolean autoClean) {
        if (streams.isEmpty()) {
            return Future.succeededFuture(new ArrayList<>());
        }
        final List<String> args = new ArrayList<>();
        args.addAll(Arrays.asList("GROUP", group, consumer));
        if (count.isPresent()) {
            args.addAll(Arrays.asList("COUNT", count.get().toString()));
        }
        if (blockMs.isPresent()) {
            args.addAll(Arrays.asList("BLOCK", blockMs.get().toString()));
        }
        if (!ack) {
            args.add("NOACK");
        }
        args.add("STREAMS");
        for (final String stream : streams) {
            args.add(stream);
        }
        //by default: only messages not received by other
        for (int i = 0; i < streams.size(); i++) {
            args.add(ids.orElse(">"));
        }
        final Promise<List<JsonObject>> promise = Promise.promise();
        this.getClient().xreadgroup(args, (res -> {
            if (res.succeeded()) {
                final List<JsonObject> jsons = new ArrayList<>();
                if (res.result() != null) {
                    for (final Response streamAndObjects : res.result()) {
                        final String name = streamAndObjects.get(0).toString();
                        final Response objects = streamAndObjects.get(1);
                        for (final Response idAndObject : objects) {
                            final String id = idAndObject.get(0).toString();
                            final Optional<JsonObject> jsonOpt = toJson(idAndObject.get(1));
                            if (jsonOpt.isPresent()) {
                                final JsonObject json = jsonOpt.get();
                                json.put(RedisClient.ID_STREAM, id);
                                json.put(RedisClient.NAME_STREAM, name);
                                jsons.add(json);
                            } else if (autoClean) {
                                //DELETE IF ENTRY HAS BEEN ACKED (nil)
                                this.xAck(name, group, id);
                            }
                        }
                    }
                }
                promise.complete(jsons);
            } else {
                promise.fail(res.cause());
            }
        }));
        return promise.future();
    }

    default Future<List<JsonObject>> xPending(String group, String consumer, String stream, Optional<Integer> count) {
        final List<String> args = new ArrayList<>();
        args.addAll(Arrays.asList(stream, group));
        if (count.isPresent()) {
            args.addAll(Arrays.asList("-", "+", count.get().toString()));
            args.add(consumer);
        }
        final Promise<List<JsonObject>> promise = Promise.promise();
        this.getClient().xpending(args, (res -> {
            if (res.succeeded()) {
                if (res.result() != null) {
                    promise.complete(toJsonList(res.result()));
                } else {
                    promise.complete(new ArrayList<>());
                }
            } else {
                promise.fail(res.cause());
            }
        }));
        return promise.future();
    }

    default Future<JsonObject> xRange(String stream, String id) {
        final List<String> args = new ArrayList<>();
        args.addAll(Arrays.asList(stream, id, id));
        final Promise<JsonObject> promise = Promise.promise();
        this.getClient().xrange(args, (res -> {
            if (res.succeeded()) {
                if (res.result() != null) {
                    final Optional<JsonObject> json = toJson(res.result());
                    if (json.isPresent()) {
                        promise.complete(json.get());
                    } else {
                        promise.fail("not found");
                    }
                } else {
                    promise.fail("not found");
                }
            } else {
                promise.fail(res.cause());
            }
        }));
        return promise.future();
    }

    default Future<Void> xcreateGroup(String group, List<String> streams) {
        if (streams.isEmpty()) {
            return Future.succeededFuture();
        }
        final List<Future> futures = new ArrayList<>();
        for (final String stream : streams) {
            final Promise<Void> promise = Promise.promise();
            futures.add(promise.future());
            //TODO listen until now?
            final List<String> req = Arrays.asList("CREATE", stream, group, "$", "MKSTREAM");
            this.getClient().xgroup(req, (res -> {
                if (res.succeeded()) {
                    promise.complete();
                } else {
                    if (res.cause().getMessage().contains("BUSYGROUP")) {
                        //already exists
                        promise.complete();
                    } else {
                        promise.fail(res.cause());
                    }
                }
            }));
        }
        return CompositeFuture.all(futures).mapEmpty();
    }

    default Future<String> xAdd(String stream, JsonObject json) {
        return xAdd(stream, Arrays.asList(json)).map(e -> e.get(0));
    }

    default Future<List<String>> xAdd(String stream, List<JsonObject> jsons) {
        if (jsons.isEmpty()) {
            return Future.succeededFuture(new ArrayList<>());
        }
        final List<Future> futures = new ArrayList<>();
        for (final JsonObject json : jsons) {
            final Promise<String> promise = Promise.promise();
            futures.add(promise.future());
            //auto generate id
            final List<String> args = new ArrayList<>();
            args.addAll(Arrays.asList(stream, "*"));
            for (final String key : json.getMap().keySet()) {
                final Object value = json.getValue(key);
                if (value != null) {
                    args.add(key);
                    args.add(value.toString());
                }
            }
            this.getClient().xadd(args, (res -> {
                if (res.succeeded()) {
                    promise.complete(res.result().toString());
                } else {
                    promise.fail(res.cause());
                }
            }));
        }
        return CompositeFuture.all(futures).map(res -> {
            return res.list().stream().map(e -> e.toString()).collect(Collectors.toList());
        });
    }

    default Future<Integer> xDel(String stream, String id) {
        return xDel(stream, Arrays.asList(id));
    }

    default Future<Integer> xDel(String stream, List<String> ids) {
        if (ids.isEmpty()) {
            return Future.succeededFuture();
        }
        final Promise<Integer> promise = Promise.promise();
        final List<String> args = new ArrayList<>();
        args.add(stream);
        for (final String id : ids) {
            args.add(id);
        }
        this.getClient().xdel(args, (res -> {
            if (res.succeeded()) {
                promise.complete(toInteger(res.result()));
            } else {
                promise.fail(res.cause());
            }
        }));
        return promise.future();
    }

    default Future<Integer> xAck(String stream, String group, String id) {
        return xAck(stream, group, Arrays.asList(id));
    }

    default Future<Integer> xAck(String stream, String group, List<String> ids) {
        if (ids.isEmpty()) {
            return Future.succeededFuture();
        }
        final Promise<Integer> promise = Promise.promise();
        final List<String> args = new ArrayList<>();
        args.add(stream);
        args.add(group);
        for (final String id : ids) {
            args.add(id);
        }
        this.getClient().xack(args, (res -> {
            if (res.succeeded()) {
                promise.complete(toInteger(res.result()));
            } else {
                promise.fail(res.cause());
            }
        }));
        return promise.future();
    }

    default Future<JsonObject> xInfo(String stream) {
        final Promise<JsonObject> promise = Promise.promise();
        final List<String> args = Arrays.asList("STREAM", stream);
        this.getClient().xinfo(args, (res -> {
            if (res.succeeded()) {
                if (res.result() != null) {
                    final Optional<JsonObject> json = toJson(res.result());
                    if (json.isPresent()) {
                        promise.complete(json.get());
                    } else {
                        promise.fail("not found");
                    }
                } else {
                    promise.fail("not found");
                }
            } else {
                promise.fail(res.cause());
            }
        }));
        return promise.future();
    }

    default Integer toInteger(final Response response){
        if("QUEUED".equals(response.toString())){
            return -1;
        }else{
            return response.toInteger();
        }
    }

    default List<JsonObject> toJsonList(final Response response) {
        final List<JsonObject> jsons = new ArrayList<>();
        for (final Response r : response) {
            final Optional<JsonObject> json = toJson(r);
            if(json.isPresent()){
                jsons.add(json.get());
            }
        }
        return jsons;
    }

    default Optional<JsonObject> toJson(final Response response) {
        if(response == null){
            return Optional.empty();
        }
        final JsonObject json = new JsonObject();
        for (int index = 1; index < response.size(); index = index + 2) {
            final String field = response.get(index - 1).toString();
            final Response value = response.get(index);
            addToJson(json, field, value);
        }
        return Optional.ofNullable(json);
    }

    default void addToJson(final JsonObject json, final String key, final Response value) {
        if(value == null || value.type() == null){
            json.putNull(key);
            return;
        }
        switch (value.type()) {
            case BULK:
                json.put(key, value.toString());
                break;
            case ERROR:
                json.put(key, value.toString());
                break;
            case NUMBER:
                json.put(key, value.toInteger());
                break;
            case MULTI:
            case SIMPLE:
                json.put(key, value.toString());
                break;
        }
    }

    RedisTransaction transaction();
}
