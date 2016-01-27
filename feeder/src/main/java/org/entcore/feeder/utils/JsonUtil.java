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
				case "siecle-address":
					res = (value != null && !"$$$".equals(value)) ? value.replaceAll("\\$", "   ").trim() : new None();
					break;
				default :
					res = value;
			}
		} catch (RuntimeException e) {
			res = value;
		}
		return res;
	}

	public static class None{}

}
