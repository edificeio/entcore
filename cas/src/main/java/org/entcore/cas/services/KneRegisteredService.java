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

package org.entcore.cas.services;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import fr.wseduc.cas.entities.User;

public class KneRegisteredService extends AbstractCas20ExtensionRegisteredService {

	private static final Logger log = LoggerFactory.getLogger(KneRegisteredService.class);

	private final Pattern mefStatPattern = Pattern.compile(".*\\$([0-9]{6}).*\\$.*");
	private final Pattern classGroupPattern = Pattern.compile(".*\\$(.*)");
	private final Pattern matPattern = Pattern.compile(".*\\$.*\\$([0-9]{6}).*");

	/* 		Tools 		*/

	private abstract class Mapper<IN, OUT>{
		abstract OUT map(IN input);
	}
	private class DefaultMapper<A> extends Mapper<A, A>{
		A map(A input){
			return input;
		}
	}

	private void addArray(String casLabel, String entLabel, JsonObject data, Document doc, List<Element> additionalAttributes, Mapper<String, String> mapper){
		Element root = createElement(casLabel+"s", doc);
		if(data.containsKey(entLabel)){
			for(Object item: data.getJsonArray(entLabel)){
				root.appendChild(createTextElement(casLabel, mapper.map((String) item), doc));
			}
		}
		additionalAttributes.add(root);
	}

	/*	*	*	*	*	*/

