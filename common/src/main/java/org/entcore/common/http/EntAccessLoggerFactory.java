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

package org.entcore.common.http;

import fr.wseduc.webutils.request.IAccessLogger;
import io.vertx.core.eventbus.EventBus;

public class EntAccessLoggerFactory {

	/**
	 * Factory method to create the appropriate EntAccessLogger.
	 *
	 * @param  format If is set to "json", returns EntAccessLoggerJson (JSON format). Otherwise, returns EntAccessLogger (plain text format).
	 * @param eb EventBus instance
	 * @return A logger
	 */
	public static IEntAccessLogger create(EventBus eb) {
		final String format = System.getenv(IAccessLogger.LOG_FORMAT_CONF_KEY);
		if ("json".equalsIgnoreCase(format)) {
			return new EntAccessLoggerJson(eb);
		} else {
			return new EntAccessLogger(eb);
		}
	}

}
