package org.entcore.common.explorer.impl;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import static java.lang.System.currentTimeMillis;
import org.entcore.common.explorer.ExplorerMessage;
import org.entcore.common.explorer.ExplorerStream;
import org.entcore.common.explorer.IExplorerPlugin;
import org.entcore.common.explorer.IExplorerPluginCommunication;
import org.entcore.common.explorer.IExplorerSubResource;
import org.entcore.common.explorer.IdAndVersion;
import org.entcore.common.user.UserInfos;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

// TODO JBER so far we have considered that every delete version can be set to now but that is not necessarily true
// We should get it from outside of these functions
public abstract class ExplorerSubResource implements IExplorerSubResource {
    protected final Logger log = LoggerFactory.getLogger(getClass());
    protected final ExplorerPlugin parent;
    protected Function<Void, Void> listener;
    protected Function<Void, Void> listenerIngestJobUpdate;

    public ExplorerSubResource(final ExplorerPlugin parent){
        this.parent = parent;
        this.parent.addSubResource(this);
    }

    public IExplorerPluginCommunication getCommunication(){
        return this.parent.getCommunication();
    }

    public boolean isForSearch(){
        return this.parent.isForSearch();
    }

    public String getApplication(){
        return this.parent.getApplication();
    }

    public String getResourceType(){
        return this.parent.getResourceType();
    }
    public abstract String getEntityType();

    @Override
    public Future<Void> notifyUpsert(final String id, final UserInfos user, final JsonObject source) {
        final ExplorerMessage message = ExplorerMessage.upsert(
                new IdAndVersion(id, getVersion(source)), user, isForSearch(), getApplication(), getResourceType(), getEntityType());
        return toMessage(message, source).compose(messages -> {
            return getCommunication().pushMessage(messages);
        });
    }

    @Override
    public Future<Void> notifyUpsert(final UserInfos user, final Map<String, JsonObject> sourceById) {
        final List<Future> futures = sourceById.entrySet().stream().map(e->{
            final String id = e.getKey();
            final ExplorerMessage message = ExplorerMessage.upsert(
                    new IdAndVersion(id, getVersion(e.getValue())), user, isForSearch(), getApplication(), getResourceType(), getEntityType());
            return toMessage(message, e.getValue());
        }).collect(Collectors.toList());
        return CompositeFuture.all(futures).compose(all->{
            final List<ExplorerMessage> messages = all.list();
            return getCommunication().pushMessage(messages);
        });
    }

    @Override
    public Future<Void> notifyUpsert(final UserInfos user, final JsonObject source) {
        final ExplorerMessage message = ExplorerMessage.upsert(
                new IdAndVersion(getParentId(source), getVersion(source)), user, isForSearch(),
                getApplication(), getResourceType(), getEntityType());
        return toMessage(message, source).compose(messages -> {
            return getCommunication().pushMessage(messages);
        });
    }

    @Override
    public Future<Void> notifyUpsert(final UserInfos user, final List<JsonObject> sources) {
        return toMessage(sources, e -> {
            final ExplorerMessage message = ExplorerMessage.upsert(
                    new IdAndVersion(getParentId(e), getVersion(e)), user, isForSearch(),
                    getApplication(), getResourceType(), getEntityType());
            return message;
        }).compose(messages -> {
            return getCommunication().pushMessage(messages);
        });
    }

    @Override
    public Future<Void> notifyUpsert(final ExplorerMessage m) {
        return parent.notifyUpsert(m);
    }

    @Override
    public Future<Void> notifyUpsert(final List<ExplorerMessage> messages) {
        return parent.notifyUpsert(messages);
    }

    @Override
    public Future<Void> notifyDeleteById(final UserInfos user, final String parentId, final String id) {
        final ExplorerMessage message = ExplorerMessage.delete(new IdAndVersion(parentId, currentTimeMillis()), user, isForSearch()).withType(getApplication(), getResourceType(), getEntityType());
        message.withSubResource(id, true);
        return getCommunication().pushMessage(message);
    }

    @Override
    public Future<Void> notifyDeleteById(final UserInfos user, final String parentId, final List<String> ids) {
        final List<ExplorerMessage> messages = ids.stream().map(id->{
            final ExplorerMessage message = ExplorerMessage.delete(new IdAndVersion(parentId, currentTimeMillis()), user, isForSearch()).withType(getApplication(), getResourceType(), getEntityType());
            message.withSubResource(id, true);
            return message;
        }).collect(Collectors.toList());
        return getCommunication().pushMessage(messages);
    }