	@Override
	protected void prepareUserCas20(User user, String userId, String service, JsonObject data, Document doc, List<Element> additionalAttributes) {
		user.setUser(data.getString(principalAttributeName));

		try {

			String queryParams = new URI(service).getQuery();
			String[] pairs;
			if(queryParams != null && queryParams.length() > 0 && (pairs = queryParams.split("&")).length > 0){
				for(String pair : pairs){
					String key = pair.substring(0, pair.indexOf('='));
					if("UAI".equals(key)){
						String value = pair.substring(pair.indexOf('=') + 1);
						additionalAttributes.add(createTextElement("ENTPersonStructRattachUAI", value, doc));
						for (Object o : data.getJsonArray("structureNodes", new JsonArray()).getList()) {
							@SuppressWarnings("unchecked")
							Map<String, Object> structure = ((Map<String, Object>) o);
							if(value.equals(structure.get("UAI"))){
								if(structure.containsKey("type")){
									String type = (String) structure.get("type");
									switch(type){
										case "ECOLE DE NIVEAU ELEMENTAIRE":
											additionalAttributes.add(createTextElement("ENTStructureTypeStruct", "1ORD", doc));
											break;
										case "COLLEGE":
										case "COLLEGE CLIMATIQUE":
											additionalAttributes.add(createTextElement("ENTStructureTypeStruct", "CLG", doc));
											break;
										case "LYCEE D ENSEIGNEMENT GENERAL":
										case "LYCEE POLYVALENT":
											additionalAttributes.add(createTextElement("ENTStructureTypeStruct", "LYC", doc));
											break;
										case "LYCEE PROFESSIONNEL":
											additionalAttributes.add(createTextElement("ENTStructureTypeStruct", "LP", doc));
											break;
										default:
											additionalAttributes.add(createTextElement("ENTStructureTypeStruct", type, doc));
									}
								}
								break;
							}
						}
						break;
					}
				}
			}

			Element rootProfiles;
			String profile = data.getJsonArray("type", new JsonArray()).size() > 0 ? data.getJsonArray("type").getString(0) : "";
			switch(profile) {
				case "Student" :
					rootProfiles = createElement("ENTPersonProfils", doc);
					rootProfiles.appendChild(createTextElement("ENTPersonProfil", "National_ELV", doc));
					additionalAttributes.add(rootProfiles);
					additionalAttributes.add(createTextElement("ENTEleveMEF", data.getString("module", ""), doc));
					addArray("ENTEleveCodeEnseignement", "fieldOfStudy", data, doc, additionalAttributes, new DefaultMapper<String>());
					addArray("ENTEleveClasse", "classes", data, doc, additionalAttributes, new Mapper<String, String>(){
						String map(String input) {
							Matcher m = classGroupPattern.matcher(input);
							if(m.matches() && m.groupCount() >= 1){
								return m.group(1);
							}
							return input;
						}
					});
					addArray("ENTEleveGroupe", "groups", data, doc, additionalAttributes, new Mapper<String, String>(){
						String map(String input) {
							Matcher m = classGroupPattern.matcher(input);
							if(m.matches() && m.groupCount() >= 1){
								return m.group(1);
							}
							return input;
						}
					});
					additionalAttributes.add(createTextElement("ENTAuxEnsClassesMatieres", "", doc));
					additionalAttributes.add(createTextElement("ENTAuxEnsGroupes", "", doc));
					additionalAttributes.add(createTextElement("ENTAuxEnsClasses", "", doc));
					additionalAttributes.add(createTextElement("ENTAuxEnsMEFs", "", doc));
					break;
				case "Teacher" :
					rootProfiles = createElement("ENTPersonProfils", doc);
					rootProfiles.appendChild(createTextElement("ENTPersonProfil", "National_ENS", doc));
					//rootProfiles.appendChild(createTextElement("ENTPersonProfil", "National_TUT", doc));
					additionalAttributes.add(rootProfiles);
					additionalAttributes.add(createTextElement("ENTEleveMEF", "", doc));
					additionalAttributes.add(createTextElement("ENTEleveCodeEnseignements", "", doc));
					additionalAttributes.add(createTextElement("ENTEleveClasses", "", doc));
					additionalAttributes.add(createTextElement("ENTEleveGroupes", "", doc));
					addArray("ENTAuxEnsClassesMatiere", "classesFieldOfStudy", data, doc, additionalAttributes, new Mapper<String, String>(){
						String map(String input) {
							Matcher m = matPattern.matcher(input);
							if(m.matches() && m.groupCount() >= 1){
								return m.group(1);
							}
							return input;
						}
					});
					addArray("ENTAuxEnsGroupe", "groups", data, doc, additionalAttributes, new Mapper<String, String>(){
						String map(String input) {
							Matcher m = classGroupPattern.matcher(input);
							if(m.matches() && m.groupCount() >= 1){
								return m.group(1);
							}
							return input;
						}
					});
					addArray("ENTAuxEnsClasse", "classes", data, doc, additionalAttributes, new Mapper<String, String>(){
						String map(String input) {
							Matcher m = classGroupPattern.matcher(input);
							if(m.matches() && m.groupCount() >= 1){
								return m.group(1);
							}
							return input;
						}
					});
					addArray("ENTAuxEnsMEF", "modules", data, doc, additionalAttributes, new Mapper<String, String>(){
						String map(String input) {
							Matcher m = mefStatPattern.matcher(input);
							if(m.matches() && m.groupCount() >= 1){
								return m.group(1);
							}
							return input;
						}
					});
					break;
				case "Relative" :
					rootProfiles = createElement("ENTPersonProfils", doc);
					rootProfiles.appendChild(createTextElement("ENTPersonProfil", "National_TUT", doc));
					additionalAttributes.add(rootProfiles);
					additionalAttributes.add(createTextElement("ENTEleveMEF", "", doc));
					additionalAttributes.add(createTextElement("ENTEleveCodeEnseignements", "", doc));
					additionalAttributes.add(createTextElement("ENTEleveClasses", "", doc));
					additionalAttributes.add(createTextElement("ENTEleveGroupes", "", doc));
					additionalAttributes.add(createTextElement("ENTAuxEnsClassesMatieres", "", doc));
					additionalAttributes.add(createTextElement("ENTAuxEnsGroupes", "", doc));
					additionalAttributes.add(createTextElement("ENTAuxEnsClasses", "", doc));
					additionalAttributes.add(createTextElement("ENTAuxEnsMEFs", "", doc));
					break;
				case "Personnel" :
					rootProfiles = createElement("ENTPersonProfils", doc);
					rootProfiles.appendChild(createTextElement("ENTPersonProfil", "National_DOC", doc));
					additionalAttributes.add(rootProfiles);
					additionalAttributes.add(createTextElement("ENTEleveMEF", "", doc));
					additionalAttributes.add(createTextElement("ENTEleveCodeEnseignements", "", doc));
					additionalAttributes.add(createTextElement("ENTEleveClasses", "", doc));
					additionalAttributes.add(createTextElement("ENTEleveGroupes", "", doc));
					additionalAttributes.add(createTextElement("ENTAuxEnsClassesMatieres", "", doc));
					additionalAttributes.add(createTextElement("ENTAuxEnsGroupes", "", doc));
					additionalAttributes.add(createTextElement("ENTAuxEnsClasses", "", doc));
					additionalAttributes.add(createTextElement("ENTAuxEnsMEFs", "", doc));
					break;
			}

		} catch (Exception e) {
			log.error("Failed to transform User for KNE", e);
		}
	}

}
