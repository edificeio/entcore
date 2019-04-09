/* Copyright © "Open Digital Education", 2014
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

 *
 */

package org.entcore.feeder.utils;

import fr.wseduc.webutils.I18n;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.utils.MapFactory;
import org.joda.time.DateTime;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;

import static fr.wseduc.webutils.Utils.isNotEmpty;

public class Validator {

	private static final Logger log = LoggerFactory.getLogger(Validator.class);
	private static Map<Object, Object> logins;
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
		patterns.put("mobile", Pattern.compile("^(00|\\+)?(?:[0-9] ?-?\\.?){6,15}$"));
		patterns.put("notEmpty", Pattern.compile("^(?=\\s*\\S).*$"));
		patterns.put("maxLength", Pattern.compile("^.{1,1000}$"));
		patterns.put("birthDate", Pattern.compile("^((19|20)\\d\\d)-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"));
		patterns.put("BCrypt", Pattern.compile("^\\$2a\\$\\d{2}\\$([A-Za-z0-9+\\\\./]{22})"));
		patterns.put("uai", Pattern.compile("^[0-9]{7}[A-Z]$"));
		patterns.put("siren", Pattern.compile("^[0-9]{3} ?[0-9]{3} ?[0-9]{3}$"));
		patterns.put("siret", Pattern.compile("^[0-9]{3} ?[0-9]{3} ?[0-9]{3} ?[0-9]{5}$"));
		patterns.put("uri", Pattern.compile("^(([^:/?#]+):)?(//([^/?#]*))?([^?#]*)(\\?([^#]*))?(#(.*))?"));
		patterns.put("loginAlias", Pattern.compile("^([0-9a-z\\-\\.]+)$"));
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
		this.validate = schema.getJsonObject("validate");
		this.generate = schema.getJsonObject("generate");
		this.required = schema.getJsonArray("required");
		this.modifiable = schema.getJsonArray("modifiable");
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
		final Set<String> attributes = new HashSet<>(object.fieldNames());
		for (String attr : attributes) {
			JsonObject v = validate.getJsonObject(attr);
			if (v == null) {
				object.remove(attr);
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
					case "login-alias" :
						err = validLoginAlias(attr, value, validator, acceptLanguage);
						break;
					default:
						err = i18n.translate("missing.type.validator", I18n.DEFAULT_DOMAIN, acceptLanguage, type);
				}
				if (err != null) {
					if (required.contains(attr)) {
						return err;
					} else {
						log.info(err);
						object.remove(attr);
						continue;
					}
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
		final Set<String> attributes = new HashSet<>(object.fieldNames());
		JsonObject generatedAttributes = null;
		for (String attr : attributes) {
			JsonObject v = validate.getJsonObject(attr);
			if (v == null || !modifiable.contains(attr)) {
				object.remove(attr);
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
					case "login-alias" :
						if (value == null || (value instanceof String && ((String) value).isEmpty())) {
							object.putNull(attr);
							err = null;
						} else {
							err = validLoginAlias(attr, value, validator);
						}
						break;
					default:
						err = "Missing type validator: " + type;
				}
				if (err != null) {
					return err;
				}
				if (value != null && generate.containsKey(attr)) {
					JsonObject g = generate.getJsonObject(attr);
					if (g != null && "displayName".equals(g.getString("generator"))) {
						if (generatedAttributes == null) {
							generatedAttributes = new JsonObject();
						}
						generatedAttributes.put(attr + SEARCH_FIELD, removeAccents(value.toString()).toLowerCase());
					}
				}
				if (value != null && generate.containsKey(attr + SEARCH_FIELD)) {
					// FIX: enable update of sanitized and lower fields
					JsonObject g = generate.getJsonObject(attr + SEARCH_FIELD);
					if (g != null && ("lower".equals(g.getString("generator"))
							|| "sanitize".equals(g.getString("generator")))) {
						if (generatedAttributes == null) {
							generatedAttributes = new JsonObject();
						}
						generatedAttributes.put(attr + SEARCH_FIELD, removeAccents(value.toString()).toLowerCase());
					}
				}
			}
		}
		if (generatedAttributes != null) {
			object.mergeIn(generatedAttributes);
		}
		JsonObject g = generate.getJsonObject("modified");
		if (g != null) {
			nowDate("modified", object);
		}
		return (object.size() > 0) ? null : "Empty object.";
	}

