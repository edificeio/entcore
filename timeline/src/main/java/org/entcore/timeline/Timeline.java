package org.entcore.timeline;

import fr.wseduc.webutils.request.filter.UserAuthFilter;
import fr.wseduc.webutils.security.oauth.DefaultOAuthResourceProvider;
import org.entcore.common.http.BaseServer;
import org.entcore.timeline.controllers.TimelineController;

public class Timeline extends BaseServer {

	@Override
	public void start() {
		clearFilters();
		setOauthClientGrant(true);
		addFilter(new UserAuthFilter(new DefaultOAuthResourceProvider(getEventBus(vertx))));
		super.start();
		addController(new TimelineController());
	}

}
