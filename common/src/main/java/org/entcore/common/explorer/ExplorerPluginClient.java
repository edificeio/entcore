package org.entcore.common.explorer;

import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.user.UserInfos;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

public abstract class ExplorerPluginClient implements IExplorerPluginClient {
    static final Logger log = LoggerFactory.getLogger(ExplorerPluginClient.class);

    @Override
    public Future<IndexResponse> getForIndexation(final UserInfos user, final Optional<Date> from, final Optional<Date> to){
        return getForIndexation(user, from, to, new HashSet<>());
    }

    @Override
    public Future<IndexResponse> getForIndexation(final UserInfos user, final Optional<Date> from, final Optional<Date> to, final Set<String> apps){
        final MultiMap headers = MultiMap.caseInsensitiveMultiMap();
        headers.add("action", ExplorerPlugin.ExplorerRemoteAction.QueryReindex.name());
        headers.add("userId", user.getUserId());
        headers.add("userName", user.getUsername());
        final JsonObject payload = new JsonObject();
        if(from.isPresent()){
            payload.put("from", from.get().getTime());
        }
        if(to.isPresent()){
            payload.put("to", to.get().getTime());
        }
        if(!apps.isEmpty()){
            payload.put("apps", new JsonArray(new ArrayList(apps)));
        }
        //nb_message,nb_batch
        final Future<JsonObject> future = send(headers, payload, Duration.ofDays(100));
        log.info(String.format("Trigger indexation from=%s to=%s",from, to));
        return future.map(res->{
            log.info(String.format("End trigger indexation from=%s to=%s metrics=%s",from, to, res));
            final int nb_message = res.getInteger("nb_message");
            final int nb_batch = res.getInteger("nb_batch");
            return new IndexResponse(nb_batch, nb_message);
        });
    }

    @Override
    public Future<List<String>> createAll(final UserInfos user, final List<JsonObject> json, final boolean isCopy){
        final MultiMap headers = MultiMap.caseInsensitiveMultiMap();
        headers.add("action", ExplorerPlugin.ExplorerRemoteAction.QueryCreate.name());
        headers.add("userId", user.getUserId());
        headers.add("userName", user.getUsername());
        final JsonObject payload = new JsonObject();
        payload.put("resources", json);
        payload.put("copy", isCopy);
        final Future<JsonArray> future = send(headers, payload, Duration.ofMinutes(10));
        return future.map(jsonarray->{
            return jsonarray.stream().map(id-> (String)id).collect(Collectors.toList());
        });
    }

    @Override
    public Future<DeleteResponse> deleteById(final UserInfos user, final Set<String> ids){
        final MultiMap headers = MultiMap.caseInsensitiveMultiMap();
        headers.add("action", ExplorerPlugin.ExplorerRemoteAction.QueryDelete.name());
        headers.add("userId", user.getUserId());
        headers.add("userName", user.getUsername());
        final JsonObject payload = new JsonObject();
        payload.put("resources", new JsonArray(new ArrayList(ids)));
        final Future<JsonObject> future = send(headers, payload, Duration.ofMinutes(5));
        //deleted, failed
        return future.map(res->{
            final List<String> deleted = res.getJsonArray("deleted").stream().map(id-> (String)id).collect(Collectors.toList());
            final List<String> failed = res.getJsonArray("failed").stream().map(id-> (String)id).collect(Collectors.toList());
            final DeleteResponse delRes = new DeleteResponse();
            delRes.deleted.addAll(deleted);
            delRes.notDeleted.addAll(failed);
            return delRes;
        });
    }

    @Override
    public Future<JsonObject> getMetrics(final UserInfos user){
        final MultiMap headers = MultiMap.caseInsensitiveMultiMap();
        headers.add("action", ExplorerPlugin.ExplorerRemoteAction.QueryMetrics.name());
        headers.add("userId", user.getUserId());
        headers.add("userName", user.getUsername());
        final Future<JsonObject> future = send(headers, new JsonObject(), Duration.ofMinutes(10));
        return future;
    }

    abstract protected <T> Future<T> send(final MultiMap headers, final JsonObject payload, final Duration timeout);


}
