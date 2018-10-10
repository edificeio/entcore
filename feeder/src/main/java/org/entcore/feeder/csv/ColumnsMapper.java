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
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;
import java.util.Map;

public class ColumnsMapper {

	private final Map<String, Object> namesMapping;
	private final Map<String, Object> relativeMapping;

	public ColumnsMapper(JsonObject additionnalsMappings) {
		JsonObject mappings = new JsonObject()
				.put("id", "externalId")
				.put("externalid", "externalId")
				.put("idexterno", "externalId")
				.put("nom", "lastName")
				.put("apellido", "lastName")
				.put("lastname", "lastName")
				.put("sobrenome", "lastName")
				.put("nomeleve", "lastName")
				.put("nomresponsable", "lastName")
				.put("nomdusageeleve", "username")
				.put("nomusageresponsable", "username")
				.put("nomusage", "username")
				.put("nomdusage", "username")
				.put("prenom", "firstName")
				.put("nombre", "firstName")
				.put("firstname", "firstName")
				.put("nome", "firstName")
				.put("prenomeleve", "firstName")
				.put("prenomresponsable", "firstName")
				.put("classe", "classes")
				.put("clase", "classes")
				.put("class", "classes")
				.put("turma", "classes")
				.put("libelleclasse", "classes")
				.put("classeouregroupement", "classes")
				.put("idenfant", "childExternalId")
				.put("datedenaissance", "birthDate")
				.put("datenaissance", "birthDate")
				.put("birthdate", "birthDate")
				.put("fechadenacimiento", "birthDate")
				.put("datadenascimento", "birthDate")
				.put("neele", "birthDate")
				.put("ne(e)le", "birthDate")
				.put("childid", "childExternalId")
				.put("childexternalid", "childExternalId")
				.put("idexternohijo", "childExternalId")
				.put("idexternofilho", "childExternalId")
				.put("nomenfant", "childLastName")
				.put("prenomenfant", "childFirstName")
				.put("classeenfant", "childClasses")
				.put("nomdusageenfant", "childUsername")
				.put("nomdefamilleenfant", "childLastName")
				.put("nomdefamilleeleve", "childLastName")
				.put("classesenfants", "childClasses")
				.put("classeseleves", "childClasses")
				.put("presencedevanteleves", "teaches")
				.put("fonction", "functions")
				.put("funcion", "functions")
				.put("function", "functions")
				.put("funcao", "functions")
				.put("niveau", "level")
				.put("regime", "accommodation")
				.put("filiere", "sector")
				.put("cycle", "sector")
				.put("mef", "module")
				.put("libellemef", "moduleName")
				.put("boursier", "scholarshipHolder")
				.put("transport", "transport")
				.put("statut", "status")
				.put("codematiere", "fieldOfStudy")
				.put("matiere", "fieldOfStudyLabels")
				.put("persreleleve", "relative")
				.put("civilite", "title")
				.put("civiliteresponsable", "title")
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
				.put("communeresponsable", "city")
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
				.put("discipline", "classCategories")
				.put("materia", "classCategories")
				.put("matiereenseignee", "subjectTaught")
				.put("email", "email")
				.put("correoelectronico", "email")
				.put("courriel", "email")
				.put("professeurprincipal", "headTeacher")
				.put("sexe", "gender")
				.put("attestationfournie", "ignore")
				.put("autorisationsassociations", "ignore")
				.put("autorisationassociations", "ignore")
				.put("autorisationsphotos", "ignore")
				.put("autorisationphoto", "ignore")
				.put("decisiondepassage", "ignore")
				.put("directeur", "ignore")
				.put("ine", "ine")
				.put("identifiantclasse", "ignore")
				.put("dateinscription", "ignore")
				.put("deuxiemeprenom", "ignore")
				.put("troisiemeprenom", "ignore")
				.put("communenaissance", "ignore")
				.put("deptnaissance", "ignore")
				.put("paysnaissance", "ignore")
				.put("etat", "ignore")
				.put("intervenant", "ignore")
				.put("regroupement(s)", "ignore")
				.put("dispositif(s)", "ignore")
				.put("", "ignore");

		mappings.mergeIn(additionnalsMappings);
		namesMapping = mappings.getMap();
		relativeMapping = mappings.copy().put("prenomeleve", "childFirstName").put("nomdusageeleve", "childUsername").getMap();
	}

	void getColumsNames(String[] strings, List<String> columns, String profile, Handler<Message<JsonObject>> handler) {
		for (int j = 0; j < strings.length; j++) {
			String cm = columnsNameMapping(strings[j], profile);
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

	JsonArray getColumsNames(String[] strings, List<String> columns, String profile) {
		JsonArray errors = new fr.wseduc.webutils.collections.JsonArray();
		for (int j = 0; j < strings.length; j++) {
			String cm = columnsNameMapping(strings[j], profile);
			if (namesMapping.containsValue(cm)) {
				columns.add(j, cm);
			} else {
				errors.add(cm);
				return errors;
			}
		}
		return errors;
	}

	String columnsNameMapping(String columnName, String profile) {
		final String key = Validator.removeAccents(columnName.trim().toLowerCase())
				.replaceAll("\\s+", "").replaceAll("\\*", "").replaceAll("'", "").replaceFirst(CSVUtil.UTF8_BOM, "");
		final Object attr = "Relative".equals(profile) ? relativeMapping.get(key) : namesMapping.get(key);
		return attr != null ? attr.toString() : key;
	}

}
