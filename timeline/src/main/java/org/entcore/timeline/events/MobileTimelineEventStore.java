package org.entcore.timeline.events;

import java.util.List;

import org.entcore.common.user.UserInfos;

import fr.wseduc.mongodb.MongoDb;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class MobileTimelineEventStore extends DefaultTimelineEventStore {
    private static final String TIMELINE_COLLECTION_MOBILE = "timelineMobile";

    private final TimelineEventStore original;

    public MobileTimelineEventStore(final TimelineEventStore originalEventStore) {
        super(TIMELINE_COLLECTION_MOBILE);
        this.original = originalEventStore;
    }

    @Override
    protected JsonObject validAndGet(JsonObject event){
        final JsonObject res = super.validAndGet(event);
        if(res != null && event.containsKey("_id")){
            res.put("_id", event.getString("_id"));
        }
        return res;
    }

    @Override
    public void add(final JsonObject event, final Handler<JsonObject> originalResult) {
        if (event.containsKey("preview")) {
            this.original.add(event, resOriginal->{
                if(resOriginal != null){
                    event.put("_id", resOriginal.getString("_id"));
                }
                super.add(event, (res) -> {
                    originalResult.handle(resOriginal);                    
                });
            });
            
        } else {
            this.original.add(event, originalResult);
        }
    }

    @Override
    public void delete(final String resource, final Handler<JsonObject> originalResult) {
        super.delete(resource, (res) -> {
            this.original.delete(resource, originalResult);
        });
    }

    @Override
    public void get(final UserInfos user, final List<String> types, final int offset, final int limit,
            final JsonObject restrictionFilter, final boolean mine, final boolean both, final String version,
            final Handler<JsonObject> result) {
        if (!"2.0".equals(version)) {
            this.original.get(user, types, offset, limit, restrictionFilter, mine, both, version, result);
            return;
        }
        //
        final String recipient = user.getUserId();
        if (recipient != null && !recipient.trim().isEmpty()) {
            final JsonObject query = new JsonObject().put("deleted", new JsonObject().put("$exists", false))
            // Mobile dont need to limit future events (only actu)?
            .put("date", new JsonObject().put("$lt", MongoDb.now()));
            // query sended / received / both
            if (mine) {
                query.put("sender", recipient);
            } else if (both) {
                query.put("$and",
                        new JsonArray().add(new JsonObject().put("$or",
                                new JsonArray().add(new JsonObject().put("sender", recipient))
                                        .add(new JsonObject().put("recipients.userId", recipient)))));
            } else {
                query.put("recipients.userId", recipient);
            }
            //
            query.put("reportAction.action", new JsonObject().put("$ne", "DELETE"));
            if (types != null && !types.isEmpty()) {
                if (types.size() == 1) {
                    query.put("type", types.get(0));
                } else {
                    final JsonArray typesFilter = new JsonArray();
                    for (final String t : types) {
                        typesFilter.add(new JsonObject().put("type", t));
                    }
                    query.put("$or", typesFilter);
                }
            }
            if (restrictionFilter != null && restrictionFilter.size() > 0) {
                final JsonArray nor = new JsonArray();
                for (final String type : restrictionFilter.getMap().keySet()) {
                    for (final Object eventType : restrictionFilter.getJsonArray(type,
                            new JsonArray())) {
                        nor.add(new JsonObject().put("type", type).put("event-type", eventType.toString()));
                    }
                    query.put("$nor", nor);
                }
            }
            final JsonObject sort = new JsonObject().put("created", -1);
            final JsonObject keys = new JsonObject().put("message", 1).put("params", 1).put("date", 1).put("sender", 1)
                    .put("comments", 1).put("type", 1).put("event-type", 1).put("resource", 1).put("sub-resource", 1)
                    .put("add-comment", 1).put("preview", 1);
            if (!mine || both) {
                keys.put("recipients",
                        new JsonObject().put("$elemMatch", new JsonObject().put("userId", user.getUserId())));
                keys.put("reporters",
                        new JsonObject().put("$elemMatch", new JsonObject().put("userId", user.getUserId())));
            }

            mongo.find(timelineCollection, query, sort, keys, offset, limit, 100, (message) -> {
                result.handle(message.body());
            });
        } else {
            result.handle(invalidArguments());
        }
    }

    @Override
    public void deleteSubResource(final String resource, final Handler<JsonObject> originalResult) {
        super.deleteSubResource(resource, (res) -> {
            this.original.deleteSubResource(resource, originalResult);
        });
    }

    @Override
    public void listTypes(final Handler<JsonArray> result) {
        this.original.listTypes(result);
    }

    @Override
    public void delete(final String id, final String sender, final Handler<Either<String, JsonObject>> originalResult) {
        super.delete(id, sender, (result) -> {
            this.original.delete(id, sender, originalResult);
        });
    }

    @Override
    public void discard(final String id, final String recipient,
            final Handler<Either<String, JsonObject>> originalResult) {
        super.discard(id, recipient, (result) -> {
            this.original.discard(id, recipient, originalResult);
        });
    }

    @Override
    public void report(final String id, final UserInfos user,
            final Handler<Either<String, JsonObject>> originalResult) {
        super.report(id, user, (result) -> {
            this.original.report(id, user, originalResult);
        });
    }

    @Override
    public void listReported(final String structure, final boolean pending, final int offset, final int limit,
            final Handler<Either<String, JsonArray>> originalResult) {
        this.original.listReported(structure, pending, offset, limit, originalResult);
    }

    @Override
    public void performAdminAction(final String id, final String structureId, final UserInfos user,
            final AdminAction action, final Handler<Either<String, JsonObject>> originalResult) {
        super.performAdminAction(id, structureId, user, action, (result) -> {
            this.original.performAdminAction(id, structureId, user, action, originalResult);
        });
    }

    @Override
    public void deleteReportNotification(final String resourceId,
            final Handler<Either<String, JsonObject>> originalResult) {
        super.deleteReportNotification(resourceId, (result) -> {
            this.original.deleteReportNotification(resourceId, originalResult);
        });
    }
}