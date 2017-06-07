/* Copyright © WebServices pour l'Éducation, 2014
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
 *
 */

package org.entcore.feeder.utils;

import fr.wseduc.webutils.I18n;
import org.entcore.common.neo4j.Neo4j;
import org.joda.time.DateTime;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.impl.VertxInternal;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.logging.impl.LoggerFactory;
import org.vertx.java.core.shareddata.ConcurrentSharedMap;
import org.vertx.java.core.spi.cluster.ClusterManager;

import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Pattern;

import static fr.wseduc.webutils.Utils.isNotEmpty;

public class Validator {

	private static final Logger log = LoggerFactory.getLogger(Validator.class);
	private static ConcurrentMap<Object, Object> logins;
	private static Map<Object, Object> invalidEmails;
	private final I18n i18n = I18n.getInstance();
	private final boolean notStoreLogins;
	private static final String[] alphabet =
			{"a","b","c","d","e","f","g","h","j","k","m","n","p","r","s","t","v","w","x","y","z","3","4","5","6","7","8","9"};
	private static final Map<String, Pattern> patterns = new HashMap<>();
	static {
		patterns.put("email", Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,4}$"));
		patterns.put("zipCode", Pattern.compile("^[A-Za-z0-9\\-\\s]{4,9}$")); // ^[0-9]{5}$
		patterns.put("phone", Pattern.compile("^(00|\\+)?(?:[0-9] ?-?\\.?){6,15}$")); // "^(0|\\+33)\\s*[0-9]([-. ]?[0-9]{2}){4}$"
		patterns.put("mobile", Pattern.compile("^(00|\\+)?(?:[0-9] ?-?\\.?){6,14}[0-9]$"));
		patterns.put("notEmpty", Pattern.compile("^(?=\\s*\\S).*$"));
		patterns.put("birthDate", Pattern.compile("^((19|20)\\d\\d)-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"));
		patterns.put("BCrypt", Pattern.compile("^\\$2a\\$\\d{2}\\$([A-Za-z0-9+\\\\./]{22})"));
		patterns.put("uai", Pattern.compile("^[0-9]{7}[A-Z]$"));
		patterns.put("siren", Pattern.compile("^[0-9]{3} ?[0-9]{3} ?[0-9]{3}$"));
		patterns.put("siret", Pattern.compile("^[0-9]{3} ?[0-9]{3} ?[0-9]{3} ?[0-9]{5}$"));
		patterns.put("uri", Pattern.compile("^(([^:/?#]+):)?(//([^/?#]*))?([^?#]*)(\\?([^#]*))?(#(.*))?"));
	}

	public static final String SEARCH_FIELD = "SearchField";

	private final JsonObject validate;
	private final JsonObject generate;
	private final JsonArray required;
	private final JsonArray modifiable;

	public Validator(JsonObject schema) {
		this(schema, false);
	}

	protected Validator(JsonObject schema, boolean notStoreLogins) {
		if (schema == null || schema.size() == 0) {
			throw new IllegalArgumentException("Missing schema.");
		}
		this.validate = schema.getObject("validate");
		this.generate = schema.getObject("generate");
		this.required = schema.getArray("required");
		this.modifiable = schema.getArray("modifiable");
		if (validate == null || generate == null || required == null || modifiable == null) {
			throw new IllegalArgumentException("Invalid schema.");
		}
		this.notStoreLogins = notStoreLogins;
	}

	public Validator(String resource) {
		this(resource, false);
	}

	public Validator(String resource, boolean notStoreLogins) {
		this(JsonUtil.loadFromResource(resource), notStoreLogins);
	}

	public String validate(JsonObject object) {
		return validate(object, "fr");
	}

	public String validate(JsonObject object, String acceptLanguage) {
		if (object == null) {
			return i18n.translate("null.object", I18n.DEFAULT_DOMAIN, acceptLanguage);
		}
		final StringBuilder calcChecksum = new StringBuilder();
		final Set<String> attributes = new HashSet<>(object.getFieldNames());
		for (String attr : attributes) {
			JsonObject v = validate.getObject(attr);
			if (v == null) {
				object.removeField(attr);
			} else {
				Object value = object.getValue(attr);
				String validator = v.getString("validator");
				String type = v.getString("type", "");
				String err;
				switch (type) {
					case "string" :
						err = validString(attr, value, validator, acceptLanguage);
						break;
					case "array-string" :
						err = validStringArray(attr, value, validator, acceptLanguage);
						break;
					case "boolean" :
						err = validBoolean(attr, value, acceptLanguage);
						break;
					default:
						err = i18n.translate("missing.type.validator", I18n.DEFAULT_DOMAIN, acceptLanguage, type);
				}
				if (err != null) {
					log.info(err);
					object.removeField(attr);
					continue;
				}
				if (value instanceof JsonArray) {
					calcChecksum.append(((JsonArray) value).encode());
				} else if (value instanceof JsonObject) {
					calcChecksum.append(((JsonObject) value).encode());
				} else {
					calcChecksum.append(value.toString());
				}
			}
		}
		try {
			checksum(object, calcChecksum.toString());
		} catch (NoSuchAlgorithmException e) {
			return e.getMessage();
		}
		generate(object);
		return required(object, acceptLanguage);
	}

