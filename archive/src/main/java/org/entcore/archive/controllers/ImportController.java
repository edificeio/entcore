package org.entcore.archive.controllers;

import fr.wseduc.bus.BusAddress;
import fr.wseduc.rs.Get;
import fr.wseduc.rs.Post;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.http.BaseController;
import fr.wseduc.webutils.http.Renders;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import org.entcore.archive.services.ImportService;
import org.entcore.archive.services.impl.DefaultImportService;
import org.entcore.common.storage.Storage;
import org.entcore.common.user.UserUtils;
import org.vertx.java.core.http.RouteMatcher;

import java.io.File;
import java.util.Map;

public class ImportController extends BaseController {
    private static final Logger log = LoggerFactory.getLogger(ImportController.class);

    private ImportService importService;
    private Storage storage;
    private Map<String, Long> archiveInProgress;

    public ImportController(ImportService importService, Storage storage, Map<String, Long> archiveInProgress) {
        this.importService = importService;
        this.storage = storage;
        this.archiveInProgress = archiveInProgress;
    }

    @Post("/import/upload")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void upload(final HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, user -> {
            if (importService.isUserAlreadyImporting(user.getUserId())) {
                renderError(request);
                log.error("[upload] User is already importing " + user.getUsername());
            } else {
                importService.uploadArchive(request, user, handler -> {
                    if (handler.isLeft()) {
                        badRequest(request, handler.left().getValue());
                        log.error("[upload] User import failed " + user.getUsername()+" - "+handler.left().getValue());
                    } else {
                        renderJson(request, new JsonObject().put("importId", handler.right().getValue()));
                    }
                });
            }
        });
    }

    @Get("import/analyze/:importId")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void analyze(final HttpServerRequest request) {
        final String importId = request.params().get("importId");
        UserUtils.getUserInfos(eb, request, user -> {
            importService.analyzeArchive(user, importId, I18n.acceptLanguage(request), config, handler -> {
                if (handler.isLeft()) {
                    renderError(request, new JsonObject().put("error", handler.left().getValue()));
                    log.error("[analyze] Analyze import failed " + user.getUsername()+" - "+handler.left().getValue());
                } else {
                    renderJson(request, handler.right().getValue());
                }
            });
        });
    }

    @Get("import/delete/:importId")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void delete(final HttpServerRequest request) {
        final String importId = request.params().get("importId");
        importService.deleteArchive(importId);
        request.response().setStatusCode(200).end();
    }

    @Post("import/:importId/launch")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void launchImport(final HttpServerRequest request)
    {
        final String importId = request.params().get("importId");
        RequestUtils.bodyToJson(request, body ->
        {
            JsonObject apps = body.getJsonObject("apps");

            UserUtils.getUserInfos(eb, request, user ->
            {
                importService.launchImport(user.getUserId(), user.getLogin(), user.getUsername(), importId,
                    I18n.acceptLanguage(request), request.headers().get("Host"), apps);
                final String address = importService.getImportBusAddress(importId);
                final MessageConsumer<JsonObject> consumer = eb.consumer(address);

                final Handler<Message<JsonObject>> importHandler = event -> {
                    if ("ok".equals(event.body().getString("status"))) {
                        event.reply(new JsonObject().put("status", "ok"));
                        renderJson(request, event.body().getJsonObject("result"));
                    } else {
                        event.reply(new JsonObject().put("status", "error"));
                        renderError(request, event.body());
                        log.error("[launch] Launch import failed " + user.getUsername()+" - "+event.body().getString("message"));
                    }
                    consumer.unregister();
                };
                request.response().closeHandler(new Handler<Void>() {
                    @Override
                    public void handle(Void event) {
                        consumer.unregister();
                        if (log.isDebugEnabled()) {
                            log.debug("Unregister handler : " + address);
                        }
                    }
                });
                consumer.handler(importHandler);
            });
        });
    }

    @BusAddress("entcore.import")
    public void export(Message<JsonObject> message) {
        String action = message.body().getString("action", "");
        switch (action) {
            case "imported" :
                String importId = message.body().getString("importId");
                String app = message.body().getString("app");
                JsonObject rapport = message.body().getJsonObject("rapport");

                importService.imported(importId, app, rapport);
                break;
            default: log.error("Archive : invalid action " + action);
        }
    }

}
