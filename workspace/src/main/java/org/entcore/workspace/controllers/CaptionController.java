package org.entcore.workspace.controllers;

import fr.wseduc.rs.Get;
import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.http.BaseController;
import io.vertx.core.http.HttpServerRequest;
import org.entcore.common.user.UserUtils;
import org.entcore.workspace.service.impl.DefaultCaptionService;

import java.io.FileNotFoundException;
import java.util.Optional;

public class CaptionController extends BaseController {
    private static final String DEFAULT_LANGUAGE = "fr";

    public final DefaultCaptionService captionService;

    public CaptionController(DefaultCaptionService captionService) {
        this.captionService = captionService;
    }

    @Get("/caption/:documentId")
    public void caption(final HttpServerRequest request) {
        final String documentId = request.params().get("documentId");
        final String userAgent = Optional.ofNullable(request.getHeader("User-Agent")).orElse("");
        final String sessionId = UserUtils.getSessionId(request).orElse("");
        final String acceptLanguage = I18n.acceptLanguage(request);
        final String language = (acceptLanguage == null || acceptLanguage.isEmpty()) ? DEFAULT_LANGUAGE : acceptLanguage.split(
                ",")[0].split("-")[0];

        UserUtils.getUserInfos(eb, request, userInfo -> {
            captionService.getCaption(userInfo, documentId, sessionId, userAgent, language)
                          .onSuccess(text -> request.response()
                                                    .putHeader("Content-Type", "text/plain; charset=utf-8")
                                                    .setStatusCode(200)
                                                    .end(text))
                          .onFailure(error -> {
                              final boolean notFound = error instanceof FileNotFoundException;
                              request.response().setStatusCode(notFound ? 404 : 500).end(error.getMessage());
                          });
        });
    }

    @Get("/ocr/:documentId")
    public void ocr(final HttpServerRequest request) {
        final String documentId = request.params().get("documentId");
        final String userAgent = Optional.ofNullable(request.getHeader("User-Agent")).orElse("");
        final String sessionId = UserUtils.getSessionId(request).orElse("");
        final String acceptLanguage = I18n.acceptLanguage(request);
        final String language = (acceptLanguage == null || acceptLanguage.isEmpty()) ? DEFAULT_LANGUAGE : acceptLanguage.split(
                ",")[0].split("-")[0];

        UserUtils.getUserInfos(eb, request, userInfo -> {
            captionService.getOcr(userInfo, documentId, sessionId, userAgent, language)
                          .onSuccess(text -> request.response()
                                                    .putHeader("Content-Type", "text/plain; charset=utf-8")
                                                    .setStatusCode(200)
                                                    .end(text))
                          .onFailure(error -> {
                              final boolean notFound = error instanceof FileNotFoundException;
                              request.response().setStatusCode(notFound ? 404 : 500).end(error.getMessage());
                          });
        });
    }

}
