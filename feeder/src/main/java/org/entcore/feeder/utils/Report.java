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

package org.entcore.feeder.utils;

import fr.wseduc.webutils.I18n;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.logging.impl.LoggerFactory;

import java.util.Arrays;

public class Report {

	public static final Logger log = LoggerFactory.getLogger(Report.class);
	public final JsonObject result;
	private final I18n i18n = I18n.getInstance();
	public final String acceptLanguage;
	public enum State { NEW, UPDATED, DELETED }

	public Report(String acceptLanguage) {
		this.acceptLanguage = acceptLanguage;
		final JsonObject errors = new JsonObject();
		final JsonObject files = new JsonObject();
		JsonObject ignored = new JsonObject();
		result = new JsonObject().putObject("errors", errors).putObject("files", files).putObject("ignored", ignored);
	}

	public Report addError(String error) {
		addErrorWithParams(error);
		return this;
	}

	public void addError(String file, String error) {
		addErrorByFile(file, error);
	}

	public void addErrorWithParams(String key, String... errors) {
		addErrorByFile("global", key, errors);
	}

	public void addFailedUser(String filename, String key, JsonObject props, String... errors) {
		final String file = "error." + filename;
		JsonArray f = result.getObject("errors").getArray(file);
		if (f == null) {
			f = new JsonArray();
			result.getObject("errors").putArray(file, f);
		}
		String error = i18n.translate(key, I18n.DEFAULT_DOMAIN, acceptLanguage, errors);
		props.putString("error", error);
		f.addObject(props);
		log.error(error + " :\n" + Arrays.asList(props));
	}

	public void addErrorByFile(String filename, String key, String... errors) {
		final String file = "error." + filename;
		JsonArray f = result.getObject("errors").getArray(file);
		if (f == null) {
			f = new JsonArray();
			result.getObject("errors").putArray(file, f);
		}
		String error = i18n.translate(key, I18n.DEFAULT_DOMAIN, acceptLanguage, errors);
		f.addString(error);
		log.error(error);
	}

	public void addUser(String file, JsonObject props) {
		JsonArray f = result.getObject("files").getArray(file);
		if (f == null) {
			f = new JsonArray();
			result.getObject("files").putArray(file, f);
		}
		f.addObject(props);
	}

	public void addIgnored(String file, String reason, JsonObject object) {
		JsonArray f = result.getObject("ignored").getArray(file);
		if (f == null) {
			f = new JsonArray();
			result.getObject("ignored").putArray(file, f);
		}
		f.addObject(new JsonObject().putString("reason", reason).putObject("object", object));
	}

	public String translate(String key, String... params) {
		return i18n.translate(key, I18n.DEFAULT_DOMAIN, acceptLanguage, params);
	}

	public JsonObject getResult() {
		return result;
	}

	public void setUsersExternalId(JsonArray usersExternalIds) {
		result.putArray("usersExternalIds", usersExternalIds);
	}

	public JsonArray getUsersExternalId() {
		final JsonArray res = new JsonArray();
		for (String f : result.getObject("files").getFieldNames()) {
			JsonArray a = result.getObject("files").getArray(f);
			if (a != null) {
				for (Object o : a) {
					if (!(o instanceof JsonObject)) continue;
					final String externalId = ((JsonObject) o).getString("externalId");
					if (externalId != null) {
						res.addString(externalId);
					}
				}
			}
		}
		return res;
	}

	public boolean containsErrors() {
		return result.getObject("errors", new JsonObject()).size() > 0;
	}

}
