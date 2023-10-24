/* Copyright © "Open Digital Education", 2014
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

 *
 */

package org.entcore.common.http.filter;

import fr.wseduc.webutils.http.Binding;
import fr.wseduc.webutils.request.filter.UserAuthFilter;
import fr.wseduc.webutils.security.SecureHttpServerRequest;
import io.vertx.core.http.*;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.http.response.DefaultPages;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;

import java.util.Set;

import static fr.wseduc.webutils.Utils.isNotEmpty;
import static fr.wseduc.webutils.request.filter.AbstractQueryParamTokenFilter.QUERYPARAM_TOKEN;



/**
 * This Filter only use HTTP to retrieve user's info.
 * It targets internal module and uses cookie to authorize user request
 */

public class HttpActionFilter extends AbstractActionFilter {

	private final HttpClient httpClient;
	private final Vertx vertx;
	private static final Logger log = LoggerFactory.getLogger(HttpActionFilter.class);
	public HttpActionFilter(Set<Binding> bindings, JsonObject conf, Vertx vertx,
			ResourcesProvider provider) {
		super(bindings, provider);
		this.vertx = vertx;
		HttpClientOptions options = new HttpClientOptions()
				.setDefaultHost("localhost")
				.setDefaultPort(conf.getInteger("entcore.port", 8009))
				.setMaxPoolSize(16);
		this.httpClient = vertx.createHttpClient(options);
	}

	public HttpActionFilter(Set<Binding> bindings, JsonObject conf, Vertx vertx) {
		this(bindings, conf, vertx, null);
	}

	@Override
	public void canAccess(final HttpServerRequest request, final Handler<Boolean> handler) {
		request.pause();
		if (!(request instanceof SecureHttpServerRequest)) {
			handler.handle(false);
			return;
		}

		final SecureHttpServerRequest sreq = (SecureHttpServerRequest) request;
		if (isNotEmpty(sreq.getParam(QUERYPARAM_TOKEN))) {
			String tokenParam = QUERYPARAM_TOKEN+"="+sreq.getParam(QUERYPARAM_TOKEN).trim();
			httpClient.request(HttpMethod.GET, "/auth/oauth2/userinfo?"+tokenParam)
					.map(req -> req.putHeader("Accept", "application/json; version=2.1"))
					.flatMap(HttpClientRequest::send)
					.onSuccess(getResponseHandler(request, handler));
		} else if (isNotEmpty(sreq.getAttribute("remote_user"))) {
			httpClient.request(HttpMethod.GET, "/auth/internal/userinfo")
					.map(req -> req.putHeader("Authorization", request.headers().get("Authorization"))
							.putHeader("Accept", "application/json; version=2.1"))
					.flatMap(HttpClientRequest::send)
					.onSuccess(getResponseHandler(request, handler));
		} else if (isNotEmpty(sreq.getAttribute("client_id"))) {
			clientIsAuthorizedByScope(sreq, handler);
		} else {
			httpClient.request(HttpMethod.GET, "/auth/oauth2/userinfo")
					.map(req -> req.putHeader("Cookie", request.headers().get("Cookie"))
							.putHeader("Accept", "application/json; version=2.1"))
					.flatMap(HttpClientRequest::send)
					.onSuccess(getResponseHandler(request, handler));
		}
	}

	private Handler<HttpClientResponse> getResponseHandler(final HttpServerRequest request, final Handler<Boolean> handler) {
		return new Handler<HttpClientResponse>() {
			@Override
			public void handle(final HttpClientResponse response) {
				response.bodyHandler(new Handler<Buffer>() {
					@Override
					public void handle(Buffer body) {
						request.resume();
						if (response.statusCode() == 200) {
							JsonObject session = new JsonObject(body.toString());
							((SecureHttpServerRequest) request).setSession(session);
							userIsAuthorized(request, session, handler);
						} else {
							log.warn("HTTP action failed:"+body.toString()+ "/"+ response.headers()+"/"+response.statusCode());
							UserAuthFilter.redirectLogin(vertx, request);
						}
					}
				});
			}
		};
	}

	@Override
	public void deny(HttpServerRequest request) {
		request.response().setStatusCode(401).setStatusMessage("Unauthorized")
				.putHeader("content-type", "text/html").end(DefaultPages.UNAUTHORIZED.getPage());
	}

}
