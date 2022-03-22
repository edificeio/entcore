package org.entcore.common.explorer.impl;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.explorer.*;
import org.entcore.common.user.UserInfos;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class ExplorerFolderTree implements IExplorerFolderTree {
    protected final Logger log = LoggerFactory.getLogger(getClass());
    protected final ExplorerPlugin parent;

    public ExplorerFolderTree(final ExplorerPlugin parent){
        this.parent = parent;
        this.parent.setFolderTree(this);
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

    protected Future<List<ExplorerMessage>> toMessage(final List<JsonObject> sources, final Function<JsonObject, ExplorerMessage> builder) {
        final List<Future> futures = sources.stream().map(e -> toMessage(builder.apply(e), e)).collect(Collectors.toList());
        return CompositeFuture.all(futures).map(e -> new ArrayList<>(e.list()));
    }

    @Override
    public Future<JsonObject> reindex(final Optional<Long> from, final Optional<Long> to) {
        final Integer reindexBatchSize = this.parent.reindexBatchSize;
        final ExplorerStream<JsonObject> stream = new ExplorerStream<>(reindexBatchSize, bulk -> {
            return toMessage(bulk, e -> {
                final String id = getFolderId(e);
                final UserInfos user = getCreatorForModel(e);
                //TODO set all values (name, parent, resourceIds->map to real id) + use resourceTYpe to route message on reader
                final ExplorerMessage mess = ExplorerMessage.upsert(id, user, isForSearch()).withType(getApplication(), getResourceType());
                return mess;
            }).compose(messages -> {
                return getCommunication().pushMessage(messages);
            });
        }, metricsEnd -> {
            log.info(String.format("Ending indexation for app=%s type=%s from=%s to=%s metrics=%s",getApplication(), getResourceType(), from, to, metricsEnd));
        });
        this.doFetchForIndex(stream, from.map(e -> new Date(e)), to.map(e -> new Date(e)));
        return stream.getEndFuture();
    }

    //abstract
    protected abstract String getFolderId(final JsonObject source);

    protected abstract String getParentId(final JsonObject source);

    protected abstract Set<String> getResourceIds(final JsonObject source);

    protected abstract UserInfos getCreatorForModel(final JsonObject json);

    protected abstract Future<ExplorerMessage> toMessage(final ExplorerMessage message, final JsonObject source);

    protected abstract void doFetchForIndex(final ExplorerStream<JsonObject> stream, final Optional<Date> from, final Optional<Date> to);
}
