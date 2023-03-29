package org.entcore.common.explorer.impl;

import fr.wseduc.mongodb.MongoDb;
import fr.wseduc.webutils.security.SecuredAction;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import static java.lang.System.currentTimeMillis;
import org.entcore.common.explorer.ExplorerMessage;
import org.entcore.common.explorer.ExplorerStream;
import org.entcore.common.explorer.IExplorerFolderTree;
import org.entcore.common.explorer.IExplorerPlugin;
import org.entcore.common.explorer.IExplorerPluginCommunication;
import org.entcore.common.explorer.IExplorerSubResource;
import org.entcore.common.explorer.IdAndVersion;
import org.entcore.common.explorer.IngestJobState;
import org.entcore.common.explorer.to.MuteRequest;
import org.entcore.common.explorer.to.MuteResponse;
import org.entcore.common.share.ShareModel;
import org.entcore.common.share.ShareService;
import org.entcore.common.share.impl.MongoDbShareService;
import org.entcore.common.share.impl.SqlShareService;
import org.entcore.common.user.UserInfos;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static io.vertx.core.json.JsonObject.mapFrom;
import static java.lang.System.currentTimeMillis;

public abstract class ExplorerPlugin implements IExplorerPlugin {
    public static final String RESOURCES_ADDRESS = "explorer.resources";
    public static final String RESOURCES_GETSHARE = "getshare";
    protected final Logger log = LoggerFactory.getLogger(getClass());
    protected final IExplorerPluginCommunication communication;
    protected Function<Void, Void> listener;
    protected Function<Void, Void> listenerIngestJobUpdate;
    protected JsonObject explorerConfig = new JsonObject();
    protected Integer reindexBatchSize = 100;
    protected Optional<IExplorerFolderTree> folderTree = Optional.empty();
    protected final List<IExplorerSubResource> subResources = new ArrayList<>();

    protected ExplorerPlugin(final IExplorerPluginCommunication communication) {
        this.communication = communication;
    }

    public void setFolderTree(final IExplorerFolderTree folderTree) {
        this.folderTree = Optional.ofNullable(folderTree);
    }

    public ExplorerPlugin addSubResource(final IExplorerSubResource sub) {
        this.subResources.add(sub);
        return this;
    }

    public List<IExplorerSubResource> getSubResources() {
        return subResources;
    }

    @Override
    public IExplorerPlugin setConfig(final JsonObject config) {
        explorerConfig = config;
        reindexBatchSize = config.getInteger("reindexBatchSize",reindexBatchSize);
        return this;
    }

    protected void onExplorerQuery(final Message<JsonObject> message) {
        final String actionStr = message.headers().get("action");
        final String userId = message.headers().get("userId");
        final String userName = message.headers().get("userName");
        final ExplorerRemoteAction action = ExplorerRemoteAction.valueOf(actionStr);
        final UserInfos user = new UserInfos();
        user.setUserId(userId);
        user.setUsername(userName);
        switch (action) {
            case QueryShare: {
                final JsonObject shares = message.body().getJsonObject("shares", new JsonObject());
                final JsonArray values = message.body().getJsonArray("resources", new JsonArray());
                onShareAction(message, user, values, shares);
                break;
            }
            case QueryCreate: {
                final JsonArray values = message.body().getJsonArray("resources", new JsonArray());
                final boolean copy = message.body().getBoolean("copy", false);
                onCreateAction(message, user, values, copy);
                break;
            }
            case QueryDelete: {
                final JsonArray values = message.body().getJsonArray("resources", new JsonArray());
                onDeleteAction(message, user, values);
                break;
            }
            case QueryReindex: {
                final boolean includeFolders = message.body().getBoolean("includeFolders", false);
                final Optional<Long> from = Optional.ofNullable(message.body().getLong("from"));
                final Optional<Long> to = Optional.ofNullable(message.body().getLong("to"));
                final Optional<JsonArray> apps = Optional.ofNullable(message.body().getJsonArray("apps"));
                onReindexAction(message, from , to, apps, includeFolders);
                break;
            }
            case QueryMetrics: {
                //TODO
                message.reply(new JsonObject());
                break;
            }
        }
    }

