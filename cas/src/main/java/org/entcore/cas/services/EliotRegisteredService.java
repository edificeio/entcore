/*
 * Copyright © WebServices pour l'Éducation, 2014
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

import fr.wseduc.cas.entities.ServiceTicket;
import fr.wseduc.cas.entities.User;

import io.vertx.core.json.JsonObject;

public class EliotRegisteredService extends DefaultRegisteredService {

	@Override
	public String formatService(String serviceUri, ServiceTicket st) {
		if (st.getService() != null && st.getService()
					.replaceFirst("autoLoginTicketSession/getCasTicket/", "")
					.replaceFirst("autoLoginTicket/getCasTicket/", "")
					.replaceFirst("&hostCAS=.*$", "")
					.replaceFirst("&rne.*$", "").equals(serviceUri)) {
			return st.getService();
		}
		return serviceUri;
	}

	@Override
	protected void prepareUser(final User user, final String userId, String service, final JsonObject data) {
		if (principalAttributeName != null) {
			user.setUser(data.getString(principalAttributeName));
			data.remove(principalAttributeName);
		} else {
			user.setUser(userId);
		}
	}

}
