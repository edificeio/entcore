package org.entcore.workspace.service;

import io.vertx.core.Future;

public interface CaptionService {

    Future<String> getCaption(String documentId, String sessionId, String userAgent);

    Future<String> getOcr(String documentId, String sessionId, String userAgent);
}
