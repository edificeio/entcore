/*
 * Copyright © "Open Digital Education", 2014
 *
 * This program is published by "Open Digital Education".
 * You must indicate the name of the software and the company in any production /contribution
 * using the software and indicate on the home page of the software industry in question,
 * "powered by Open Digital Education" with a reference to the website: https://opendigitaleducation.com/.
 *
 * This program is free software, licensed under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation, version 3 of the License.
 *
 * You can redistribute this application and/or modify it since you respect the terms of the GNU Affero General Public License.
 * If you modify the source code and then use this modified source code in your creation, you must make available the source code of your modifications.
 *
 * You should have received a copy of the GNU Affero General Public License along with the software.
 * If not, please see : <http://www.gnu.org/licenses/>. Full compliance requires reading the terms of this license and following its directives.

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
