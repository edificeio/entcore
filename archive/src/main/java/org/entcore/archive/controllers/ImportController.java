package org.entcore.archive.controllers;

import fr.wseduc.rs.Get;
import fr.wseduc.rs.Post;
import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.http.BaseController;
import fr.wseduc.webutils.http.Renders;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import org.entcore.archive.services.ImportService;
import org.entcore.archive.services.impl.DefaultImportService;
import org.entcore.common.storage.Storage;
import org.entcore.common.user.UserUtils;

import java.io.File;

public class ImportController extends BaseController {

    private ImportService importService;
    private Storage storage;

    public ImportController(Vertx vertx, Storage storage, String importPath) {
        this.storage = storage;
        this.importService = new DefaultImportService(vertx, storage, importPath);
    }

    @Post("/import/upload")
    public void upload(final HttpServerRequest request) {
        importService.uploadArchive(request, handler -> {
            if (handler.isLeft()) {
                badRequest(request, handler.left().getValue());
            } else {
                renderJson(request, new JsonObject().put("importId",handler.right().getValue()));
            }
        });
    }

    @Get("import/analyze/:importId")
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
    public void delete(final HttpServerRequest request) {
        final String importId = request.params().get("importId");
        importService.deleteArchive(importId);
        request.response().setStatusCode(200).end();
    }

    @Post("import/:importId/launch")
    public void launchImport(final HttpServerRequest request) {
        final String importId = request.params().get("importId");
        RequestUtils.bodyToJson(request, body -> {
            String importPath = body.getString("importPath");
            JsonObject apps = body.getJsonObject("apps");
            UserUtils.getUserInfos(eb, request, user -> {
                importService.launchImport(user.getUserId(), importId, importPath, apps);
                request.response().setStatusCode(200).end();
            });
        });
    }

}
