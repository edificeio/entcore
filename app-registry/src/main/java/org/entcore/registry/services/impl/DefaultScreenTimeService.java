package org.entcore.registry.services.impl;

import fr.wseduc.webutils.Either;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.*;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.registry.services.ScreenTimeService;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.time.LocalDate;
import java.util.Random;

public class DefaultScreenTimeService implements ScreenTimeService {
    private final Vertx vertx;
    private final HttpClient httpClient;

    private static final Logger log = LoggerFactory.getLogger(DefaultLibraryService.class);
    private final JsonObject authConfig;

    private final JsonObject apiConfig;


    public DefaultScreenTimeService(Vertx vertx, JsonObject config) {
        this.vertx = vertx;
        this.httpClient = vertx.createHttpClient(new HttpClientOptions().setSsl(true).setTrustAll(true));
        this.authConfig = config.getJsonObject("screen-time-config").getJsonObject("auth");
        this.apiConfig = config.getJsonObject("screen-time-config").getJsonObject("api");

    }

    @Override
    public Future<JsonObject> getDailyScreenTime(HttpServerRequest request, String token, String userId, LocalDate date, JsonObject config, Handler<Either<JsonObject, JsonObject>> eitherHandler) {
        Promise<JsonObject> promise = Promise.promise();
        // TODO remove when real data is available
        if ("true".equals(request.getParam("mock"))) {
            JsonObject mockData = getMockDailyScreenTime();
            eitherHandler.handle(new Either.Right(mockData));
            promise.complete(mockData);
            return promise.future();
        }

        String uri = config.getString("screen-time-url") + userId + "?date=" + date.toString();
        String host = config.getString("host");
        int port = config.getInteger("port", 443);
        boolean ssl = config.getBoolean("ssl", true);

        this.httpClient.request(new RequestOptions()
                        .setMethod(HttpMethod.GET)
                        .setSsl(ssl)
                        .setHost(host)
                        .setPort(port)
                        .setURI(uri))
                .compose(req -> {
                    req.putHeader("Authorization", "Bearer " + token);
                    return req.send();
                })
                .onSuccess(response -> {
                    int code = response.statusCode();
                    if (code == 200) {
                        response.body().onSuccess(body -> {
                            JsonObject json = body.toJsonObject();
                            JsonObject processed = calculateDailyTotalScreenTime(json);
                            eitherHandler.handle(new Either.Right(processed));
                            promise.complete(processed);
                        }).onFailure(err -> {
                            log.error("[screen time] - Failed to read response body: " + err.getMessage(), err);
                            eitherHandler.handle(new Either.Left(new JsonObject().put("statusCode", 500).put("message", "Failed to read response body")));
                            promise.fail(err);
                        });
                    } else if (code == 204) {
                        log.info("[screen time] - No content available yet (204)");

                        JsonObject result = new JsonObject()
                                .put("durations", new JsonArray())
                                .put("totalDurationHours", 0.0);

                        eitherHandler.handle(new Either.Right(result));
                        promise.complete(result);
                    } else {
                        response.body().onSuccess(body -> {
                            JsonObject error = new JsonObject()
                                    .put("statusCode", code)
                                    .put("message", body.toString());
                            log.error("[screen time] - Error: " + error.encodePrettily());
                            eitherHandler.handle(new Either.Left(error));
                            promise.fail(error.encode());
                        }).onFailure(err -> {
                            JsonObject error = new JsonObject().put("statusCode", 500).put("message", "Failed to read error body");
                            log.error("[screen time] - " + error.encodePrettily(), err);
                            eitherHandler.handle(new Either.Left(error));
                            promise.fail(err);
                        });
                    }
                })
                .onFailure(err -> {
                    JsonObject error = new JsonObject().put("statusCode", 500).put("message", "Request failed: " + err.getMessage());
                    log.error("[screen time] - " + error.encodePrettily(), err);
                    eitherHandler.handle(new Either.Left(error));
                    promise.fail(err);
                });

        return promise.future();
    }