    protected void onCreateAction(final Message<JsonObject> message, final UserInfos user, final JsonArray values, final boolean copy){
        final long now = currentTimeMillis();
        final List<JsonObject> jsons = values.stream().filter(e -> e instanceof JsonObject).map(e -> (JsonObject) e).collect(Collectors.toList());
        for (JsonObject json : jsons) {
            this.setVersion(json, now);
        }
        doCreate(user, jsons, copy).onComplete(idsRes -> {
            if (idsRes.succeeded()) {
                final List<String> ids = idsRes.result();
                final List<JsonObject> safeSources = new ArrayList<>();
                final List<String> safeIds = new ArrayList<>();
                for (int i = 0; i < jsons.size() && i < ids.size(); i++) {
                    safeSources.add(jsons.get(i));
                    safeIds.add(ids.get(i));
                }
                toMessage(safeSources, source -> {
                    final int index = safeSources.indexOf(source);
                    final String id = safeIds.get(index);
                    final ExplorerMessage mess = ExplorerMessage.upsert(
                            new IdAndVersion(id, now), user, isForSearch(),
                            getApplication(), getResourceType(), getResourceType());
                    return mess;
                }).compose(messages -> {
                    return communication.pushMessage(messages);
                }).onComplete(resPush -> {
                    if (resPush.succeeded()) {
                        message.reply(new JsonArray(ids));
                    } else {
                        message.reply(new JsonArray(ids), new DeliveryOptions().addHeader("error", ExplorerRemoteError.CreatePushFailed.getError()));
                        log.error("Failed to push created resource coming from explorer: ", idsRes.cause());
                    }
                });
            } else {
                message.fail(500, ExplorerRemoteError.CreateFailed.getError());
                log.error("Failed to create resource coming from explorer: ", idsRes.cause());
            }
        });
    }

    protected void onDeleteAction(final Message<JsonObject> message, final UserInfos user, final JsonArray values){
        final long now = System.currentTimeMillis();
        final List<String> ids = values.stream().filter(e -> e instanceof String).map(e -> (String) e).collect(Collectors.toList());
        doDelete(user, ids).onComplete(idsRes -> {
            if (idsRes.succeeded()) {
                final List<Boolean> deleteStatus = idsRes.result();
                final List<ExplorerMessage> messages = new ArrayList<>();
                final List<String> ok = new ArrayList<>();
                final List<String> nok = new ArrayList<>();
                for (int i = 0; i < deleteStatus.size() && i < ids.size(); i++) {
                    if (deleteStatus.get(i)) {
                        final ExplorerMessage mess = ExplorerMessage.delete(new IdAndVersion(ids.get(i), now), user,isForSearch())
                                .withType(getApplication(), getResourceType(), getResourceType());
                        messages.add(mess);
                        ok.add(ids.get(i));
                    } else {
                        nok.add(ids.get(i));
                    }
                }
                if(!ok.isEmpty()){
                    for(final IExplorerSubResource sub : this.subResources){
                        sub.onDeleteParent(ok);
                    }
                }
                communication.pushMessage(messages).onComplete(resPush -> {
                    final JsonObject payload = new JsonObject().put("deleted", new JsonArray(ok)).put("failed", new JsonArray(nok));
                    if (resPush.succeeded()) {
                        message.reply(payload);
                    } else {
                        message.reply(payload, new DeliveryOptions().addHeader("error", ExplorerRemoteError.DeletePushFailed.getError()));
                        log.error("Failed to push deleted resource coming from explorer: ", idsRes.cause());
                    }
                });
            } else {
                message.fail(500, ExplorerRemoteError.DeleteFailed.getError());
                log.error("Failed to delete resource coming from explorer: ", idsRes.cause());
            }
        });
    }

