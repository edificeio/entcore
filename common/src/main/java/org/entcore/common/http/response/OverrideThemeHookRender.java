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
import fr.wseduc.webutils.http.Renders;
import fr.wseduc.webutils.request.CookieHelper;
import io.vertx.core.json.JsonObject;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServerRequest;

import static fr.wseduc.webutils.Utils.isNotEmpty;
import static org.entcore.common.user.SessionAttributes.THEME_ATTRIBUTE;

public class OverrideThemeHookRender implements HookProcess {

	private final EventBus eb;
	private final JsonObject themeByDomain;

	public OverrideThemeHookRender(EventBus eb, JsonObject themeByDomain) {
		this.eb = eb;
		this.themeByDomain = themeByDomain;
	}

	@Override
	public void execute(final HttpServerRequest request, final Handler<Void> handler) {
		if (themeByDomain.isEmpty()) {
			if (CookieHelper.get("theme", request) != null) {
				CookieHelper.set("theme", "", 0l, request);
			}
		} else {
			final String theme = themeByDomain.getString(Renders.getHost(request));
			if (isNotEmpty(theme)) {
				CookieHelper.set("theme", theme, request);
			} else {
				if (CookieHelper.get("theme", request) != null) {
					CookieHelper.set("theme", "", 0l, request);
				}
			}
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
