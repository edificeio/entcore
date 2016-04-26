package org.entcore.cas.services;

import fr.wseduc.cas.entities.User;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.logging.impl.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class LabomepRegisteredService extends AbstractCas20ExtensionRegisteredService {

	private static final Logger log = LoggerFactory.getLogger(LabomepRegisteredService.class);

	@Override
	public void configure(org.vertx.java.core.eventbus.EventBus eb, Map<String,Object> conf) {
		super.configure(eb, conf);
		this.directoryAction = "getUserInfos";
	};

	@Override
	protected void prepareUserCas20(User user, String userId, String service, JsonObject data, Document doc, List<Element> additionnalAttributes) {
		user.setUser(data.getString(principalAttributeName));

		try {
			// Uid
			if (data.containsField("externalId")) {
				additionnalAttributes.add(createTextElement("uid", data.getString("externalId"), doc));
			}

			// Structures
			Element structures = createElement("structures", doc);
			for (Object o : data.getArray("structures", new JsonArray())) {
				JsonObject structure = (JsonObject) o;
				if (structure.getString("UAI") != null) {
					structures.appendChild(createTextElement("structure", structure.getString("UAI").toString(), doc));
				}
			}
			additionnalAttributes.add(structures);

			// classes
			Element classes = createElement("classes", doc);
			for (Object o : data.getArray("classes", new JsonArray())) {
				JsonObject classe = (JsonObject) o;
				classes.appendChild(createTextElement("classe", classe.getString("name"), doc));
			}
			additionnalAttributes.add(classes);

			// Profile
			switch(data.getString("type")) {
				case "Student" :
					additionnalAttributes.add(createTextElement("profile", "National_1", doc));
					break;
				case "Teacher" :
					additionnalAttributes.add(createTextElement("profile", "National_3", doc));
					break;
				case "Relative" :
					additionnalAttributes.add(createTextElement("profile", "National_2", doc));
					break;
				case "Personnel" :
					additionnalAttributes.add(createTextElement("profile", "National_4", doc));
					break;
			}

			// Lastname
			if (data.containsField("lastName")) {
				additionnalAttributes.add(createTextElement("nom", data.getString("lastName"), doc));
			}

			// Firstname
			if (data.containsField("firstName")) {
				additionnalAttributes.add(createTextElement("prenom", data.getString("firstName"), doc));
			}

			// Email
			if (data.containsField("email")) {
				additionnalAttributes.add(createTextElement("email", data.getString("email"), doc));
			}

		} catch (Exception e) {
			log.error("Failed to extract user's attributes for Labomep", e);
		}
	}

}
