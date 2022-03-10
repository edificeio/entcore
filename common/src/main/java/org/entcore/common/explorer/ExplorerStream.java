package org.entcore.common.explorer;


import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class ExplorerStream<T> {
    final Function<List<T>,Future<Void>> handler;
    final Handler<JsonObject> onEnd;
    private final JsonObject metrics = new JsonObject();
    private final Integer batchSize;
    private final List<T> pending = new ArrayList<>();
    private final Promise<JsonObject> endPromise = Promise.promise();

    public ExplorerStream(final Integer batchSize, final Function<List<T>,Future<Void>> h, final Handler<JsonObject> onEnd){
        this.handler = h;
        this.onEnd = onEnd;
        metrics.put("nb_batch", 0);
        metrics.put("nb_message", 0);
        this.batchSize = batchSize;
    }

    public Future<Void> add(List<T> batch){
        metrics.put("nb_batch", metrics.getInteger("nb_batch", 0)+1);
        metrics.put("nb_message", metrics.getInteger("nb_message", 0)+batch.size());
        pending.addAll(batch);
        if(batchSize <= pending.size()){
            final List<T> toTrigger = new ArrayList<>(pending);
            pending.clear();
            return this.handler.apply(toTrigger);
        }else{
            return Future.succeededFuture();
        }
    }

    public Future<Void> end(List<T> lastBatch){
        final List<T> toTrigger = new ArrayList<>(pending);
        toTrigger.addAll(lastBatch);
        pending.clear();
        if(toTrigger.isEmpty()){
            this.onEnd.handle(metrics);
            this.endPromise.complete(metrics);
            return Future.succeededFuture();
        }else{
            return this.handler.apply(toTrigger).onComplete(e->{
                this.onEnd.handle(metrics);
                this.endPromise.complete(metrics);
            });
        }
    }

    public Future<Void> end(){
        return this.end(new ArrayList<>());
    }

    public Future<JsonObject> getEndFuture() {
        return endPromise.future();
    }
}
