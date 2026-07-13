package org.entcore.workspace.controllers;

import fr.wseduc.rs.Post;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.http.BaseController;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import org.entcore.common.user.UserUtils;
import org.entcore.workspace.service.impl.DefaultCaptionService;

import java.io.FileNotFoundException;
import java.util.Optional;
import java.util.concurrent.TimeoutException;

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
                final String imageUrl = payload.getString("imageUrl");
                final boolean hasDocumentId = documentId != null && !documentId.isEmpty();
                final boolean hasImageUrl = imageUrl != null && !imageUrl.isEmpty();

                if (hasDocumentId == hasImageUrl) {
                    badRequest(request, "Exactly one of documentId or imageUrl is required");
                    return;
                }

                final String userAgent = Optional.ofNullable(request.getHeader("User-Agent")).orElse("");
                final String sessionId = UserUtils.getSessionId(request).orElse("");
                final String language = resolveLanguage(request);

                final Future<String> caption = hasDocumentId
                        ? captionService.getCaption(userInfos, documentId, sessionId, userAgent, language)
                        : captionService.getCaptionFromUrl(userInfos, imageUrl, sessionId, userAgent, language);

                caption.onSuccess(altText -> renderJson(request, new JsonObject().put("text", altText)))
                        .onFailure(error -> renderError(request, new JsonObject().put("error", error.getMessage()),
                                                        statusCodeFor(error), error.getMessage()));
            });
        }).onFailure(error -> request.response().setStatusCode(401).end(error.getMessage()));
    }

    @Post("/ocr")
    @SecuredAction("workspace.caption.generate")
    public void ocr(final HttpServerRequest request) {
        UserUtils.getAuthenticatedUserInfos(eb, request).onSuccess(userInfos -> {
            RequestUtils.bodyToJson(request, payload -> {
                final String documentId = payload.getString("documentId");
                final String imageUrl = payload.getString("imageUrl");
                final boolean hasDocumentId = documentId != null && !documentId.isEmpty();
                final boolean hasImageUrl = imageUrl != null && !imageUrl.isEmpty();

                if (hasDocumentId == hasImageUrl) {
                    badRequest(request, "Exactly one of documentId or imageUrl is required");
                    return;
                }

                final String userAgent = Optional.ofNullable(request.getHeader("User-Agent")).orElse("");
                final String sessionId = UserUtils.getSessionId(request).orElse("");
                final String language = resolveLanguage(request);

                final Future<String> ocr = hasDocumentId
                        ? captionService.getOcr(userInfos, documentId, sessionId, userAgent, language)
                        : captionService.getOcrFromUrl(userInfos, imageUrl, sessionId, userAgent, language);

                ocr.onSuccess(ocrText -> renderJson(request, new JsonObject().put("text", ocrText)))
                        .onFailure(error -> renderError(request, new JsonObject().put("error", error.getMessage()),
                                                        statusCodeFor(error), error.getMessage()));
            });
        }).onFailure(error -> request.response().setStatusCode(401).end(error.getMessage()));
    }

    private int statusCodeFor(Throwable error) {
        if (error instanceof IllegalArgumentException) {
            return 400;
        }

        if (error instanceof FileNotFoundException) {
            return 404;
        }

        if (error instanceof TimeoutException) {
            return 408;
        }

        return 500;
    }

    private String resolveLanguage(HttpServerRequest request) {
        final String acceptLanguage = I18n.acceptLanguage(request);
        if (acceptLanguage == null || acceptLanguage.isEmpty()) {
            return DEFAULT_LANGUAGE;
        }
        return acceptLanguage.split(",")[0].split("-")[0];
    }

}
