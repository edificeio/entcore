package org.entcore.common.explorer.impl;

import fr.wseduc.webutils.DefaultAsyncResult;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.explorer.IExplorerPluginClient;
import org.entcore.common.explorer.to.ExplorerReindexResourcesRequest;
import org.entcore.common.explorer.to.ExplorerReindexResourcesResponse;
import org.entcore.common.user.UserInfos;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class ExplorerPluginClient implements IExplorerPluginClient {
    static final Logger log = LoggerFactory.getLogger(ExplorerPluginClient.class);
    private final Vertx vert;

    public ExplorerPluginClient(final Vertx vert) {
        this.vert = vert;
    }

    private void tryReindexRecursively(final Promise<IndexResponse> promise, final UserInfos userInfos, final ExplorerReindexResourcesRequest request, final int times, final int delay) {
        // trigger reindex
        this.reindex(userInfos, request).onComplete((reindexResponse) -> {
            // if reindex failed or reindex does not found any messsage (replication lag) => retry later
            if (reindexResponse.failed() || reindexResponse.succeeded() && reindexResponse.result().nbMessage == 0) {
                // decrease counter
                final int newTimes = times - 1;
                // check wether we reached limit of retry
                if (newTimes <= 0) {
                    // finished
                    promise.handle(reindexResponse);
                    return;
                }else{
                    // retry later
                    this.vert.setTimer(delay, (time) -> {
                        this.tryReindexRecursively(promise, userInfos, request, newTimes, delay);
                    });
                }
            } else {
                // if reindex succeed => trigger response
                promise.handle(reindexResponse);
            }
        });
    }

    @Override
    public Future<IndexResponse> tryReindex(final UserInfos user, final ExplorerReindexResourcesRequest request, final int times, final int delay) {
        final Promise<IndexResponse> promise = Promise.promise();
        this.tryReindexRecursively(promise, user, request, times, delay);
        return promise.future();
    }

    @Override
    public Future<IndexResponse> reindex(final UserInfos user, final ExplorerReindexResourcesRequest request) {
        final MultiMap headers = MultiMap.caseInsensitiveMultiMap();
        headers.add("action", ExplorerPlugin.ExplorerRemoteAction.QueryReindex.name());
        if (user != null) {
            if (user.getUserId() != null) {
                headers.add("userId", user.getUserId());
            }
            if (user.getUsername() != null) {
                headers.add("userName", user.getUsername());
            }
        }
        final Future<JsonObject> future = send(headers, JsonObject.mapFrom(request), Duration.ofDays(100));
        log.info("Trigger indexation " + request);
        return future.map(res -> {
            log.info(String.format("End trigger indexation " + res));
            final ExplorerReindexResourcesResponse response = res.mapTo(ExplorerReindexResourcesResponse.class);
            final int nb_message = response.getNbMessages();
            final int nb_batch = response.getNbBatch();
            return new IndexResponse(nb_batch, nb_message);
        }).onFailure(e -> {
            log.error("Trigger indexation failed. request=" + request.toString(), e);
        });
    }

    @Override
    public Future<List<String>> createAll(final UserInfos user, final List<JsonObject> json, final boolean isCopy) {
        final MultiMap headers = MultiMap.caseInsensitiveMultiMap();
        headers.add("action", ExplorerPlugin.ExplorerRemoteAction.QueryCreate.name());
        headers.add("userId", user.getUserId());
        headers.add("userName", user.getUsername());
        final JsonObject payload = new JsonObject();
        payload.put("resources", json);
        payload.put("copy", isCopy);
        final Future<JsonArray> future = send(headers, payload, Duration.ofMinutes(10));
        return future.map(jsonarray -> {
            return jsonarray.stream().map(id -> (String) id).collect(Collectors.toList());
        });
    }

    @Override
    public Future<DeleteResponse> deleteById(final UserInfos user, final Set<String> ids) {
        final MultiMap headers = MultiMap.caseInsensitiveMultiMap();
        headers.add("action", ExplorerPlugin.ExplorerRemoteAction.QueryDelete.name());
        headers.add("userId", user.getUserId());
        headers.add("userName", user.getUsername());
        final JsonObject payload = new JsonObject();
        payload.put("resources", new JsonArray(new ArrayList(ids)));
        final Future<JsonObject> future = send(headers, payload, Duration.ofMinutes(5));
        //deleted, failed
        return future.map(res -> {
            final List<String> deleted = res.getJsonArray("deleted").stream().map(id -> (String) id).collect(Collectors.toList());
            final List<String> failed = res.getJsonArray("failed").stream().map(id -> (String) id).collect(Collectors.toList());
            final DeleteResponse delRes = new DeleteResponse();
            delRes.deleted.addAll(deleted);
            delRes.notDeleted.addAll(failed);
            return delRes;
        });
    }

    @Override
    public Future<ShareResponse> shareByIds(final UserInfos user, final Set<String> ids, final JsonObject shares) {
        final MultiMap headers = MultiMap.caseInsensitiveMultiMap();
        headers.add("action", ExplorerPlugin.ExplorerRemoteAction.QueryShare.name());
        headers.add("userId", user.getUserId());
        headers.add("userName", user.getUsername());
        final JsonObject payload = new JsonObject();
        payload.put("resources", new JsonArray(new ArrayList(ids)));
        payload.put("shares", shares);
        final Future<JsonObject> future = send(headers, payload, Duration.ofMinutes(5));
        return future.map(res -> {
            final int nbShared = res.getInteger("nbShared", 0);
            final JsonObject notifyTimelineMap = res.getJsonObject("notifyTimelineMap", new JsonObject());
            final ShareResponse sres = new ShareResponse(nbShared, notifyTimelineMap);
            return sres;
        });
    }

    @Override
    public Future<JsonObject> getMetrics(final UserInfos user) {
        final MultiMap headers = MultiMap.caseInsensitiveMultiMap();
        headers.add("action", ExplorerPlugin.ExplorerRemoteAction.QueryMetrics.name());
        headers.add("userId", user.getUserId());
        headers.add("userName", user.getUsername());
        final Future<JsonObject> future = send(headers, new JsonObject(), Duration.ofMinutes(10));
        return future;
    }

    abstract protected <T> Future<T> send(final MultiMap headers, final JsonObject payload, final Duration timeout);

    abstract protected <T> Future<T> send(final MultiMap headers, final Object payload, Class<T> responseType, final Duration timeout);


}
