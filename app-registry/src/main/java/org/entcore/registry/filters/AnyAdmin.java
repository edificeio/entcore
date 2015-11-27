package org.entcore.registry.filters;

import java.util.Map;

import org.entcore.common.http.filter.ResourcesProvider;
import org.entcore.common.user.DefaultFunctions;
import org.entcore.common.user.UserInfos;
import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServerRequest;

import fr.wseduc.webutils.http.Binding;

public class AnyAdmin implements ResourcesProvider{

	@Override
	public void authorize(HttpServerRequest resourceRequest, Binding binding, UserInfos user, Handler<Boolean> handler) {
		Map<String, UserInfos.Function> functions = user.getFunctions();

		if (functions == null || functions.isEmpty()) {
			handler.handle(false);
			return;
		}
		if (functions.containsKey(DefaultFunctions.SUPER_ADMIN) ||
			functions.containsKey(DefaultFunctions.ADMIN_LOCAL) ||
			functions.containsKey(DefaultFunctions.CLASS_ADMIN)) {
			handler.handle(true);
			return;
		}

		handler.handle(false);
	}

}