    @Override
    public Future<JsonObject> getWeeklyScreenTime(HttpServerRequest request,
                                                  String token,
                                                  String userId,
                                                  LocalDate startDate,
                                                  LocalDate endDate,
                                                  JsonObject config,
                                                  Handler<Either<JsonObject, JsonObject>> eitherHandler) {

        Promise<JsonObject> promise = Promise.promise();

        // TODO remove when real data is available
        if ("true".equals(request.getParam("mock"))) {
            JsonObject mockData = getMockWeeklyScreenTime(startDate, endDate);
            eitherHandler.handle(new Either.Right(mockData));
            promise.complete(mockData);
            return promise.future();
        }

        String uri = config.getString("screen-time-weekly-url") + userId +// "?startDate=2025-06-01&endDate=2025-06-06";
                "?startDate=" + startDate.toString() +
                "&endDate=" + endDate.toString();

        String host = config.getString("host");
        int port = config.getInteger("port", 443);
        boolean ssl = config.getBoolean("ssl", true);

        this.httpClient.request(new RequestOptions()
                        .setMethod(HttpMethod.GET)
                        .setSsl(ssl)
                        .setHost(host)
                        .setPort(port)
                        .setURI(uri))
                .compose(req -> {
                    req.putHeader("Authorization", "Bearer " + token);
                    return req.send();
                })
                .onSuccess(response -> {
                    int code = response.statusCode();
                    if (code == 200) {
                        response.body().onSuccess(body -> {
                            JsonObject json = body.toJsonObject();
                            JsonObject result = calculateWeeklyAverageScreenTime(json, startDate, endDate);
                            eitherHandler.handle(new Either.Right(result));
                            promise.complete(result);
                        }).onFailure(err -> {
                            log.error("[screen time] - Failed to read body: " + err.getMessage(), err);
                            eitherHandler.handle(new Either.Left(new JsonObject().put("statusCode", 500).put("message", "Failed to read response body")));
                            promise.fail(err);
                        });
                    } else if (code == 204) {
                        log.info("[screen time] - No content available yet (204)");

                        JsonObject result = new JsonObject()
                                .put("dailySummaries", new JsonArray())
                                .put("averageDurationHours", 0.0)
                                .put("averageSchoolUsePercentage", 0.0);

                        eitherHandler.handle(new Either.Right(result));
                        promise.complete(result);
                    } else {
                        response.body().onSuccess(body -> {
                            JsonObject error = new JsonObject()
                                    .put("statusCode", code)
                                    .put("message", body.toString());
                            log.error("[screen time] - Error: " + error.encodePrettily());
                            eitherHandler.handle(new Either.Left(error));
                            promise.fail(error.encode());
                        }).onFailure(err -> {
                            JsonObject error = new JsonObject().put("statusCode", 500).put("message", "Failed to read error body");
                            log.error("[screen time] - " + error.encodePrettily(), err);
                            eitherHandler.handle(new Either.Left(error));
                            promise.fail(err);
                        });
                    }
                })
                .onFailure(err -> {
                    JsonObject error = new JsonObject().put("statusCode", 500).put("message", "Request failed: " + err.getMessage());
                    log.error("[screen time] - " + error.encodePrettily(), err);
                    eitherHandler.handle(new Either.Left(error));
                    promise.fail(err);
                });

        return promise.future();
    }

    @Override
    public  Future<JsonObject> getAccessToken(JsonObject config) throws UnsupportedEncodingException {
        Promise<JsonObject> promise = Promise.promise();

        String host = config.getString("host");
        int port = config.getInteger("port", 443);
        boolean ssl = config.getBoolean("ssl", true);
        String uri = config.getString("path");
        String scope = config.getString("scope");

        String formPayload =
                "client_id=" + URLEncoder.encode(config.getString("clientId"), "UTF-8") +
                        "&client_secret=" + URLEncoder.encode(config.getString("clientSecret"), "UTF-8") +
                        "&grant_type=client_credentials" +
                        "&scope=" + URLEncoder.encode(scope, "UTF-8");

        Buffer formBody = Buffer.buffer(formPayload);

        this.httpClient.request(new RequestOptions()
                        .setMethod(HttpMethod.POST)
                        .setSsl(ssl)
                        .setHost(host)
                        .setPort(port)
                        .setURI(uri))
                .compose(req -> {
                    req.putHeader("Content-Type", "application/x-www-form-urlencoded");
                    req.putHeader("Content-Length", String.valueOf(formBody.length()));
                    return req.send(formBody);
                })
                .onSuccess(response -> {
                    if (response.statusCode() == 200) {
                        response.bodyHandler(body -> {
                            JsonObject json = body.toJsonObject();
                            promise.complete(json);
                        });
                    } else {
                        log.error("[screen time] - Auth failed with status " + response.statusCode() + ": " + response.statusMessage());
                        promise.fail("Auth failed: " + response.statusMessage());
                    }
                })
                .onFailure(err -> {
                    log.error("[screen time] - HTTP request failed: " + err.getMessage(), err);
                    promise.fail(err);
                });

        return promise.future();
    }

