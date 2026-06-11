package org.entcore.timeline.services;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;

import java.time.Instant;
import java.util.Date;

public interface CronMailerService {

    Future<JsonObject> sendDailyMails(Date date, int dayDelta);

    Future<JsonObject> sendDailyMails(int dayDelta);

    /**
     * Timezone-aware daily mailing, meant to be triggered hourly.
     * Processes only the users whose local hour is 06:00 at runTime, over a
     * 24h sliding window [runTime - 24h, runTime], and schedules mails at 07:00 local time.
     *
     * @param runTime the run instant (truncated to the hour internally); pass a past instant to replay a missed run
     */
    Future<JsonObject> sendDailyMailsByTimezone(Instant runTime);

    Future<JsonObject> sendWeeklyMails(Date date, int dayDelta);

    Future<JsonObject> sendWeeklyMails(int dayDelta);
}
