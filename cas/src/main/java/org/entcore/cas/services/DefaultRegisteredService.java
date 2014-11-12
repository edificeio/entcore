/*
 * Copyright © WebServices pour l'Éducation, 2014
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

package org.entcore.cas.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.logging.impl.LoggerFactory;

import fr.wseduc.cas.async.Handler;
import fr.wseduc.cas.entities.User;

public class DefaultRegisteredService implements RegisteredService {

	protected final List<Pattern> patterns = new ArrayList<Pattern>();
	protected EventBus eb;
	protected String principalAttributeName = "login";

	private static final Logger log = LoggerFactory.getLogger(DefaultRegisteredService.class);
	protected static final String CONF_PATTERNS = "patterns";
	protected static final String CONF_PRINCIPAL_ATTR_NAME = "principalAttributeName";

	@Override
	public void configure(final EventBus eb, final Map<String, Object> conf) {
		this.eb = eb;
		try {
			List<String> patterns = (List<String>) conf.get(CONF_PATTERNS);
			for (String pattern : patterns) {
				try {
					this.patterns.add(Pattern.compile(pattern));
				}
				catch (PatternSyntaxException pe) {
					log.error("Bad service configuration : failed to compile regex : " + pattern);
				}
			}

			this.principalAttributeName = String.valueOf(conf.get(CONF_PRINCIPAL_ATTR_NAME));
		}
		catch (Exception e) {
			log.error("Failed to parse configuration");
		}
	}

	@Override
	public boolean matches(final String serviceUri) {
		for (Pattern pattern : patterns) {
			Matcher matcher = pattern.matcher(serviceUri);
			if (matcher.matches()) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void getUser(final String userId, final Handler<User> userHandler) {
		JsonObject jo = new JsonObject();
		jo.putString("action", "getUser").putString("userId", userId);
		eb.send("directory", jo, new org.vertx.java.core.Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				JsonObject res = event.body().getObject("result");
				log.debug("res : " + res);
				if ("ok".equals(event.body().getString("status")) && res != null) {
					User user = new User();
					prepareUser(user, userId, res);
					userHandler.handle(user);
				} else {
					userHandler.handle(null);
				}
			}
		});
	}

	@Override
	public String formatService(String serviceUri) {
		return serviceUri;
	}

	protected void prepareUser(final User user, final String userId, final JsonObject data) {
		if (principalAttributeName != null) {
			user.setUser(data.getString(principalAttributeName));
			data.removeField(principalAttributeName);
		}
		else {
			user.setUser(userId);
		}
		data.removeField("password");

		Map<String, String> attributes = new HashMap<>();
		for (String attr : data.getFieldNames()) {
			attributes.put(attr, data.getValue(attr).toString());
		}
		user.setAttributes(attributes);
	}

	@Override
	public int hashCode() {
		return this.getClass().hashCode() + patterns.hashCode() + principalAttributeName.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return obj != null && obj instanceof DefaultRegisteredService &&
				this.patterns.equals(((DefaultRegisteredService) obj).patterns) &&
				this.principalAttributeName.equals(((DefaultRegisteredService) obj).principalAttributeName);
	}

}
