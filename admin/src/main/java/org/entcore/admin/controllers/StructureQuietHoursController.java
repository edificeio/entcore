/*
 * Copyright © "Open Digital Education", 2024
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

package org.entcore.admin.controllers;

import fr.wseduc.rs.Get;
import fr.wseduc.rs.Post;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.MfaProtected;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.http.BaseController;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import org.entcore.common.http.filter.AdmlOfStructure;
import org.entcore.common.http.filter.ResourceFilter;

public class StructureQuietHoursController extends BaseController {

    private static final String STRUCTURE_PREFERENCES_ADDRESS = "directory.structure.quiethours.preferences";
    private static final String USERBOOK_PREFERENCES_ADDRESS  = "userbook.preferences";

    @Get("api/structure/:structureId/quiethours-preferences")
    @SecuredAction(type = ActionType.RESOURCE, value = "")
    @ResourceFilter(AdmlOfStructure.class)
    @MfaProtected()
    public void getPreferences(HttpServerRequest request) {
        final String structureId = request.params().get("structureId");
        final JsonObject message = new JsonObject()
                .put("action", "get")
                .put("structureId", structureId);
        sendAndReply(request, STRUCTURE_PREFERENCES_ADDRESS, message);
    }

    @Post("api/structure/:structureId/quiethours-preferences")
    @SecuredAction(type = ActionType.RESOURCE, value = "")
    @ResourceFilter(AdmlOfStructure.class)
    @MfaProtected()
    public void setPreferences(HttpServerRequest request) {
        final String structureId = request.params().get("structureId");
        bodyToJson(request).compose(preferences -> {
            final JsonObject saveMessage = new JsonObject()
                    .put("action", "set")
                    .put("structureId", structureId)
                    .put("preferences", preferences);
            return busRequest(STRUCTURE_PREFERENCES_ADDRESS, saveMessage);
        }).compose(savedPreferences -> {
            final JsonObject cascadeMessage = new JsonObject()
                    .put("action", "cascade.structure.quiethours.preferences")
                    .put("structureId", structureId);
            return busRequest(USERBOOK_PREFERENCES_ADDRESS, cascadeMessage)
                    .map(cascadeResult -> new JsonObject().put("preferences", savedPreferences).put("cascade", cascadeResult))
                    .recover(error -> Future.succeededFuture(
                            new JsonObject()
                                    .put("preferences", savedPreferences)
                                    .put("cascade", new JsonObject().put("error", error.getMessage()))));
        }).onSuccess(result -> renderJson(request, result))
          .onFailure(failure -> renderJson(request, new JsonObject().put("error", failure.getMessage()), 400));
    }

    private void sendAndReply(HttpServerRequest request, String address, JsonObject message) {
        busRequest(address, message)
                .onSuccess(payload -> renderJson(request, payload))
                .onFailure(failure -> renderJson(request, new JsonObject().put("error", failure.getMessage()), 400));
    }

    private Future<JsonObject> busRequest(String address, JsonObject message) {
        Promise<JsonObject> promise = Promise.promise();
        
        vertx.eventBus().request(address, message, reply -> {
            if (reply.failed()) {
                promise.fail(reply.cause());
                return;
            }
            
            if (!(reply.result().body() instanceof JsonObject)) {
                promise.fail("invalid.bus.response");
                return;
            }
            
            JsonObject response = (JsonObject) reply.result().body();
            if (!"ok".equals(response.getString("status"))) {
                promise.fail(response.getString("message", "bus.error"));
                return;
            }
            
            Object payload = response.getValue("message");
            promise.complete(payload instanceof JsonObject ? (JsonObject) payload : new JsonObject());
        });
        
        return promise.future();
    }

    private Future<JsonObject> bodyToJson(HttpServerRequest request) {
        Promise<JsonObject> promise = Promise.promise();
        
        request.bodyHandler(buffer -> {
            if (buffer == null || buffer.length() == 0) {
                promise.fail("body is missing");
                return;
            }
            
            try {
                promise.complete(buffer.toJsonObject());
            }
            catch (Exception exception) {
                promise.fail("invalid json body");
            }
        });
        
        return promise.future();
    }
}
