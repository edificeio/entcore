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

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import fr.wseduc.webutils.I18n;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import fr.wseduc.cas.async.Handler;
import fr.wseduc.cas.entities.ServiceTicket;
import fr.wseduc.cas.entities.User;

import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;

public class DefaultRegisteredService implements RegisteredService {

	protected final I18n i18n = I18n.getInstance();
	protected final Set<Pattern> patterns = new HashSet<>();
	private final Set<Pattern> confPatterns = new HashSet<>();
	protected EventBus eb;
	protected String principalAttributeName = "login";
	protected String directoryAction = "getUser";

	protected static final Logger log = LoggerFactory.getLogger(DefaultRegisteredService.class);
	protected static final String CONF_PATTERNS = "patterns";
	protected static final String CONF_PRINCIPAL_ATTR_NAME = "principalAttributeName";

	@Override
	public void configure(final EventBus eb, final Map<String, Object> conf) {
		this.eb = eb;
		try {
			List<String> patterns = ((JsonArray) conf.get(CONF_PATTERNS)).getList();
			if (patterns != null && !patterns.isEmpty()) {
				addConfPatterns(patterns.toArray(new String[patterns.size()]));
			}
			this.principalAttributeName = String.valueOf(conf.get(CONF_PRINCIPAL_ATTR_NAME));
		}
		catch (Exception e) {
			log.error("Failed to parse configuration", e);
		}
	}

	@Override
	public boolean matches(final String serviceUri) {
		for (Pattern pattern : patterns) {
			Matcher matcher = pattern.matcher(serviceUri);
			if (matcher.matches()) {
				if (log.isDebugEnabled()) log.debug("service URI + |" + serviceUri + "| matches with pattern : " + pattern.pattern());
				return true;
			}
		}
		return false;
	}

	@Override
	public void getUser(final String userId, final String service, final Handler<User> userHandler) {
		JsonObject jo = new JsonObject();
		jo.put("action", directoryAction).put("userId", userId);
		eb.send("directory", jo, handlerToAsyncHandler(new io.vertx.core.Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				JsonObject res = event.body().getJsonObject("result");
				log.debug("res : " + res);
				if ("ok".equals(event.body().getString("status")) && res != null) {
					User user = new User();
					prepareUser(user, userId, service, res);
					userHandler.handle(user);
				} else {
					userHandler.handle(null);
				}
			}
		}));
	}

	@Override
	public String formatService(String serviceUri, ServiceTicket st) {
		return serviceUri;
	}

	private void addConfPatterns(String... patterns) {
		for (String pattern : patterns) {
			try {
				this.confPatterns.add(Pattern.compile(pattern, Pattern.CASE_INSENSITIVE));
				this.patterns.add(Pattern.compile(pattern, Pattern.CASE_INSENSITIVE));
			}
			catch (PatternSyntaxException pe) {
				log.error("Bad service configuration : failed to compile regex : " + pattern);
			}
		}
	}

	@Override
	public void addPatterns(String... patterns) {
		for (String pattern : patterns) {
			try {
				this.patterns.add(Pattern.compile(pattern, Pattern.CASE_INSENSITIVE));
			}
			catch (PatternSyntaxException pe) {
				log.error("Bad service configuration : failed to compile regex : " + pattern);
			}
		}
	}

	@Override
	public void cleanPatterns() {
		this.patterns.clear();
		this.patterns.addAll(this.confPatterns);
	}

	@Override
	public JsonObject getInfos(String acceptLanguage) {
		String baseKey = getId();
		return new JsonObject()
				.put("id", baseKey)
				.put("name", i18n.translate(baseKey + ".name", I18n.DEFAULT_DOMAIN, acceptLanguage))
				.put("description", i18n.translate(baseKey + ".description", I18n.DEFAULT_DOMAIN, acceptLanguage));
	}

	@Override
	public String getId() {
		return this.getClass().getSimpleName();
	}

	protected void prepareUser(final User user, final String userId, String service, final JsonObject data) {
		if (principalAttributeName != null) {
			user.setUser(data.getString(principalAttributeName));
			data.remove(principalAttributeName);
		}
		else {
			user.setUser(userId);
		}
		data.remove("password");

		Map<String, String> attributes = new HashMap<>();
		for (String attr : data.fieldNames()) {
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