    protected void onShareAction(final Message<JsonObject> message, final UserInfos user, final JsonArray values, final JsonObject shares){
        final long now = currentTimeMillis();
        final List<String> ids = values.stream().filter(e -> e instanceof String).map(e -> (String) e).collect(Collectors.toList());
        final Optional<ShareService> shareServiceOpt = getShareService();
        if(shareServiceOpt.isPresent()){
            final List<Future> futures = new ArrayList<>();
            final String userId = user.getUserId();
            final Map<String, JsonObject> generatedShared = new HashMap<>();
            final JsonObject notifyTimeline = new JsonObject();
            for(final String resourceId: ids){
                final Promise<Void> promise = Promise.promise();
                futures.add(promise.future());
                futures.add(shareServiceOpt.get().share(userId,resourceId,shares, e->{
                    if(e.isRight()){
                        final JsonArray nta = e.right().getValue().getJsonArray("notify-timeline-array");
                        notifyTimeline.put(resourceId, nta);
                        promise.complete(null);
                        //TODO notify timeline....
                    }

                }).compose(e->{
                    generatedShared.put(resourceId, e);
                    return Future.succeededFuture(e);
                }));
            }
            CompositeFuture.all(futures).onComplete(all->{
                if (all.succeeded()) {
                    final List<ExplorerMessage> messages = new ArrayList<>();
                    for(final String id : ids) {
                        final ExplorerMessage mess = ExplorerMessage.upsert(
                                new IdAndVersion(id, now), user, isForSearch(),
                                getApplication(), getResourceType(), getResourceType());
                        final JsonArray shared = generatedShared.get(id).getJsonArray("shared");
                        mess.withShared(new ShareModel(shared, getSecuredActions(), Optional.empty()));
                        mess.withVersion(now);
                        mess.withType(getApplication(), getResourceType(), getResourceType());
                        messages.add(mess);
                    }
                    communication.pushMessage(messages).onComplete(resPush -> {
                        final JsonObject payload = new JsonObject().put("nbShared", ids.size()).put("notifyTimelineMap", notifyTimeline);
                        if (resPush.succeeded()) {
                            message.reply(payload);
                        } else {
                            message.reply(payload, new DeliveryOptions().addHeader("error", ExplorerRemoteError.ShareFailedPush.getError()));
                            log.error("Failed to push shared resource coming from explorer: ", resPush.cause());
                        }
                    });
                } else {
                    message.fail(500, ExplorerRemoteError.ShareFailed.getError());
                    log.error("Failed to share resource coming from explorer: ", all.cause());
                }
            });
        }else{
            message.fail(500, ExplorerRemoteError.ShareFailedMissing.getError());
            log.error("Missing share service");
        }
    }

    protected void onReindexAction(final Message<JsonObject> message, final Optional<Long> from, final Optional<Long> to, final Optional<JsonArray> apps, final boolean includeFolders){
        final long now = currentTimeMillis();
        if(apps.isPresent() && !apps.get().contains(getApplication())){
            log.info(String.format("Skip indexation for app=%s filter=%s", getApplication(), apps));
            message.reply(new JsonObject());
            return;
        }
        log.info(String.format("Starting indexation for app=%s type=%s from=%s to=%s includeFolders=%s",getApplication(), getResourceType(), from, to, includeFolders));
        final JsonObject metrics = new JsonObject();
        final ExplorerStream<JsonObject> stream = new ExplorerStream<>(reindexBatchSize, bulk -> {
            // TODO JBE missing saving state and version here
            for (JsonObject entry : bulk) {
                setVersion(entry, now);
            }
            return toMessage(bulk, e -> {
                final String id = getIdForModel(e);
                final UserInfos user = getCreatorForModel(e).orElseGet(() -> {
                    log.error("Could not found creator for "+getApplication()+ " with id : "+id);
                    return new UserInfos();
                });
                final ExplorerMessage mess = ExplorerMessage.upsert(
                        new IdAndVersion(id, now), user, isForSearch(),
                        getApplication(), getResourceType(), getResourceType());
                mess.withVersion(now);
                return mess;
            }).compose(messages -> {
                return communication.pushMessage(messages);
            });
        }, metricsEnd -> {
            log.info(String.format("Ending indexation for app=%s type=%s from=%s to=%s metrics=%s",getApplication(), getResourceType(), from, to, metricsEnd));
            metrics.mergeIn(metricsEnd);
        });
        doFetchForIndex(stream, from.map(e -> new Date(e)), to.map(e -> new Date(e)));
        stream.getEndFuture().onComplete(root->{
            if(root.succeeded()){
                //reindex subresources
                final List<Future> futures = new ArrayList<>();
                for(final IExplorerSubResource sub : this.subResources){
                    futures.add(sub.reindex(from, to).onSuccess(submetrics->{
                        final JsonArray tmp = metrics.getJsonArray("subresources", new JsonArray());
                        tmp.add(submetrics);
                        metrics.put("subresources", tmp);
                    }));
                }
                //reindex folders
                if(includeFolders && folderTree.isPresent()){
                    futures.add(folderTree.get().reindex(from, to).onSuccess(folderMetrics->{
                        metrics.put("folders", folderMetrics);
                    }));
                }
                //wait all
                CompositeFuture.all(futures).onComplete(e->{
                    if(e.succeeded()){
                        message.reply(metrics);
                    }else{
                        message.fail(500, ExplorerRemoteError.ReindexFailed.getError());
                        log.error("Failed to reindex subresources: "+ getApplication(), root.cause());
                    }
                });
            }else{
                message.fail(500, ExplorerRemoteError.ReindexFailed.getError());
                log.error("Failed to reindex resources: "+ getApplication(), root.cause());
            }
        });
    }

