package org.entcore.common.redis;

import io.vertx.core.*;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.redis.client.Redis;
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
    protected final io.vertx.redis.client.Redis client;
    protected final RedisOptions redisOptions;
    protected Logger log = LoggerFactory.getLogger(RedisClient.class);

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

    public RedisClient(final io.vertx.redis.client.Redis redis, final RedisOptions redisOptions) {
        this.client = redis;
        this.redisOptions = redisOptions;
    }

    public RedisClient(final Vertx vertx, final JsonObject redisConfig) {
        final String host = redisConfig.getString("host");
        final Integer port = redisConfig.getInteger("port");
        final String username = redisConfig.getString("username","");
        final String password = redisConfig.getString("password");
        final Integer select = redisConfig.getInteger("select", 0);
        if (StringUtils.isEmpty(password)) {
            final String url = String.format("redis://%s:%s/%s", host, port, select);
            this.redisOptions = new RedisOptions().setConnectionString(url);
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
        client = io.vertx.redis.client.Redis.createClient(vertx, redisOptions);
    }

    public io.vertx.redis.client.Redis getClient() {
        return client;
    }

    public Future<List<JsonObject>> xreadGroup(final String group, final String consumer, final String stream, final boolean ack) {
        return xreadGroup(group, consumer, Arrays.asList(stream), ack, Optional.empty(), Optional.empty(), Optional.empty());
    }

    public Future<List<JsonObject>> xreadGroup(final String group, final String consumer, final String stream, final boolean ack, final Optional<Integer> count) {
        return xreadGroup(group, consumer, Arrays.asList(stream), ack, count, Optional.empty(), Optional.empty());
    }

    public Future<List<JsonObject>> xreadGroup(final String group, final String consumer, final String stream, final boolean ack, final Optional<Integer> count, final Optional<Integer> blockMs) {
        return xreadGroup(group, consumer, Arrays.asList(stream), ack, count, blockMs, Optional.empty());
    }

    public Future<List<JsonObject>> xreadGroup(final String group, final String consumer, final String stream, final boolean ack, final Optional<Integer> count, final Optional<Integer> blockMs, final Optional<String> ids) {
        return xreadGroup(group, consumer, Arrays.asList(stream), ack, count, blockMs, ids);
    }

    public Future<List<JsonObject>> xreadGroup(final String group, final String consumer, final List<String> streams) {
        return xreadGroup(group, consumer, streams, false, Optional.empty(), Optional.empty(), Optional.empty());
    }

    public Future<List<JsonObject>> xreadGroup(final String group, final String consumer, final List<String> streams, final boolean ack) {
        return xreadGroup(group, consumer, streams, ack, Optional.empty(), Optional.empty(), Optional.empty());
    }

    public Future<List<JsonObject>> xreadGroup(final String group, final String consumer, final List<String> streams, final boolean ack, final Optional<Integer> count) {
        return xreadGroup(group, consumer, streams, ack, count, Optional.empty(), Optional.empty());
    }

    public Future<List<JsonObject>> xreadGroup(final String group, final String consumer, final List<String> streams, final boolean ack, final Optional<Integer> count, final Optional<Integer> blockMs, final Optional<String> ids) {
        if (streams.isEmpty()) {
            return Future.succeededFuture(new ArrayList<>());
        }
        final Request req = Request.cmd(Command.XREADGROUP).arg("GROUP").arg(group).arg(consumer);
        if (count.isPresent()) {
            req.arg("COUNT").arg(count.get());
        }
        if (blockMs.isPresent()) {
            req.arg("BLOCK").arg(blockMs.get());
        }
        if (!ack) {
            req.arg("NOACK");
        }
        req.arg("STREAMS");
        for (final String stream : streams) {
            req.arg(stream);
        }
        //by default: only messages not received by other
        for (int i = 0; i < streams.size(); i++) {
            req.arg(ids.orElse(">"));
        }
        final Promise<List<JsonObject>> promise = Promise.promise();
        send(req, res -> {
            if (res.succeeded()) {
                final List<JsonObject> jsons = new ArrayList<>();
                if (res.result() != null) {
                    for (final Response streamAndObjects : res.result()) {
                        final String name = streamAndObjects.get(0).toString();
                        final Response objects = streamAndObjects.get(1);
                        for (final Response idAndObject : objects) {
                            final String id = idAndObject.get(0).toString();
                            final JsonObject json = toJson(idAndObject.get(1));
                            json.put(ID_STREAM, id);
                            json.put(NAME_STREAM, name);
                            jsons.add(json);
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
        final Request req = Request.cmd(Command.XPENDING).arg(stream).arg(group);
        if (count.isPresent()) {
            req.arg("-").arg("+").arg(count.get());
            req.arg(consumer);
        }
        final Promise<List<JsonObject>> promise = Promise.promise();
        send(req, res -> {
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
        final Request req = Request.cmd(Command.XRANGE).arg(stream).arg(id).arg(id);
        final Promise<JsonObject> promise = Promise.promise();
        send(req, res -> {
            if (res.succeeded()) {
                if (res.result() != null) {
                    final JsonObject json = toJson(res.result());
                    promise.complete(json);
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
            final Request req = Request.cmd(Command.XGROUP).arg("CREATE").arg(stream).arg(group).arg("$").arg("MKSTREAM");
            send(req, res -> {
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
            final Request req = Request.cmd(Command.XADD).arg(stream).arg("*");
            for (final String key : json.getMap().keySet()) {
                req.arg(key);
                req.arg(json.getValue(key).toString());
            }
            send(req, res -> {
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
        final Request req = Request.cmd(Command.XDEL).arg(stream);
        for (final String id : ids) {
            req.arg(id);
        }
        send(req, res -> {
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
        final Request req = Request.cmd(Command.XACK).arg(stream).arg(group);
        for (final String id : ids) {
            req.arg(id);
        }
        send(req, res -> {
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
        final Request req = Request.cmd(Command.XINFO).arg("STREAM").arg(stream);
        send(req, res -> {
            if (res.succeeded()) {
                if (res.result() != null) {
                    promise.complete(toJson(res.result()));
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
            jsons.add(toJson(r));
        }
        return jsons;
    }

    protected JsonObject toJson(final Response response) {
        final JsonObject json = new JsonObject();
        for (int index = 1; index < response.size(); index = index + 2) {
            final String field = response.get(index - 1).toString();
            final Response value = response.get(index);
            addToJson(json, field, value);
        }
        return json;
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
        return new RedisTransaction(this);
    }

    public RedisBatch batch() {
        return new RedisBatch(this);
    }

    protected Redis send(final Request command, final Handler<AsyncResult<Response>> onSend) {
        return client.send(command, onSend);
    }
}
