package org.entcore.archive.filters;

import fr.wseduc.webutils.http.Binding;
import org.entcore.common.http.filter.BaseResourceProvider;
import org.entcore.common.user.UserInfos;
import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServerRequest;

public class ArchiveFilter extends BaseResourceProvider {

	@Override
	protected String defaultFilter() {
		return "downloadExport";
	}

	public void authorize(HttpServerRequest resourceRequest, Binding binding,
						  UserInfos user, Handler<Boolean> handler) {
		String exportId = resourceRequest.params().get("exportId");
		handler.handle(exportId != null && exportId.endsWith(user.getUserId()));
	}

}
