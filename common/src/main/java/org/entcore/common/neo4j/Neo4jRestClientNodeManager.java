package org.entcore.common.neo4j;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.json.JsonObject;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

public class Neo4jRestClientNodeManager {
    private final Random random = new Random();
    private final Neo4jRestClientCheck checker;
    private final List<Neo4jRestClientNode> clients = new ArrayList<>();

    public Neo4jRestClientNodeManager(URI[] uris, Vertx vertx, long checkDelay, int poolSize, boolean keepAlive, String authorizationHeader, JsonObject neo4jConfig) {
        this(uris, vertx, checkDelay, poolSize, keepAlive, authorizationHeader, neo4jConfig, true);
    }

    public Neo4jRestClientNodeManager(URI[] uris, Vertx vertx, long checkDelay, int poolSize, boolean keepAlive, String authorizationHeader, JsonObject neo4jConfig, boolean autoStart) {
        final long banDurationSeconds = neo4jConfig.getLong("ban-duration-seconds", 60l);
        for (final URI uri : uris) {
            final HttpClientOptions options = new HttpClientOptions()
                    .setDefaultHost(uri.getHost())
                    .setDefaultPort(uri.getPort())
                    .setMaxPoolSize(poolSize)
                    .setKeepAlive(keepAlive);
            final HttpClient httpClient = vertx.createHttpClient(options);
            clients.add(new Neo4jRestClientNode(uri.toString(), httpClient, banDurationSeconds));
        }
        this.checker = Neo4jRestClientCheck.create(this, vertx, authorizationHeader, checkDelay, neo4jConfig);
        if (autoStart) {
            this.checker.start(this);
        }
    }

    public Neo4jRestClientCheck getChecker() {
        return checker;
    }

    public List<Neo4jRestClientNode> getClients() {
        return clients;
    }

    public boolean isCluster() {
        return this.clients.size() > 1;
    }

    public void disableReadCheck() {
        for (final Neo4jRestClientNode node : this.clients) {
            node.setReadable(true);
        }
    }

    public void setModuleName(String name) {
        for (final Neo4jRestClientNode node : this.clients) {
            node.setModuleName(name);
        }
    }

    public void disableClusterCheck() {
        for (final Neo4jRestClientNode node : this.clients) {
            node.setAnyType();
            node.setAvailable(true);
        }
    }

    public HttpClient getMasterClient() throws Neo4jConnectionException {
        return getMasterNode().getHttpClient();
    }

    public Neo4jRestClientNode getMasterNode() throws Neo4jConnectionException {
        try {
            //try to get master available
            final Optional<Neo4jRestClientNode> master = clients.stream().filter(c -> c.isMasterAvailable()).findFirst();
            if (master.isPresent()) {
                return master.get();
            } else {
                //try to get any available
                final Optional<Neo4jRestClientNode> available = clients.stream().filter(c -> c.isAvailable()).findAny();
                if (available.isPresent()) {
                    return available.get();
                } else if (clients.size() > 0) {
                    //try to get first
                    return clients.get(0);
                } else {
                    throw new Neo4jConnectionException("Could not found master (0 nodes)");
                }
            }
        } catch (Neo4jConnectionException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new Neo4jConnectionException("Can't get master connection.", e);
        }
    }

    public HttpClient getSlaveClient() throws Neo4jConnectionException {
        return getSlaveNode().getHttpClient();
    }

    public Neo4jRestClientNode getSlaveNode() throws Neo4jConnectionException {
        try {
            final List<Neo4jRestClientNode> slaves = clients.stream().filter(c -> c.isSlaveAvailable()).collect(Collectors.toList());
            if (slaves.isEmpty()) {
                return getMasterNode();
            } else {
                final Neo4jRestClientNode item = slaves.get(random.nextInt(slaves.size()));
                return item;
            }
        } catch (Neo4jConnectionException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new Neo4jConnectionException("Can't get master connection.", e);
        }
    }

    public void close() {
        this.checker.stop(this);
        for (Neo4jRestClientNode client : clients) {
            client.close();
        }
    }

}
