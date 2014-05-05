/*
 * Copyright. Tous droits réservés. WebServices pour l’Education.
 */

package org.entcore.feeder.utils;

import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

public class Joiner {

	private final String separator;

	private Joiner(String separator) {
		this.separator = separator;
	}

	public static Joiner on(String separator) {
		if (separator != null) {
			return new Joiner(separator);
		}
		return null;
	}

	public String join(JsonArray items) {
		StringBuilder sb = new StringBuilder();
		for (Object item: items) {
			String s;
			if (item instanceof JsonArray) {
				s = ((JsonArray) item).encode();
			} else if (item instanceof JsonObject) {
				s = ((JsonObject) item).encode();
			} else {
				s = item.toString();
			}
			sb.append(separator).append(s);
		}
		return (sb.length() > separator.length()) ? sb.substring(separator.length()) : "";
	}

}
