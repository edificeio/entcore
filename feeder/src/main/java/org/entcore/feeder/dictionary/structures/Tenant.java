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

import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.Json;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.neo4j.Neo4jUtils;
import org.entcore.feeder.exceptions.TransactionException;
import org.entcore.feeder.exceptions.ValidationException;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.feeder.utils.TransactionHelper;
import org.entcore.feeder.utils.TransactionManager;
import org.entcore.feeder.utils.Validator;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import static fr.wseduc.webutils.Utils.isEmpty;

public class Tenant {

	public static final Validator validator = new Validator("dictionary/schema/Tenant.json");
	private static final Logger log = LoggerFactory.getLogger(Tenant.class);

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

	static void linkStructures() {
		final String query =
				"MATCH (t:Tenant) " +
				"WHERE has(t.linkRules) " +
				"RETURN t.id as id, t.linkRules as rules ";
		TransactionManager.getNeo4jHelper().execute(query, new JsonObject(), r -> {
			if ("ok".equals(r.body().getString("status"))) {
				final JsonArray t = r.body().getJsonArray("result");
				if (t != null) {
					final String baseLink = "MATCH (t:Tenant {id:{id}}), (s:Structure) ";
					final String mergeLink = "MERGE t<-[r:HAS_STRUCT]-s SET r.lastUpdated = {now} ";
					final long now = System.currentTimeMillis();
					final JsonObject params = new JsonObject().put("now", now);
					try {
						final TransactionHelper tx = TransactionManager.getTransaction();
						for (Object o : t) {
							final JsonObject p = params.copy();
							final String whereClause = calcWhere(o, p);
							if (whereClause != null) {
								tx.add(baseLink + whereClause + mergeLink, p);
							}
						}
						final String deleteOld =
								"MATCH (t:Tenant)<-[r:HAS_STRUCT]-(s:Structure) " +
								"WHERE r.lastUpdated <> {now} " +
								"DELETE r ";
						tx.add(deleteOld, params);
						// TODO add structure tenant uniqueness
						tx.commit(res -> {
							if (!"ok".equals(res.body().getString("status"))) {
								log.error("Error when commit tenant link structures transaction : " +
										res.body().getString("message"));
							}
						});
					} catch (TransactionException e) {
						log.error("Error opening transaction in link structures", e);
					}
				}
			} else {
				log.error("Error when link structures with tenant : " + r.body().getString("message"));
			}
		});
	}

	private static String calcWhere(Object o, JsonObject p) {
		if (!(o instanceof JsonObject)) return null;
		final String id = ((JsonObject) o).getString("id");
		if (isEmpty(id)) {
			return null;
		}
		p.put("id", id);
		final JsonArray rules = ((JsonObject) o).getJsonArray("rules");
		if (rules != null && !rules.isEmpty()) {
			final StringBuilder sb = new StringBuilder("WHERE ");
			for (Object o3 : rules) {
				if (!(o3 instanceof String)) continue;
				final JsonArray o2 = new JsonArray((String) o3);
				if (o2.size() < 2) continue;
				switch (o2.getString(0)) {
					case "source" :
						sb.append("s.source = {source} AND ");
						p.put("source", o2.getString(1));
						break;
					case "prefix" :
						sb.append("(s.externalId STARTS WITH {prefix} OR head(s.joinKey) STARTS WITH {prefix}) AND ");
						p.put("prefix", o2.getString(1));
						break;
					case "UAIRegex" :
						sb.append("s.UAI =~ {UAIRegex} AND ");
						p.put("UAIRegex", o2.getString(1));
						break;
					case "UAIList" :
						sb.append("s.UAI IN {UAIList} AND ");
						final JsonArray a = o2.copy();
						a.remove(0);
						p.put("UAIList", a);
						break;
				}
			}
			final int len = sb.length();
			if (len > 10) {
				return sb.substring(0, len - 4);
			}
		}
		return null;
	}

}
