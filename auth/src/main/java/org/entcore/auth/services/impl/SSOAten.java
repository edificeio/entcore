/*
 * Copyright © WebServices pour l'Éducation, 2015
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
			JsonArray uais = new JsonArray();
			JsonArray attachmentId = new JsonArray();
			JsonArray firstName = new JsonArray();
			JsonArray lastName = new JsonArray();
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
