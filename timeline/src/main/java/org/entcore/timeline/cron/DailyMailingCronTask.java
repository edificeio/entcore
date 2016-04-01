package org.entcore.timeline.cron;

import org.entcore.common.notification.TimelineMailer;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.logging.impl.LoggerFactory;

import fr.wseduc.webutils.Either;

public class DailyMailingCronTask implements Handler<Long> {

	private static final Logger log = LoggerFactory.getLogger(DailyMailingCronTask.class);
	private final TimelineMailer mailer;
	private final int dayDelta;

	public DailyMailingCronTask(TimelineMailer mailer, int dayDelta){
		this.mailer = mailer;
		this.dayDelta = dayDelta;
	}

	@Override
	public void handle(Long event) {
		log.info("[Daily mailing] Starting ...");
		mailer.sendDailyMails(dayDelta, new Handler<Either<String,JsonObject>>() {
			public void handle(Either<String, JsonObject> event) {
				if(event.isLeft()){
					log.error("[Daily mailing] Error encountered : " + event.left().getValue());
				} else {
					log.info("[Daily mailing] Completed : " + event.right().getValue().encodePrettily());
				}
			}
		});
	}

}
