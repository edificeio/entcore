package org.entcore.common.explorer;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import org.entcore.common.user.UserInfos;

import java.util.List;

public interface IExplorerPlugin {
    static String addressFor(String application, String resourceType) {
        final String id = String.format("explorer.application.%s.%s", application, resourceType);
        return id;
    }

    void start();

    void stop();

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

    public enum ExplorerRemoteAction {
        QueryReindex,
        QueryCreate,
        QueryDelete,
        QueryMetrics,
    }

    public enum ExplorerRemoteError {
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
