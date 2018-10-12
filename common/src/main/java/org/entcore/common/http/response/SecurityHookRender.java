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

package org.entcore.common.http.response;

import fr.wseduc.webutils.http.HookProcess;
import fr.wseduc.webutils.request.CookieHelper;
import fr.wseduc.webutils.security.SecureHttpServerRequest;
import org.entcore.common.user.UserUtils;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.UUID;

import static fr.wseduc.webutils.Utils.isNotEmpty;

public class SecurityHookRender implements HookProcess {

	private static final Logger log = LoggerFactory.getLogger(SecurityHookRender.class);
	private final EventBus eb;
	private final boolean csrf;
	private final String contentSecurityPolicy;

	public SecurityHookRender(EventBus eb, boolean csrf, String contentSecurityPolicy) {
		this.eb = eb;
		this.csrf = csrf;
		this.contentSecurityPolicy = contentSecurityPolicy;
	}

	@Override
	public void execute(final HttpServerRequest request, final Handler<Void> handler) {
		if (request instanceof SecureHttpServerRequest) {
			final JsonObject session = ((SecureHttpServerRequest) request).getSession();
			if (session == null) {
				handler.handle(null);
				return;
			}
			contentSecurityPolicyHeader(request, session);
			csrfToken(request, handler, session);
		} else {
			contentSecurityPolicyHeader(request, null);
			handler.handle(null);
		}
	}

	private void contentSecurityPolicyHeader(HttpServerRequest request, JsonObject session) {
		if (contentSecurityPolicy == null) return;
		final String csp;
		if (session != null && session.getJsonObject("cache") != null &&
				session.getJsonObject("cache").getString("content-security-policy") != null) {
			csp = session.getJsonObject("cache").getString("content-security-policy");
		} else if (session != null && session.getJsonArray("apps") != null) {
			final StringBuilder sb = new StringBuilder(contentSecurityPolicy);
			if (!contentSecurityPolicy.contains("frame-src")) {
				if (!contentSecurityPolicy.trim().endsWith(";")) {
					sb.append("; ");
				}
				sb.append("frame-src 'self'");
			}
			for (Object o : session.getJsonArray("apps")) {
				if (!(o instanceof JsonObject)) continue;
				String address = ((JsonObject) o).getString("address");
				if (address != null && address.contains("adapter#")) {
					String [] s = address.split("adapter#");
					if (s.length == 2 && isNotEmpty(s[1])) {
						try {
							URI uri = new URI(s[1]);
							sb.append(" ").append(uri.getHost());
						} catch (URISyntaxException e) {
							log.warn("Invalid adapter URI : " + s[1], e);
						}
					}
				}
			}
			csp = sb.append(";").toString();
			UserUtils.addSessionAttribute(eb, session.getString("userId"), "content-security-policy", csp, null);
		} else {
			csp = contentSecurityPolicy;
		}
		request.response().putHeader("Content-Security-Policy", csp);
	}

	private void csrfToken(final HttpServerRequest request, final Handler<Void> handler, JsonObject session) {
		if (!csrf || request.path().contains("preview") ||
				"XMLHttpRequest".equals(request.headers().get("X-Requested-With"))) {
			handler.handle(null);
			return;
		}

		String token = null;
		final String xsrfToken;
		final String userId = session.getString("userId");
		if (session.getJsonObject("cache") != null) {
			token = session.getJsonObject("cache").getString("xsrf-token");
			if (token == null) { // TODO remove when support session cache persistence
				String t = CookieHelper.get("XSRF-TOKEN", request);
				xsrfToken = ((t != null) ? t : UUID.randomUUID().toString());
			} else {
				xsrfToken = token;
			}
		} else {
			xsrfToken = UUID.randomUUID().toString();
		}

		if (token == null) {
			UserUtils.addSessionAttribute(eb, userId, "xsrf-token", xsrfToken, new Handler<Boolean>() {
				@Override
				public void handle(Boolean s) {
					if (Boolean.TRUE.equals(s)) {
						CookieHelper.set("XSRF-TOKEN", xsrfToken, request);
					}
					handler.handle(null);
				}
			});
		} else {
			handler.handle(null);
		}
	}

}
