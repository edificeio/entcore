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
import org.entcore.archive.services.ImportService;
import org.entcore.archive.services.impl.DefaultImportService;
import org.entcore.common.storage.Storage;
import org.entcore.common.user.UserUtils;
import org.vertx.java.core.http.RouteMatcher;

import java.io.File;
import java.util.Map;

public class ImportController extends BaseController {

    private ImportService importService;
    private Storage storage;
    private Map<String, Long> archiveInProgress;

    public ImportController(ImportService importService, Storage storage, Map<String, Long> archiveInProgress) {
        this.importService = importService;
        this.storage = storage;
        this.archiveInProgress = archiveInProgress;
    }

    @Post("/import/upload")
    public void upload(final HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, user -> {
            importService.uploadArchive(request, user, handler -> {
                if (handler.isLeft()) {
                    badRequest(request, handler.left().getValue());
                } else {
                    renderJson(request, new JsonObject().put("importId", handler.right().getValue()));
                }
            });
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
            String importPath = body.getString("importPath");
            JsonObject apps = body.getJsonObject("apps");

            UserUtils.getUserInfos(eb, request, user ->
            {
                importService.launchImport(user.getUserId(), user.getUsername(), importId, importPath,
                    I18n.acceptLanguage(request), apps);
                final String address = importService.getImportBusAddress(importId);
                final MessageConsumer<JsonObject> consumer = eb.consumer(address);

                final Handler<Message<JsonObject>> importHandler = event -> {
                    if ("ok".equals(event.body().getString("status"))) {
                        event.reply(new JsonObject().put("status", "ok"));
                        renderJson(request, event.body().getJsonObject("result"));
                    } else {
                        event.reply(new JsonObject().put("status", "error"));
                        renderError(request, event.body());
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
                String resourcesNumber = message.body().getString("resourcesNumber");
                String duplicatesNumber = message.body().getString("duplicatesNumber");
                String errorsNumber = message.body().getString("errorsNumber");
                importService.imported(importId, app, resourcesNumber, duplicatesNumber, errorsNumber);
                break;
            default: log.error("Archive : invalid action " + action);
        }
    }

}
