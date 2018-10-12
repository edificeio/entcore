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

package org.entcore.directory.security;

import fr.wseduc.webutils.http.Binding;
import fr.wseduc.webutils.security.SecureHttpServerRequest;
import org.entcore.common.http.filter.CsrfFilter;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServerRequest;

import java.util.Set;

public class UserbookCsrfFilter extends CsrfFilter {

	public UserbookCsrfFilter(EventBus eb, Set<Binding> bindings) {
		super(eb, bindings);
	}

	@Override
	public void canAccess(final HttpServerRequest request, final Handler<Boolean> handler) {
		if (request instanceof SecureHttpServerRequest && "GET".equals(request.method()) &&
				(request.uri().contains("/userbook/api/edit") || request.uri().contains("/userbook/api/set"))) {
			compareToken(request, handler);
		} else {
			handler.handle(true);
		}
	}

}
