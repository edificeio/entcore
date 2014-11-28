package org.entcore.cas.services;

import java.util.List;

import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.logging.impl.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import fr.wseduc.cas.entities.User;

public class UidRegisteredService extends AbstractCas20ExtensionRegisteredService {

	private static final Logger log = LoggerFactory.getLogger(UidRegisteredService.class);

	protected static final String UID = "uid";

	@Override
	public void configure(org.vertx.java.core.eventbus.EventBus eb, java.util.Map<String,Object> conf) {
		super.configure(eb, conf);
	};

	@Override
	protected void prepareUserCas20(User user, String userId, JsonObject data, Document doc, List<Element> additionnalAttributes) {
		user.setUser(data.getString(principalAttributeName));

		try {
			// Uid
			if (data.containsField("externalId")) {
				additionnalAttributes.add(createTextElement(UID, data.getString("externalId"), doc));
			}

		} catch (Exception e) {
			log.error("Failed to transform User for Uid service", e);
		}
	}

}
