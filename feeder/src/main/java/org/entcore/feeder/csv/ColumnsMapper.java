/*
 * Copyright © WebServices pour l'Éducation, 2016
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
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.util.List;
import java.util.Map;

public class ColumnsMapper {

	private final Map<String, Object> namesMapping;

	public ColumnsMapper(JsonObject additionnalsMappings) {
		JsonObject mappings = new JsonObject()
				.putString("id", "externalId")
				.putString("externalid", "externalId")
				.putString("nom", "lastName")
				.putString("nomeleve", "lastName")
				.putString("nomresponsable", "lastName")
				.putString("nomdusageeleve", "username")
				.putString("nomusageresponsable", "username")
				.putString("nomusage", "username")
				.putString("prenom", "firstName")
				.putString("prenomeleve", "firstName")
				.putString("prenomresponsable", "firstName")
				.putString("classe", "classes")
				.putString("libelleclasse", "classes")
				.putString("classeouregroupement", "classes")
				.putString("idenfant", "childExternalId")
				.putString("datedenaissance", "birthDate")
				.putString("datenaissance", "birthDate")
				.putString("neele", "birthDate")
				.putString("ne(e)le", "birthDate")
				.putString("childid", "childExternalId")
				.putString("childexternalid", "childExternalId")
				.putString("nomenfant", "childLastName")
				.putString("prenomenfant", "childFirstName")
				.putString("classeenfant", "childClasses")
				.putString("nomdusageenfant", "childUsername")
				.putString("nomdefamilleenfant", "childLastName")
				.putString("classesenfants", "childClasses")
				.putString("presencedevanteleves", "teaches")
				.putString("fonction", "functions")
				.putString("niveau", "level")
				.putString("regime", "accommodation")
				.putString("filiere", "sector")
				.putString("cycle", "sector")
				.putString("mef", "module")
				.putString("libellemef", "moduleName")
				.putString("boursier", "scholarshipHolder")
				.putString("transport", "transport")
				.putString("statut", "status")
				.putString("codematiere", "fieldOfStudy")
				.putString("matiere", "fieldOfStudyLabels")
				.putString("persreleleve", "relative")
				.putString("civilite", "title")
				.putString("civiliteresponsable", "title")
				.putString("telephone", "homePhone")
				.putString("telephonedomicile", "homePhone")
				.putString("telephonetravail", "workPhone")
				.putString("telephoneportable", "mobile")
				.putString("adresse", "address")
				.putString("adresse1", "address")
				.putString("adresseresponsable", "address")
				.putString("adresse2", "address2")
				.putString("cp", "zipCode")
				.putString("cpresponsable", "zipCode")
				.putString("cp1", "zipCode")
				.putString("cp2", "zipCode2")
				.putString("ville", "city")
				.putString("communeresponsable", "city")
				.putString("commune", "city")
				.putString("commune1", "city")
				.putString("commune2", "city2")
				.putString("pays", "country")
				.putString("pays1", "country")
				.putString("pays2", "country2")
				.putString("discipline", "classCategories")
				.putString("matiereenseignee", "subjectTaught")
				.putString("email", "email")
				.putString("courriel", "email")
				.putString("professeurprincipal", "headTeacher")
				.putString("sexe", "gender")
				.putString("attestationfournie", "ignore")
				.putString("autorisationsassociations", "ignore")
				.putString("autorisationassociations", "ignore")
				.putString("autorisationsphotos", "ignore")
				.putString("autorisationphoto", "ignore")
				.putString("decisiondepassage", "ignore")
				.putString("directeur", "ignore")
				.putString("ine", "ignore")
				.putString("identifiantclasse", "");

		mappings.mergeIn(additionnalsMappings);
		namesMapping = mappings.toMap();
	}

	void getColumsNames(String[] strings, List<String> columns, Handler<Message<JsonObject>> handler) {
		for (int j = 0; j < strings.length; j++) {
			String cm = columnsNameMapping(strings[j]);
			if (namesMapping.containsValue(cm)) {
				try {
					columns.add(j, cm);
				} catch (ArrayIndexOutOfBoundsException e) {
					columns.clear();
					handler.handle(new ResultMessage().error("invalid.column " + cm));
					return;
				}
			} else {
				columns.clear();
				handler.handle(new ResultMessage().error("invalid.column " + cm));
				return;
			}
		}
	}

	JsonArray getColumsNames(String[] strings, List<String> columns) {
		JsonArray errors = new JsonArray();
		for (int j = 0; j < strings.length; j++) {
			String cm = columnsNameMapping(strings[j]);
			if (namesMapping.containsValue(cm)) {
				columns.add(j, cm);
			} else {
				errors.add(cm);
				return errors;
			}
		}
		return errors;
	}

	String columnsNameMapping(String columnName) {
		final String key = Validator.removeAccents(columnName.trim().toLowerCase())
				.replaceAll("\\s+", "").replaceAll("\\*", "").replaceAll("'", "").replaceFirst(CSVUtil.UTF8_BOM, "");
		final Object attr = namesMapping.get(key);
		return attr != null ? attr.toString() : key;
	}

}
