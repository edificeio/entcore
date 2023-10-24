package org.entcore.timeline.events;

import com.mongodb.client.model.Filters;
import fr.wseduc.mongodb.MongoDb;
import fr.wseduc.mongodb.MongoQueryBuilder;
import fr.wseduc.webutils.Either;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.cache.CacheService;
import org.entcore.common.notification.TimelineNotificationsLoader;
import org.entcore.common.user.UserInfos;
import org.entcore.timeline.services.TimelineConfigService;

import java.util.*;
import java.util.stream.Collectors;


public class CachedTimelineEventStore implements TimelineEventStore {
    private static Logger logger = LoggerFactory.getLogger(CachedTimelineEventStore.class);
    private final Map<String, String> registeredNotifications;
    private final TimelineConfigService configService;
    private final TimelineEventStore original;
    private final CacheService cacheService;
    private final int pageSize;
    private JsonObject externalNotificationsCache;
    protected MongoDb mongo = MongoDb.getInstance();

    private static String getKey(String userId){
        return "timeline:" + userId;
    }
    public CachedTimelineEventStore(TimelineEventStore original, CacheService cacheService, int pageSize, TimelineConfigService configService, Map<String, String> registeredNotifications) {
        this.original = original;
        this.pageSize = pageSize;
        this.cacheService = cacheService;
        this.configService = configService;
        this.registeredNotifications = registeredNotifications;
    }

    private Future<Boolean> shouldAddToCache(final JsonObject notif){
        return getExternalNotifications().compose(restrictionFilter->{
            for(final String type : restrictionFilter.getMap().keySet()){
                for(final Object eventTypeObj : restrictionFilter.getJsonArray(type, new JsonArray())){
                    final String eventType = eventTypeObj.toString();
                    if(notif.getString("type","").equals(type)
                            && notif.getString("event-type", "").equals(eventType)){
                        return Future.succeededFuture(false);
                    }
                }
            }
            return Future.succeededFuture(true);
        });
    }

    private Future<JsonObject> getExternalNotifications() {
        Promise<JsonObject> future = Promise.promise();
        if (externalNotificationsCache != null) {
            return Future.succeededFuture(externalNotificationsCache);
        }
        configService.list(event -> {
            if (event.isLeft()) {
                future.fail((event.left().getValue()));
                return;
            }
            final JsonObject restricted = new JsonObject();
            for (String key : registeredNotifications.keySet()) {
                JsonObject notif = new JsonObject(registeredNotifications.get(key));
                String restriction = notif.getString("restriction", TimelineNotificationsLoader.Restrictions.NONE.name());
                for (Object notifConfigObj : event.right().getValue()) {
                    JsonObject notifConfig = (JsonObject) notifConfigObj;
                    if (notifConfig.getString("key", "").equals(key)) {
                        restriction = notifConfig.getString("restriction", restriction);
                        break;
                    }
                }
                if (restriction.equals(TimelineNotificationsLoader.Restrictions.EXTERNAL.name()) ||
                        restriction.equals(TimelineNotificationsLoader.Restrictions.HIDDEN.name())) {
                    String notifType = notif.getString("type");
                    if (!restricted.containsKey(notifType)) {
                        restricted.put(notifType, new JsonArray());
                    }
                    restricted.getJsonArray(notifType).add(notif.getString("event-type"));
                }
            }
            externalNotificationsCache = restricted;
            future.complete((restricted));
        });
        return future.future();
    }

    @Override
    public void add(JsonObject event, Handler<JsonObject> result) {
        original.add(event, resOriginal->{
            result.handle(resOriginal);
            //add to cache if needed
            final JsonObject copy = event.copy();
            final JsonArray recipients = copy.getJsonArray("recipients", new JsonArray());
            copy.remove("recipients");
            copy.remove("recipientsIds");
            copy.remove("request");
            if (!copy.containsKey("date")) {
                copy.put("date", MongoDb.now());
            }
            copy.put("created", copy.getJsonObject("date"));
            copy.put("_id", resOriginal.getString("_id", ""));
            shouldAddToCache(copy).onComplete(resShouldAdd -> {
                final List<Future<?>> futures = new ArrayList<>();
                if(resShouldAdd.succeeded() && resShouldAdd.result()){
                    for (Object recipient : recipients) {
                        final JsonObject recipientJson = (JsonObject) recipient;
                        final Promise<Void> future = Promise.promise();
                        futures.add(future.future());
                        final String key = getKey(recipientJson.getString("userId"));
                        cacheService.prependToList(key, copy.encode(), res -> {
                            if (res.succeeded()) {
                                if (res.result().intValue() > this.pageSize) {
                                    cacheService.removeLastFromList(key, resRemove -> {
                                        if (resRemove.succeeded()) {
                                            future.complete(null);
                                        } else {
                                            future.fail(resRemove.cause());
                                        }
                                    });
                                } else {
                                    future.complete(null);
                                }
                            } else {
                                future.fail(res.cause());
                            }
                        });
                    }
                }
                //call original store
                Future.all(futures).onComplete(res -> {
                    if (!res.succeeded()) {
                        logger.error("Failed to add event:", res.cause());
                    }
                });
            });
        });
    }

    @Override
    public void delete(String resource, Handler<JsonObject> result) {
        original.delete(resource, result);
    }