    @Override
    public Future<JsonArray> getShareInfo(String id) {
        return getShareInfo(new HashSet<>(Arrays.asList(id))).map(e->{
            return e.getOrDefault(id, new JsonArray());
        });
    }

    @Override
    public Future<Map<String, JsonArray>> getShareInfo(Set<String> ids) {
        final JsonArray payload = new JsonArray(new ArrayList(ids));
        final Promise<Map<String,JsonArray>> promise = Promise.promise();
        final DeliveryOptions deliveryOptions = new DeliveryOptions().addHeader("action", RESOURCES_GETSHARE);
        communication.vertx().eventBus().request(RESOURCES_ADDRESS, payload, deliveryOptions, message -> {
            if(message.succeeded()) {
                final Map<String, JsonArray> map = new HashMap<>();
                final JsonObject received = (JsonObject) message.result().body();
                for(final String id : ids){
                    map.put(id, received.getJsonArray(id, new JsonArray()));
                }
                promise.complete(map);
            }else{
                promise.fail(message.cause());
            }
        });
        return promise.future();
    }

    @Override
    public Future<Void> notifyShare(IdAndVersion id, UserInfos user, JsonArray shared) {
        final ExplorerMessage message = ExplorerMessage.upsert(id, user, isForSearch(),
                getApplication(), getResourceType(), getResourceType());
        message.withShared(new ShareModel(shared, getSecuredActions(), Optional.empty()));
        return communication.pushMessage(message);
    }

    @Override
    public Future<Void> notifyShare(final Set<IdAndVersion> ids, final UserInfos user, final JsonArray shared) {
        final List<ExplorerMessage> messages = ids.stream().map(id->{
            final ExplorerMessage message = ExplorerMessage.upsert(id, user, isForSearch(),
                    getApplication(), getResourceType(), getResourceType());
            return message.withShared(new ShareModel(shared, getSecuredActions(), Optional.empty()));
        }).collect(Collectors.toList());
        return communication.pushMessage(messages);
    }

    @Override
    public Future<Void> notifyUpsert(final UserInfos user, final Map<String, JsonObject> sourceById, final Optional<Number> folderId) {
        final List<Future> futures = sourceById.entrySet().stream().map(e->{
            final String id = e.getKey();
            final ExplorerMessage message = ExplorerMessage.upsert(
                    new IdAndVersion(id, e.getValue().getLong("version")), user, isForSearch(),
                    getApplication(), getResourceType(), getResourceType());
            message.withType(getApplication(), getResourceType(), getResourceType());
            message.withParentId(folderId.map(folId -> folId.longValue()));
            return toMessage(message, e.getValue());
        }).collect(Collectors.toList());
        return CompositeFuture.all(futures).compose(all->{
            final List<ExplorerMessage> messages = all.list();
            return communication.pushMessage(messages);
        });
    }

    @Override
    public Future<Void> notifyUpsert(final IdAndVersion id, UserInfos user, final JsonObject source, final Optional<Number> folderId) {
        final ExplorerMessage message = ExplorerMessage.upsert(
                id, user, isForSearch(),
                getApplication(), getResourceType(), getResourceType());
        message.withType(getApplication(), getResourceType(), getResourceType());
        message.withParentId(folderId.map(folId -> folId.longValue()));
        return toMessage(message, source).compose(messages -> {
            return communication.pushMessage(messages);
        });
    }

    @Override
    public Future<Void> notifyUpsert(final UserInfos user, final JsonObject source, final Optional<Number> folderId) {
        final ExplorerMessage message = ExplorerMessage.upsert(
                new IdAndVersion(getIdForModel(source), source.getLong("version")), user, isForSearch(),
                getApplication(), getResourceType(), getResourceType());
        message.withType(getApplication(), getResourceType(), getResourceType());
        message.withParentId(folderId.map(folId -> folId.longValue()));
        return toMessage(message, source).compose(messages -> {
            return communication.pushMessage(messages);
        });
    }

    @Override
    public Future<Void> notifyUpsert(final UserInfos user, final List<JsonObject> sources, final Optional<Number> folderId) {
        setIngestJobState(sources, IngestJobState.TO_BE_SENT);
        return toMessage(sources, e -> {
            final ExplorerMessage message = ExplorerMessage.upsert(
                    new IdAndVersion(getIdForModel(e), e.getLong("version")), user, isForSearch(),
                    getApplication(), getResourceType(), getResourceType());
            message.withType(getApplication(), getResourceType(), getResourceType());
            message.withParentId(folderId.map(folId -> folId.longValue()));
            return message;
        }).compose(messages -> {
            return communication.pushMessage(messages);
        });
    }

