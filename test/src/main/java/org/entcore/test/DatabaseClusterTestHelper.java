package org.entcore.test;

import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateNetworkCmd;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import org.junit.rules.ExternalResource;
import org.junit.rules.TestRule;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.containers.Network;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class DatabaseClusterTestHelper {
    public static final int NEO4J_SWITCH_TIMEOUT_MS = 50;
    static final boolean NEO4J_AUTH_ENABLED = false;
    static final String NEO4J_CLUSTER_IMAGE = "neo4j:3.1.6-enterprise";
    private final Vertx vertx;

    public DatabaseClusterTestHelper(Vertx v) {
        this.vertx = v;
    }

    private GenericContainer configureNeo4j(final Neo4jContainer container, final Neo4jCluster cluster, final String id, final String name, final boolean slaveOnly, final List<String> names) {
        final StringBuilder initialHost = new StringBuilder();
        boolean first = true;
        for (final String n : names) {
            if (first) {
                initialHost.append(n).append(":5001");
                first = false;
            } else {
                initialHost.append(",").append(n).append(":5001");
            }
        }
        container.withCreateContainerCmdModifier(new Consumer<CreateContainerCmd>() {
            @Override
            public void accept(CreateContainerCmd createContainerCmd) {
                createContainerCmd.withName(name);
            }
        }).withNetwork(cluster.network).withNetworkAliases(cluster.networkName).withStartupAttempts(3);
        container
                .withNeo4jConfig("dbms.mode", "HA")
                .withNeo4jConfig("ha.serverId", id)
                .withNeo4jConfig("ha.join_timeout", "60s")
                .withNeo4jConfig("ha.slave_only", slaveOnly ? "true" : "false")
                .withNeo4jConfig("ha.role_switch_timeout", NEO4J_SWITCH_TIMEOUT_MS + "ms")
                .withNeo4jConfig("dbms.security.auth_enabled", NEO4J_AUTH_ENABLED ? "true" : "false")
                .withNeo4jConfig("ha.host.coordination", name + ":5001")
                .withNeo4jConfig("ha.host.data", name + ":6001")
                .withNeo4jConfig("ha.initialHosts", initialHost.toString());
        return container;
    }

    public Neo4jCluster initNeo4j(int nbNode, int nbSlaveOnly) {
        final List<String> names = new ArrayList<>();
        final List<Boolean> slaveOnly = new ArrayList<>();
        final Neo4jCluster cluster = new Neo4jCluster();
        for (int i = 0; i < nbNode; i++) {
            names.add(String.format("cluster%sneoha%s", cluster.index, i));
            slaveOnly.add(i < nbSlaveOnly);
        }
        for (int i = 0; i < nbNode; i++) {
            final boolean slaveOnlyValue = slaveOnly.get(i);
            final Neo4jContainer<?> neoha = new Neo4jContainer<>(NEO4J_CLUSTER_IMAGE);
            configureNeo4j(neoha, cluster, String.valueOf(i), names.get(i), slaveOnlyValue, names);
            if (slaveOnlyValue) {
                cluster.addSlave(neoha);
            } else {
                cluster.addPotentialMaster(neoha);
            }
        }
        return cluster.start();
    }

    public static class Neo4jCluster extends ExternalResource implements TestRule {
        private static int clusterCounter = 0;
        private final List<Neo4jContainer> potentialMaster = new ArrayList<>();
        private final List<Neo4jContainer> slave = new ArrayList<>();
        private final Map<String, Neo4jContainer> containerByUrl = new HashMap<>();
        private final Network network;
        private final int index;
        private final String networkName;

        Neo4jCluster() {
            this.index = clusterCounter++;
            this.networkName = String.format("cluster%s", this.index);
            this.network = Network.builder().driver("bridge").createNetworkCmdModifier(new Consumer<CreateNetworkCmd>() {
                @Override
                public void accept(CreateNetworkCmd createNetworkCmd) {
                    createNetworkCmd.withName(networkName);
                }
            }).build();
        }

        public List<Neo4jContainer> getPotentialMaster() {
            return potentialMaster;
        }

        public List<Neo4jContainer> getSlave() {
            return slave;
        }

        public List<Neo4jContainer> getAll() {
            final List<Neo4jContainer> all = new ArrayList<>(potentialMaster);
            all.addAll(slave);
            return all;
        }

        public List<String> getUrls() {
            return getAll().stream().map(e -> e.getHttpUrl()).collect(Collectors.toList());
        }

        public URI[] getUris() {
            final List<URI> all = getUrls().stream().map(e -> {
                try {
                    return new URI(e + "/db/data");
                } catch (URISyntaxException ee) {
                    throw new RuntimeException(ee);
                }
            }).collect(Collectors.toList());
            return all.toArray(new URI[all.size()]);
        }

        public Neo4jCluster start() {
            getAll().stream().parallel().forEach(e -> {
                e.start();
                this.containerByUrl.put(e.getHttpUrl(), e);
            });
            return this;
        }

        public Neo4jCluster stop() {
            for (final Neo4jContainer container : getAll()) {
                container.stop();
            }
            return this;
        }

        public Neo4jCluster addPotentialMaster(final Neo4jContainer container) {
            this.potentialMaster.add(container);
            return this;
        }

        public Neo4jCluster addSlave(final Neo4jContainer container) {
            this.slave.add(container);
            return this;
        }

        public boolean stop(String url) {
            url = url.replaceAll("/db/data", "");
            if (this.containerByUrl.containsKey(url)) {
                this.containerByUrl.get(url).stop();
                return true;
            }
            return false;
        }

        public boolean start(String url) {
            url = url.replaceAll("/db/data", "");
            if (this.containerByUrl.containsKey(url)) {
                this.containerByUrl.get(url).start();
                return true;
            }
            return false;
        }

        public Future<AsyncResult<Object>> start(Vertx vertx, String urlO) {
            final String url = urlO.replaceAll("/db/data", "");
            if (this.containerByUrl.containsKey(url)) {
                return vertx.executeBlocking(() -> {
                    Neo4jContainer container = this.containerByUrl.get(url);
                    container.start();
                    this.containerByUrl.put(container.getHttpUrl(), container);
                    return null;
                });
            }
            return Future.failedFuture("Cant start docker (notfound): " + url);
        }

        public Future<Void> start(Vertx vertx) {
            final long count = getAll().stream().filter(e -> !e.isRunning()).count();
            if(count == 0){
                return Future.succeededFuture();
            }
            return vertx.executeBlocking(()->{
                getAll().parallelStream().forEach(e->{
                    if(e.isRunning()){
                        e.stop();
                    }
                });
                return null;
            }).compose(t ->
                vertx.executeBlocking(()->{
                    getAll().parallelStream().forEach(e->{
                        if(!e.isRunning()){
                            e.start();
                            this.containerByUrl.put(e.getHttpUrl(), e);
                        }
                    });
                    return null;
                })
            );
        }

        @Override
        protected void after() {
            super.after();
            for (final Neo4jContainer container : getAll()) {
                try {
                    container.close();
                } catch (Exception e) {
                }
            }
            try {
                network.close();
            } catch (Exception e) {
            }
        }

        public String getNewUrl(String url) {
            url = url.replaceAll("/db/data", "");
            return this.containerByUrl.get(url).getHttpUrl();
        }
    }
}