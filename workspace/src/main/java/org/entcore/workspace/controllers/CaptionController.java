package org.entcore.workspace.controllers;

import fr.wseduc.rs.Post;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.http.BaseController;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
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

    @Post("/caption")
    @SecuredAction("workspace.caption.generate")
    public void caption(final HttpServerRequest request) {
        UserUtils.getAuthenticatedUserInfos(eb, request).onSuccess(userInfos -> {
            RequestUtils.bodyToJson(request, payload -> {
                final String documentId = payload.getString("documentId");

                if (documentId == null || documentId.isEmpty()) {
                    badRequest(request, "documentId is required");
                    return;
                }

                final String userAgent = Optional.ofNullable(request.getHeader("User-Agent")).orElse("");
                final String sessionId = UserUtils.getSessionId(request).orElse("");
                final String language = resolveLanguage(request);

                captionService.getCaption(userInfos, documentId, sessionId, userAgent, language).onSuccess(altText -> {
                    final JsonObject body = new JsonObject().put("text", altText);

                    request.response()
                            .putHeader("content-type", "application/json")
                            .setStatusCode(200)
                            .end(body.encode());
                }).onFailure(error -> {
                    final boolean notFound = error instanceof FileNotFoundException;
                    request.response().setStatusCode(notFound ? 404 : 500).end(error.getMessage());
                });
            });
        }).onFailure(error -> request.response().setStatusCode(401).end(error.getMessage()));
    }

    @Post("/ocr")
    @SecuredAction("workspace.caption.generate")
    public void ocr(final HttpServerRequest request) {
        UserUtils.getAuthenticatedUserInfos(eb, request).onSuccess(userInfos -> {
            RequestUtils.bodyToJson(request, payload -> {
                final String documentId = payload.getString("documentId");

                if (documentId == null || documentId.isEmpty()) {
                    badRequest(request, "documentId is required");
                    return;
                }

                final String userAgent = Optional.ofNullable(request.getHeader("User-Agent")).orElse("");
                final String sessionId = UserUtils.getSessionId(request).orElse("");
                final String language = resolveLanguage(request);

                captionService.getOcr(userInfos, documentId, sessionId, userAgent, language).onSuccess(ocr -> {
                    final JsonObject body = new JsonObject().put("text", ocr);

                    request.response()
                            .putHeader("content-type", "application/json")
                            .setStatusCode(200)
                            .end(body.encode());
                }).onFailure(error -> {
                    final JsonObject body = new JsonObject().put("error", error.getMessage());
                    final boolean notFound = error instanceof FileNotFoundException;

                    System.out.println(error.getMessage());

                    request.response().setStatusCode(notFound ? 404 : 500).end(body.encode());
                });
            });
        }).onFailure(error -> request.response().setStatusCode(401).end(error.getMessage()));
    }

    private String resolveLanguage(HttpServerRequest request) {
        final String acceptLanguage = I18n.acceptLanguage(request);
        if (acceptLanguage == null || acceptLanguage.isEmpty()) {
            return DEFAULT_LANGUAGE;
        }
        return acceptLanguage.split(",")[0].split("-")[0];
    }

}
