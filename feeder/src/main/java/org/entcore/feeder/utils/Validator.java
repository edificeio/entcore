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

public class Validator {

	private static final Logger log = LoggerFactory.getLogger(Validator.class);
	//private static final Set<String> logins = Collections.synchronizedSet(new HashSet<String>());
	private static ConcurrentMap<Object, Object> logins;
	private static final String[] alphabet =
			{"a","b","c","d","e","f","g","h","j","k","m","n","p","r","s","t","v","w","x","y","z","3","4","5","6","7","8","9"};
	private static final Map<String, Pattern> patterns = new HashMap<>();
	static {
		patterns.put("email", Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,4}$"));
		patterns.put("zipCode", Pattern.compile("^[0-9]{5}$"));
		patterns.put("phone", Pattern.compile("^(00|\\+)?(?:[0-9] ?-?\\.?){6,14}[0-9]$")); // "^(0|\\+33)\\s*[0-9]([-. ]?[0-9]{2}){4}$"
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
	}

	public Validator(String resource) {
		this(JsonUtil.loadFromResource(resource));
	}

	public String validate(JsonObject object) {
		if (object == null) {
			return "Null object.";
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
						err = validString(attr, value, validator);
						break;
					case "array-string" :
						err = validStringArray(attr, value, validator);
						break;
					case "boolean" :
						err = validBoolean(attr, value);
						break;
					default:
						err = "Missing type validator: " + type;
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
		return required(object);
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
		return (object.size() > 0) ? null : "Empty object.";
	}

	private void checksum(JsonObject object, String values) throws NoSuchAlgorithmException {
		String checksum = Hash.sha1(values.getBytes());
		object.putString("checksum", checksum);
	}

	private String required(JsonObject object) {
		Map<String, Object> m = object.toMap();
		for (Object o : required) {
			if (!m.containsKey(o.toString())) {
				return "Missing attribute: " + o.toString();
			}
		}
		return null;
	}

	private void generate(JsonObject object) {
		for (String attr : generate.getFieldNames()) {
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
				while (logins.putIfAbsent(l, "") != null) {
					l = login + i++;
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
		if (!(value instanceof Boolean)) {
			return "Attribute " + attr + " is invalid.";
		}
		return null;
	}

	private String validStringArray(String attr, Object value, String validator) {
		if (validator == null) {
			return "Null array validator";
		}
		if (!(value instanceof JsonArray)) {
			return "Attribute " + attr + " is invalid. Expected type is JsonArray but type is "
					+ value.getClass().getSimpleName();
		}
		String err = null;
		switch (validator) {
			case "notEmpty" :
				if (!(((JsonArray) value).size() > 0)) {
					err = "Attribute " + attr + " is empty.";
				}
				break;
			default:
				err = "Missing array validator: " + validator;
		}
		return err;
	}

	private String validString(String attr, Object value, String validator) {
		Pattern p = patterns.get(validator);
		if (p == null) {
			return "Missing validator: " + validator;
		}
		if (value instanceof String && p.matcher((String) value).matches()) {
			return null;
		} else {
			return "Attribute " + attr + " contains an invalid value: " + value;
		}
	}

	public String getType(String attr) {
		JsonObject a = validate.getObject(attr);
		return a != null ? a.getString("type", "") : "";
	}

	public static void initLogin(Neo4j neo4j, Vertx vertx) {
		if (logins == null) {
			ConcurrentSharedMap<Object, Object> server = vertx.sharedData().getMap("server");
			Boolean cluster = (Boolean) server.get("cluster");
			if (Boolean.TRUE.equals(cluster)) {
				ClusterManager cm = ((VertxInternal) vertx).clusterManager();
				logins = (ConcurrentMap<Object, Object>) cm.getSyncMap("usedLogins");
			} else {
				logins = new ConcurrentHashMap<>();
			}
		} else {
			logins.clear();
		}
		String query = "MATCH (u:User) RETURN COLLECT(DISTINCT u.login) as logins";
		neo4j.execute(query, new JsonObject(), new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> message) {
				JsonArray r = message.body().getArray("result");
				if ("ok".equals(message.body().getString("status")) && r != null && r.size() == 1) {
					JsonArray l = ((JsonObject) r.get(0)).getArray("logins");
					if (l != null) {
						for (Object o : l) {
							if (!(o instanceof String)) continue;
							logins.putIfAbsent(o, "");
						}
					}
				}
			}
		});
	}

}
