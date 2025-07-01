package org.entcore.registry.services;

import fr.wseduc.webutils.Either;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;

import java.io.UnsupportedEncodingException;
import java.time.LocalDate;

public interface ScreenTimeService {

    Future<JsonObject> getAccessToken(JsonObject config) throws UnsupportedEncodingException;
    Future<JsonObject> getDailyScreenTime(HttpServerRequest request, String accessToken, String id, LocalDate date, JsonObject config , Handler<Either<JsonObject, JsonObject>> eitherHandler) throws UnsupportedEncodingException;
    Future<JsonObject> getWeeklyScreenTime(HttpServerRequest request, String accessToken, String id, LocalDate startDate, LocalDate endDate, JsonObject config , Handler<Either<JsonObject, JsonObject>> eitherHandler) throws UnsupportedEncodingException;

}
