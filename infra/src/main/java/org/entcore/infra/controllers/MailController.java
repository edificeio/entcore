package org.entcore.infra.controllers;

import fr.wseduc.bus.BusAddress;
import fr.wseduc.rs.Get;
import fr.wseduc.webutils.http.BaseController;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.email.impl.PostgresEmailBuilder;
import org.entcore.common.email.impl.PostgresEmailHelper;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static org.entcore.common.http.response.DefaultResponseHandler.defaultResponseHandler;

public class MailController extends BaseController implements Handler<Message<JsonObject>> {
    private final PostgresEmailHelper helper;
    private final String image;
    public MailController(Vertx vertx, JsonObject config){
        this.image = config.getString("tracking-image", "R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7");
        this.helper = PostgresEmailHelper.createDefault(vertx, config.getJsonObject("postgresql"));
    }

    @Get("/mail/:id")
    public void checkScanReport(final HttpServerRequest request) {
        final String id = request.getParam("id");
        helper.setRead(true, UUID.fromString(id));
        final HttpServerResponse response = request.response();
        response.putHeader("Content-Type", "image/png");
        response.putHeader("Cache-Control", "no-cache, no-store, must-revalidate, max-age=0");
        response.end(Buffer.buffer(Base64.getDecoder().decode(image)));
    }

    @BusAddress("org.entcore.email")
    public void handle(final Message<JsonObject> message) {
        try {
            switch (message.body().getString("action", "")) {
                case "setRead": {
                    final String mailId = message.body().getString("mailId");
                    final boolean read = message.body().getBoolean("read");
                    this.helper.setRead(read, UUID.fromString(mailId)).setHandler(r->{
                        if (r.succeeded()) {
                            message.reply(new JsonObject().put("success", true));
                        } else {
                            message.fail(400, r.cause().getMessage());
                        }
                    });
                    break;
                }
                case "send": {
                    final JsonObject jsonMail = message.body().getJsonObject("mail", new JsonObject());
                    final JsonArray attachmentsJson = message.body().getJsonArray("attachments", new JsonArray());
                    final PostgresEmailBuilder.EmailBuilder mail = PostgresEmailBuilder.mail().fromJson(jsonMail);
                    final List<PostgresEmailBuilder.AttachmentBuilder> attachments = new ArrayList<>();
                    for (final Object attObject : attachmentsJson) {
                        if (attObject instanceof JsonObject) {
                            final JsonObject json = (JsonObject) attObject;
                            attachments.add(PostgresEmailBuilder.attachment(mail).fromJson(json));
                        }
                    }
                    this.helper.createWithAttachments(mail, attachments).setHandler(r -> {
                        if (r.succeeded()) {
                            message.reply(new JsonObject().put("success", true));
                        } else {
                            message.fail(400, r.cause().getMessage());
                        }
                    });
                    break;
                }
            }
        } catch (Exception e){
            message.fail(500, e.getMessage());
        }
    }
}
