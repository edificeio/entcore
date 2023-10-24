/*
 * Copyright © "Open Digital Education", 2019
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

package org.entcore.registry.controllers;

import fr.wseduc.rs.Post;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.http.BaseController;
import fr.wseduc.webutils.http.Renders;
import io.vertx.core.*;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.events.EventHelper;
import org.entcore.common.events.EventStore;
import org.entcore.common.events.EventStoreFactory;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;
import org.entcore.common.utils.StringUtils;
import org.entcore.registry.AppRegistry;
import org.entcore.registry.services.LibraryService;
import org.entcore.registry.services.impl.DefaultLibraryService;

import java.util.ArrayList;
import java.util.List;

public class LibraryController extends BaseController {
    final LibraryService service;
    final JsonObject config;
    final EventHelper eventHelper;
    static final String RESOURCE_NAME = "library";

    public LibraryController(Vertx vertx, JsonObject config) throws Exception {
        service = new DefaultLibraryService(vertx, config);
        final EventStore eventStore = EventStoreFactory.getFactory().getEventStore(AppRegistry.class.getSimpleName());
        this.eventHelper = new EventHelper(eventStore);
        this.config = config;
    }

    @Post("/library/resource")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void publishToLibrary(final HttpServerRequest request) {
        request.setExpectMultipart(true);
        final Future<UserInfos> futureUser = getUser(request);
        CompositeFuture.all(getCoverAndTeacherAvatar(request), getAttributes(request, futureUser), futureUser).compose(res -> {
            List<Buffer> coverAndAvatarBufferList = res.resultAt(0);
            MultiMap attributes = res.resultAt(1);
            UserInfos user = res.resultAt(2);
            return service.publish(user, I18n.acceptLanguage(request), attributes, coverAndAvatarBufferList.get(0), coverAndAvatarBufferList.get(1));
        }).onComplete(res -> {
            if (res.succeeded()) {
                final JsonObject json = res.result();
                if (json.getBoolean("success", false)) {
                    eventHelper.onCreateResource(request, RESOURCE_NAME);
                    renderJson(request, res.result());
                } else {
                    renderError(request, res.result());
                }
            } else {
                final JsonObject resultJson = res.result();
                final String message = resultJson.getString("message", "");
                if (message.equals(DefaultLibraryService.MESSAGE.CONTENT_TOO_LARGE.name())) {
                    renderError(request, resultJson, 413, message);
                } else {
                    final JsonObject body = new JsonObject().put("success", false).put("reason", "unknown").put("message", res.cause().getMessage());
                    renderError(request, body);
                    log.error("An unknown error occurred while calling the library publish service : \nResponse : " + res.result().encode(), res.cause());
                }
            }
        });
    }

    private Future<List<Buffer>> getCoverAndTeacherAvatar(HttpServerRequest request) {
        Promise<List<Buffer>> bufferListFuture = Promise.promise();
        List<Buffer> bufferList = new ArrayList<Buffer>();
        request.uploadHandler(upload -> {
            final Buffer buffer = Buffer.buffer();
            upload.handler(b -> {
                buffer.appendBuffer(b);
            });
            upload.endHandler(v -> {
                bufferList.add(buffer);
                if (bufferList.size() == 2) {
                    bufferListFuture.complete(bufferList);
                }
            });
            upload.exceptionHandler(v -> bufferListFuture.fail(v));
        });
        return bufferListFuture.future();
    }

    private Future<UserInfos> getUser(HttpServerRequest request) {
        Promise<UserInfos> futureUser = Promise.promise();
        UserUtils.getUserInfos(eb, request, res -> {
            if (res == null) {
                futureUser.fail("Could not found user info");
            } else {
                futureUser.complete(res);
            }
        });
        return futureUser.future();
    }

    private Future<MultiMap> getAttributes(HttpServerRequest request, Future<UserInfos> futureUser) {
        MultiMap form = request.formAttributes();
        form.add("platformURL", Renders.getHost(request));

        Promise<MultiMap> future = Promise.promise();
        request.endHandler(res -> {
            futureUser.onComplete(resuser -> {
                if (resuser.succeeded()) {
                    final UserInfos user = resuser.result();
                    form.add("teacherFullName", user.getFirstName() + ' ' + user.getLastName())
                        .add("teacherId", user.getUserId());
                    future.complete(form);
                } else {
                    future.fail(resuser.cause());
                }
            });
        });
        return future.future();
    }
}