    @Override
    public Future<Void> notifyDelete(final UserInfos user, final JsonObject source) {
        final ExplorerMessage message = ExplorerMessage.delete(new IdAndVersion(getParentId(source), currentTimeMillis()), user, isForSearch()).withType(getApplication(), getResourceType(), getEntityType());
        message.withSubResource(getChildId(source), true);
        return getCommunication().pushMessage(message);
    }

    @Override
    public Future<Void> notifyDelete(final UserInfos user, final List<JsonObject> sources) {
        return toMessage(sources, e -> {
            final ExplorerMessage message = ExplorerMessage.delete(new IdAndVersion(getParentId(e), currentTimeMillis()), user, isForSearch()).withType(getApplication(), getResourceType(), getEntityType());
            message.withSubResource(getChildId(e), true);
            return message;
        }).compose(messages -> {
            return getCommunication().pushMessage(messages);
        });
    }

    protected Future<List<ExplorerMessage>> toMessage(final List<JsonObject> sources, final Function<JsonObject, ExplorerMessage> builder) {
        final List<Future> futures = sources.stream().map(e -> toMessage(builder.apply(e), e)).collect(Collectors.toList());
        return CompositeFuture.all(futures).map(e -> new ArrayList<>(e.list()));
    }

    @Override
    public Future<JsonObject> reindex(final Optional<Long> from, final Optional<Long> to) {
        final long now = currentTimeMillis();
        final Integer reindexBatchSize = this.parent.reindexBatchSize;
        final ExplorerStream<JsonObject> stream = new ExplorerStream<>(reindexBatchSize, bulk -> {
            return toMessage(bulk, e -> {
                final String id = getParentId(e);
                final UserInfos user = getCreatorForModel(e).orElseGet(() -> {
                    log.error("Could not found creator for subresource "+getApplication()+ " with id : "+id);
                    return new UserInfos();
                });
                final ExplorerMessage mess = ExplorerMessage.upsert(new IdAndVersion(id, now), user, isForSearch(),
                                getApplication(), parent.getResourceType(), getEntityType())
                        .withVersion(now);
                return mess;
            })
            .compose(messages -> {
                return getCommunication().pushMessage(messages);
            });
        }, metricsEnd -> {
            log.info(String.format("Ending indexation for app=%s type=%s from=%s to=%s metrics=%s",getApplication(), getResourceType(), from, to, metricsEnd));
        });
        this.doFetchForIndex(stream, from.map(e -> new Date(e)), to.map(e -> new Date(e)));
        return stream.getEndFuture();
    }

    //abstract
    protected abstract String getChildId(final JsonObject source);

    protected abstract String getParentId(final JsonObject source);

    protected abstract Optional<UserInfos> getCreatorForModel(final JsonObject json);

    protected abstract Date getCreatedAtForModel(final JsonObject json);

    protected Future<ExplorerMessage> toMessage(final ExplorerMessage message, final JsonObject source) {
        message.withType(getApplication(), getResourceType(), getEntityType());
        message.withVersion(source.getLong("version"));
        //dont set creator and createdat (parent resource is responsible of setting it)
        return doToMessage(message, source);
    }

    protected abstract Future<ExplorerMessage> doToMessage(final ExplorerMessage message, final JsonObject source);

    protected abstract void doFetchForIndex(final ExplorerStream<JsonObject> stream, final Optional<Date> from, final Optional<Date> to);


    public void start() {
        final String idUpdate = IExplorerPlugin.addressForIngestStateUpdate(getApplication(), getEntityType());
        this.listenerIngestJobUpdate = parent.communication.listenForAcks(idUpdate, messages -> {
            onJobStateUpdatedMessageReceived(messages)
                .onSuccess(e -> log.debug("Update successul of " + messages.size() + " messages"))
                .onFailure(th -> log.error("Update error of " + messages, th));
        });
    }

    @Override
    public void stop() {
        if (this.listenerIngestJobUpdate != null) {
            this.listenerIngestJobUpdate.apply(null);
        }
        this.listenerIngestJobUpdate = null;
    }

    private long getVersion(final JsonObject source) {
        return source.getLong("version");
    }
}
