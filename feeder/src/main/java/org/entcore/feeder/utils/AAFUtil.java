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

package org.entcore.feeder.utils;

import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AAFUtil {

	private static final Pattern datePatter = Pattern.compile("^([0-9]{4})-([0-9]{2})-([0-9]{2})$");
	private static final JsonObject functionCodes = new JsonObject().putString("ADMIN_LOCAL", "AL");

	public static Object convert(Object value, String type) {
		if (value == null) {
			return "";
		}
		if (type == null) {
			return value;
		}
		Object res;
		try {
			switch (type) {
				case "boolean-ON" :
					res = ((boolean) value) ? "O" : "N";
					break;
				case "head-array" :
					res = ((JsonArray) value).get(0);
					break;
				case "date" :
					Matcher m = datePatter.matcher(value.toString());
					if (m.find()) {
						res = m.group(3) + "/" + m.group(2) + "/" + m.group(1);
					} else {
						res = value;
					}
					break;
				case "siecle" :
					res = siecleConverter((JsonArray) value);
					break;
				case "functions-etab" :
					res = functionsEtabConverter((JsonArray) value);
					break;
				default :
					res = value;
			}
		} catch (RuntimeException e) {
			res = value;
		}
		return res;
	}

	private static JsonArray functionsEtabConverter(JsonArray value) {
		JsonArray res = new JsonArray();
		for (Object o : value) {
			if (!(o instanceof JsonArray)) continue;
			JsonArray a = (JsonArray) o;
			final String c = a.get(0);
			if (c != null) {
				final String code = functionCodes.getString(c, c);
				for (Object s : (JsonArray) a.get(1)) {
					res.add("EtabEducNat$" + s + "$" + code);
				}
			}
		}
		return res;
	}

	private static JsonObject siecleConverter(JsonArray value) {
		JsonObject res = new JsonObject();
		JsonArray ENTEleveParents = new JsonArray();
		JsonArray ENTEleveAutoriteParentale = new JsonArray();
		String ENTElevePersRelEleve1 = "";
		String ENTEleveQualitePersRelEleve1 = "";
		String ENTElevePersRelEleve2 = "";
		String ENTEleveQualitePersRelEleve2 = "";
		for (Object o : value) {
			String [] s = ((String) o).split("\\$");
			if ("1".equals(s[1]) || "2".equals(s[1])) {
				if (!ENTEleveParents.contains(s[0])) {
					ENTEleveParents.add(s[0]);
				}
			} else if ("1".equals(s[2]) || "1".equals(s[4]) || "1".equals(s[5])) {
				if ("1".equals(s[2])) {
					if (ENTElevePersRelEleve1 == null || ENTElevePersRelEleve1.isEmpty()) {
						ENTElevePersRelEleve1 = s[0];
						ENTEleveQualitePersRelEleve1 = "FINANCIER";
					} else if (ENTElevePersRelEleve2 == null || ENTElevePersRelEleve2.isEmpty()) {
						ENTElevePersRelEleve2 = s[0];
						ENTEleveQualitePersRelEleve2 = "FINANCIER";
					} else if ("PAIEMENT".equals(ENTEleveQualitePersRelEleve1)) {
						ENTElevePersRelEleve1 = s[0];
						ENTEleveQualitePersRelEleve1 = "FINANCIER";
					} else if ("PAIEMENT".equals(ENTEleveQualitePersRelEleve2)) {
						ENTElevePersRelEleve2 = s[0];
						ENTEleveQualitePersRelEleve2 = "FINANCIER";
					}
				} else if ("1".equals(s[4])) {
					if (ENTElevePersRelEleve1 == null || ENTElevePersRelEleve1.isEmpty()) {
						ENTElevePersRelEleve1 = s[0];
						ENTEleveQualitePersRelEleve1 = "CONTACT";
					} else if (ENTElevePersRelEleve2 == null || ENTElevePersRelEleve2.isEmpty()) {
						ENTElevePersRelEleve2 = s[0];
						ENTEleveQualitePersRelEleve2 = "CONTACT";
					} else if ("PAIEMENT".equals(ENTEleveQualitePersRelEleve1)) {
						ENTElevePersRelEleve1 = s[0];
						ENTEleveQualitePersRelEleve1 = "CONTACT";
					} else if ("PAIEMENT".equals(ENTEleveQualitePersRelEleve2)) {
						ENTElevePersRelEleve2 = s[0];
						ENTEleveQualitePersRelEleve2 = "CONTACT";
					}
				} else if ("1".equals(s[5])) {
					if (ENTElevePersRelEleve1 == null || ENTElevePersRelEleve1.isEmpty()) {
						ENTElevePersRelEleve1 = s[0];
						ENTEleveQualitePersRelEleve1 = "PAIEMENT";
					} else if (ENTElevePersRelEleve2 == null || ENTElevePersRelEleve2.isEmpty()) {
						ENTElevePersRelEleve2 = s[0];
						ENTEleveQualitePersRelEleve2 = "PAIEMENT";
					}
				}
			}
			if ("1".equals(s[3]) || "2".equals(s[3])) {
				if (!ENTEleveAutoriteParentale.contains(s[0])) {
					ENTEleveAutoriteParentale.add(s[0]);
				}
			}
		}
		res.putArray("ENTEleveParents", ENTEleveParents);
		res.putArray("ENTEleveAutoriteParentale", ENTEleveAutoriteParentale);
		res.putString("ENTElevePersRelEleve1", ENTElevePersRelEleve1);
		res.putString("ENTEleveQualitePersRelEleve1", ENTEleveQualitePersRelEleve1);
		res.putString("ENTElevePersRelEleve2", ENTElevePersRelEleve2);
		res.putString("ENTEleveQualitePersRelEleve2", ENTEleveQualitePersRelEleve2);
		return res;
	}

}
