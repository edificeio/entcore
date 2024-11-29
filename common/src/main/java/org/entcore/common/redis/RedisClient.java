package org.entcore.common.redis;

import io.vertx.core.*;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.redis.client.*;

import java.util.ArrayList;
import java.util.List;

import org.entcore.common.utils.StringUtils;

public class RedisClient implements IRedisClient {
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
        List<String> hosts = new ArrayList<>();
        if (redisConfig.containsKey("hosts")) {
            JsonArray hostsList = redisConfig.getJsonArray("hosts");
            for (int i = 0; i < hostsList.size(); i++) {
                hosts.add(hostsList.getString(i));
            }
        }
        else {
            hosts.add(redisConfig.getString("host"));
        }

        final Integer port = redisConfig.getInteger("port");
        final String username = redisConfig.getString("username","");
        final String password = redisConfig.getString("password");
        final String auth = redisConfig.getString("auth");
        final Integer select = redisConfig.getInteger("select", 0);

        RedisOptions redisOptions = new RedisOptions();
        for(int i = 0; i < hosts.size(); i++) {
            String host = hosts.get(i);
            if (StringUtils.isEmpty(password)) {
                if (StringUtils.isEmpty(auth)) {
                    final String url = String.format("redis://%s:%s/%s", host, port, select);
                    redisOptions.addConnectionString(url);
                }else{
                    final String url = String.format("redis://%s:%s/%s?password=%s", host, port, select, auth);
                    redisOptions.addConnectionString(url);
                }
            } else {
                final String url = String.format("redis://%s:%s@%s:%s/%s", username, password, host, port, select);
                redisOptions.addConnectionString(url);
            }
        }
        this.redisOptions = redisOptions;

        redisOptions.setType(hosts.size() > 1 ? RedisClientType.SENTINEL : RedisClientType.STANDALONE);
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

    @Override
    public RedisAPI getClient() {
        return client;
    }

    @Override
    public RedisTransaction transaction() {
        return new RedisTransaction(this.client);
    }

}
