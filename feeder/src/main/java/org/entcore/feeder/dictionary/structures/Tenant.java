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
