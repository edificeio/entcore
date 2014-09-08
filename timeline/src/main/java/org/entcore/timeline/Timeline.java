package org.entcore.timeline;

import org.entcore.common.http.BaseServer;
import org.entcore.timeline.controllers.TimelineController;

public class Timeline extends BaseServer {

	@Override
	public void start() {
		super.start();
		addController(new TimelineController());
	}

}
