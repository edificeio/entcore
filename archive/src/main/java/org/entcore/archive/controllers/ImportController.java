package org.entcore.archive.controllers;

import fr.wseduc.rs.Get;
import fr.wseduc.rs.Post;
import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.http.BaseController;
import fr.wseduc.webutils.http.Renders;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import org.entcore.archive.services.ImportService;
import org.entcore.archive.services.impl.DefaultImportService;
import org.entcore.common.storage.Storage;

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
        importService.analyzeArchive(importId, I18n.acceptLanguage(request), handler -> {
            if (handler.isLeft()) {
                renderError(request, new JsonObject().put("error", handler.left().getValue()));
            } else {
                renderJson(request, handler.right().getValue());
            }
        });
    }

}
