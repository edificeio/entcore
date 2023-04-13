package org.entcore.common.messaging;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import org.entcore.common.messaging.impl.RedisMessagingClient;
import org.entcore.common.redis.RedisClient;

/**
 * <p>
 *     <h3>Overview</h3>
 *     Produce {@code IMessagingClient} that allows communication through Redis stream.
 * </p>
 * <p>
 *     <h3>Usage</h3>
 *<pre>{@code
 * final IMessagingClient client = new RedisStreamClientFactory(vertx, config).create();
 * }</pre>
 * </p>
 */
public class RedisStreamClientFactory implements IMessagingClientFactory {
    private final JsonObject config;
    private final Vertx vertx;
    static final int DEFAULT_BLOCK_MS = 0;//infinity
    static final String DEFAULT_CONSUMER_NAME = "ent_stream_reader";
    static final String DEFAULT_CONSUMER_GROUP = "app_message_reader_group";

    /**
     * <p>
     *     <h3>Overview</h3>
     *     <div>Creates the factory that will create identical IMessagingClient based on the {@code config} parameter.</div>
     * </p>
     * <p>
     *     <h3>Configuration</h3>
     *     <pre>{@code
     *     {
     *         "stream": "address-to-send-receive",
     *         "consumer-block-ms": 0,
     *         "consumer-name": "client-consumer-name",
     *         "consumer-group": "client-group-name",
     *         ...
     *     }
     *     }</pre>
     *     <p>
     *         <ul>
     *             <li>stream, is the name of the stream in which we want to send/receive messages</li>
     *             <li>consumer-block-ms, the time to wait for new messages, 0 to indicate to wait indefinitely</li>
     *             <li>consumer-name, the name of this consumer (should be unique)</li>
     *             <li>consumer-group, the name of the group to which the consumer belongs (should not be unique
     *             for different instances of the same listener)</li>
     *         </ul>
     *     </p>
     * </p>
     *
     *
     * @param vertx Instance of VertX
     * @param config Configuration to connect to Redis and to parametrize the client.
     */
    public RedisStreamClientFactory(final Vertx vertx, final JsonObject config){
        this.vertx = vertx;
        this.config = config;

    }

    @Override
    public IMessagingClient create() {
        final RedisClient redis;
        try {
            redis = RedisClient.create(vertx, config);
        } catch (Exception e) {
            throw new MessagingException("redis.client.creation.error", e);
        }
        final String stream = config.getString("stream");
        if(isEmpty(stream)) {
            throw new MessagingException("missing stream name in redis stream configuration");
        }
        final int consumerBlockMs = config.getInteger("consumer-block-ms", DEFAULT_BLOCK_MS);
        final String consumerName = config.getString("consumer-name");
        final String consumerGroup = config.getString("consumer-group");
        return new RedisMessagingClient(vertx, redis, stream, consumerGroup, consumerName, consumerBlockMs);
    }
}
