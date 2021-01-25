package org.entcore.common.neo4j;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.shareddata.LocalMap;

public class Neo4jRestClientCheckLocalMap implements  Neo4jRestClientCheck{
    private final boolean writeCheck;
    private final LocalMap<String, JsonArray> map;
    final JsonArray lastMasters = new JsonArray();
    final JsonArray lastSlaves = new JsonArray();
    private boolean changed = false;

    Neo4jRestClientCheckLocalMap(final Vertx vertx, final boolean writeCheck){
        this.writeCheck = writeCheck;
        this.map = vertx.sharedData().getLocalMap("Neo4jRestClientCheckLocalMap");
    }

    @Override
    public Future<Void> check(Neo4jRestClientNodeManager manager) {
        if(!writeCheck) {
            final JsonArray masters = map.getOrDefault("masters", new JsonArray());
            final JsonArray slaves = map.getOrDefault("slaves", new JsonArray());
            for(final Neo4jRestClientNode node : manager.getClients()){
                if(masters.contains(node.getUrl())){
                    node.setMaster().setAvailable(true);
                }else if(slaves.contains(node.getUrl())){
                    node.setSlave().setAvailable(true);
                } else {
                    node.setAvailable(false);
                }
            }
        }
        return Future.succeededFuture();
    }

    @Override
    public void afterCheck(Neo4jRestClientNodeManager manager) {
        if(writeCheck){
            final JsonArray masters = new JsonArray();
            final JsonArray slaves = new JsonArray();
            for(final Neo4jRestClientNode node: manager.getClients()){
                if(node.isMasterAvailable()){
                    masters.add(node.getUrl());
                }
                if(node.isSlaveAvailable()){
                    slaves.add(node.getUrl());
                }
            }
            this.changed = false;
            if(hasChanged(lastMasters, masters)) {
                map.put("masters", masters);
                this.changed = true;
            }
            if(hasChanged(lastSlaves, slaves)) {
                map.put("slaves", slaves);
                this.changed = true;
            }
            //
            lastMasters.clear().addAll(masters);
            lastSlaves.clear().addAll(slaves);
        }
    }

    private boolean hasChanged(JsonArray previous, JsonArray current){
        if(previous.size() != current.size()){
            return true;
        }
        for(Object o : current){
            if(!previous.contains(o)){
                return true;
            }
        }
        return false;
    }

    public boolean isChanged() {
        return changed;
    }
}
