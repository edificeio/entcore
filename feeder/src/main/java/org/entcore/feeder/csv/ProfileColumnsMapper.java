/*
 * Copyright © WebServices pour l'Éducation, 2017
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

package org.entcore.feeder.csv;

import org.entcore.feeder.utils.CSVUtil;
import org.entcore.feeder.utils.ResultMessage;
import org.entcore.feeder.utils.Validator;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.*;

import static fr.wseduc.webutils.Utils.isEmpty;
import static fr.wseduc.webutils.Utils.isNotEmpty;

public class ProfileColumnsMapper {

	private final Map<String, Map<String, Object>> profilesNamesMapping = new HashMap<>();

	public ProfileColumnsMapper() {
		defaultInit();
	}

	private void defaultInit() {
		JsonObject baseMappings = new JsonObject()
				.put("id", "externalId")
				.put("externalid", "externalId")
				.put("idexterno", "externalId")
				.put("nom", "lastName")
				.put("apellido", "lastName")
				.put("lastname", "lastName")
				.put("sobrenome", "lastName")
				.put("nomeleve", "lastName")
				.put("nomresponsable", "lastName")
				.put("nomdusageeleve", "surname")
				.put("nomusageresponsable", "surname")
				.put("nomusage", "surname")
				.put("nomdusage", "surname")
				.put("nomusage", "surname")
				.put("prenom", "firstName")
				.put("nombre", "firstName")
				.put("firstname", "firstName")
				.put("nome", "firstName")
				.put("prenomeleve", "firstName")
				.put("prenomresponsable", "firstName")
				.put("classe", "classes")
				.put("class", "classes")
				.put("turma", "classes")
				.put("clase", "classes")
				.put("libelleclasse", "classes")
				.put("classeouregroupement", "classes")
				.put("datedenaissance", "birthDate")
				.put("date_naiss", "birthDate")
				.put("datenaissance", "birthDate")
				.put("birthdate", "birthDate")
				.put("fechadenacimiento", "birthDate")
				.put("datadenascimento", "birthDate")
				.put("neele", "birthDate")
				.put("ne(e)le", "birthDate")
				.put("fonction", "functions")
				.put("funcion", "functions")
				.put("function", "functions")
				.put("funcao", "functions")
				.put("statut", "status")
				.put("datedenaissance", "birthDate")
				.put("datenaissance", "birthDate")
				.put("civilite", "title")
				.put("telephone", "homePhone")
				.put("telefono", "homePhone")
				.put("phone", "homePhone")
				.put("telefone", "homePhone")
				.put("telephonedomicile", "homePhone")
				.put("telephonetravail", "workPhone")
				.put("telefonotrabajo", "workPhone")
				.put("phonework", "workPhone")
				.put("telefonetrabalho", "workPhone")
				.put("telephoneportable", "mobile")
				.put("adresse", "address")
				.put("adresse1", "address")
				.put("adresseresponsable", "address")
				.put("direccion", "address")
				.put("address", "address")
				.put("endereco", "address")
				.put("adresse2", "address2")
				.put("cp", "zipCode")
				.put("cpresponsable", "zipCode")
				.put("codigopostal", "zipCode")
				.put("postcode", "zipCode")
				.put("cp1", "zipCode")
				.put("cp2", "zipCode2")
				.put("ville", "city")
				.put("commune", "city")
				.put("commune1", "city")
				.put("commune2", "city2")
				.put("ciudad", "city")
				.put("city", "city")
				.put("cidade", "city")
				.put("pays", "country")
				.put("pais", "country")
				.put("country", "country")
				.put("pays1", "country")
				.put("pays2", "country2")
				.put("email", "email")
				.put("correoelectronico", "email")
				.put("courriel", "email")
				.put("sexe", "gender")
				.put("alias", "loginAlias")
				.put("login", "loginAlias")
				.put("multietab", "multiEtab")
				//.put("importid", "importId")
				.put("identifiantclasse", "ignore")
				.put("dateinscription", "ignore")
				.put("deuxiemeprenom", "ignore")
				.put("troisiemeprenom", "ignore")
				.put("communenaissance", "ignore")
				.put("deptnaissance", "ignore")
				.put("paysnaissance", "ignore")
				.put("etat", "ignore")
				.put("intervenant", "ignore")
				.put("ignore", "ignore");
		JsonObject studentMappings = baseMappings.copy()
				.put("nomeleve", "lastName")
				.put("nomdusageeleve", "surname")
				.put("prenomeleve", "firstName")
				.put("niveau", "level")
				.put("regime", "accommodation")
				.put("filiere", "sector")
				.put("cycle", "sector")
				.put("attestationfournie", "ignore")
				.put("autorisationsassociations", "ignore")
				.put("autorisationassociations", "ignore")
				.put("autorisationsphotos", "ignore")
				.put("autorisationphoto", "ignore")
				.put("decisiondepassage", "ignore")
				.put("boursier", "scholarshipHolder")
				.put("transport", "transport")
				.put("statut", "status")
				.put("regroupement(s)", "groups")
				.put("regroupement", "groups")
				.put("groupes", "groups")
				.put("persreleleve", "relative")
				.put("ine", "ine")
				.put("niveau", "level")
				.put("regime", "accommodation")
				.put("filiere", "sector")
				.put("cycle", "sector")
				.put("mef", "module")
				.put("libellemef", "moduleName")
				.put("transport", "transport")
				.put("codematiere", "fieldOfStudy")
				.put("matiere", "fieldOfStudyLabels")
				.put("persreleleve", "relative")
				.put("relative", "relative")
				.put("r1_nom", "r_nom")
				.put("r1_prenom", "r_prenom")
				.put("r2_nom", "r_nom")
				.put("r2_prenom", "r_prenom")
				.put("boursier", "scholarshipHolder")
				.put("dispositif(s)", "ignore");
		JsonObject relativeMapping = baseMappings.copy()
				.put("nomresponsable", "lastName")
				.put("nomusageresponsable", "surname")
				.put("prenomresponsable", "firstName")
				.put("idenfant", "childExternalId")
				.put("childid", "childExternalId")
				.put("childexternalid", "childExternalId")
				.put("nomenfant", "childLastName")
				.put("prenomenfant", "childFirstName")
				.put("classeenfant", "childClasses")
				.put("nomusageenfant", "childUsername")
				.put("nomfamilleenfant", "childLastName")
				.put("classesenfants", "childClasses")
				.put("civiliteresponsable", "title")
				.put("adresseresponsable", "address")
				.put("cpresponsable", "zipCode")
				.put("communeresponsable", "city")
				.put("idexternohijo", "childExternalId")
				.put("idexternofilho", "childExternalId")
				.put("nomenfant", "childLastName")
				.put("nomdusageenfant", "childUsername")
				.put("nomdefamilleenfant", "childLastName")
				.put("nomdusageeleve", "childUsername")
				.put("nomdefamilleeleve", "childLastName")
				.put("prenomeleve", "childFirstName")
				.put("classeseleves", "childClasses");
		JsonObject teacherMapping = baseMappings.copy()
				.put("presencedevanteleves", "teaches")
				.put("fonction", "functions")
				.put("mef", "module")
				.put("libellemef", "moduleName")
				.put("professeurprincipal", "headTeacher")
				.put("discipline", "classCategories")
				.put("matiereenseignee", "subjectTaught")
				.put("discipline", "classCategories")
				.put("materia", "classCategories")
				.put("directeur", "ignore");
		profilesNamesMapping.put("Teacher", teacherMapping.getMap());
		profilesNamesMapping.put("Personnel", teacherMapping.getMap());
		profilesNamesMapping.put("Student", studentMappings.getMap());
		profilesNamesMapping.put("Relative", relativeMapping.getMap());
		profilesNamesMapping.put("Guest", baseMappings.getMap());
	}

	public ProfileColumnsMapper(JsonObject mapping) {
		if (mapping == null || mapping.size() == 0) {
			defaultInit();
		} else {
			for (String profile: mapping.fieldNames()) {
				final JsonObject m = mapping.getJsonObject(profile);
				if (m != null) {
					JsonObject j = new JsonObject()
							.put("externalid", "externalId")
							.put("childexternalid", "childExternalId");
							//.put("importid", "importId");
					for (String attr : m.fieldNames()) {
						j.put(cleanKey(attr), m.getString(attr));
					}
					profilesNamesMapping.put(profile, j.getMap());
				}
			}
		}
	}

	void getColumsNames(String profile, String[] strings, List<String> columns, Handler<Message<JsonObject>> handler) {
		boolean existEmptyCm = false;
		for (int j = 0; j < strings.length; j++) {
			String cm = columnsNameMapping(profile, strings[j]);
			if (profilesNamesMapping.get(profile).containsValue(cm)) {
				if (existEmptyCm) {
					columns.clear();
					handler.handle(new ResultMessage().error("invalid.column " + cm));
					return;
				}
				try {
					columns.add(j, cm);
				} catch (ArrayIndexOutOfBoundsException e) {
					columns.clear();
					handler.handle(new ResultMessage().error("invalid.column " + cm));
					return;
				}
			} else if (isNotEmpty(cm)) {
				// Fix ticket #24705
				columns.add(j, "ignore");
//				columns.clear();
//				handler.handle(new ResultMessage().error("invalid.column " + cm));
//				return;
			} else {
				existEmptyCm = true;
			}
		}
	}

	JsonArray getColumsNames(String profile, String[] strings, List<String> columns) {
		JsonArray errors = new fr.wseduc.webutils.collections.JsonArray();
		for (int j = 0; j < strings.length; j++) {
			String cm = columnsNameMapping(profile, strings[j]);
			if (profilesNamesMapping.get(profile).containsValue(cm)) {
				columns.add(j, cm);
			} else if ("Student".equals(profile) && "relative".equals(cm)) {
				columns.add(j, cm);
			} else {
				errors.add(cm);
				return errors;
			}
		}
		return errors;
	}

	String columnsNameMapping(String profile, String columnName) {
		final String key = cleanKey(columnName);
		final Object attr = profilesNamesMapping.get(profile).get(key);
		return attr != null ? attr.toString() : key;
	}

	private static String cleanKey(String columnName) {
		String cleanKey = Validator.removeAccents(columnName.trim().toLowerCase())
				.replaceAll("\\s+", "").replaceAll("\\*", "").replaceAll("'", "")
				.replaceFirst(CSVUtil.UTF8_BOM, "");
		final byte[] bytes = cleanKey.getBytes();
		if (bytes.length > 3 && bytes[0] == (byte) 0xEF && bytes[1] == (byte) 0xBB && bytes[2] == (byte) 0xBF) {
			cleanKey = new String(Arrays.copyOfRange(bytes, 3, bytes.length));
		}
		return cleanKey;

	}

	public JsonObject getColumsMapping(String profile, String[] strings) {
		JsonObject mapping = new JsonObject();
		for (String key : strings) {
			if (isEmpty(key)) {
				mapping.put("", "ignore");
				continue;
			}
			String cm = columnsNameMapping(profile, key);
			if (profilesNamesMapping.get(profile).containsValue(cm)) {
				mapping.put(key, cm);
			} else {
				mapping.put(key, "");
			}
		}
		return mapping;
	}

	public JsonObject availableFields() {
		JsonObject j = new JsonObject();
		for (String profile : profilesNamesMapping.keySet()) {
			j.put(profile, new fr.wseduc.webutils.collections.JsonArray(
					new ArrayList<>(new HashSet<>(profilesNamesMapping.get(profile).values()))));
		}
		return j;
	}

}
