package org.entcore.workspace.controllers;

import fr.wseduc.rs.Get;
import fr.wseduc.webutils.http.BaseController;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import org.entcore.common.user.UserUtils;
import org.entcore.workspace.service.impl.DefaultCaptionService;

import java.util.Optional;

public class CaptionController extends BaseController {
    public final Vertx vertx;
    public final DefaultCaptionService captionService;

    public CaptionController(Vertx vertx,  DefaultCaptionService captionService) {
        this.vertx = vertx;
        this.captionService = captionService;
    }

    @Get("/caption/:documentId")
    public void caption(final HttpServerRequest request) {
        final String documentId = request.params().get("documentId");
        final String userAgent = Optional.ofNullable(request.getHeader("User-Agent")).orElse("");
        final String sessionId = UserUtils.getSessionId(request).orElse("");

        captionService.getCaption(documentId, sessionId, userAgent)
                .onSuccess(result -> {
                    request.response()
                           .setStatusMessage(result)
                           .setStatusCode(200)
                           .end();
                }).onFailure(error -> {
                    request.response()
                           .setStatusMessage("Error retrieving alt text: " + error.getMessage())
                           .setStatusCode(500)
                           .end();
                });
    }

    @Get("/ocr/:documentId")
    public void ocr(HttpServerRequest request) {
        final String documentId = request.params().get("documentId");
        final String userAgent = Optional.ofNullable(request.getHeader("User-Agent")).orElse("");
        final String sessionId = UserUtils.getSessionId(request).orElse("");


        captionService.getOcr(documentId, sessionId, userAgent)
              .onSuccess(result -> {
                  request.response()
                         .setStatusMessage(result)
                         .setStatusCode(200)
                         .end();
              }).onFailure(error -> {
                  request.response()
                         .setStatusMessage("Error retrieving OCR: " + error.getMessage())
                         .setStatusCode(500)
                         .end();
              });
    }

}
