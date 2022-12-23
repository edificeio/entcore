package org.entcore.common.explorer.impl;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.explorer.*;
import org.entcore.common.user.UserInfos;

import java.util.*;
import java.util.stream.Collectors;

import static java.lang.System.currentTimeMillis;

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

    public String getFolderResourceType(){
        return IExplorerFolderTree.FOLDER_TYPE;
    }

    public String getParentResourceType(){
        return this.parent.getResourceType();
    }

    @Override
    public Future<JsonObject> reindex(final Optional<Long> from, final Optional<Long> to) {
        final Integer reindexBatchSize = this.parent.reindexBatchSize;
        final ExplorerStream<JsonObject> stream = new ExplorerStream<>(reindexBatchSize, bulk -> {
            final List<ExplorerMessage> messages =  toMessages(bulk);
            return getCommunication().pushMessage(messages);
        }, metricsEnd -> {
            log.info(String.format("Ending indexation for app=%s type=%s from=%s to=%s metrics=%s",getApplication(), getFolderResourceType(), from, to, metricsEnd));
        });
        this.doFetchForIndex(stream, from.map(e -> new Date(e)), to.map(e -> new Date(e)));
        return stream.getEndFuture();
    }

    protected List<ExplorerMessage> toMessages(final List<JsonObject> sources) {
        final List<ExplorerMessage> messages = sources.stream().flatMap(e -> toMessages(e).stream()).collect(Collectors.toList());
        return messages;
    }

    protected List<ExplorerMessage> toMessages(final JsonObject source){
        final List<ExplorerMessage> messages = new ArrayList<>();
        final String id = getFolderId(source);
        final UserInfos user = getCreatorForModel(source);
        //folder
        // TODO JBER check if we cannot get version from somewhere else like the source
        final ExplorerMessage message = ExplorerMessage.upsert(new IdAndVersion(id, currentTimeMillis()), user, isForSearch(),
                getApplication(), getFolderResourceType(), getFolderResourceType()).withVersion(source.getLong("version", System.currentTimeMillis()));
        message.withName(getName(source));
        message.withTrashed(isTrashed(source));
        message.withParentEntId(getParentId(source));
        message.withMigrationFlag(true);
        message.withCreator(user);
        message.withCreatedAt(getCreatedAtForModel(source));
        //resources
        final Set<String> resourceIds = getResourceIds(source);
        message.withChildrenEntId(resourceIds);
        messages.add(message);
        return messages;
    }

    //abstract
    protected abstract String getFolderId(final JsonObject source);

    protected abstract String getName(final JsonObject source);

    protected abstract Optional<String> getParentId(final JsonObject source);

    protected abstract boolean isTrashed(final JsonObject source);

    protected abstract Set<String> getResourceIds(final JsonObject source);

    protected abstract UserInfos getCreatorForModel(final JsonObject json);

    protected abstract Date getCreatedAtForModel(final JsonObject json);

    protected abstract void doFetchForIndex(final ExplorerStream<JsonObject> stream, final Optional<Date> from, final Optional<Date> to);
}
