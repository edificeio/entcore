/*
 * Copyright © "Open Digital Education", 2014
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

package org.entcore.common.http.i18n;

import java.util.Locale;

import fr.wseduc.webutils.I18n;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;

/**
 * Handle access to i18n of a module through vertx bus.
 * Required when a module has dependencies on another module's translations.
 * @deprecated when current (2024-09) i18n implementation will be refactored.
 */
public class I18nHandler implements Handler<Message<JsonObject>> {
	@Override
	public void handle(final Message<JsonObject> message) {
		final JsonObject translations = new JsonObject();
		I18nBusRequest request = I18nBusRequest.fromMessage(message.body());
		if(request!=null && request.isValid()) {
			final String domain = request.getDomain();
			final String theme = request.getTheme();
			final Locale locale = request.getLocale();
			final String[] args = request.getArgs();
			for( String key : request.getKeys()) {
				translations.put(key, I18n.getInstance().translate(
					key, domain, theme, locale, args));
			}
		}
		message.reply(translations);
	}
}
