package org.entcore.common.redis;

import io.vertx.core.*;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.redis.client.*;
import org.entcore.common.utils.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class RedisClient {
    public static final String ID_STREAM = "$id_stream";
    public static final String NAME_STREAM = "$name_stream";
    protected final RedisAPI client;
    protected final RedisOptions redisOptions;
    protected Logger log = LoggerFactory.getLogger(RedisClient.class);

    public RedisClient(final io.vertx.redis.client.Redis redis, final RedisOptions redisOptions) {
        this.client = RedisAPI.api(redis);
        this.redisOptions = redisOptions;
    }

    public RedisClient(final RedisAPI redis, final RedisOptions redisOptions) {
        this.client = redis;
        this.redisOptions = redisOptions;
    }

    public static RedisClient create(final Vertx vertx, final JsonObject config) throws Exception{
        if (config.getJsonObject("redisConfig") != null) {
            final JsonObject redisConfig = config.getJsonObject("redisConfig");
            final RedisClient redisClient = new RedisClient(vertx, redisConfig);
            return redisClient;
        }else{
            final String redisConfig = (String) vertx.sharedData().getLocalMap("server").get("redisConfig");
            if(redisConfig!=null){
                final RedisClient redisClient = new RedisClient(vertx, new JsonObject(redisConfig));
                return redisClient;
            }else{
                throw new Exception("Missing redisConfig config");
            }
        }
    }

    public RedisClient(final Vertx vertx, final JsonObject redisConfig) {
        final String host = redisConfig.getString("host");
        final Integer port = redisConfig.getInteger("port");
        final String username = redisConfig.getString("username","");
        final String password = redisConfig.getString("password");
        final String auth = redisConfig.getString("auth");
        final Integer select = redisConfig.getInteger("select", 0);
        if (StringUtils.isEmpty(password)) {
            if (StringUtils.isEmpty(auth)) {
                final String url = String.format("redis://%s:%s/%s", host, port, select);
                this.redisOptions = new RedisOptions().setConnectionString(url);
            }else{
                final String url = String.format("redis://%s:%s/%s?password=%s", host, port, select, auth);
                this.redisOptions = new RedisOptions().setConnectionString(url);
            }
        } else {
            final String url = String.format("redis://%s:%s@%s:%s/%s", username, password, host, port, select);
            this.redisOptions = new RedisOptions().setConnectionString(url);
        }
        if(redisConfig.getInteger("maxWaitingHandlers") !=null){
            redisOptions.setMaxWaitingHandlers(redisConfig.getInteger("maxWaitingHandlers"));
        }
        if(redisConfig.getInteger("maxPoolSize") !=null){
            redisOptions.setMaxPoolSize(redisConfig.getInteger("maxPoolSize"));
        }
        if(redisConfig.getInteger("maxPoolWaiting") !=null){
            redisOptions.setMaxPoolWaiting(redisConfig.getInteger("maxPoolWaiting"));
        }
        final io.vertx.redis.client.Redis oldClient = io.vertx.redis.client.Redis.createClient(vertx, redisOptions);
        client = RedisAPI.api(oldClient);
    }

    public RedisAPI getClient() {
        return client;
    }

    public Future<List<JsonObject>> xreadGroup(final String group, final String consumer, final String stream, final boolean ack) {
        return xreadGroup(group, consumer, Arrays.asList(stream), ack, Optional.empty(), Optional.empty(), Optional.empty(), false);
    }

    public Future<List<JsonObject>> xreadGroup(final String group, final String consumer, final String stream, final boolean ack, final Optional<Integer> count) {
        return xreadGroup(group, consumer, Arrays.asList(stream), ack, count, Optional.empty(), Optional.empty(), false);
    }

    public Future<List<JsonObject>> xreadGroup(final String group, final String consumer, final String stream, final boolean ack, final Optional<Integer> count, final Optional<Integer> blockMs) {
        return xreadGroup(group, consumer, Arrays.asList(stream), ack, count, blockMs, Optional.empty(), false);
    }

    public Future<List<JsonObject>> xreadGroup(final String group, final String consumer, final String stream, final boolean ack, final Optional<Integer> count, final Optional<Integer> blockMs, final Optional<String> ids) {
        return xreadGroup(group, consumer, Arrays.asList(stream), ack, count, blockMs, ids, false);
    }

    public Future<List<JsonObject>> xreadGroup(final String group, final String consumer, final String stream, final boolean ack, final Optional<Integer> count, final Optional<Integer> blockMs, final Optional<String> ids, final boolean autoClean) {
        return xreadGroup(group, consumer, Arrays.asList(stream), ack, count, blockMs, ids, autoClean);
    }

    public Future<List<JsonObject>> xreadGroup(final String group, final String consumer, final List<String> streams) {
        return xreadGroup(group, consumer, streams, false, Optional.empty(), Optional.empty(), Optional.empty(), false);
    }

    public Future<List<JsonObject>> xreadGroup(final String group, final String consumer, final List<String> streams, final boolean ack) {
        return xreadGroup(group, consumer, streams, ack, Optional.empty(), Optional.empty(), Optional.empty(), false);
    }

    public Future<List<JsonObject>> xreadGroup(final String group, final String consumer, final List<String> streams, final boolean ack, final Optional<Integer> count) {
        return xreadGroup(group, consumer, streams, ack, count, Optional.empty(), Optional.empty(), false);
    }

    public Future<List<JsonObject>> xreadGroup(final String group, final String consumer, final List<String> streams, final boolean ack, final Optional<Integer> count, final Optional<String> ids) {
        return xreadGroup(group, consumer, streams, ack, count, Optional.empty(), ids, false);
    }

    public Future<List<JsonObject>> xreadGroup(final String group, final String consumer, final List<String> streams, final boolean ack, final Optional<Integer> count, final Optional<Integer> blockMs, final Optional<String> ids, final boolean autoClean) {
        if (streams.isEmpty()) {
            return Future.succeededFuture(new ArrayList<>());
        }
        final List<String> args = new ArrayList<>();
        args.addAll(Arrays.asList("GROUP",group,consumer));
        if (count.isPresent()) {
            args.addAll(Arrays.asList("COUNT",count.get().toString()));
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
        this.client.xreadgroup(args, res -> {
            if (res.succeeded()) {
                final List<JsonObject> jsons = new ArrayList<>();
                if (res.result() != null) {
                    for (final Response streamAndObjects : res.result()) {
                        final String name = streamAndObjects.get(0).toString();
                        final Response objects = streamAndObjects.get(1);
                        for (final Response idAndObject : objects) {
                            final String id = idAndObject.get(0).toString();
                            final Optional<JsonObject> jsonOpt = toJson(idAndObject.get(1));
                            if(jsonOpt.isPresent()){
                                final JsonObject json = jsonOpt.get();
                                json.put(ID_STREAM, id);
                                json.put(NAME_STREAM, name);
                                jsons.add(json);
                            }else if(autoClean){
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
        });
        return promise.future();
    }

    public Future<List<JsonObject>> xPending(final String group, final String consumer, final String stream, final Optional<Integer> count) {
        final List<String> args = new ArrayList<>();
        args.addAll(Arrays.asList(stream,group));
        if (count.isPresent()) {
            args.addAll(Arrays.asList("-","+",count.get().toString()));
            args.add(consumer);
        }
        final Promise<List<JsonObject>> promise = Promise.promise();
        this.client.xpending(args, res -> {
            if (res.succeeded()) {
                if (res.result() != null) {
                    promise.complete(toJsonList(res.result()));
                } else {
                    promise.complete(new ArrayList<>());
                }
            } else {
                promise.fail(res.cause());
            }
        });
        return promise.future();
    }

    public Future<JsonObject> xRange(final String stream, final String id) {
        final List<String> args = new ArrayList<>();
        args.addAll(Arrays.asList(stream,id,id));
        final Promise<JsonObject> promise = Promise.promise();
        this.client.xrange(args, res -> {
            if (res.succeeded()) {
                if (res.result() != null) {
                    final Optional<JsonObject> json = toJson(res.result());
                    if(json.isPresent()){
                        promise.complete(json.get());
                    }else{
                        promise.fail("not found");
                    }
                } else {
                    promise.fail("not found");
                }
            } else {
                promise.fail(res.cause());
            }
        });
        return promise.future();
    }

    public Future<Void> xcreateGroup(final String group, final List<String> streams) {
        if (streams.isEmpty()) {
            return Future.succeededFuture();
        }
        final List<Future> futures = new ArrayList<>();
        for (final String stream : streams) {
            final Promise<Void> promise = Promise.promise();
            futures.add(promise.future());
            //TODO listen until now?
            final List<String> req = Arrays.asList("CREATE",stream,group,"$","MKSTREAM");
            this.client.xgroup(req, res -> {
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
            });
        }
        return CompositeFuture.all(futures).mapEmpty();
    }

    public Future<String> xAdd(final String stream, final JsonObject json) {
        return xAdd(stream, Arrays.asList(json)).map(e -> e.get(0));
    }

    public Future<List<String>> xAdd(final String stream, final List<JsonObject> jsons) {
        if (jsons.isEmpty()) {
            return Future.succeededFuture(new ArrayList<>());
        }
        final List<Future> futures = new ArrayList<>();
        for (final JsonObject json : jsons) {
            final Promise<String> promise = Promise.promise();
            futures.add(promise.future());
            //auto generate id
            final List<String> args = new ArrayList<>();
            args.addAll(Arrays.asList(stream,"*"));
            for (final String key : json.getMap().keySet()) {
                final Object value = json.getValue(key);
                if(value != null){
                    args.add(key);
                    args.add(value.toString());
                }
            }
            this.client.xadd(args, res -> {
                if (res.succeeded()) {
                    promise.complete(res.result().toString());
                } else {
                    promise.fail(res.cause());
                }
            });
        }
        return CompositeFuture.all(futures).map(res -> {
            return res.list().stream().map(e -> e.toString()).collect(Collectors.toList());
        });
    }

    public Future<Integer> xDel(final String stream, final String id) {
        return xDel(stream, Arrays.asList(id));
    }

    public Future<Integer> xDel(final String stream, final List<String> ids) {
        if (ids.isEmpty()) {
            return Future.succeededFuture();
        }
        final Promise<Integer> promise = Promise.promise();
        final List<String> args = new ArrayList<>();
        args.add(stream);
        for (final String id : ids) {
            args.add(id);
        }
        this.client.xdel(args, res -> {
            if (res.succeeded()) {
                promise.complete(res.result().toInteger());
            } else {
                promise.fail(res.cause());
            }
        });
        return promise.future();
    }


    public Future<Integer> xAck(final String stream, final String group, final String id) {
        return xAck(stream, group, Arrays.asList(id));
    }

    public Future<Integer> xAck(final String stream, final String group, final List<String> ids) {
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
        this.client.xack(args, res -> {
            if (res.succeeded()) {
                promise.complete(res.result().toInteger());
            } else {
                promise.fail(res.cause());
            }
        });
        return promise.future();
    }

    public Future<JsonObject> xInfo(final String stream) {
        final Promise<JsonObject> promise = Promise.promise();
        final List<String> args = Arrays.asList("STREAM",stream);
        this.client.xinfo(args, res -> {
            if (res.succeeded()) {
                if (res.result() != null) {
                    final Optional<JsonObject> json = toJson(res.result());
                    if(json.isPresent()){
                        promise.complete(json.get());
                    }else{
                        promise.fail("not found");
                    }
                } else {
                    promise.fail("not found");
                }
            } else {
                promise.fail(res.cause());
            }
        });
        return promise.future();
    }

    protected List<JsonObject> toJsonList(final Response response) {
        final List<JsonObject> jsons = new ArrayList<>();
        for (final Response r : response) {
            final Optional<JsonObject> json = toJson(r);
            if(json.isPresent()){
                jsons.add(json.get());
            }
        }
        return jsons;
    }

    protected Optional<JsonObject> toJson(final Response response) {
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

    protected void addToJson(final JsonObject json, final String key, final Response value) {
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
            case INTEGER:
                json.put(key, value.toInteger());
                break;
            case MULTI:
            case SIMPLE:
                json.put(key, value.toString());
                break;
        }
    }

    public RedisTransaction transaction() {
        return new RedisTransaction(this.client, this.redisOptions);
    }

}
