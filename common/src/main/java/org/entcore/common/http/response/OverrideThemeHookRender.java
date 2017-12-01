/*
 * Copyright © WebServices pour l'Éducation, 2017
 *
 * This file is part of ENT Core. ENT Core is a versatile ENT engine based on the JVM.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 * For the sake of explanation, any module that communicate over native
 * Web protocols, such as HTTP, with ENT Core is outside the scope of this
 * license and could be license under its own terms. This is merely considered
 * normal use of ENT Core, and does not fall under the heading of "covered work".
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */

package org.entcore.common.http.response;

import fr.wseduc.webutils.http.HookProcess;
import fr.wseduc.webutils.http.Renders;
import fr.wseduc.webutils.request.CookieHelper;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServerRequest;

import static org.entcore.common.user.SessionAttributes.THEME_ATTRIBUTE;

public class OverrideThemeHookRender implements HookProcess {

	private final EventBus eb;
	private final String theme;

	public OverrideThemeHookRender(EventBus eb, String theme) {
		this.eb = eb;
		this.theme = theme;
	}

	@Override
	public void execute(final HttpServerRequest request, final Handler<Void> handler) {
		if (theme.isEmpty()) {
			if (CookieHelper.get("theme", request) != null) {
				CookieHelper.set("theme", "", 0l, request);
			}
		} else {
			CookieHelper.set("theme", theme, request);
		}
		// remove theme cache
		UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
			@Override
			public void handle(final UserInfos user) {
				if (user != null) {
					UserUtils.removeSessionAttribute(eb, user.getUserId(), THEME_ATTRIBUTE + Renders.getHost(request), null);
				}
				handler.handle(null);
			}
		});
	}

}
