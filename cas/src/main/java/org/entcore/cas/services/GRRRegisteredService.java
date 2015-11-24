package org.entcore.cas.services;

import fr.wseduc.cas.entities.User;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.logging.impl.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.List;

public class GRRRegisteredService extends AbstractCas20ExtensionRegisteredService {

	private static final Logger log = LoggerFactory.getLogger(GRRRegisteredService.class);


	@Override
	public void configure(org.vertx.java.core.eventbus.EventBus eb, java.util.Map<String,Object> conf) {
		super.configure(eb, conf);
	};

	@Override
	protected void prepareUserCas20(User user, String userId, String service, JsonObject data, Document doc, List<Element> additionnalAttributes) {
		user.setUser(data.getString(principalAttributeName));
		try {
			additionnalAttributes.add(createTextElement("user_nom_ldap", data.getString("lastName"), doc));
			additionnalAttributes.add(createTextElement("user_prenom_ldap", data.getString("firstName"), doc));
			additionnalAttributes.add(createTextElement("user_mail_ldap", data.getString("email"), doc));
			// Profile
			if (data.getArray("type") != null && data.getArray("type").size() > 0){
				additionnalAttributes.add(createTextElement("user_code_fonction_ldap", data.getArray("type").get(0).toString(), doc));
				additionnalAttributes.add(createTextElement("user_libelle_fonction_ldap", data.getArray("type").get(0).toString(), doc));
			}
			} catch (Exception e) {
			log.error("Failed to transform User for GRR CAS extension", e);
		}
	}

}