	public String modifiableValidate(JsonObject object) {
		if (object == null) {
			return "Null object.";
		}
		final Set<String> attributes = new HashSet<>(object.getFieldNames());
		JsonObject generatedAttributes = null;
		for (String attr : attributes) {
			JsonObject v = validate.getObject(attr);
			if (v == null || !modifiable.contains(attr)) {
				object.removeField(attr);
			} else {
				Object value = object.getValue(attr);
				String validator = v.getString("validator");
				String type = v.getString("type", "");
				String err;
				switch (type) {
					case "string" :
						if (!required.contains(attr) &&
								(value == null || (value instanceof String && ((String) value).isEmpty()))) {
							err = null;
						} else {
							err = validString(attr, value, validator);
						}
						break;
					case "array-string" :
						if (!required.contains(attr) &&
								(value == null || (value instanceof JsonArray && ((JsonArray) value).size() == 0))) {
							err = null;
						} else {
							err = validStringArray(attr, value, validator);
						}
						break;
					case "boolean" :
						err = validBoolean(attr, value);
						break;
					default:
						err = "Missing type validator: " + type;
				}
				if (err != null) {
					return err;
				}
				if (value != null && generate.containsField(attr)) {
					JsonObject g = generate.getObject(attr);
					if (g != null && "displayName".equals(g.getString("generator"))) {
						if (generatedAttributes == null) {
							generatedAttributes = new JsonObject();
						}
						generatedAttributes.putString(attr + SEARCH_FIELD, removeAccents(value.toString()).toLowerCase());
					}
				}
			}
		}
		if (generatedAttributes != null) {
			object.mergeIn(generatedAttributes);
		}
		JsonObject g = generate.getObject("modified");
		if (g != null) {
			nowDate("modified", object);
		}
		return (object.size() > 0) ? null : "Empty object.";
	}

	private void checksum(JsonObject object, String values) throws NoSuchAlgorithmException {
		String checksum = Hash.sha1(values.getBytes());
		object.putString("checksum", checksum);
	}

	private String required(JsonObject object) {
		return required(object, "fr");
	}

	private String required(JsonObject object, String acceptLanguage) {
		Map<String, Object> m = object.toMap();
		for (Object o : required) {
			if (!m.containsKey(o.toString())) {
				return i18n.translate("missing.attribute", I18n.DEFAULT_DOMAIN, acceptLanguage, i18n.translate(o.toString(), I18n.DEFAULT_DOMAIN, acceptLanguage));
			}
		}
		return null;
	}

	private void generate(JsonObject object) {
		for (String attr : generate.getFieldNames()) {
			if (object.containsField(attr)) continue;
			JsonObject j = generate.getObject(attr);
			switch (j.getString("generator", "")) {
				case "uuid4" :
					uuid4(attr, object);
					break;
				case "login" :
					loginGenerator(attr, object, getParameters(object, j));
					break;
				case "displayName" :
					displayNameGenerator(attr, object, getParameters(object, j));
					break;
				case "activationCode" :
					activationCodeGenerator(attr, object, getParameter(object, j));
					break;
				case "nowDate" :
					nowDate(attr, object);
					break;
				default:
			}
		}
	}

	private String[] getParameters(JsonObject object, JsonObject j) {
		String[] v = null;
		JsonArray args = j.getArray("args");
		if (args != null && args.size() > 0) {
			v = new String[args.size()];
			for (int i = 0; i < args.size(); i++) {
				v[i] = object.getString((String) args.get(i));
			}
		}
		return v;
	}

	private String getParameter(JsonObject object, JsonObject j) {
		String v = null;
		JsonArray args = j.getArray("args");
		if (args != null && args.size() == 1) {
			v = object.getString((String) args.get(0));
		}
		return v;
	}

	private void nowDate(String attr, JsonObject object) {
		object.putString(attr, DateTime.now().toString());
	}

	private void activationCodeGenerator(String attr, JsonObject object, String password) {
		if (password == null || password.trim().isEmpty()) {
			StringBuilder sb = new StringBuilder();
			for(int i = 0; i < 8; i++) {
				sb.append(alphabet[Integer.parseInt(Long.toString(Math.abs(Math.round(Math.random() * 27D))))]);
			}
			object.putString(attr, sb.toString());
		}
	}

	private void displayNameGenerator(String attr, JsonObject object, String... in) {
		if (in != null && in.length == 2) {
			String firstName = in[0];
			String lastName = in[1];
			if (firstName != null && lastName != null) {
				String displayName = firstName + " " + lastName;
				object.putString(attr, displayName);
				object.putString(attr + SEARCH_FIELD, removeAccents(displayName).toLowerCase());
			}
		}
	}

