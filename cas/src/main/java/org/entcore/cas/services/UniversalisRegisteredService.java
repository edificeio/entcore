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

public class UniversalisRegisteredService extends AbstractCas20ExtensionRegisteredService {

	private static final Logger log = LoggerFactory.getLogger(UniversalisRegisteredService.class);

	protected static final String UNVS_ID = "uid";
	protected static final String UNVS_STRUCTURE_UAI = "ENTPersonStructRattachRNE";
	protected static final String UNVS_PROFILES = "ENTPersonProfils";
	protected static final String UNVS_LASTNAME = "Nom";
	protected static final String UNVS_FIRSTNAME = "Prenoms";

	@Override
	public void configure(org.vertx.java.core.eventbus.EventBus eb, java.util.Map<String,Object> conf) {
		super.configure(eb, conf);
		this.directoryAction = "getUserInfos";
	};

	@Override
	protected void prepareUserCas20(User user, String userId, JsonObject data, Document doc, List<Element> additionnalAttributes) {
		user.setUser(data.getString(principalAttributeName));

		try {
			// Uid
			if (data.containsField("externalId")) {
				additionnalAttributes.add(createTextElement(UNVS_ID, data.getString("externalId"), doc));
			}

			// Structures
			for (Object o : data.getArray("structures", new JsonArray()).toList()) {
				Map<String, Object> structure = ((Map<String, Object>) o);
				if (structure.containsKey("UAI")) {
					additionnalAttributes.add(createTextElement(UNVS_STRUCTURE_UAI, structure.get("UAI").toString(), doc));
				}
			}

			// Profile
			switch(data.getString("type")) {
			case "Student" :
				additionnalAttributes.add(createTextElement(UNVS_PROFILES, "National_1", doc));
				break;
			case "Teacher" :
				additionnalAttributes.add(createTextElement(UNVS_PROFILES, "National_3", doc));
				break;
			case "Relative" :
				additionnalAttributes.add(createTextElement(UNVS_PROFILES, "National_2", doc));
				break;
			case "Personnel" :
				additionnalAttributes.add(createTextElement(UNVS_PROFILES, "National_4", doc));
				break;
			}

			// Lastname
			if (data.containsField("lastName")) {
				additionnalAttributes.add(createTextElement(UNVS_LASTNAME, data.getString("lastName"), doc));
			}

			// Firstname
			if (data.containsField("firstName")) {
				additionnalAttributes.add(createTextElement(UNVS_FIRSTNAME, data.getString("firstName"), doc));
			}

		} catch (Exception e) {
			log.error("Failed to transform User for Universalis", e);
		}
	}

}
