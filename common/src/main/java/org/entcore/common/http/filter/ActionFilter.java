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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import fr.wseduc.webutils.Server;
import fr.wseduc.webutils.request.filter.UserAuthFilter;
import fr.wseduc.webutils.security.SecureHttpServerRequest;
import org.entcore.common.http.response.DefaultPages;
import org.entcore.common.user.UserUtils;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;

import fr.wseduc.webutils.http.Binding;


public class ActionFilter extends AbstractActionFilter {

	private final Vertx vertx;
	private final EventBus eb;

	public ActionFilter(Set<Binding> bindings, Vertx vertx, ResourcesProvider provider) {
		super(bindings, provider);
		this.vertx = vertx;
		this.eb = Server.getEventBus(vertx);
	}

	public ActionFilter(Set<Binding> bindings, Vertx vertx) {
		this(bindings, vertx, null);
	}

	public ActionFilter(List<Set<Binding>> bindings, Vertx vertx, ResourcesProvider provider) {
		super(new HashSet<Binding>(), provider);
		if (bindings != null) {
			for (Set<Binding> bs: bindings) {
				this.bindings.addAll(bs);
			}
		}
		this.vertx = vertx;
		this.eb = Server.getEventBus(vertx);
	}

	public ActionFilter(List<Set<Binding>> bindings, Vertx vertx) {
		this(bindings, vertx, null);
	}

	@Override
	public void canAccess(final HttpServerRequest request, final Handler<Boolean> handler) {
		UserUtils.getSession(eb, request, new Handler<JsonObject>() {

			@Override
			public void handle(JsonObject session) {
				if (session != null) {
					userIsAuthorized(request, session, handler);
				} else if (request instanceof SecureHttpServerRequest &&
						((SecureHttpServerRequest) request).getAttribute("client_id") != null) {
					clientIsAuthorizedByScope((SecureHttpServerRequest) request, handler);
				} else {
					UserAuthFilter.redirectLogin(vertx, request);
				}
			}
		});
	}

	@Override
	public void deny(HttpServerRequest request) {
		request.response().setStatusCode(401).setStatusMessage("Unauthorized")
				.putHeader("content-type", "text/html").end(DefaultPages.UNAUTHORIZED.getPage());
	}

}
