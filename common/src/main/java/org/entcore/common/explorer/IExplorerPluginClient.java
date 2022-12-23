package org.entcore.common.explorer;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.explorer.impl.ExplorerPluginClient;
import org.entcore.common.explorer.impl.ExplorerPluginClientDefault;
import org.entcore.common.explorer.to.MuteResponse;
import org.entcore.common.user.UserInfos;

import java.util.*;

public interface IExplorerPluginClient {
    static ExplorerPluginClient withBus(Vertx vertx, String application, String type) {
        return new ExplorerPluginClientDefault(vertx, application, type);
    }

    Future<IndexResponse> getForIndexation(UserInfos user, Optional<Date> from, Optional<Date> to);

    Future<IndexResponse> getForIndexation(UserInfos user, Optional<Date> from, Optional<Date> to, Set<String> apps);

    Future<IndexResponse> getForIndexation(UserInfos user, Optional<Date> from, Optional<Date> to, Set<String> apps, boolean includeFolders);

    Future<List<String>> createAll(UserInfos user, List<JsonObject> json, boolean isCopy);

    Future<DeleteResponse> deleteById(UserInfos user, Set<String> ids);

    default Future<ShareResponse> shareById(UserInfos user, String id, JsonObject shares){
        final Set<String> ids = new HashSet<>();
        ids.add(id);
        return shareByIds(user, ids, shares);
    }

    Future<ShareResponse> shareByIds(UserInfos user, Set<String> ids, JsonObject shares);

    Future<JsonObject> getMetrics(UserInfos user);

    /**
     *
     * @param userInfos User that performed the mute action
     * @param resourceIds entIds of the resources whose mute status should change
     * @param muteStatus <code>true</code> if the user wants to mute the notifications on the resources,
     *                   <code>false</code> if the user want to keep on receiving notifications for the resources
     * @return
     */
    Future<MuteResponse> setMuteStatusByIds(final UserInfos userInfos, final Set<IdAndVersion> resourceIds, final boolean muteStatus);

    class DeleteResponse {
        public final List<String> deleted = new ArrayList<>();
        public final List<String> notDeleted = new ArrayList<>();
    }
    class ShareResponse {
        public final int nbShared;
        public final Map<String, JsonArray> notifyTimelineMap;

        public ShareResponse(int nbShared, JsonObject notifyTimelineMap) {
            this.notifyTimelineMap = (Map<String, JsonArray>)(Object)notifyTimelineMap.getMap();
            this.nbShared = nbShared;
        }
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
