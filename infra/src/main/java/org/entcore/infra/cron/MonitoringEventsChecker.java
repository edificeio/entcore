/*
 * Copyright © "Open Digital Education", 2023
 *
 * This program is published by "Open Digital Education".
 * You must indicate the name of the software and the company in any production /contribution
 * using the software and indicate on the home page of the software industry in question,
 * "powered by Open Digital Education" with a reference to the website: https://opendigitaleducation.com/.
 *
 * This program is free software, licensed under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation, version 3 of the License.
 *
 * You can redistribute this application and/or modify it since you respect the terms of the GNU Affero General Public License.
 * If you modify the source code and then use this modified source code in your creation, you must make available the source code of your modifications.
 *
 * You should have received a copy of the GNU Affero General Public License along with the software.
 * If not, please see : <http://www.gnu.org/licenses/>. Full compliance requires reading the terms of this license and following its directives.

 */

package org.entcore.infra.cron;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalUnit;
import java.util.Date;

import org.entcore.common.user.UserUtils;
import org.entcore.common.utils.HostUtils;

import fr.wseduc.mongodb.MongoDb;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class MonitoringEventsChecker implements Handler<Long> {

    private static final String SERVICE_LAUNCHER = "service-launcher.deployment";
    private static final Logger log = LoggerFactory.getLogger(MonitoringEventsChecker.class);

    private final MongoDb mongoDb = MongoDb.getInstance();
    private final int minDelay;
    private final int threshold;
    private final EventBus eb;
    private final long windowDuration;

    public MonitoringEventsChecker(EventBus eb, int minDelay, int threshold, long windowDuration) {
        this.eb = eb;
        this.minDelay = minDelay;
        this.threshold = threshold;
        this.windowDuration = windowDuration;
    }

    @Override
    public void handle(Long l) {
        final JsonObject query = new JsonObject()
                .put("epoch", new JsonObject().put("$gt", Instant.now().minusMillis(windowDuration).toEpochMilli()))
                .put("type", UserUtils.FIND_SESSION)
                .put("hostname", HostUtils.getHostName())
                .put("delay", new JsonObject().put("$gt", minDelay));
        mongoDb.count(UserUtils.MONITORINGEVENTS, query, message -> {
			final JsonObject body = message.body();
			if ("ok".equals(body.getString("status"))) {
                if (body.getInteger("count") > threshold) {
                    log.warn("Count monitoring events findSession : " + body.getInteger("count"));
                    eb.request(SERVICE_LAUNCHER, new JsonObject().put("action", "restart-module").put("module-name", "session-redis"), ar -> {
                        if (ar.succeeded()) {
                            log.info("Successful restart module session-redis on " + HostUtils.getHostName());
                        } else {
                            log.error("Error restart module session-redis on " + HostUtils.getHostName(), ar.cause());
                        }
                    });
                }
			} else {
				log.error("Error getting count of findSession monitoring events");
			}
		});
    }

}