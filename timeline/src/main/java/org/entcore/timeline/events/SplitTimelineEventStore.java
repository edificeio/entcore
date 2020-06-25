/*
 * Copyright © "Open Digital Education", 2020
 *
 * This program is published by "Open Digital Education".
 * You must indicate the name of the software and the company in any production /contribution
 * using the software and indicate on the home page of the software industry in question,
 * "powered by Open Digital Education" with a reference to the website: https://opendigitaleducation.com/.
 *
 * This program is free software, licensed under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation, version 3 of the License.
 *
 * You can redistribute this application and/or modify it since you respect the terms of the GNU Affero General Public License.
 * If you modify the source code and then use this modified source code in your creation, you must make available the source code of your modifications.
 *
 * You should have received a copy of the GNU Affero General Public License along with the software.
 * If not, please see : <http://www.gnu.org/licenses/>. Full compliance requires reading the terms of this license and following its directives.

 */
package org.entcore.timeline.events;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.entcore.common.user.UserInfos;

import fr.wseduc.webutils.Either;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class SplitTimelineEventStore implements TimelineEventStore {
    private boolean combineResult;
    private final int maxRecipients;
    private final TimelineEventStore original;

    public SplitTimelineEventStore(TimelineEventStore aStore, int maxRecipients) {
        this(aStore, maxRecipients, false);
    }

    public SplitTimelineEventStore(TimelineEventStore aStore, int maxRecipients, boolean combine) {
        this.original = aStore;
        this.maxRecipients = maxRecipients;
        this.combineResult = combine;
    }

    private static class NotificationChunk {
        final List<Object> recipients = new ArrayList<>();
        final List<Object> recipientsIds = new ArrayList<>();

        void addRecipientId(Object id) {
            recipientsIds.add(id);
        }

        void addRecipient(Object id) {
            recipients.add(id);
        }

        int size() {
            return recipients.size();
        }
    }

    public boolean isCombineResult() {
        return combineResult;
    }

    public void setCombineResult(boolean combineResult) {
        this.combineResult = combineResult;
    }

    @Override
    public void add(JsonObject event, Handler<JsonObject> result) {
        final JsonArray recipientsIds = event.getJsonArray("recipientsIds", new JsonArray());
        final JsonArray recipients = event.getJsonArray("recipients", new JsonArray());
        if (recipients.size() > this.maxRecipients) {
            // create chunks
            final List<NotificationChunk> chunks = new ArrayList<>();
            NotificationChunk chunk = new NotificationChunk();
            for (int i = 0; i < recipients.size(); i++) {
                if (chunk.size() == maxRecipients) {
                    chunks.add(chunk);
                    chunk = new NotificationChunk();
                }
                chunk.addRecipient(recipients.getValue(i));
                if (i < recipientsIds.size()) {
                    chunk.addRecipientId(recipientsIds.getValue(i));
                }
            }
            // add last chunk
            if (chunk.size() > 0) {
                chunks.add(chunk);
            }
            final List<Future> futures = new ArrayList<>();
            for (final NotificationChunk part : chunks) {
                final JsonObject copy = event.copy();
                copy.put("recipients", new JsonArray(part.recipients));
                copy.put("recipientsIds", new JsonArray(part.recipients));
                final Future<JsonObject> future = Future.future();
                original.add(copy, res -> {
                    future.complete(res);
                });
                futures.add(future);
            }
            if (combineResult) {
                CompositeFuture.all(futures).setHandler(res -> {
                    final JsonObject json = res.result().list().stream().map(JsonObject.class::cast)
                            .reduce(new JsonObject().put("_ids", new JsonArray()), (a, b) -> {
                                final String idb = b.getString("_id");
                                final String status = b.getString("status");
                                final JsonArray ids = a.getJsonArray("_ids", new JsonArray()).add(idb);
                                return new JsonObject().put("_ids", ids).put("status", status);
                            });
                    result.handle(json);
                });
            } else {
                for (Future<JsonObject> future : futures) {
                    future.setHandler(res -> {
                        if (res.succeeded()) {
                            result.handle(res.result());
                        } else {
                            result.handle(null);
                        }
                    });
                }
            }
        } else {
            original.add(event, result);
        }
    }

    @Override
    public void delete(String resource, Handler<JsonObject> result) {
        original.delete(resource, result);
    }

    @Override
    public void get(UserInfos recipient, List<String> types, int offset, int limit, JsonObject restrictionFilter,
            boolean mine, boolean both, String version, Handler<JsonObject> result) {
        original.get(recipient, types, offset, limit, restrictionFilter, mine, both, version, result);
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
        original.delete(id, sender, result);
    }

    @Override
    public void discard(String id, String recipient, Handler<Either<String, JsonObject>> result) {
        original.discard(id, recipient, result);
    }

    @Override
    public void report(String id, UserInfos user, Handler<Either<String, JsonObject>> result) {
        original.report(id, user, result);
    }

    @Override
    public void listReported(String structure, boolean pending, int offset, int limit,
            Handler<Either<String, JsonArray>> result) {
        original.listReported(structure, pending, offset, limit, result);
    }

    @Override
    public void performAdminAction(String id, String structureId, UserInfos user, AdminAction action,
            Handler<Either<String, JsonObject>> result) {
        original.performAdminAction(id, structureId, user, action, result);
    }

    @Override
    public void deleteReportNotification(String resourceId, Handler<Either<String, JsonObject>> result) {
        original.deleteReportNotification(resourceId, result);
    }
}