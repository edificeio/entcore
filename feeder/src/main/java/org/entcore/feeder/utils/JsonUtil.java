/*
 * Copyright. Tous droits réservés. WebServices pour l’Education.
 */

package org.entcore.feeder.utils;

import org.vertx.java.core.json.JsonObject;

import java.util.Scanner;

public final class JsonUtil {

	private JsonUtil() {}

	public static JsonObject loadFromResource(String resource) {
		String src = new Scanner(JsonUtil.class.getClassLoader()
				.getResourceAsStream(resource), "UTF-8")
				.useDelimiter("\\A").next();
		return new JsonObject(src);
	}

	public static Object convert(String value, String type) {
		if (type == null) {
			return value;
		}
		Object res;
		try {
			switch (type.replaceAll("array-", "")) {
				case "boolean" :
					String v = value.toLowerCase().replaceFirst("(y|o)", "true").replaceFirst("n", "false");
					res = Boolean.parseBoolean(v);
					break;
				case "int" :
					res = Integer.parseInt(value);
					break;
				case "long" :
					res = Long.parseLong(value);
					break;
				default :
					res = value;
			}
		} catch (RuntimeException e) {
			res = value;
		}
		return res;
	}

}
