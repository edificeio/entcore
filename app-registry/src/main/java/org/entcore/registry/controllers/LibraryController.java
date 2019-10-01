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
import fr.wseduc.webutils.http.BaseController;
import fr.wseduc.webutils.http.Renders;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;
import org.entcore.registry.services.LibraryService;
import org.entcore.registry.services.impl.DefaultLibraryService;

import java.util.ArrayList;
import java.util.List;

public class LibraryController extends BaseController {
    final LibraryService service;

    public LibraryController(Vertx vertx, JsonObject config) throws Exception {
        service = new DefaultLibraryService(vertx, config);
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
            return service.publish(user, attributes, coverAndAvatarBufferList.get(0), coverAndAvatarBufferList.get(1));
        }).setHandler(res -> {
            if (res.succeeded()) {
                final JsonObject json = res.result();
                if (json.getBoolean("success", false)) {
                    renderJson(request, res.result());
                } else {
                    renderError(request, res.result());
                }
            } else {
                final JsonObject body = new JsonObject().put("success", false).put("reason", "unknown").put("message", res.cause().getMessage());
                renderError(request, body);
            }
        });
    }

    private Future<List<Buffer>> getCoverAndTeacherAvatar(HttpServerRequest request) {
        Future<List<Buffer>> bufferListFuture = Future.future();
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
        return bufferListFuture;
    }

    private Future<UserInfos> getUser(HttpServerRequest request) {
        Future<UserInfos> futureUser = Future.future();
        UserUtils.getUserInfos(eb, request, res -> {
            if (res == null) {
                futureUser.fail("Could not found user info");
            } else {
                futureUser.complete(res);
            }
        });
        return futureUser;
    }

    private Future<MultiMap> getAttributes(HttpServerRequest request, Future<UserInfos> futureUser) {
        MultiMap form = request.formAttributes();
        form.add("platformURL", Renders.getHost(request));

        Future<MultiMap> future = Future.future();
        request.endHandler(res -> {
            futureUser.setHandler(resuser -> {
                if (resuser.succeeded()) {
                    final UserInfos user = resuser.result();
                    form
                            .add("teacherFullName", user.getFirstName() + ' ' + user.getLastName())
                            .add("teacherSchool", user.getStructureNames().stream().findFirst().orElse(""))
                            .add("teacherId", user.getUserId());
                    future.complete(form);
                } else {
                    future.fail(resuser.cause());
                }
            });
        });
        return future;
    }
}
