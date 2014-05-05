/*
 * Copyright. Tous droits réservés. WebServices pour l’Education.
 */

package org.entcore.common.http.filter;

import org.entcore.common.user.UserInfos;
import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServerRequest;

import fr.wseduc.webutils.http.Binding;

public interface ResourcesProvider {

	void authorize(HttpServerRequest resourceRequest, Binding binding,
				   UserInfos user, Handler<Boolean> handler);

}