    private Future<List<JsonObject>> getListFiltered(String userId, List<String> types){
        Promise<List<JsonObject>> future = Promise.promise();
        cacheService.getList(getKey(userId), res -> {
            if (res.succeeded()) {
                final List<String> all = res.result();
                final Set<String> uniqIds = new HashSet<>();
                final long now = System.currentTimeMillis();
                final List<JsonObject> allJson = all.stream().map(json -> new JsonObject(json)).filter(json -> {
                    final String id = json.getString("_id");
                    final boolean alreadyAdded = uniqIds.contains(id);
                    if(alreadyAdded) return false;
                    uniqIds.add(id);
                    //date filter
                    final long date = json.getJsonObject("date", new JsonObject()).getLong("$date", now);
                    if(date > now){
                        return false;
                    }
                    //
                    if (types != null && !types.isEmpty()) {
                        final String type = json.getString("type", "");
                        return types.contains(type);
                    } else {
                        return true;
                    }
                }).collect(Collectors.toList());
                future.complete(allJson);
            } else {
                future.fail(res.cause());
            }
        });
        return future.future();
    }

    private Future<List<JsonObject>> getListUnfiltered(String userId){
        Promise<List<JsonObject>> future = Promise.promise();
        cacheService.getList(getKey(userId), res -> {
            if (res.succeeded()) {
                final List<String> all = res.result();
                final Set<String> uniqIds = new HashSet<>();
                final List<JsonObject> allJson = all.stream().map(json -> new JsonObject(json)).collect(Collectors.toList());
                future.complete(allJson);
            } else {
                future.fail(res.cause());
            }
        });
        return future.future();
    }

    @Override
    public void get(UserInfos recipient, List<String> types, int offset, int limit, JsonObject restrictionFilter, boolean mine, boolean both, String version, Handler<JsonObject> result) {
        //offset 0 and not mobile not mine
        final boolean fromCache =  !"2.0".equals(version) && !both && !mine;
        if (fromCache) {
            final String userId = recipient.getUserId();
            if(offset == 0){
                getListFiltered(userId, types).onComplete(resJson ->{
                    if(resJson.succeeded()){
                        final List<JsonObject> allJson = resJson.result();
                        final JsonObject payload = new JsonObject();
                        payload.put("number", allJson.size());
                        payload.put("results", new JsonArray(allJson));
                        payload.put("status", "ok");
                        result.handle(payload);
                    } else {
                        logger.error("Failed to get events:", resJson.cause());
                        original.get(recipient, types, offset, limit, restrictionFilter, mine, both, version, result);
                    }
                });
            } else if(offset <= this.pageSize){
                getListFiltered(userId, types).onComplete(resJson ->{
                    if(resJson.succeeded()){
                        final int length = resJson.result().size();
                        int newOffset = offset;
                        int newLimit = limit;
                        if(length < pageSize) {
                            newOffset = length;
                            newLimit = (pageSize - length) + offset;
                        }
                        original.get(recipient, types, newOffset, newLimit, restrictionFilter, mine, both, version, result);
                    }else{
                        original.get(recipient, types, offset, limit, restrictionFilter, mine, both, version, result);
                    }
                });
            } else {
                original.get(recipient, types, offset, limit, restrictionFilter, mine, both, version, result);
            }
        } else {
            original.get(recipient, types, offset, limit, restrictionFilter, mine, both, version, result);
        }
    }

    @Override
    public void deleteSubResource(String resource, Handler<JsonObject> result) {
        original.deleteSubResource(resource, result);
    }

    @Override
    public void listTypes(Handler<JsonArray> result) {
        original.listTypes(result);
    }

    @Override
    public void delete(String id, String sender, Handler<Either<String, JsonObject>> result) {
        removeById(id, res->{
            original.delete(id, sender, result);
        });
    }

    protected void removeById(String id, final Handler<Void> handler){
        mongo.findOne(DefaultTimelineEventStore.TIMELINE_COLLECTION, MongoQueryBuilder.build(Filters.eq("_id", id)), message->{
            //do action
            handler.handle(null);
            //then delete from cache
            final JsonObject body = message.body();
			if ("ok".equals(body.getString("status"))) {
                final JsonObject notif = body.getJsonObject("result", new JsonObject());
                final JsonArray recipients = notif.getJsonArray("recipients", new JsonArray());
                for(final Object recipient : recipients){
                    final JsonObject recipientJson = (JsonObject) recipient;
                    final String recipientId = recipientJson.getString("userId");
                    removeFromCache(recipientId, id);
                }
			}
        });
    }

    protected void removeFromCache(String recipient, String id){
        getListUnfiltered(recipient).onComplete(res->{
            if(res.succeeded()){
                final List<JsonObject> jsons = res.result();
                for(int i = 0 ; i < jsons.size(); i ++){
                    final JsonObject current = jsons.get(i);
                    if(id.equals(current.getString("_id"))){
                        cacheService.removeFromList(getKey(recipient), current.encode(), resR->{});
                    }
                }
            }
         });
    }

    @Override
    public void discard(String id, String recipient, Handler<Either<String, JsonObject>> result) {
        original.discard(id, recipient, result);
        removeFromCache(recipient, id);
    }

    @Override
    public void report(String id, UserInfos user, Handler<Either<String, JsonObject>> result) {
        original.report(id, user, result);
    }

    @Override
    public void listReported(String structure, boolean pending, int offset, int limit, Handler<Either<String, JsonArray>> result) {
        original.listReported(structure, pending, offset, limit, result);
    }

    @Override
    public void performAdminAction(String id, String structureId, UserInfos user, AdminAction action, Handler<Either<String, JsonObject>> result) {
        if(action == AdminAction.DELETE) {
            removeById(id, res->{
                original.performAdminAction(id, structureId, user, action, result);
            });
        }else{
            original.performAdminAction(id, structureId, user, action, result);
        }
    }

    @Override
    public void deleteReportNotification(String resourceId, Handler<Either<String, JsonObject>> result) {
        original.deleteReportNotification(resourceId, result);
    }
}
