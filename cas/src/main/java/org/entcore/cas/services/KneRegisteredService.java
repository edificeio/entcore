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
		Element root = createElement(entLabel+"s", doc);
		for(Object item: data.getArray(entLabel)){
			root.appendChild(createTextElement(casLabel, (String) item, doc));
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

			/*
			for (Object o : data.getArray("structures", new JsonArray()).toList()) {
				Map<String, Object> structure = ((Map<String, Object>) o);
				additionnalAttributes.add(createTextElement("ENTPersonStructRattachUAI", structure.get("UAI").toString(), doc));
				additionnalAttributes.add(createTextElement("ENTStructureTypeStruct", structure.get("type").toString(), doc));
			}
			*/

			switch(data.getString("type")) {
				case "Student" :
					additionalAttributes.add(createTextElement("ENTPersonProfil", "National_ELV", doc));
					additionalAttributes.add(createTextElement("ENTEleveMEF", data.getString("module"), doc));
					addStringArray("ENTEleveCodeEnseignement", "fieldOfStudy", data, doc, additionalAttributes);
					addStringArray("ENTEleveClasse", "classes", data, doc, additionalAttributes);
					addStringArray("ENTEleveGroupe", "groups", data, doc, additionalAttributes);
					additionalAttributes.add(createTextElement("ENTAuxEnsClassesMatieres", "", doc));
					additionalAttributes.add(createTextElement("ENTAuxEnsGroupes", "", doc));
					additionalAttributes.add(createTextElement("ENTAuxEnsClasses", "", doc));
					additionalAttributes.add(createTextElement("ENTAuxEnsMEFs", "", doc));
					break;
				case "Teacher" :
					Element root = createElement("ENTPersonProfils", doc);
					root.appendChild(createTextElement("ENTPersonProfil", "National_ENS", doc));
					root.appendChild(createTextElement("ENTPersonProfil", "National_TUT", doc));
					additionalAttributes.add(root);
					additionalAttributes.add(createTextElement("ENTEleveMEF", "", doc));
					additionalAttributes.add(createTextElement("ENTEleveCodeEnseignements", "", doc));
					additionalAttributes.add(createTextElement("ENTEleveClasses", "", doc));
					additionalAttributes.add(createTextElement("ENTEleveGroupes", "", doc));
					addStringArray("ENTAuxEnsClassesMatiere", "classesFieldOfStudy", data, doc, additionalAttributes);
					addStringArray("ENTAuxEnsGroupe", "groups", data, doc, additionalAttributes);
					addStringArray("ENTAuxEnsClasse", "classes", data, doc, additionalAttributes);
					addStringArray("ENTAuxEnsMEF", "modules", data, doc, additionalAttributes);
					break;
				case "Relative" :
					additionalAttributes.add(createTextElement("ENTPersonProfil", "National_TUT", doc));
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
					additionalAttributes.add(createTextElement("ENTPersonProfil", "National_DOC", doc));
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
