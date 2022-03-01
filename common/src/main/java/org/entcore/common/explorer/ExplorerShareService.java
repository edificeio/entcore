package org.entcore.common.explorer;

import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.security.SecuredAction;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.share.ShareInfosQuery;
import org.entcore.common.share.ShareService;
import org.entcore.common.share.impl.GenericShareService;
import org.entcore.common.user.UserInfos;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class ExplorerShareService extends GenericShareService implements ShareService {
    private final ShareService shareService;
    private final IExplorerPlugin plugin;

    public ExplorerShareService(final ShareService shareService, final IExplorerPlugin plugin, final EventBus eb,
                                final Map<String, SecuredAction> securedActions, Map<String, List<String>> groupedActions) {
        super(eb, securedActions, groupedActions);
        this.plugin = plugin;
        this.shareService = shareService;
    }

    @Override
    public void shareInfosWithoutVisible(String userId, String resourceId, Handler<Either<String, JsonArray>> handler) {
        this.shareService.shareInfosWithoutVisible(userId, resourceId, handler);
    }

    @Override
    public void inheritShareInfos(String userId, String resourceId, String acceptLanguage, String search, Handler<Either<String, JsonObject>> handler) {
        this.shareService.inheritShareInfos(userId, resourceId, acceptLanguage, search, handler);
    }

    @Override
    public void shareInfos(String userId, String resourceId, String acceptLanguage, String search, Handler<Either<String, JsonObject>> handler) {
        this.shareService.shareInfos(userId, resourceId, acceptLanguage, search, handler);
    }

    @Override
    public void shareInfos(String userId, String resourceId, String acceptLanguage, ShareInfosQuery query, Handler<Either<String, JsonObject>> handler) {
        this.shareService.shareInfos(userId, resourceId, acceptLanguage, query, handler);
    }

    @Override
    public void groupShare(String userId, String groupShareId, String resourceId, List<String> actions, Handler<Either<String, JsonObject>> handler) {
        this.shareService.groupShare(userId, groupShareId, resourceId, actions, handler);
    }

    @Override
    public void userShare(String userId, String userShareId, String resourceId, List<String> actions, Handler<Either<String, JsonObject>> handler) {
        this.shareService.userShare(userId, userShareId, resourceId, actions, handler);
    }

    @Override
    public void removeGroupShare(String groupId, String resourceId, List<String> actions, Handler<Either<String, JsonObject>> handler) {
        this.shareService.removeGroupShare(groupId, resourceId, actions, handler);
    }

    @Override
    public void removeUserShare(String userId, String resourceId, List<String> actions, Handler<Either<String, JsonObject>> handler) {
        this.shareService.removeUserShare(userId, resourceId, actions, handler);
    }

    @Override
    public Future<JsonObject> share(String userId, String resourceId, JsonObject share, Handler<Either<String, JsonObject>> handler) {
        final UserInfos user = new UserInfos();
        user.setUserId(userId);
        return share(user, resourceId, share, handler);
    }

    @Override
    public Future<JsonObject> share(UserInfos user, String resourceId, JsonObject share, Handler<Either<String, JsonObject>> handler) {
        return this.shareService.share(user.getUserId(), resourceId, share, e->{
            if(e.isRight()){
                this.shareService.shareInfosWithoutVisible(user.getUserId(), resourceId, res -> {
                    if(res.isRight()){
                        final JsonArray shared = res.right().getValue();
                        this.plugin.notifyShare(resourceId, user, shared).onComplete(not->{
                            if(not.failed()){
                                log.error("Failed to notify shared: ", not);
                            }
                            handler.handle(e);
                        });
                    }else{
                        log.error("Failed to read shared: "+res.left().getValue());
                    }
                });
            }else{
                handler.handle(e);
            }
        });
    }

    @Override
    public void findUserIdsForShare(String resourceId, String userId, Optional<Set<String>> actions, Handler<AsyncResult<Set<String>>> h) {
        this.shareService.findUserIdsForShare(resourceId, userId, actions, h);
    }

    @Override
    public void findUserIdsForInheritShare(String resourceId, String userId, Optional<Set<String>> actions, Handler<AsyncResult<Set<String>>> h) {
        this.shareService.findUserIdsForInheritShare(resourceId, userId, actions, h);
    }

    @Override
    protected void prepareSharedArray(String resourceId, String type, JsonArray shared, String attr, Set<String> actions) {
        final JsonObject el = new JsonObject().put(type, attr);
        for (String action : actions) {
            el.put(action, true);
        }
        shared.add(el);
    }
}
