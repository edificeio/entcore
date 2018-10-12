/*
 * Copyright © "Open Digital Education", 2016
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

package org.entcore.timeline.cron;

import org.entcore.timeline.services.TimelineMailerService;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import fr.wseduc.webutils.Either;

public class WeeklyMailingCronTask implements Handler<Long> {

	private static final Logger log = LoggerFactory.getLogger(WeeklyMailingCronTask.class);
	private final TimelineMailerService mailerService;
	private final int dayDelta;

	public WeeklyMailingCronTask(TimelineMailerService mailer, int dayDelta){
		this.mailerService = mailer;
		this.dayDelta = dayDelta;
	}

	@Override
	public void handle(Long event) {
		log.info("[Weekly mailing] Starting ...");
		mailerService.sendWeeklyMails(dayDelta, new Handler<Either<String,JsonObject>>() {
			public void handle(Either<String, JsonObject> event) {
				if(event.isLeft()){
					log.error("[Weekly mailing] Error encountered : " + event.left().getValue());
				} else {
					log.info("[Weekly mailing] Completed : " + event.right().getValue().encodePrettily());
				}
			}
		});
	}

}