	private void checksum(JsonObject object, String values) throws NoSuchAlgorithmException {
		String checksum = Hash.sha1(values.getBytes());
		object.put("checksum", checksum);
	}

	private String required(JsonObject object) {
		return required(object, "fr");
	}

	private String required(JsonObject object, String acceptLanguage) {
		Map<String, Object> m = object.getMap();
		for (Object o : required) {
			if (!m.containsKey(o.toString())) {
				return i18n.translate("missing.attribute", I18n.DEFAULT_DOMAIN, acceptLanguage, i18n.translate(o.toString(), I18n.DEFAULT_DOMAIN, acceptLanguage));
			}
		}
		return null;
	}

	private void generate(JsonObject object) {
		for (String attr : generate.fieldNames()) {
			if (object.containsKey(attr)) continue;
			JsonObject j = generate.getJsonObject(attr);
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
				case "sanitize" :
					sanitizeGenerator(attr, object, getParameter(object, j));
					break;
				case "lower" :
					lowerGenerator(attr, object, getParameter(object, j));
					break;
				default:
			}
		}
	}

	private String[] getParameters(JsonObject object, JsonObject j) {
		String[] v = null;
		JsonArray args = j.getJsonArray("args");
		if (args != null && args.size() > 0) {
			v = new String[args.size()];
			for (int i = 0; i < args.size(); i++) {
				v[i] = object.getString(args.getString(i));
			}
		}
		return v;
	}

	private String getParameter(JsonObject object, JsonObject j) {
		String v = null;
		JsonArray args = j.getJsonArray("args");
		if (args != null && args.size() == 1) {
			v = object.getString(args.getString(0));
		}
		return v;
	}

	private void nowDate(String attr, JsonObject object) {
		object.put(attr, DateTime.now().toString());
	}

	private void sanitizeGenerator(String attr, JsonObject object, String field) {
		if (isNotEmpty(field)) {
			object.put(attr, sanitize(field));
		}
	}

	public static String sanitize(String field) {
		return removeAccents(field)
				.replaceAll("\\s+", "")
				.replaceAll("\\-","")
				.replaceAll("'","")
				.toLowerCase();
	}

	private void activationCodeGenerator(String attr, JsonObject object, String password) {
		if (password == null || password.trim().isEmpty()) {
			StringBuilder sb = new StringBuilder();
			for(int i = 0; i < 8; i++) {
				sb.append(alphabet[Integer.parseInt(Long.toString(Math.abs(Math.round(Math.random() * 27D))))]);
			}
			object.put(attr, sb.toString());
		}
	}

	private void displayNameGenerator(String attr, JsonObject object, String... in) {
		if (in != null && in.length == 2) {
			String firstName = in[0];
			String lastName = in[1];
			if (firstName != null && lastName != null) {
				String displayName = lastName + " " + firstName;
				object.put(attr, displayName);
				object.put(attr + SEARCH_FIELD, sanitize(displayName));
			}
		}
	}

	private void loginGenerator(String attr, JsonObject object, String... in) {
		if (in != null && in.length == 2) {
			String firstName = in[0];
			String lastName = in[1];
			if (firstName != null && lastName != null) {
				String login = (removeAccents(firstName).replaceAll("\\s+", "").toLowerCase()
						+ "." + removeAccents(lastName).replaceAll("\\s+", "").toLowerCase())
						.replaceAll("'", "");
				int i = 2;
				String l = login + "";
				if (!notStoreLogins) {
					while (logins.putIfAbsent(l, "") != null) {
						l = login + i++;
					}
				}
				object.put(attr, l);
			}
		}
	}



	private void lowerGenerator(String attr, JsonObject object, String... in) {
		if (in != null && in.length == 1) {
			String value = in[0];
			if (value != null) {
				object.put(attr + SEARCH_FIELD, value.toLowerCase());
			}
		}
	}

	public static String removeAccents(String str) {
		return Normalizer.normalize(str, Normalizer.Form.NFD)
				.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
	}

	private void uuid4(String attr, JsonObject object) {
		object.put(attr, UUID.randomUUID().toString());
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
			if ("nop".equals(validator)) {
				return null;
			}
			if (validator != null && validator.startsWith("empty-")) {
				if (value instanceof String && ((String) value).isEmpty()) {
					return null;
				}
				p = patterns.get(validator.substring(6));
			}
			if (p == null) {
				return i18n.translate("missing.validator", I18n.DEFAULT_DOMAIN, acceptLanguage, validator);
			}
		}
		// hack #16883
		if ("mobile".equals(validator) && value instanceof String && ((String) value).isEmpty()) {
			return null;
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

	private String validLoginAlias(String attr, Object value, String validator) {
		return validLoginAlias(attr, value, validator, "fr");
	}
	
	private String validLoginAlias(String attr, Object value, String validator, String acceptLanguage) {
		Pattern p = patterns.get(validator);
		if (p == null) {
			return i18n.translate("missing.validator", I18n.DEFAULT_DOMAIN, acceptLanguage, validator);
		}
		if (!p.matcher((String) value).find()) {
			return i18n.translate("invalid.value", I18n.DEFAULT_DOMAIN, acceptLanguage, attr, (value != null ? value.toString() : "null"));
		}

		if (logins.putIfAbsent(value, "") != null) {
			return i18n.translate("invalid.duplicate", I18n.DEFAULT_DOMAIN, acceptLanguage, attr, (value != null ? value.toString() : "null"));
		}
		return null;
	}
	
	public String getType(String attr) {
		JsonObject a = validate.getJsonObject(attr);
		return a != null ? a.getString("type", "") : "";
	}

	public static void initLogin(Neo4j neo4j, Vertx vertx) {
		final long startInit = System.currentTimeMillis();
		if (logins == null) {
			logins = MapFactory.getSyncClusterMap("usedLogins", vertx);
			initLogins(neo4j, startInit, false);
		} else {
			initLogins(neo4j, startInit, true);
		}

		if (invalidEmails == null) {
			invalidEmails = MapFactory.getSyncClusterMap("invalidEmails", vertx);
		}
	}

	protected static void initLogins(Neo4j neo4j, final long startInit, final boolean remove) {
		String query = "MATCH (u:User) RETURN COLLECT(DISTINCT u.login) as logins, COLLECT(DISTINCT u.loginAlias) as loginAliases";
		neo4j.execute(query, new JsonObject(), new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> message) {
				JsonArray r = message.body().getJsonArray("result");
				if ("ok".equals(message.body().getString("status")) && r != null && r.size() == 1) {
					JsonArray l = (r.getJsonObject(0)).getJsonArray("logins");
					JsonArray aliases = (r.getJsonObject(0)).getJsonArray("loginAliases");

					for(Object alias : aliases) {
						l.add(alias);
					}

					if (l != null) {
						final Set<Object> tmp = new HashSet<>(l.getList());
						if (remove) {
							for (Object key : logins.keySet()) {
								if (!tmp.contains(key)) {
									logins.remove(key);
								} else {
									tmp.remove(key);
								}
							}
							putLogin(tmp);
						} else {
							putLogin(tmp);
						}
					}
				}
			}

			protected void putLogin(Set<Object> tmp) {
				for (Object o : tmp) {
					logins.putIfAbsent(o, "");
				}
				log.info("Init delay : " + (System.currentTimeMillis() - startInit));
			}
		});
	}

	public static void removeLogins(Set<String> oldLogins) {
		if (logins != null && oldLogins != null) {
			for (String l: oldLogins) {
				logins.remove(l);
			}
		}
	}

}
