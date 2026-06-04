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
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import org.entcore.common.http.filter.AdmlOfStructure;
import org.entcore.common.http.filter.ResourceFilter;

public class StructureQuietHoursController extends BaseController {

    private static final String STRUCTURE_PREFERENCES_ADDRESS = "directory.structure.quiethours.preferences";
    private static final String USERBOOK_PREFERENCES_ADDRESS  = "userbook.preferences";

    @Get("api/structures/:structureId/quiethours-preferences")
    @SecuredAction(type = ActionType.RESOURCE, value = "")
    @ResourceFilter(AdmlOfStructure.class)
    @MfaProtected()
    public void getPreferences(HttpServerRequest request) {
        final String structureId = request.params().get("structureId");
        final JsonObject message = new JsonObject()
                .put("action", "get")
                .put("structureId", structureId);
        vertx.eventBus().request(STRUCTURE_PREFERENCES_ADDRESS, message, reply -> {
            if (reply.failed()) {
                renderJson(request, new JsonObject().put("error", reply.cause().getMessage()), 400);
                return;
            }
            
            JsonObject response = (JsonObject) reply.result().body();
            if (!"ok".equals(response.getString("status"))) {
                renderJson(request, new JsonObject().put("error", response.getString("message", "bus.error")), 400);
                return;
            }
            
            Object payload = response.getValue("message");
            renderJson(request, payload instanceof JsonObject ? (JsonObject) payload : new JsonObject());
        });
    }

    @Post("api/structures/:structureId/quiethours-preferences")
    @SecuredAction(type = ActionType.RESOURCE, value = "")
    @ResourceFilter(AdmlOfStructure.class)
    @MfaProtected()
    public void setPreferences(HttpServerRequest request) {
        final String structureId = request.params().get("structureId");
        RequestUtils.bodyToJson(request, preferences -> {
            if (preferences == null) {
                renderJson(request, new JsonObject().put("error", "Invalid or empty JSON body"), 400);
                return;
            }
            
            final JsonObject saveMessage = new JsonObject()
                    .put("action", "set")
                    .put("structureId", structureId)
                    .put("preferences", preferences);
            vertx.eventBus().request(STRUCTURE_PREFERENCES_ADDRESS, saveMessage, saveReply -> {
                if (saveReply.failed()) {
                    renderJson(request, new JsonObject().put("error", saveReply.cause().getMessage()), 400);
                    return;
                }
                
                JsonObject saveResponse = (JsonObject) saveReply.result().body();
                if (!"ok".equals(saveResponse.getString("status"))) {
                    renderJson(request, new JsonObject().put("error", saveResponse.getString("message", "bus.error")), 400);
                    return;
                }
                
                Object savedPayload = saveResponse.getValue("message");
                JsonObject savedPreferences = savedPayload instanceof JsonObject ? (JsonObject) savedPayload : new JsonObject();

                // Cascade — never fails the HTTP response
                final JsonObject cascadeMessage = new JsonObject()
                        .put("action", "cascade.structure.quiethours.preferences")
                        .put("structureId", structureId);
                vertx.eventBus().request(USERBOOK_PREFERENCES_ADDRESS, cascadeMessage, cascadeReply -> {
                    JsonObject result = new JsonObject().put("preferences", savedPreferences);
                    if (cascadeReply.failed()) {
                        result.put("cascade", new JsonObject().put("error", cascadeReply.cause().getMessage()));
                    } else {
                        JsonObject cascadeResponse = (JsonObject) cascadeReply.result().body();
                        if (!"ok".equals(cascadeResponse.getString("status"))) {
                            result.put("cascade", new JsonObject().put("error", cascadeResponse.getString("message", "bus.error")));
                        } else {
                            Object cascadePayload = cascadeResponse.getValue("message");
                            result.put("cascade", cascadePayload instanceof JsonObject ? (JsonObject) cascadePayload : new JsonObject());
                        }
                    }
                    renderJson(request, result);
                });
            });
        });
    }
}
