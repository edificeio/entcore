package org.entcore.history;

import org.entcore.history.controllers.HistoryController;
import fr.wseduc.webutils.Server;
import org.entcore.common.http.filter.ActionFilter;
import fr.wseduc.webutils.request.filter.SecurityHandler;

public class History extends Server {

	@Override
	public void start() {
		super.start();
		HistoryController historyController = new HistoryController(vertx, container, rm, securedActions);

		historyController
				.get("/admin", "history")
				.get("/admin/logs","logs");

		SecurityHandler.addFilter(new ActionFilter(historyController.securedUriBinding(), getEventBus(vertx)));
	}
}