	private void loginGenerator(String attr, JsonObject object, String... in) {
		if (in != null && in.length == 2) {
			String firstName = in[0];
			String lastName = in[1];
			if (firstName != null && lastName != null) {
				String login = (removeAccents(firstName).replaceAll("\\s+", "-").toLowerCase()
						+ "." + removeAccents(lastName).replaceAll("\\s+", "-").toLowerCase())
						.replaceAll("'", "");
				int i = 2;
				String l = login + "";
				if (!notStoreLogins) {
					while (logins.putIfAbsent(l, "") != null) {
						l = login + i++;
					}
				}
				object.putString(attr, l);
			}
		}
	}

	public static String removeAccents(String str) {
		return Normalizer.normalize(str, Normalizer.Form.NFD)
				.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
	}

	private void uuid4(String attr, JsonObject object) {
		object.putString(attr, UUID.randomUUID().toString());
	}

	private String validBoolean(String attr, Object value) {
		return validBoolean(attr, value, "fr");
	}

	private String validBoolean(String attr, Object value, String acceptLanguage) {
		if (!(value instanceof Boolean)) {
			return i18n.translate("invalid.attribute", I18n.DEFAULT_DOMAIN, acceptLanguage, attr);
		}
		return null;
	}

	private String validStringArray(String attr, Object value, String validator) {
		return validStringArray(attr, value, validator, "fr");
	}

	private String validStringArray(String attr, Object value, String validator, String acceptLanguage) {
		if (validator == null) {
			return i18n.translate("null.array.validator", I18n.DEFAULT_DOMAIN, acceptLanguage);
		}
		if (!(value instanceof JsonArray)) {
			return i18n.translate("invalid.array.type", I18n.DEFAULT_DOMAIN, acceptLanguage, attr, value.getClass().getSimpleName());
		}
		String err = null;
		switch (validator) {
			case "notEmpty" :
				if (!(((JsonArray) value).size() > 0)) {
					err = i18n.translate("empty.attribute", I18n.DEFAULT_DOMAIN, acceptLanguage, attr);
				}
				break;
			case "nop":
				break;
			default:
				err =  i18n.translate("missing.validator", I18n.DEFAULT_DOMAIN, acceptLanguage, validator);
		}
		return err;
	}

	private String validString(String attr, Object value, String validator) {
		return validString(attr, value, validator, "fr");
	}

	private String validString(String attr, Object value, String validator, String acceptLanguage) {
		Pattern p = patterns.get(validator);
		if (p == null) {
			return i18n.translate("missing.validator", I18n.DEFAULT_DOMAIN, acceptLanguage, validator);
		}
		if (value instanceof String && p.matcher((String) value).matches()) {
			if ("email".equals(validator) && !"emailAcademy".equals(attr) &&
					invalidEmails != null && invalidEmails.containsKey(value)) {
				return i18n.translate("invalid.bounce.email", I18n.DEFAULT_DOMAIN, acceptLanguage, attr, (String) value);
			}
			return null;
		} else {
			return i18n.translate("invalid.value", I18n.DEFAULT_DOMAIN, acceptLanguage, attr, (value != null ? value.toString() : "null"));
		}
	}

	public String getType(String attr) {
		JsonObject a = validate.getObject(attr);
		return a != null ? a.getString("type", "") : "";
	}

	public static void initLogin(Neo4j neo4j, Vertx vertx) {
		final long startInit = System.currentTimeMillis();
		final boolean remove;
		if (logins == null) {
			ConcurrentSharedMap<Object, Object> server = vertx.sharedData().getMap("server");
			Boolean cluster = (Boolean) server.get("cluster");
			if (Boolean.TRUE.equals(cluster)) {
				ClusterManager cm = ((VertxInternal) vertx).clusterManager();
				logins = (ConcurrentMap<Object, Object>) cm.getSyncMap("usedLogins");
			} else {
				logins = new ConcurrentHashMap<>();
			}
			remove = false;
		} else {
			remove = true;
		}
		String query = "MATCH (u:User) RETURN COLLECT(DISTINCT u.login) as logins";
		neo4j.execute(query, new JsonObject(), new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> message) {
				JsonArray r = message.body().getArray("result");
				if ("ok".equals(message.body().getString("status")) && r != null && r.size() == 1) {
					JsonArray l = ((JsonObject) r.get(0)).getArray("logins");
					if (l != null) {
						if (remove) {
							Set<Object> tmp = new HashSet<>(l.toList());
							for (Object key : logins.keySet()) {
								if (!tmp.contains(key)) {
									logins.remove(key);
								}
							}
						} else {
							for (Object o : l) {
								if (!(o instanceof String)) continue;
								logins.putIfAbsent(o, "");
							}
						}
						log.info("Init delay : " + (System.currentTimeMillis() - startInit));
					}
				}
			}
		});
		if (invalidEmails == null) {
			ConcurrentSharedMap<Object, Object> server = vertx.sharedData().getMap("server");
			Boolean cluster = (Boolean) server.get("cluster");
			if (Boolean.TRUE.equals(cluster)) {
				ClusterManager cm = ((VertxInternal) vertx).clusterManager();
				invalidEmails = cm.getSyncMap("invalidEmails");
			} else {
				invalidEmails = vertx.sharedData().getMap("invalidEmails");
			}
		}
	}

}
