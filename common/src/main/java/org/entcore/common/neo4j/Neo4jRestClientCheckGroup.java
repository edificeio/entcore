package org.entcore.common.neo4j;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class Neo4jRestClientCheckGroup implements Neo4jRestClientCheck {
    private static Logger log = LoggerFactory.getLogger(Neo4jRestClientCheckGroup.class);
    private final Vertx vertx;
    private long delay;
    private long checkTimerId = -1;
    private final List<Neo4jRestClientCheck> checks = new ArrayList<>();

    Neo4jRestClientCheckGroup(final Vertx vertx, final long delay){
        this.vertx = vertx;
        this.delay = delay;
    }

    public void setDelay(long delay) {
        this.delay = delay;
    }

    public void add(Neo4jRestClientCheck check){
        this.checks.add(check);
    }

    @Override
    public void start(Neo4jRestClientNodeManager manager){
        for(Neo4jRestClientCheck check : checks){
            check.start(manager);
        }
        if(checks.isEmpty()){
            log.warn("Neo4j has no checker configured!");
        } else {
            checkTimerId = vertx.setPeriodic(delay, event -> {
                check(manager);
            });
            check(manager);
        }
    }

    @Override
    public Future<Void> check(Neo4jRestClientNodeManager manager){
        final List<Future> futures = new ArrayList<>();
        for(Neo4jRestClientCheck check : checks){
            try{
                check.beforeCheck(manager);
            }catch(Exception e){
                log.error("Failed to execute Neo4j beforeCheck: "+check.getClass(), e);
            }
        }
        for(Neo4jRestClientCheck check : checks){
            futures.add(check.check(manager).otherwise(e->{
                log.error("Failed to execute Neo4j check: "+check.getClass(), e);
                return null;
            }));
        }
        return CompositeFuture.all(futures).compose(r->{
            for(Neo4jRestClientCheck check : checks){
                try{
                    check.afterCheck(manager);
                }catch(Exception e){
                    log.error("Failed to execute Neo4j afterCheck: "+check.getClass(), e);
                }
            }
            return Future.succeededFuture();
        }).mapEmpty();
    }

    @Override
    public void stop(Neo4jRestClientNodeManager manager){
        for(Neo4jRestClientCheck check : checks){
            check.stop(manager);
        }
        if (checkTimerId > 0) {
            vertx.cancelTimer(checkTimerId);
        }
    }

    public List<Neo4jRestClientCheck> getChecks() {
        return checks;
    }
}
