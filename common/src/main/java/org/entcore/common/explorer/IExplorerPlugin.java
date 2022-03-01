package org.entcore.common.explorer;

import fr.wseduc.mongodb.MongoDb;
import fr.wseduc.webutils.security.SecuredAction;
import io.vertx.core.Future;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.share.ShareService;
import org.entcore.common.user.UserInfos;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface IExplorerPlugin {
    static String addressFor(String application, String resourceType) {
        final String id = String.format("explorer.application.%s.%s", application, resourceType);
        return id;
    }

    void start();

    void stop();

    ShareService createPostgresShareService(Map<String, SecuredAction> securedActions, Map<String, List<String>> groupedActions);

    ShareService createPostgresShareService(EventBus eb, Map<String, SecuredAction> securedActions, Map<String, List<String>> groupedActions);

    ShareService createMongoShareService(String collection, Map<String, SecuredAction> securedActions, Map<String, List<String>> groupedActions);

    ShareService createMongoShareService(EventBus eb, MongoDb mongo, String collection, Map<String, SecuredAction> securedActions, Map<String, List<String>> groupedActions);

    IExplorerPlugin setConfig(JsonObject config);

    Future<JsonArray> getShareInfo(String id);

    Future<Map<String, JsonArray>> getShareInfo(Set<String> id);

    Future<Void> notifyShare(String id, UserInfos user, JsonArray shared);

    Future<Void> notifyShare(Set<String> id, UserInfos user, JsonArray shared);

    Future<Void> notifyUpsert(String id, UserInfos user, JsonObject source);

    Future<Void> notifyUpsert(UserInfos user, Map<String, JsonObject> sourceById);

    Future<Void> notifyUpsert(UserInfos user, JsonObject source);

    Future<Void> notifyUpsert(UserInfos user, List<JsonObject> sources);

    Future<Void> notifyUpsert(ExplorerMessage m);

    Future<Void> notifyUpsert(List<ExplorerMessage> messages);

    Future<Void> notifyDeleteById(UserInfos user, String id);

    Future<Void> notifyDeleteById(UserInfos user, List<String> ids);

    Future<Void> notifyDelete(UserInfos user, JsonObject source);

    Future<Void> notifyDelete(UserInfos user, List<JsonObject> sources);

    Future<String> create(UserInfos user, JsonObject source, boolean isCopy);

    Future<List<String>> create(UserInfos user, List<JsonObject> sources, boolean isCopy);

    Future<Boolean> delete(UserInfos user, String id);

    Future<List<Boolean>> delete(UserInfos user, List<String> ids);

    IExplorerPluginCommunication getCommunication();

    enum ExplorerRemoteAction {
        QueryReindex,
        QueryCreate,
        QueryDelete,
        QueryMetrics,
    }

    enum ExplorerRemoteError {
        CreateFailed("explorer.remote.error.create"),
        CreatePushFailed("explorer.remote.error.create_push"),
        DeleteFailed("explorer.remote.error.delete"),
        DeletePushFailed("explorer.remote.error.delete_push");
        private final String error;

        ExplorerRemoteError(final String e) {
            this.error = e;
        }

        public String getError() {
            return error;
        }
    }
}
