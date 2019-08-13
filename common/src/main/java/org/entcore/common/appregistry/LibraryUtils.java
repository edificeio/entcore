package org.entcore.common.appregistry;

import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;

import java.util.concurrent.CompletableFuture;

import static fr.wseduc.webutils.http.Renders.renderJson;

public final class LibraryUtils {
    public static final String LIBRARY_BUS_ADDRESS = "wse.app.registry.library.publish";

    public static void publish(String application, EventBus eb, final HttpServerRequest request) {
        request.setExpectMultipart(true);
        CompletableFuture<Buffer> coverFuture = new CompletableFuture<>();
        request.uploadHandler(upload -> {
            final Buffer buffer = Buffer.buffer();
            upload.handler(b -> buffer.appendBuffer(b));
            upload.endHandler(v -> coverFuture.complete(buffer));
        });

        CompletableFuture<MultiMap> formFuture = new CompletableFuture<>();
        request.endHandler(v -> formFuture.complete(request.formAttributes()));

        CompletableFuture<UserInfos> userFuture = new CompletableFuture<>();
        UserUtils.getUserInfos(eb, request, userFuture::complete);

        CompletableFuture.allOf(coverFuture, formFuture, userFuture).thenAccept(v -> {
            Buffer cover = coverFuture.join();
            MultiMap form = formFuture.join();
            UserInfos user = userFuture.join();

            Buffer message = Buffer.buffer();
            LibraryBusObjectCodec libraryBusObjectCodec = new LibraryBusObjectCodec();
            libraryBusObjectCodec.encodeToWire(message, new LibraryBusObject(addAttributes(form, application, user), cover));

            eb.send(LIBRARY_BUS_ADDRESS, message,
                    reply -> {
                        if (reply.succeeded()) {
                            JsonObject response = (JsonObject) reply.result().body();
                            boolean success = response.getBoolean("success", false);
                            if (success) {
                                renderJson(request, response, 200);
                            } else {
                                renderJson(request, response, 500);
                            }
                        } else {
                            JsonObject response = new JsonObject()
                                    .put("error", reply.cause().getMessage());
                            renderJson(request, response, 500);
                        }
                    });
        });
    }

    private static MultiMap addAttributes(MultiMap form, String application, UserInfos user) {
        return form
                .add("teacherFullName", user.getFirstName() + ' ' + user.getLastName())
                .add("teacherCity", "")
                .add("teacherSchool", "")
                .add("application", application);
    }
}
