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
import org.opensaml.saml2.core.Assertion;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;

public class SSOAten extends AbstractSSOProvider {

	@Override
	public void execute(Assertion assertion, Handler<Either<String, Object>> handler) {
		if (!validConditions(assertion, handler)) return;

		List<String> vectors = getAttributes(assertion, "FrEduVecteur");
		if (vectors == null || vectors.isEmpty()) {
			handler.handle(new Either.Left<String, Object>("invalid.vector"));
			return;
		}

		if (vectors.size() > 1) {
			JsonArray uais = new fr.wseduc.webutils.collections.JsonArray();
			JsonArray attachmentId = new fr.wseduc.webutils.collections.JsonArray();
			JsonArray firstName = new fr.wseduc.webutils.collections.JsonArray();
			JsonArray lastName = new fr.wseduc.webutils.collections.JsonArray();
			for (String vector : vectors) {
				String values[] = vector.split("\\|");
				if (values.length < 5 || values[3].trim().isEmpty() || values[4].trim().isEmpty() ||
						(!"1".equals(values[0]) && !"2".equals(values[0]))) {
					handler.handle(new Either.Left<String, Object>("invalid.vector"));
					return;
				}
				uais.add(values[4]);
				attachmentId.add(values[3]);
				firstName.add(values[2]);
				lastName.add(values[1]);
			}
			String query = "MATCH (student:User)-[:RELATED]->(u:User)-[:IN]->(:ProfileGroup)" +
					"-[:DEPENDS]->(s:Structure) " +
					"WHERE HEAD(u.profiles) = 'Relative' AND s.UAI IN {UAI} AND student.attachmentId IN {attachmentId} " +
					"AND u.firstName IN {firstName} AND u.lastName IN {lastName} AND NOT(HAS(u.mergedWith)) ";
			JsonObject params = new JsonObject()
					.put("attachmentId", attachmentId)
					.put("UAI", uais)
					.put("firstName", firstName)
					.put("lastName", lastName);
			executeMultiVectorQuery(query, params, assertion, handler);
		} else {
			String values[] = vectors.get(0).split("\\|");
			if (values.length > 4 && !values[3].trim().isEmpty() && !values[4].trim().isEmpty()) { // Eleve, PersRelEleve
				JsonObject params = new JsonObject()
						.put("attachmentId", values[3])
						.put("UAI", values[4]);
				String query;
				switch (values[0]) {
					case "1": // PersRelEleve 1d
					case "2": // PersRelEleve 2d
						query = "MATCH (:User {attachmentId: {attachmentId}})-[:RELATED]->(u:User)-[:IN]->(:ProfileGroup)" +
								"-[:DEPENDS]->(s:Structure) " +
								"WHERE HEAD(u.profiles) = 'Relative' AND s.UAI = {UAI} " +
								"AND u.firstName = {firstName} AND u.lastName = {lastName} ";
						params.put("firstName", values[2]).put("lastName", values[1]);
						break;
					case "3": // Eleve 1d
					case "4": // Eleve 2d
						query = "MATCH (u:User {attachmentId: {attachmentId}})-[:IN]->(:ProfileGroup)-[:DEPENDS]->(s:Structure) " +
								"WHERE HEAD(u.profiles) = 'Student' AND s.UAI = {UAI} ";
						break;
					default:
						handler.handle(new Either.Left<String, Object>("invalid.user.profile"));
						return;
				}
				executeQuery(query, params, assertion, handler);
			} else {
				handler.handle(new Either.Left<String, Object>("invalid.vector"));
			}
		}
	}

}
