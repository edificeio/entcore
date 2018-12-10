/*
 * Copyright © "Open Digital Education", 2015
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

package org.entcore.common.utils;

import io.vertx.core.json.JsonObject;

public class Config {
	private static final long NINETY_DAYS = 90 * 24 * 3600 * 1000L;
	public static final long defaultDeleteUserDelay = NINETY_DAYS;
	public static final long defaultPreDeleteUserDelay = NINETY_DAYS;

	private JsonObject config;

	private Config(){}

	public JsonObject getConfig() {
		return config;
	}

	public void setConfig(JsonObject config) {
		this.config = config;
	}

	private static class ConfigHolder {
		private static final Config instance = new Config();
	}

	public static Config getInstance() {
		return ConfigHolder.instance;
	}

	public static JsonObject getConf() {
		return getInstance().getConfig();
	}

}
