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
import fr.wseduc.webutils.request.CookieHelper;
import org.vertx.java.core.VoidHandler;
import org.vertx.java.core.http.HttpServerRequest;

public class OverrideThemeHookRender implements HookProcess {

	private final String theme;

	public OverrideThemeHookRender(String theme) {
		this.theme = theme;
	}

	@Override
	public void execute(HttpServerRequest request, VoidHandler handler) {
		if (theme.isEmpty()) {
			if (CookieHelper.get("theme", request) != null) {
				CookieHelper.set("theme", "", 0l, request);
			}
		} else {
			CookieHelper.set("theme", theme, request);
		}
		handler.handle(null);
	}

}
