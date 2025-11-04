package org.entcore.broker.config;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

@RunWith(VertxUnitRunner.class)
public class NATSBrokersConfigTest {

    private Vertx vertx;

    @Before
    public void setUp() {
        vertx = Vertx.vertx();
    }

    @After
    public void tearDown() {
        if (vertx != null) {
            vertx.close();
        }
    }

    @Test
    public void testLegacyConfig() {
        // Given
        JsonObject config = new JsonObject()
            .put("nats", new JsonObject()
                .put("io.nats.client.url", "nats://localhost:4222")
                .put("io.nats.client.username", "test")
            );
        vertx.getOrCreateContext().config().mergeIn(config);

        // When
        NATSBrokersConfig brokersConfig = new NATSBrokersConfig(vertx);

        // Then
        assertFalse(brokersConfig.isMultiBrokerMode());
        assertEquals(1, brokersConfig.getBrokers().size());
        assertEquals("default", brokersConfig.getBrokers().get(0).getName());
        assertTrue(brokersConfig.getBrokers().get(0).isDefault());
        assertEquals(1, brokersConfig.getBrokers().get(0).getRouting().size());
        assertEquals("*", brokersConfig.getBrokers().get(0).getRouting().get(0));
    }

    @Test
    public void testMultiBrokerConfig() {
        // Given
        JsonObject config = new JsonObject()
            .put("nats", new io.vertx.core.json.JsonArray()
                .add(new JsonObject()
                    .put("name", "default")
                    .put("default", true)
                    .put("routing", new io.vertx.core.json.JsonArray().add("*"))
                    .put("client", new JsonObject()
                        .put("io.nats.client.url", "nats://localhost:4222")
                    )
                )
                .add(new JsonObject()
                    .put("name", "analytics")
                    .put("routing", new io.vertx.core.json.JsonArray().add("analytics.*"))
                    .put("client", new JsonObject()
                        .put("io.nats.client.url", "nats://analytics:4222")
                    )
                )
            );
        vertx.getOrCreateContext().config().mergeIn(config);

        // When
        NATSBrokersConfig brokersConfig = new NATSBrokersConfig(vertx);

        // Then
        assertTrue(brokersConfig.isMultiBrokerMode());
        assertEquals(2, brokersConfig.getBrokers().size());

        // Check default broker
        NATSBrokersConfig.NATSBrokerConfig defaultBroker = brokersConfig.getBrokers().get(0);
        assertEquals("default", defaultBroker.getName());
        assertTrue(defaultBroker.isDefault());
        assertEquals("*", defaultBroker.getRouting().get(0));

        // Check analytics broker
        NATSBrokersConfig.NATSBrokerConfig analyticsBroker = brokersConfig.getBrokers().get(1);
        assertEquals("analytics", analyticsBroker.getName());
        assertFalse(analyticsBroker.isDefault());
        assertEquals("analytics.*", analyticsBroker.getRouting().get(0));
    }

    @Test
    public void testRouting() {
        // Given
        JsonObject config = new JsonObject()
            .put("nats", new io.vertx.core.json.JsonArray()
                .add(new JsonObject()
                    .put("name", "default")
                    .put("default", true)
                    .put("routing", new io.vertx.core.json.JsonArray().add("*"))
                    .put("client", new JsonObject().put("io.nats.client.url", "nats://default:4222"))
                )
                    .add(new JsonObject()
                            .put("name", "analytics")
                            .put("routing", new io.vertx.core.json.JsonArray().add("analytics.*.*").add("logs.*"))
                            .put("client", new JsonObject().put("io.nats.client.url", "nats://analytics:4222"))
                    )
                    .add(new JsonObject()
                            .put("name", "blog")
                            .put("routing", new io.vertx.core.json.JsonArray().add("blog.>"))
                            .put("client", new JsonObject().put("io.nats.client.url", "nats://blog:4222"))
                    )
            );
        vertx.getOrCreateContext().config().mergeIn(config);
        NATSBrokersConfig brokersConfig = new NATSBrokersConfig(vertx);

        // When & Then
        assertEquals("default", brokersConfig.getBrokerForSubject("system.health").getName());
        assertEquals("analytics", brokersConfig.getBrokerForSubject("analytics.user.created").getName());
        assertEquals("analytics", brokersConfig.getBrokerForSubject("logs.error").getName());
        assertEquals("blog", brokersConfig.getBrokerForSubject("blog.error").getName());
        assertEquals("blog", brokersConfig.getBrokerForSubject("blog.resource.list").getName());
        assertEquals("default", brokersConfig.getBrokerForSubject("unknown.topic").getName());
    }

    @Test
    public void testWildcardRouting() {
        // Given
        JsonObject config = new JsonObject()
            .put("nats", new io.vertx.core.json.JsonArray()
                .add(new JsonObject()
                    .put("name", "default")
                    .put("default", true)
                    .put("routing", new io.vertx.core.json.JsonArray().add("*"))
                    .put("client", new JsonObject().put("io.nats.client.url", "nats://default:4222"))
                )
                .add(new JsonObject()
                    .put("name", "events")
                    .put("routing", new io.vertx.core.json.JsonArray().add("events.>"))
                    .put("client", new JsonObject().put("io.nats.client.url", "nats://events:4222"))
                )
            );
        vertx.getOrCreateContext().config().mergeIn(config);
        NATSBrokersConfig brokersConfig = new NATSBrokersConfig(vertx);

        // When & Then
        assertEquals("events", brokersConfig.getBrokerForSubject("events.user.created").getName());
        assertEquals("events", brokersConfig.getBrokerForSubject("events.system.notification.email.sent").getName());
        assertEquals("default", brokersConfig.getBrokerForSubject("system.health").getName());
    }

    @Test
    public void testAutoDefaultBroker() {
        // Given - no default specified
        JsonObject config = new JsonObject()
            .put("nats", new io.vertx.core.json.JsonArray()
                .add(new JsonObject()
                    .put("name", "broker1")
                    .put("routing", new io.vertx.core.json.JsonArray().add("topic1.*"))
                    .put("client", new JsonObject().put("io.nats.client.url", "nats://broker1:4222"))
                )
                .add(new JsonObject()
                    .put("name", "broker2")
                    .put("routing", new io.vertx.core.json.JsonArray().add("topic2.*"))
                    .put("client", new JsonObject().put("io.nats.client.url", "nats://broker2:4222"))
                )
            );
        vertx.getOrCreateContext().config().mergeIn(config);

        // When
        NATSBrokersConfig brokersConfig = new NATSBrokersConfig(vertx);

        // Then
        assertTrue(brokersConfig.getDefaultBroker().isDefault());
        assertEquals("broker1", brokersConfig.getDefaultBroker().getName());
    }
}