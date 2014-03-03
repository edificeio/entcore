package org.entcore.feeder.utils;

import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;

public class Validator {

	private static final Set<String> logins = Collections.synchronizedSet(new HashSet<String>());
	private static final String[] alphabet =
			{"a","b","c","d","e","f","g","h","j","k","m","n","p","r","s","t","v","w","x","y","z","3","4","5","6","7","8","9"};
	private static final Map<String, Pattern> patterns = new HashMap<>();
	static {
		patterns.put("email", Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,4}$"));
		patterns.put("zipCode", Pattern.compile("^[0-9]{5}$"));
		patterns.put("phone", Pattern.compile("^(0|\\+33)\\s*[0-9]([-. ]?[0-9]{2}){4}$")); // "^(0|\\+33)\\s*[1-9]([-. ]?[0-9]{2}){4}$"
		patterns.put("mobile", Pattern.compile("^(0|\\+33)\\s*[67]([-. ]?[0-9]{2}){4}$"));
		patterns.put("notEmpty", Pattern.compile("^(?=\\s*\\S).*$"));
		patterns.put("birthDate", Pattern.compile("^(0[1-9]|[12][0-9]|3[01])/(0[1-9]|1[012])/((19|20)\\d\\d)$"));
		patterns.put("BCrypt", Pattern.compile("^\\$2a\\$\\d{2}\\$([A-Za-z0-9+\\\\./]{22})"));
		patterns.put("uai", Pattern.compile("^[0-9]{7}[A-Z]$"));
		patterns.put("siren", Pattern.compile("^[0-9]{3} ?[0-9]{3} ?[0-9]{3}$"));
		patterns.put("siret", Pattern.compile("^[0-9]{3} ?[0-9]{3} ?[0-9]{3} ?[0-9]{5}$"));
		patterns.put("uri", Pattern.compile("^(([^:/?#]+):)?(//([^/?#]*))?([^?#]*)(\\?([^#]*))?(#(.*))?"));
	}

	private final JsonObject validate;
	private final JsonObject generate;
	private final JsonArray required;

	public Validator(JsonObject schema) {
		if (schema == null || schema.size() == 0) {
			throw new IllegalArgumentException("Missing schema.");
		}
		this.validate = schema.getObject("validate");
		this.generate = schema.getObject("generate");
		this.required = schema.getArray("required");
		if (validate == null || generate == null || required == null) {
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
					return err;
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
				object.putString(attr, firstName + " " + lastName);
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
				while (!logins.add(l)) {
					l = login + i++;
				}
				object.putString(attr, l);
			}
		}
	}

	private static String removeAccents(String str) {
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

}
