/*
 * Copyright © "Open Digital Education", 2016
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

package org.entcore.common.email;

import fr.wseduc.webutils.email.EmailSender;
import fr.wseduc.webutils.email.SendInBlueSender;
import fr.wseduc.webutils.email.GoMailSender;
import fr.wseduc.webutils.exception.InvalidConfigurationException;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.shareddata.LocalMap;

import java.net.URISyntaxException;

public class EmailFactory {

	private final Vertx vertx;
	private final JsonObject config;
	private final Logger log = LoggerFactory.getLogger(EmailFactory.class);

	public EmailFactory(Vertx vertx) {
		this(vertx, null);
	}

	public EmailFactory(Vertx vertx, JsonObject config) {
		this.vertx = vertx;
		if (config != null && config.getJsonObject("emailConfig") != null) {
			this.config = config.getJsonObject("emailConfig");
		} else {
			LocalMap<Object, Object> server = vertx.sharedData().getLocalMap("server");
			String s = (String) server.get("emailConfig");
			if (s != null) {
				this.config = new JsonObject(s);
			} else {
				this.config = null;
			}
		}
	}

	public EmailSender getSender() {
		EmailSender sender = null;
		if (config != null){
			if ("SendInBlue".equals(config.getString("type"))) {
				try {
					sender = new SendInBlueSender(vertx, config);
				} catch (InvalidConfigurationException | URISyntaxException e) {
					log.error(e.getMessage(), e);
					vertx.close();
				}
			} else if ("GoMail".equals(config.getString("type"))) {
				try {
					sender = new GoMailSender(vertx, config);
				} catch (InvalidConfigurationException | URISyntaxException e) {
					log.error(e.getMessage(), e);
					vertx.close();
				}
			}
		} else {
			sender = new SmtpSender(vertx);
		}
		return sender;
	}

}
