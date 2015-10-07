package org.entcore.cas.services;

import java.net.URI;
import java.util.List;
import java.util.Map;

import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.logging.impl.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import fr.wseduc.cas.entities.User;

public class KneRegisteredService extends AbstractCas20ExtensionRegisteredService {

	private static final Logger log = LoggerFactory.getLogger(KneRegisteredService.class);

	private void addStringArray(String casLabel, String entLabel, JsonObject data, Document doc, List<Element> additionalAttributes){
		Element root = createElement(casLabel+"s", doc);
		if(data.containsField(entLabel)){
			for(Object item: data.getArray(entLabel)){
				root.appendChild(createTextElement(casLabel, (String) item, doc));
			}
		}
		additionalAttributes.add(root);
	}

	private void addObjectArrayProp(String casLabel, String entLabel, String objProperty, JsonObject data,
			Document doc, List<Element> additionalAttributes){
		addObjectArrayProp(casLabel, entLabel, objProperty, objProperty, data, doc, additionalAttributes);
	}
	private void addObjectArrayProp(String casLabel, String entLabel, String objProperty, String conditionProperty,
			JsonObject data, Document doc, List<Element> additionalAttributes){
		Element root = createElement(casLabel+"s", doc);
		if(data.containsField(entLabel)){
			for(Object item: data.getArray(entLabel)){
				JsonObject jsonItem = (JsonObject) item;
				if(jsonItem.containsField(conditionProperty))
					root.appendChild(createTextElement(casLabel, jsonItem.getString(objProperty), doc));
			}
		}
		additionalAttributes.add(root);
	}

	@Override
    public void configure(org.vertx.java.core.eventbus.EventBus eb, java.util.Map<String,Object> conf){
        super.configure(eb, conf);
        this.directoryAction = "getUserInfos";
    }

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
						for (Object o : data.getArray("structures", new JsonArray()).toList()) {
							@SuppressWarnings("unchecked")
							Map<String, Object> structure = ((Map<String, Object>) o);
							if(value.equals(structure.get("UAI").toString())){
								additionalAttributes.add(createTextElement("ENTStructureTypeStruct", structure.get("type").toString(), doc));
								break;
							}
						}
						break;
					}
				}
			}

			Element rootProfiles;
			switch(data.getString("type")) {
				case "Student" :
					rootProfiles = createElement("ENTPersonProfils", doc);
					rootProfiles.appendChild(createTextElement("ENTPersonProfil", "National_ELV", doc));
					additionalAttributes.add(rootProfiles);
					additionalAttributes.add(createTextElement("ENTEleveMEF", data.getString("module"), doc));
					addStringArray("ENTEleveCodeEnseignement", "fieldOfStudy", data, doc, additionalAttributes);
					addObjectArrayProp("ENTEleveClasse", "classes", "name", data, doc, additionalAttributes);
					addObjectArrayProp("ENTEleveGroupe", "groups", "name", "externalId",data, doc, additionalAttributes);
					additionalAttributes.add(createTextElement("ENTAuxEnsClassesMatieres", "", doc));
					additionalAttributes.add(createTextElement("ENTAuxEnsGroupes", "", doc));
					additionalAttributes.add(createTextElement("ENTAuxEnsClasses", "", doc));
					additionalAttributes.add(createTextElement("ENTAuxEnsMEFs", "", doc));
					break;
				case "Teacher" :
					rootProfiles = createElement("ENTPersonProfils", doc);
					rootProfiles.appendChild(createTextElement("ENTPersonProfil", "National_ENS", doc));
					rootProfiles.appendChild(createTextElement("ENTPersonProfil", "National_TUT", doc));
					additionalAttributes.add(rootProfiles);
					additionalAttributes.add(createTextElement("ENTEleveMEF", "", doc));
					additionalAttributes.add(createTextElement("ENTEleveCodeEnseignements", "", doc));
					additionalAttributes.add(createTextElement("ENTEleveClasses", "", doc));
					additionalAttributes.add(createTextElement("ENTEleveGroupes", "", doc));
					addStringArray("ENTAuxEnsClassesMatiere", "classesFieldOfStudy", data, doc, additionalAttributes);
					addObjectArrayProp("ENTAuxEnsGroupe", "groups", "name", "externalId", data, doc, additionalAttributes);
					addObjectArrayProp("ENTAuxEnsClasse", "classes", "name", data, doc, additionalAttributes);
					addStringArray("ENTAuxEnsMEF", "modules", data, doc, additionalAttributes);
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
