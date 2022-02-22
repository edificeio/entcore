package org.entcore.common.explorer;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.entcore.common.user.UserInfos;

import java.util.*;

public interface IExplorerPluginClient {
    static ExplorerPluginClient withBus(Vertx vertx, String application, String type) {
        return new ExplorerPluginClientDefault(vertx, application, type);
    }

    Future<IndexResponse> getForIndexation(UserInfos user, Optional<Date> from, Optional<Date> to);

    Future<List<String>> createAll(UserInfos user, List<JsonObject> json, boolean isCopy);

    Future<DeleteResponse> deleteById(UserInfos user, Set<String> ids);

    Future<JsonObject> getMetrics(UserInfos user);

    class DeleteResponse {
        public final List<String> deleted = new ArrayList<>();
        public final List<String> notDeleted = new ArrayList<>();
    }

    class IndexResponse {
        public final int nbBatch;
        public final int nbMessage;

        public IndexResponse(int nbBatch, int nbMessage) {
            this.nbBatch = nbBatch;
            this.nbMessage = nbMessage;
        }

        public JsonObject toJson() {
            return new JsonObject().put("nbBatch", nbBatch).put("nbMessage", nbMessage);
        }

    }
}
