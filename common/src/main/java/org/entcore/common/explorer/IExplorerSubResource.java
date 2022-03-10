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
import java.util.Optional;
import java.util.Set;

public interface IExplorerSubResource {

    Future<Void> notifyUpsert(String id, UserInfos user, JsonObject source);

    Future<Void> notifyUpsert(UserInfos user, Map<String, JsonObject> sourceById);

    Future<Void> notifyUpsert(UserInfos user, JsonObject source);

    Future<Void> notifyUpsert(UserInfos user, List<JsonObject> sources);

    Future<Void> notifyUpsert(ExplorerMessage m);

    Future<Void> notifyUpsert(List<ExplorerMessage> messages);

    Future<Void> notifyDeleteById(UserInfos user, String parentId, String id);

    Future<Void> notifyDeleteById(UserInfos user, String parentId, List<String> ids);

    Future<Void> notifyDelete(UserInfos user, JsonObject source);

    Future<Void> notifyDelete(UserInfos user, List<JsonObject> sources);

    Future<JsonObject> reindex(final Optional<Long> from, final Optional<Long> to);
}
