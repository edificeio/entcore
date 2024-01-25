/*
 * Copyright © "Open Digital Education", 2023
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

import org.entcore.common.validation.StringValidation;
import org.opensaml.saml2.core.Assertion;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

import static fr.wseduc.webutils.Utils.isNotEmpty;
import static fr.wseduc.webutils.Utils.isEmpty;

public class SSOAzure extends AbstractSSOProvider {

	protected static final String EMAIL_ATTTRIBUTE = "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/emailaddress";
	protected static final String ENTPERSONJOINTURE_ATTTRIBUTE = "ENTPersonJointure";

	private final JsonObject issuerAcademiePrefix;

	public SSOAzure() {
		this(
			Vertx.currentContext().config().getJsonObject("azure-issuer-academy-prefix", new JsonObject())
		);
	}

	public SSOAzure(JsonObject issuerAcademiePrefix) {
		this.issuerAcademiePrefix = issuerAcademiePrefix;
	}

	@Override
	public void execute(Assertion assertion, Handler<Either<String, Object>> handler) {
		if (!validConditions(assertion, handler)) return;

		final String entPersonJointure = getAttribute(assertion, ENTPERSONJOINTURE_ATTTRIBUTE);
		if (isNotEmpty(entPersonJointure)) {
			final String externalId = getExternalId(assertion, entPersonJointure);
			executeQuery("MATCH (u:User {externalId:{joinKey}}) ", new JsonObject().put("joinKey", externalId),
						assertion, handler);
		} else {
			final String mail = getAttribute(assertion, EMAIL_ATTTRIBUTE);
			if (isEmpty(mail)) {
				handler.handle(new Either.Left<>("invalid.email"));
				return;
			}
			if (StringValidation.isEmail(mail)) {
				executeQuery("MATCH (u:User {emailAcademy:{email}}) ", new JsonObject().put("email", mail),
						assertion, handler);
			} else {
				handler.handle(new Either.Left<>("invalid.email"));
			}
		}
	}

	protected String getExternalId(Assertion assertion, String entPersonJointure) {
		if (issuerAcademiePrefix == null || issuerAcademiePrefix.isEmpty()) {
			return entPersonJointure;
		}
		return issuerAcademiePrefix.getString(assertion.getIssuer().getValue(), "") + entPersonJointure;
	}

}
