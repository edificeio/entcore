/*
 * Copyright © "Open Digital Education", 2015
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

package org.entcore.auth.services.impl;

import fr.wseduc.webutils.Either;
import org.entcore.common.validation.StringValidation;
import org.opensaml.saml2.core.Assertion;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;

public class SSOAgents extends AbstractSSOProvider {

	@Override
	public void execute(Assertion assertion, Handler<Either<String, Object>> handler) {
		if (!validConditions(assertion, handler)) return;

		String mail = getAttribute(assertion, "mail");
		if (mail == null) {
			mail = getAttribute(assertion, "ctemail");
			if (mail == null) {
				handler.handle(new Either.Left<String, Object>("invalid.email"));
				return;
			}
		}

		if (StringValidation.isEmail(mail)) { // PersEducNat
			executeQuery("MATCH (u:User {emailAcademy:{email}}) ", new JsonObject().put("email", mail),
					assertion, handler);
		} else {
			handler.handle(new Either.Left<String, Object>("invalid.email"));
		}
	}

}
