package org.entcore.common.redis;

import io.vertx.core.*;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.redis.client.*;
import org.entcore.common.utils.StringUtils;

import java.security.InvalidParameterException;

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


    /**
     * Creates a Redis client object from the supplied configuration or from the shared configuration.
     * @param vertx Vertx instance
     * @param config An object <b><u>containing a field redisConfig</u></b> which holds redis configuration
     * @return A client to call Redis
     */
    public static RedisClient create(final Vertx vertx, final JsonObject config) {
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
                throw new InvalidParameterException("Missing redisConfig config");
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

    @Override
    public RedisAPI getClient() {
        return client;
    }

    @Override
    public RedisTransaction transaction() {
        return new RedisTransaction(this.client);
    }

}
