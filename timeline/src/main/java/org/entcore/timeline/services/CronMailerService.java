package org.entcore.timeline.services;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;

import java.util.Date;

public interface CronMailerService {

    Future<JsonObject> sendDailyMails(Date date, int dayDelta);

    Future<JsonObject> sendDailyMails(int dayDelta);

    Future<JsonObject> sendWeeklyMails(Date date, int dayDelta);

    Future<JsonObject> sendWeeklyMails(int dayDelta);
}