    private JsonObject calculateDailyTotalScreenTime(JsonObject screenTimeData) {
        JsonArray durations = new JsonArray();
        int totalSeconds = 0;

        for (int i = 0; i < 24; i++) {
            String hourKey = String.format("%02d", i);
            JsonObject hourData = screenTimeData.getJsonObject(hourKey);

            int durationSeconds = 0;
            double percentage = 0.0;

            if (hourData != null && hourData.containsKey("duration")) {
                durationSeconds = hourData.getInteger("duration", 0);
                percentage = Math.round(hourData.getDouble("schoolUsePercentage", 0.0) * 10.0) / 10.0;
            }

            int durationMinutes = (int) Math.round(durationSeconds / 60.0);

            JsonObject durationEntry = new JsonObject()
                    .put("hour", hourKey)
                    .put("durationMinutes", durationMinutes)
                    .put("schoolUsePercentage", percentage);

            durations.add(durationEntry);
            totalSeconds += durationSeconds;
        }

        double totalHoursRounded = Math.round((totalSeconds / 3600.0) * 100.0) / 100.0;

        int hours = totalSeconds / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        int seconds = totalSeconds % 60;

        StringBuilder totalDurationString = new StringBuilder();
        if (hours > 0) totalDurationString.append(hours).append("h");
        if (minutes > 0) totalDurationString.append(minutes).append("m");
        if (seconds > 0) totalDurationString.append(seconds).append("s");

        return new JsonObject()
                .put("durations", durations)
                .put("totalDurationHours", totalHoursRounded)
                .put("totalDurationString", totalDurationString.toString());
    }


    private JsonObject calculateWeeklyAverageScreenTime(JsonObject weeklyData, LocalDate weekStartDate, LocalDate weekEndDate) {
        JsonArray dailySummaries = new JsonArray();
        int totalDurationSeconds = 0;
        double totalSchoolUsePercentage = 0.0;
        int dayCount = 0;

        LocalDate date = weekStartDate;
        while (!date.isAfter(weekEndDate)) {
            String dateKey = date.toString(); // yyyy-MM-dd
            JsonObject dayData = null;

            // Try to find exact match or timestamped version in weeklyData
            for (String key : weeklyData.fieldNames()) {
                if (key.startsWith(dateKey)) {
                    dayData = weeklyData.getJsonObject(key);
                    break;
                }
            }

            int durationSeconds = 0;
            double percentage = 0.0;

            if (dayData != null && dayData.containsKey("duration")) {
                durationSeconds = dayData.getInteger("duration", 0);
                percentage = dayData.getDouble("schoolUsePercentage", 0.0);

                totalDurationSeconds += durationSeconds;
                totalSchoolUsePercentage += percentage;
                dayCount++;
            }

            JsonObject daySummary = new JsonObject()
                    .put("date", dateKey)
                    .put("durationMinutes", (int) Math.round(durationSeconds / 60.0))
                    .put("schoolUsePercentage", Math.round(percentage * 10.0) / 10.0);

            dailySummaries.add(daySummary);
            date = date.plusDays(1);
        }

        double averageDurationHours = dayCount > 0
                ? Math.round((totalDurationSeconds / 3600.0 / dayCount) * 100.0) / 100.0
                : 0.0;

        double averageSchoolUse = dayCount > 0
                ? Math.round((totalSchoolUsePercentage / dayCount) * 10.0) / 10.0
                : 0.0;

        // Compute average duration string
        int avgSecondsPerDay = dayCount > 0 ? totalDurationSeconds / dayCount : 0;
        int hours = avgSecondsPerDay / 3600;
        int minutes = (avgSecondsPerDay % 3600) / 60;
        int seconds = avgSecondsPerDay % 60;

        StringBuilder averageDurationString = new StringBuilder();
        if (hours > 0) averageDurationString.append(hours).append("h");
        if (minutes > 0) averageDurationString.append(minutes).append("m");
        if (seconds > 0) averageDurationString.append(seconds).append("s");

        return new JsonObject()
                .put("dailySummaries", dailySummaries)
                .put("averageDurationHours", averageDurationHours)
                .put("averageSchoolUsePercentage", averageSchoolUse)
                .put("averageDurationString", averageDurationString.toString());
    }



    private JsonObject getMockDailyScreenTime() {
        Random random = new Random();
        JsonObject data = new JsonObject();

        for (int hour = 0; hour < 24; hour++) {
            JsonObject hourData = new JsonObject()
                    .put("duration", random.nextInt(600)) // up to 10 min per hour (600s)
                    .put("schoolUsePercentage", random.nextDouble() * 100);
            data.put(String.format("%02d", hour), hourData);
        }

        return calculateDailyTotalScreenTime(data);
    }

    private JsonObject getMockWeeklyScreenTime(LocalDate startDate, LocalDate endDate) {
        Random random = new Random();
        JsonObject data = new JsonObject();

        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            String isoDate = currentDate + "T00:00:00Z";
            data.put(isoDate, new JsonObject()
                    .put("duration", 3600 + random.nextInt(10800)) // 1 to 4 hours
                    .put("schoolUsePercentage", 70 + random.nextDouble() * 30)); // 70% to 100%

            currentDate = currentDate.plusDays(1);
        }

        return calculateWeeklyAverageScreenTime(data, startDate, endDate);
    }
}
