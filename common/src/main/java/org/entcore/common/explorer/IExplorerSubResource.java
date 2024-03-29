package org.entcore.common.explorer;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import org.entcore.common.explorer.to.ExplorerReindexSubResourcesRequest;
import org.entcore.common.user.UserInfos;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface IExplorerSubResource {

    void start();

    void stop();

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

    /**
     * @param subResourcesRequest Filter that sub-resources to reindex should match
     * @return A swift report of the reindexation
     */
    Future<JsonObject> reindex(final ExplorerReindexSubResourcesRequest subResourcesRequest);

    Future<Void> onDeleteParent(final Collection<String> parentIds);

    default void setIngestJobStateAndVersion(final JsonObject source, final IngestJobState state, final long version) {
        source.put("version", version);
        setIngestJobState(source, state);
    }
    default void setIngestJobState(final JsonObject source, final IngestJobState state) {
        source.put("ingest_job_state", state.name());
    }

    /**
     * Methods call by Ingest Job to notify a plugin that a batch of messages have a status update.
     * @param messages Messages whose status has changed
     * @return A future which will succeed if all messages are ack-ed
     */
    Future<Void> onJobStateUpdatedMessageReceived(final List<IngestJobStateUpdateMessage> messages);
}
