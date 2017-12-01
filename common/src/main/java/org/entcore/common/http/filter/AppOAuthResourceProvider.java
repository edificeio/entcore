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

package org.entcore.common.http.filter;

import fr.wseduc.webutils.security.SecureHttpServerRequest;
import fr.wseduc.webutils.security.oauth.DefaultOAuthResourceProvider;
import io.vertx.core.eventbus.EventBus;

import java.util.regex.Pattern;

import static fr.wseduc.webutils.Utils.isEmpty;
import static fr.wseduc.webutils.Utils.isNotEmpty;

public class AppOAuthResourceProvider extends DefaultOAuthResourceProvider {

	private final Pattern prefixPattern;

	public AppOAuthResourceProvider(EventBus eb, String prefix) {
		super(eb);
		final String p = prefix.isEmpty() ? "portal" : prefix.substring(1);
		prefixPattern = Pattern.compile("(^|\\s)" + p + "(\\s|$)");
	}

	@Override
	protected boolean customValidation(SecureHttpServerRequest request) {
		final String scope = request.getAttribute("scope");
		return isNotEmpty(scope) &&
				(prefixPattern.matcher(scope).find() ||
						request.path().contains("/auth/internal/userinfo") ||
						(scope.contains("userinfo") && request.path().contains("/auth/oauth2/userinfo")) ||
						("OAuthSystemUser".equals(request.getAttribute("remote_user")) && isNotEmpty(request.getAttribute("client_id"))));
						//(scope.contains("openid") && request.path().contains())
	}

}
