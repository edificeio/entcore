package org.entcore.admin.filters;


import org.entcore.common.http.filter.AdmlResourcesProvider;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserInfos.Function;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;

import fr.wseduc.webutils.http.Binding;

public class AdminStructureFilter extends AdmlResourcesProvider {

	@Override
	public void authorizeAdml(HttpServerRequest resourceRequest,
				Binding binding, UserInfos user, Function adminLocal,
				Handler<Boolean> handler) {
		String structureId = resourceRequest.params().get("id");
		handler.handle(adminLocal.getScope().contains(structureId));
	}


}
