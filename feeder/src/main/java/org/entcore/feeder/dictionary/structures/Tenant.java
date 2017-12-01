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

package org.entcore.feeder.dictionary.structures;

import org.entcore.common.neo4j.Neo4jUtils;
import org.entcore.feeder.exceptions.ValidationException;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.feeder.utils.TransactionHelper;
import org.entcore.feeder.utils.Validator;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class Tenant {

	public static final Validator validator = new Validator("dictionary/schema/Tenant.json");

	public static void createOrUpdate(JsonObject object, TransactionHelper transactionHelper)
			throws ValidationException {
		final String error = validator.validate(object);
		if (error != null) {
			throw new ValidationException(error);
		} else {
			String query =
						"MERGE (t:Tenant { externalId : {externalId}}) " +
						"ON CREATE SET t.id = {id} " +
						"WITH t " +
						"WHERE t.checksum IS NULL OR t.checksum <> {checksum} " +
						"SET " + Neo4jUtils.nodeSetPropertiesFromJson("t", object, "id") +
						"RETURN t.id as id ";
			transactionHelper.add(query, object);
		}
	}

	public static void count(TransactionHelper transactionHelper) {
		JsonObject params = new JsonObject();
		String query = "MATCH (t:Tenant) RETURN count(distinct t) as nb";
		transactionHelper.add(query, params);
	}

	public static void list(JsonArray attributes, Integer skip, Integer limit, TransactionHelper transactionHelper) {
		StringBuilder query = new StringBuilder("MATCH (t:Tenant) ");
		JsonObject params = new JsonObject();
		if (attributes != null && attributes.size() > 0) {
			query.append("RETURN DISTINCT");
			for (Object attribute : attributes) {
				query.append(" t.").append(attribute).append(" as ").append(attribute).append(",");
			}
			query.deleteCharAt(query.length() - 1);
			query.append(" ");
		} else {
			query.append("RETURN DISTINCT t ");
		}
		if (skip != null && limit != null) {
			query.append("ORDER BY externalId ASC " +
					"SKIP {skip} " +
					"LIMIT {limit} ");
			params.put("skip", skip);
			params.put("limit", limit);
		}
		transactionHelper.add(query.toString(), params);
	}

}
