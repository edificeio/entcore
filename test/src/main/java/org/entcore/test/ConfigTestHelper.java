package org.entcore.test;

import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateNetworkCmd;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.junit.rules.ExternalResource;
import org.junit.rules.TestRule;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.output.OutputFrame;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class ConfigTestHelper {
    private final Vertx vertx;

    public ConfigTestHelper(Vertx v) {
        this.vertx = v;
    }

    /** Adds event-store (stats) configuration to the test environment. */
    public ConfigTestHelper withStatConfig(final PostgreSQLContainer<?> pgContainer){
        final JsonObject postgresql = new JsonObject().put("host", pgContainer.getHost()).put("database", pgContainer.getDatabaseName()).put("user", pgContainer.getUsername()).put("password", pgContainer.getPassword()).put("port", pgContainer.getMappedPort(5432));
        final JsonObject config = new JsonObject().put("postgresql", postgresql).put("platform", "test");
        vertx.sharedData().getLocalMap("server").put("event-store", config.toString());
        return this;
    }
    /** Adds email configuration to the test environment. */
    public ConfigTestHelper withMailerConfig(final PostgreSQLContainer<?> pgContainer){
        final JsonObject postgresql = new JsonObject().put("host", pgContainer.getHost()).put("database", pgContainer.getDatabaseName()).put("user", pgContainer.getUsername()).put("password", pgContainer.getPassword()).put("port", pgContainer.getMappedPort(5432));
        final JsonObject config = new JsonObject().put("type", "Postgres").put("postgresql", postgresql).put("email", "test@entcore.org").put("host", "https://test.entcore.org");
        vertx.sharedData().getLocalMap("server").put("emailConfig", config.toString());
        return this;
    }
}