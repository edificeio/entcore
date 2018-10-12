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

import fr.wseduc.webutils.http.Binding;
import fr.wseduc.webutils.http.Renders;
import fr.wseduc.webutils.request.CookieHelper;
import fr.wseduc.webutils.request.filter.Filter;
import fr.wseduc.webutils.security.SecureHttpServerRequest;
import org.entcore.common.user.UserUtils;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;

import static fr.wseduc.webutils.Utils.isEmpty;

public class CsrfFilter implements Filter {

	private static final Logger log = LoggerFactory.getLogger(CsrfFilter.class);
	private static final List<String> securedMethods = Arrays.asList("POST", "PUT", "DELETE");
	private final EventBus eb;
	private final Set<Binding> bindings;
	private Set<Binding> ignoreBinding;

	public CsrfFilter(EventBus eb, Set<Binding> bindings) {
		this.eb = eb;
		this.bindings = bindings;
	}

	private void loadIgnoredMethods() {
		ignoreBinding = new HashSet<>();
		InputStream is = CsrfFilter.class.getClassLoader().getResourceAsStream(IgnoreCsrf.class.getSimpleName() + ".json");
		if (is != null) {
			BufferedReader r = null;
			try {
				r = new BufferedReader(new InputStreamReader(is, "UTF-8"));
				String line;
				while((line = r.readLine()) != null) {
					final JsonObject ignore = new JsonObject(line);
					if (ignore.getBoolean("ignore", false)) {
						for (Binding binding: bindings) {
							if (binding != null && ignore.getString("method", "").equals(binding.getServiceMethod())) {
								ignoreBinding.add(binding);
								break;
							}
						}
					}
				}
			} catch (IOException | DecodeException e) {
				log.error("Unable to load ignoreCsrf", e);
			} finally {
				if (r != null) {
					try {
						r.close();
					} catch (IOException e) {
						log.error("Close inputstream error", e);
					}
				}
			}
		}
	}

	@Override
	public void canAccess(final HttpServerRequest request, final Handler<Boolean> handler) {
		if (ignoreBinding == null) {
			loadIgnoredMethods();
		}
		if (request instanceof SecureHttpServerRequest && securedMethods.contains(request.method()) &&
				isEmpty(((SecureHttpServerRequest) request).getAttribute("client_id")) && !ignore(request)) {
			compareToken(request, handler);
		} else {
			handler.handle(true);
		}
	}

	protected void compareToken(final HttpServerRequest request, final Handler<Boolean> handler) {
		UserUtils.getSession(eb, request, new Handler<JsonObject>() {
			@Override
			public void handle(JsonObject session) {
				String XSRFToken = null;
				if (session != null && session.getJsonObject("cache") != null) {
					XSRFToken = session.getJsonObject("cache").getString("xsrf-token");
					if (XSRFToken == null) { // TODO remove when support session cache persistence
						XSRFToken = CookieHelper.get("XSRF-TOKEN", request);
					}
				}
				handler.handle(XSRFToken != null && !XSRFToken.isEmpty() &&
						XSRFToken.equals(request.headers().get("X-XSRF-TOKEN")));
			}
		});
	}

	@Override
	public void deny(HttpServerRequest request) {
		Renders.forbidden(request, "invalid.xsrf.token");
	}

	protected boolean ignore(HttpServerRequest request) {
		if (!ignoreBinding.isEmpty()) {
			for (Binding binding : ignoreBinding) {
				if (!request.method().equals(binding.getMethod().name())) {
					continue;
				}
				Matcher m = binding.getUriPattern().matcher(request.path());
				if (m.matches()) {
					return true;
				}
			}
		}
		return false;
	}

}
