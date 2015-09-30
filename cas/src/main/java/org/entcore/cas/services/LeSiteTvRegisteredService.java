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

public class LeSiteTvRegisteredService extends AbstractCas20ExtensionRegisteredService {

	private static final Logger log = LoggerFactory.getLogger(LeSiteTvRegisteredService.class);

	protected static final String LSTV_ROOT = "FRANCE5";
	protected static final String LSTV_ID = "Identifiant";
	protected static final String LSTV_STRUCTURE_UAI = "CodeRNE";
	protected static final String LSTV_PROFILES = "Profil";
	protected static final String LSTV_LASTNAME = "Nom";
	protected static final String LSTV_FIRSTNAME = "Prenom";
	protected static final String LSTV_EMAIL = "Mail";

	@Override
	public void configure(org.vertx.java.core.eventbus.EventBus eb, java.util.Map<String,Object> conf) {
		super.configure(eb, conf);
		this.directoryAction = "getUserInfos";
	};

	@Override
	protected void prepareUserCas20(User user, String userId, String service, JsonObject data, Document doc, List<Element> additionnalAttributes) {
		user.setUser(data.getString(principalAttributeName));

		try {
			Element root = createElement(LSTV_ROOT, doc);

			// Uid
			if (data.containsField("externalId")) {
				root.appendChild(createTextElement(LSTV_ID, data.getString("externalId"), doc));
			}

			// Structures
			for (Object o : data.getArray("structures", new JsonArray()).toList()) {
				Map<String, Object> structure = ((Map<String, Object>) o);
				if (structure.containsKey("UAI")) {
					root.appendChild(createTextElement(LSTV_STRUCTURE_UAI, structure.get("UAI").toString(), doc));
				}
			}

			// Profile
			switch(data.getString("type")) {
			case "Student" :
				root.appendChild(createTextElement(LSTV_PROFILES, "National_1", doc));
				break;
			case "Teacher" :
				root.appendChild(createTextElement(LSTV_PROFILES, "National_3", doc));
				break;
			case "Relative" :
				root.appendChild(createTextElement(LSTV_PROFILES, "National_2", doc));
				break;
			case "Personnel" :
				root.appendChild(createTextElement(LSTV_PROFILES, "National_4", doc));
				break;
			}

			// Lastname
			if (data.containsField("lastName")) {
				root.appendChild(createTextElement(LSTV_LASTNAME, data.getString("lastName"), doc));
			}

			// Firstname
			if (data.containsField("firstName")) {
				root.appendChild(createTextElement(LSTV_FIRSTNAME, data.getString("firstName"), doc));
			}

			// Email
			if (data.containsField("email")) {
				root.appendChild(createTextElement(LSTV_EMAIL, data.getString("email"), doc));
			}

			additionnalAttributes.add(root);

		} catch (Exception e) {
			log.error("Failed to transform User for LeSite.tv", e);
		}
	}

}
