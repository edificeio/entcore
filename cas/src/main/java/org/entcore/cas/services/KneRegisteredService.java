package org.entcore.cas.services;

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

	protected static final String KNE_ROOT = "KNE";
	protected static final String KNE_UID = "uid";
	protected static final String KNE_STUDENT_CLASSES = "ENTEleveClasses";
	protected static final String KNE_STUDENT_LEVEL = "ENTEleveNivFormation";
	protected static final String KNE_STRUCTURE_UAI = "ENTPersonStructRattachRNE";
	protected static final String KNE_PROFILES = "ENTPersonProfils";

	@Override
	public void configure(org.vertx.java.core.eventbus.EventBus eb, java.util.Map<String,Object> conf) {
		super.configure(eb, conf);
		this.directoryAction = "getUserInfos";
	};

	@Override
	protected void prepareUserCas20(User user, String userId, JsonObject data, Document doc, List<Element> additionnalAttributes) {
		user.setUser(data.getString(principalAttributeName));

		try {
			Element root = createElement(KNE_ROOT, doc);

			// Uid
			if (data.containsField("externalId")) {
				root.appendChild(createTextElement(KNE_UID, data.getString("externalId"), doc));
			}

			// Structures
			for (Object o : data.getArray("structures", new JsonArray()).toList()) {
				Map<String, Object> structure = ((Map<String, Object>) o);
				if (structure.containsKey("UAI")) {
					root.appendChild(createTextElement(KNE_STRUCTURE_UAI, structure.get("UAI").toString(), doc));
				}
			}

			// Profile
			switch(data.getString("type")) {
			case "Student" :
				root.appendChild(createTextElement(KNE_PROFILES, "National_1", doc));

				// Student : Classes
				for (Object o : data.getArray("classes", new JsonArray()).toList()) {
					Map<String, Object> classe = ((Map<String, Object>) o);
					if (classe.containsKey("name")) {
						root.appendChild(createTextElement(KNE_STUDENT_CLASSES, classe.get("name").toString(), doc));
					}
				}

				// Student : Level
				if (data.containsField("level")) {
					root.appendChild(createTextElement(KNE_STUDENT_LEVEL, data.getString("level"), doc));
				}

				break;
			case "Teacher" :
				root.appendChild(createTextElement(KNE_PROFILES, "National_3", doc));
				break;
			case "Relative" :
				root.appendChild(createTextElement(KNE_PROFILES, "National_2", doc));
				break;
			case "Personnel" :
				root.appendChild(createTextElement(KNE_PROFILES, "National_4", doc));
				break;
			}

			additionnalAttributes.add(root);

		} catch (Exception e) {
			log.error("Failed to transform User for KNE", e);
		}
	}

}
