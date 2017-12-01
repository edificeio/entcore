/*
 * Copyright © WebServices pour l'Éducation, 2016
 *
 * This file is part of ENT Core. ENT Core is a versatile ENT engine based on the JVM.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 * For the sake of explanation, any module that communicate over native
 * Web protocols, such as HTTP, with ENT Core is outside the scope of this
 * license and could be license under its own terms. This is merely considered
 * normal use of ENT Core, and does not fall under the heading of "covered work".
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
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
			this.config = config;
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
