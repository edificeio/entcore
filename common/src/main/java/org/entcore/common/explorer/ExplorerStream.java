package org.entcore.common.explorer;


import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;

import java.util.List;
import java.util.function.Function;

public class ExplorerStream<T> {
    final Function<List<T>,Future<Void>> handler;
    final Handler<JsonObject> onEnd;
    private final JsonObject metrics = new JsonObject();

    public ExplorerStream(final Function<List<T>,Future<Void>> h, final Handler<JsonObject> onEnd){
        this.handler = h;
        this.onEnd = onEnd;
        metrics.put("nb_batch", 0);
        metrics.put("nb_message", 0);
    }

    public Future<Void> add(List<T> batch){
        metrics.put("nb_batch", metrics.getInteger("nb_batch", 0)+1);
        metrics.put("nb_message", metrics.getInteger("nb_message", 0)+batch.size());
        return this.handler.apply(batch);
    }

    public Future<Void> end(List<T> lastBatch){
        return this.handler.apply(lastBatch).onComplete(e->{
            this.end();
        });
    }

    public void end(){
        this.onEnd.handle(metrics);
    }
}