    @Override
    public Future<Void> notifyUpsert(final ExplorerMessage m) {
        m.withType(getApplication(), getResourceType(), getResourceType());
        return communication.pushMessage(Arrays.asList(m));
    }

    @Override
    public Future<Void> notifyUpsert(final List<ExplorerMessage> messages) {
        for(final ExplorerMessage m : messages){
            m.withType(getApplication(), getResourceType(), getResourceType());
        }
        return communication.pushMessage(messages);
    }

    @Override
    public Future<Void> notifyDeleteById(final UserInfos user, final IdAndVersion id) {
        final ExplorerMessage message = ExplorerMessage.delete(id, user, isForSearch())
        .withVersion(currentTimeMillis())
        .withType(getApplication(), getResourceType(), getResourceType());
        return communication.pushMessage(message);
    }

    @Override
    public Future<Void> notifyDeleteById(final UserInfos user, final List<IdAndVersion> ids) {
        final List<ExplorerMessage> messages = ids.stream().map(id->{
            final ExplorerMessage message = ExplorerMessage.delete(id, user, isForSearch())
                    .withVersion(currentTimeMillis())
                    .withType(getApplication(), getResourceType(), getResourceType());
            return message;
        }).collect(Collectors.toList());
        return communication.pushMessage(messages);
    }

    @Override
    public Future<Void> notifyDelete(final UserInfos user, final JsonObject source) {
        final String id = getIdForModel(source);
        final long version = source.getLong("version");
        final ExplorerMessage message = ExplorerMessage.delete(new IdAndVersion(id, version), user, isForSearch())
                .withType(getApplication(), getResourceType(), getResourceType());
        return toMessage(message, source).compose(messages -> {
            return communication.pushMessage(messages);
        });
    }

    @Override
    public Future<Void> notifyDelete(final UserInfos user, final List<JsonObject> sources) {
        return toMessage(sources, e -> {
            final String id = getIdForModel(e);
            final long version = e.getLong("version");
            final ExplorerMessage message = ExplorerMessage.delete(new IdAndVersion(id, version), user, isForSearch())
                    .withVersion(currentTimeMillis())
                    .withType(getApplication(), getResourceType(), getResourceType());
            return message;
        }).compose(messages -> {
            return communication.pushMessage(messages);
        });
    }


    protected final Future<List<ExplorerMessage>> toMessage(final List<JsonObject> sources, final Function<JsonObject, ExplorerMessage> builder) {
        final List<Future> futures = sources.stream().map(e -> toMessage(builder.apply(e), e)).collect(Collectors.toList());
        return CompositeFuture.all(futures).map(e -> new ArrayList<ExplorerMessage>(e.list()));
    }

    @Override
    public final Future<String> create(final UserInfos user, final JsonObject source, final boolean isCopy, final Optional<Number> folderId){
        return doCreate(user, Arrays.asList(source), isCopy).compose(ids->{
            setIdForModel(source, ids.get(0));
            return notifyUpsert(user, source, folderId).map(ids);
        }).map(ids -> ids.get(0));
    }

    @Override
    public final Future<List<String>> create(final UserInfos user, final List<JsonObject> sources, final boolean isCopy, final Optional<Number> folderId){
        this.setIngestJobState(sources, IngestJobState.TO_BE_SENT);
        return doCreate(user, sources, isCopy).compose(ids->{
            for(int i = 0 ; i < ids.size(); i++){
                setIdForModel(sources.get(i), ids.get(i));
            }
            return notifyUpsert(user, sources, folderId).map(ids);
        }).map(ids -> ids);
    }

    @Override
    public final Future<Boolean> delete(final UserInfos user, final String id){
        return delete(user, Arrays.asList(id)).map(e-> e.get(0));
    }

    @Override
    public final Future<List<Boolean>> delete(final UserInfos user, final List<String> ids){
        return doDelete(user, ids).compose(oks->{
            final List<JsonObject> sources = ids.stream().map(id -> {
                final JsonObject source = new JsonObject();
                setIdForModel(source, id);
                setVersion(source, currentTimeMillis());
                return source;
            }).collect(Collectors.toList());
            return notifyDelete(user, sources).map(oks);
        });
    }

