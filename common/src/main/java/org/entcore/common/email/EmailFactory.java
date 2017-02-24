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
import org.vertx.java.core.Vertx;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.logging.impl.LoggerFactory;
import org.vertx.java.core.shareddata.ConcurrentSharedMap;
import org.vertx.java.platform.Container;

import java.net.URISyntaxException;

public class EmailFactory {

	private final Vertx vertx;
	private final Container container;
	private final JsonObject config;
	private final Logger log = LoggerFactory.getLogger(EmailFactory.class);

	public EmailFactory(Vertx vertx, Container container) {
		this(vertx, container, null);
	}

	public EmailFactory(Vertx vertx, Container container, JsonObject config) {
		this.vertx = vertx;
		this.container = container;
		if (config != null && config.getObject("emailConfig") != null) {
			this.config = config;
		} else {
			ConcurrentSharedMap<Object, Object> server = vertx.sharedData().getMap("server");
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
					sender = new SendInBlueSender(vertx, container, config);
				} catch (InvalidConfigurationException | URISyntaxException e) {
					log.error(e.getMessage(), e);
					vertx.stop();
				}
			} else if ("GoMail".equals(config.getString("type"))) {
				try {
					sender = new GoMailSender(vertx, container, config);
				} catch (InvalidConfigurationException | URISyntaxException e) {
					log.error(e.getMessage(), e);
					vertx.stop();
				}
			}
		} else {
			sender = new SmtpSender(vertx, container);
		}
		return sender;
	}

}
