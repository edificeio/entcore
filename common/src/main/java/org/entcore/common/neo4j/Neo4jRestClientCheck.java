package org.entcore.common.neo4j;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.http.BaseServer;


public interface Neo4jRestClientCheck {
    Logger log = LoggerFactory.getLogger(Neo4jRestClientCheck.class);

    default void start(Neo4jRestClientNodeManager manager) { }

    default void beforeCheck(Neo4jRestClientNodeManager manager) { }

    Future<Void> check(Neo4jRestClientNodeManager manager);

    default void afterCheck(Neo4jRestClientNodeManager manager) { }

    default void stop(Neo4jRestClientNodeManager manager) { }

    static Neo4jRestClientCheck create(final Neo4jRestClientNodeManager manager, final Vertx vertx, final String authorizationHeader, final long checkDelay, final JsonObject neo4jConfig) {
        final boolean optimized = neo4jConfig.getBoolean("optimized-check-enable", false);
        final long optimizedDelay = neo4jConfig.getLong("optimized-check-delay", checkDelay);
        final boolean activeCheck = neo4jConfig.getBoolean("optimized-check-active", false);
        final boolean enableHealthCheck = neo4jConfig.getBoolean("healthcheck-enable", true);
        final boolean enableReadCheck = neo4jConfig.getBoolean("readcheck-enable", true);
        final boolean enableNotifications = neo4jConfig.getBoolean("notification-enable", false);
        final String moduleName = BaseServer.getModuleName();
        final Neo4jRestClientCheckGroup group = new Neo4jRestClientCheckGroup(vertx, checkDelay);
        manager.setModuleName(moduleName);
        if (optimized) {
            //optimized mode only check on few modules
            if (activeCheck) {
                if (enableHealthCheck) {
                    //if cluster enable health check to know master and slaves
                    if (manager.isCluster()) {
                        group.add(new Neo4jRestClientCheckHealth(vertx, authorizationHeader));
                    } else {
                        manager.disableClusterCheck();
                    }
                } else {
                    //disable node type checking (any type)
                    manager.disableClusterCheck();
                }
                //add read check only if enable
                if (enableReadCheck) {
                    group.add(new Neo4jRestClientCheckRead(vertx, authorizationHeader, neo4jConfig));
                }else{
                    manager.disableReadCheck();
                }
                //write result to localmap
                group.add(new Neo4jRestClientCheckLocalMap(vertx, true));
                //log warn
                log.info("Neo4j ACTIVE check is starting on module: " + moduleName);
            }else{
                //disabled because we delegate it
                manager.disableClusterCheck();
                manager.disableReadCheck();
                //another module is checking -> read every x ms to avoid a lot concurrrent hashmap access
                group.add(new Neo4jRestClientCheckLocalMap(vertx, false));
                //the delay for reader should me smaller to signal unavailable node as soon as possible
                group.setDelay(optimizedDelay);
                log.info("Neo4j PASSIVE check is starting on module: " + moduleName);
            }
        } else {
            //non optimized mode
            if (enableHealthCheck) {
                //if cluster enable health check to known master and slaves
                if (manager.isCluster()) {
                    group.add(new Neo4jRestClientCheckHealth(vertx, authorizationHeader));
                } else {
                    manager.disableClusterCheck();
                }
            } else {
                //disable node type checking (any type)
                manager.disableClusterCheck();
            }
            //add read check only if enable
            if (enableReadCheck) {
                group.add(new Neo4jRestClientCheckRead(vertx, authorizationHeader, neo4jConfig));
            }else{
                manager.disableReadCheck();
            }
        }
        //notifier
        if(enableNotifications) {
            group.add(new Neo4jRestClientCheckNotifier(vertx, neo4jConfig).setModuleName(moduleName));
        }
        return group;
    }
}