    @Override
    public IExplorerPluginCommunication getCommunication() {
        return communication;
    }

    @Override
    public ShareService createMongoShareService(String collection, Map<String, SecuredAction> securedActions, Map<String, List<String>> groupedActions) {
        return createMongoShareService(communication.vertx().eventBus(), MongoDb.getInstance(), collection, securedActions, groupedActions);
    }

    @Override
    public ShareService createMongoShareService(EventBus eb, MongoDb mongo, String collection, Map<String, SecuredAction> securedActions, Map<String, List<String>> groupedActions) {
        final ShareService inner = new MongoDbShareService(eb , mongo, collection, securedActions, groupedActions);
        return new ExplorerShareService(inner, this, eb, securedActions, groupedActions);
    }

    @Override
    public ShareService createPostgresShareService(String schema, String shareTable, EventBus eb, Map<String, SecuredAction> securedActions, Map<String, List<String>> groupedActions){
        final ShareService inner = new SqlShareService(schema, shareTable, eb, securedActions, groupedActions);
        return new ExplorerShareService(inner, this, eb, securedActions, groupedActions);
    }

    @Override
    public ShareService createPostgresShareService(Map<String, SecuredAction> securedActions, Map<String, List<String>> groupedActions) {
        return createPostgresShareService(communication.vertx().eventBus(), securedActions, groupedActions);
    }

    @Override
    public ShareService createPostgresShareService(EventBus eb, Map<String, SecuredAction> securedActions, Map<String, List<String>> groupedActions) {
        final ShareService inner = new SqlShareService(eb , securedActions, groupedActions);
        return new ExplorerShareService(inner, this, eb, securedActions, groupedActions);
    }

    //abstract
    protected abstract Map<String, SecuredAction> getSecuredActions();

    protected abstract Optional<UserInfos> getCreatorForModel(final JsonObject json);

    protected abstract Date getCreatedAtForModel(final JsonObject json);

    protected abstract String getIdForModel(final JsonObject json);

    protected abstract JsonObject setIdForModel(final JsonObject json, final String id);

    protected abstract String getApplication();

    protected abstract boolean isForSearch();

    protected abstract String getResourceType();

    protected abstract Optional<ShareService> getShareService();

    protected abstract void doFetchForIndex(final ExplorerStream<JsonObject> stream, final Optional<Date> from, final Optional<Date> to);

    protected abstract Future<List<String>> doCreate(final UserInfos user, final List<JsonObject> sources, final boolean isCopy);

    protected abstract Future<List<Boolean>> doDelete(final UserInfos user, final List<String> ids);

    protected final Future<ExplorerMessage> toMessage(final ExplorerMessage message, final JsonObject source) {
        message.withType(getApplication(), getResourceType(), getResourceType());
        message.withVersion(source.getLong("version"));
        // optional in case of update
        final Optional<UserInfos> creator = getCreatorForModel(source);
        if(creator.isPresent()){
            message.withCreator(creator.get());
        }
        message.withCreatedAt(getCreatedAtForModel(source));
        return doToMessage(message, source);
    }

    protected abstract Future<ExplorerMessage> doToMessage(final ExplorerMessage message, final JsonObject source);

    protected abstract List<ExplorerSubResource> getSubResourcesPlugin() ;

    @Override
    public void start() {
        final String id = IExplorerPlugin.addressFor(getApplication(), getResourceType());
        this.listener = communication.listen(id, message -> {
            onExplorerQuery(message);
        });
        final String idUpdate = IExplorerPlugin.addressForIngestStateUpdate(getApplication(), getResourceType());
        this.listenerIngestJobUpdate = communication.listenForAcks(idUpdate, messages -> {
            onJobStateUpdatedMessageReceived(messages)
                .onSuccess(e -> log.debug("Update successul of " + messages.size() + " messages"))
                .onFailure(th -> log.error("Update error of " + messages, th));
        });
        for (ExplorerSubResource explorerSubResource : getSubResourcesPlugin()) {
            explorerSubResource.start();
        }
    }

    @Override
    public void stop() {
        if (this.listener != null) {
            this.listener.apply(null);
        }
        this.listener = null;
        if (this.listenerIngestJobUpdate != null) {
            this.listenerIngestJobUpdate.apply(null);
        }
        this.listenerIngestJobUpdate = null;
        for (ExplorerSubResource explorerSubResource : getSubResourcesPlugin()) {
            explorerSubResource.stop();
        }
    }


}
