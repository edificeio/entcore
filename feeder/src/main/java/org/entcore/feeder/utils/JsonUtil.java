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

import fr.wseduc.webutils.security.Md5;
import fr.wseduc.webutils.security.Sha256;
import io.vertx.core.json.JsonObject;

import java.security.NoSuchAlgorithmException;
import java.util.Scanner;
import java.util.TreeSet;

import static fr.wseduc.webutils.Utils.isNotEmpty;

public final class JsonUtil {

	private JsonUtil() {}

	public enum HashAlgorithm { SHA256, MD5 }

	public static JsonObject loadFromResource(String resource) {
		String src = new Scanner(JsonUtil.class.getClassLoader()
				.getResourceAsStream(resource), "UTF-8")
				.useDelimiter("\\A").next();
		return new JsonObject(src);
	}

	public static Object convert(String value, String type) {
		return convert(value, type, null);
	}

	public static Object convert(String value, String type, String prefix) {
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
					if (value != null && !"$$$".equals(value)) {
						res = value.replaceAll("\\$", "   ").trim();
					} else {
						return new None();
					}
					break;
				case "classe-group-fieldOfStudy":
					String [] items;
					if (isNotEmpty(prefix) && (items = value.split("\\$")).length == 3) {
						res = items[0] + "$" + items[1] + "$" + prefix + items[2];
					} else {
						res = value;
					}
					break;
				default :
					res = value;
			}
		} catch (RuntimeException e) {
			res = value;
		}
		return isNotEmpty(prefix) ? prefix + res : res;
	}

	public static class None{}

	public static String checksum(JsonObject object) throws NoSuchAlgorithmException {
		return checksum(object, HashAlgorithm.SHA256);
	}

	public static String checksum(JsonObject object, HashAlgorithm hashAlgorithm) throws NoSuchAlgorithmException {
		if (object == null) {
			return null;
		}
		final TreeSet<String> sorted = new TreeSet<>(object.fieldNames());
		final JsonObject j = new JsonObject();
		for (String attr : sorted) {
			j.put(attr, object.getValue(attr));
		}
		switch (hashAlgorithm) {
			case MD5:
				return Md5.hash(j.encode());
			default:
				return Sha256.hash(j.encode());
		}
	}

}
