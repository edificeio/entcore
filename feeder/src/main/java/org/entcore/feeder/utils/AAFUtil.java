/*
 * Copyright © "Open Digital Education", 2014
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

 */

package org.entcore.feeder.utils;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static fr.wseduc.webutils.Utils.getOrElse;
import static fr.wseduc.webutils.Utils.isEmpty;
import static fr.wseduc.webutils.Utils.isNotEmpty;

public class AAFUtil {

	private static final Pattern datePatter = Pattern.compile("^([0-9]{4})-([0-9]{2})-([0-9]{2})$");
	private static final JsonObject functionCodes = new JsonObject().put("ADMIN_LOCAL", "AL");
	private static final Pattern frenchDatePatter = Pattern.compile("^([0-9]{2})/([0-9]{2})/([0-9]{4})$");
	private static final List<String> groupImportsList = Arrays.asList("AAF", "EDT", "UDT");

	public static Object convert(Object value, String type) {
		if (value == null && !"groups-source".equals(type)) {
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
					res = ((JsonArray) value).getValue(0);
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
				case "structure-to-function":
					res = structureToFunction((JsonArray) value);
					break;
				case "groups-source" :
					res = groupsSource(value);
					break;
				case "functionalGroup" :
					res = functionalGroupConverter(value);
					break;
				default :
					res = value;
			}
		} catch (RuntimeException e) {
			res = value;
		}
		return res;
	}

	private static Object functionalGroupConverter(Object value) {
		final JsonArray res = new JsonArray();
		if (value instanceof JsonArray) {
			for (Object o : ((JsonArray) value)) {
				if (o instanceof JsonObject) {
					final JsonObject j = (JsonObject) o;
					final String structureExternalId = j.getString("structureExternalId");
					if (isNotEmpty(structureExternalId)) {
						final String idrgpmt = j.getString("idrgpmt");
						final String name = j.getString("name");
						if (isNotEmpty(idrgpmt)) {
							res.add(structureExternalId + "$" + name + "$" + idrgpmt);
						} else if (isNotEmpty(j.getString("idgpe"))) {
							if (!getOrElse(j.getBoolean("usedInCourses"), false)) continue;
							final String code = j.getString("code");
							if (isNotEmpty(code) && isNotEmpty(j.getString("code_gep"))) {
								res.add(structureExternalId + "$" + j.getString("code_gep") + "_" + code + "$" + j.getString("idgpe"));
							} else if (isNotEmpty(code) && isNotEmpty(j.getString("code_div"))) {
								res.add(structureExternalId + "$" + j.getString("code_div") + "_" + code + "$" + j.getString("idgpe"));
							}
						} else if (isEmpty(j.getString("externalId")) && isNotEmpty(name)) {
							res.add(structureExternalId + "$" + name + "$" + j.getString("id"));
						} else if (isNotEmpty(j.getString("externalId"))) {
							res.add(j.getString("externalId") + "$" + j.getString("id"));
						}
					}
				}
			}
		}
		return res;
	}

	private static Object groupsSource(Object value) {
		if (value instanceof String) {
			return groupImportsList.contains(value.toString()) ? value : "";
		}
		return "AAF";
	}

	private static Object structureToFunction(JsonArray value) {
		JsonArray res = new fr.wseduc.webutils.collections.JsonArray();
		if (value != null) {
			for (Object o : value) {
				if (o != null && !o.toString().isEmpty()) {
					res.add("EtabEducNat$" + o.toString() + "$UI");
				}
			}
		}
		return res;
	}

	private static JsonArray functionsEtabConverter(JsonArray value) {
		JsonArray res = new fr.wseduc.webutils.collections.JsonArray();
		for (Object o : value) {
			if (!(o instanceof JsonArray)) continue;
			JsonArray a = (JsonArray) o;
			final String c = a.getString(0);
			if (c != null) {
				final String code = functionCodes.getString(c, c);
				for (Object s : a.getJsonArray(1)) {
					res.add("EtabEducNat$" + s + "$" + code);
				}
			}
		}
		return res;
	}

	private static JsonObject siecleConverter(JsonArray value) {
		JsonObject res = new JsonObject();
		JsonArray ENTEleveParents = new fr.wseduc.webutils.collections.JsonArray();
		Set<String> ENTEleveAutoriteParentale = new HashSet<>();
		String ENTEleveAutoriteParentale1 = "";
		String ENTEleveAutoriteParentale2 = "";
		String ENTElevePersRelEleve1 = "";
		String ENTEleveQualitePersRelEleve1 = "";
		String ENTElevePersRelEleve2 = "";
		String ENTEleveQualitePersRelEleve2 = "";

		// prevent missing "ENTEleveAutoriteParentale" if item is duplicate
		if (value.size() > 2) {
			final Map<String, String> tmp = new HashMap<>();
			for (Object o : value) {
				final String[] s = ((String) o).split("\\$", 2);
				final String v = tmp.get(s[0]);
				if (v == null || "1$1$1$1$0".equals(v)) {
					tmp.put(s[0], s[1]);
				}
			}
			JsonArray tmpArray = new fr.wseduc.webutils.collections.JsonArray();
			for (Map.Entry<String, String> e : tmp.entrySet()) {
				tmpArray.add(e.getKey() + "$" + e.getValue());
			}
			value = tmpArray;
		}

		for (Object o : value) {
			String [] s = ((String) o).split("\\$");
			if ("1".equals(s[1]) || "2".equals(s[1]) || "10".equals(s[1]) || "20".equals(s[1])) {
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
			if ("1".equals(s[3])) {
				if (isEmpty(ENTEleveAutoriteParentale1)) {
					ENTEleveAutoriteParentale1 = s[0];
				}
				ENTEleveAutoriteParentale.add(s[0]);
			}
			if (isEmpty(ENTEleveAutoriteParentale2) && "2".equals(s[3])) {
				ENTEleveAutoriteParentale2 = s[0];
			}
		}

		if (!ENTEleveAutoriteParentale1.isEmpty() && !ENTEleveAutoriteParentale.contains(ENTEleveAutoriteParentale1)) {
			ENTEleveAutoriteParentale.add(ENTEleveAutoriteParentale1);
		}
		if (!ENTEleveAutoriteParentale2.isEmpty() && !ENTEleveAutoriteParentale.contains(ENTEleveAutoriteParentale2)) {
			ENTEleveAutoriteParentale.add(ENTEleveAutoriteParentale2);
		}
		res.put("ENTEleveParents", ENTEleveParents);
		res.put("ENTEleveAutoriteParentale", new fr.wseduc.webutils.collections.JsonArray(new ArrayList<>(ENTEleveAutoriteParentale)));
		res.put("ENTElevePersRelEleve1", ENTElevePersRelEleve1);
		res.put("ENTEleveQualitePersRelEleve1", ENTEleveQualitePersRelEleve1);
		res.put("ENTElevePersRelEleve2", ENTElevePersRelEleve2);
		res.put("ENTEleveQualitePersRelEleve2", ENTEleveQualitePersRelEleve2);
		res.put("ENTElevePersRelEleve", value);
		return res;
	}

	public static String convertDate(String s) {
		Matcher m = frenchDatePatter.matcher(s);
		if (m.find()) {
			return m.group(3) + "-" + m.group(2) + "-" + m.group(1);
		}
		return s;
	}

}
