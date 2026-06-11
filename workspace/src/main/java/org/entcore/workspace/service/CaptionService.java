package org.entcore.workspace.service;

import io.vertx.core.Future;
import org.entcore.common.user.UserInfos;

public interface CaptionService {

    Future<String> getCaption(UserInfos user, String documentId, String sessionId, String userAgent, String language);

    Future<String> getOcr(UserInfos user, String documentId, String sessionId, String userAgent, String language);
}
