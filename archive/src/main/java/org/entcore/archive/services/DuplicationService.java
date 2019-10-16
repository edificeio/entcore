package org.entcore.archive.services;

import fr.wseduc.webutils.Either;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;

import org.entcore.common.user.UserInfos;

public interface DuplicationService
{
  void duplicateSingleResource(final UserInfos user, final HttpServerRequest request, JsonArray apps, JsonArray resourcesIds,
		JsonObject config, Handler<Either<String, String>> handler);

  void exported(final String exportId, String status, final String locale, final String host);
  void imported(String importId, String app, String resourcesNumber, String duplicatesNumber, String errorsNumber);
}
