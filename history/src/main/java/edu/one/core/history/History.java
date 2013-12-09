package edu.one.core.history;

import edu.one.core.history.controllers.HistoryController;
import edu.one.core.infra.Server;
import edu.one.core.common.http.filter.ActionFilter;
import edu.one.core.infra.request.filter.SecurityHandler;

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