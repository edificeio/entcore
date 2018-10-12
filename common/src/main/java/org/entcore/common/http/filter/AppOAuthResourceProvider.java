/*
 * Copyright © "Open Digital Education", 2017
 *
 * This program is published by "Open Digital Education".
 * You must indicate the name of the software and the company in any production /contribution
 * using the software and indicate on the home page of the software industry in question,
 * "powered by Open Digital Education" with a reference to the website: https://opendigitaleducation.com/.
 *
 * This program is free software, licensed under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation, version 3 of the License.
 *
 * You can redistribute this application and/or modify it since you respect the terms of the GNU Affero General Public License.
 * If you modify the source code and then use this modified source code in your creation, you must make available the source code of your modifications.
 *
 * You should have received a copy of the GNU Affero General Public License along with the software.
 * If not, please see : <http://www.gnu.org/licenses/>. Full compliance requires reading the terms of this license and following its directives.

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
