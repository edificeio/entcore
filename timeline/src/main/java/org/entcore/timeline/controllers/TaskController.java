package org.entcore.timeline.controllers;

import fr.wseduc.rs.Post;
import fr.wseduc.webutils.http.BaseController;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import org.entcore.timeline.cron.DailyMailingCronTask;
import org.entcore.timeline.cron.PurgeMessageCronTask;
import org.entcore.timeline.cron.WeeklyMailingCronTask;

public class TaskController extends BaseController {
	protected static final Logger log = LoggerFactory.getLogger(TaskController.class);

	final DailyMailingCronTask dailyMailingCronTask;
	final WeeklyMailingCronTask weeklyMailingCronTask;
	final PurgeMessageCronTask purgeMessageCronTask;

	public TaskController(DailyMailingCronTask dailyMailingCronTask, WeeklyMailingCronTask weeklyMailingCronTask, PurgeMessageCronTask purgeMessageCronTask) {
		this.dailyMailingCronTask = dailyMailingCronTask;
		this.weeklyMailingCronTask = weeklyMailingCronTask;
		this.purgeMessageCronTask = purgeMessageCronTask;
	}

	@Post("api/internal/daily-mailing")
	public void dailyMailing(final HttpServerRequest request) {
		log.info("Triggered daily mailing task");
		dailyMailingCronTask.handle(0L);
		render(request, null, 202);
	}

	@Post("api/internal/weekly-mailing")
	public void weeklyMailing(final HttpServerRequest request) {
		log.info("Triggered weekly mailing task");
		weeklyMailingCronTask.handle(0L);
		render(request, null, 202);
	}

	@Post("api/internal/purge/messages")
	public void purgeMessages(final HttpServerRequest request) {
		log.info("Triggered purge message task");
		purgeMessageCronTask.handle(0L);
		render(request, null, 202);
	}


}
