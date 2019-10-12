/*
 * Copyright © "Open Digital Education", 2019
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
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */

package org.entcore.communication.services.impl;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import static org.entcore.common.neo4j.Neo4jResult.validResultHandler;

public class XpCommunicationService extends DefaultCommunicationService {

	@Override
	public void visibleUsers(String userId, String structureId, JsonArray expectedTypes, boolean itSelf,
			boolean myGroup, boolean profile, String preFilter, String customReturn, JsonObject additionnalParams, String userProfile,
			final Handler<Either<String, JsonArray>> handler) {
		StringBuilder query = new StringBuilder();
		JsonObject params = new JsonObject();
		String condition = itSelf ? "" : "AND m.id <> {userId} ";
		StringBuilder union = null;
		String conditionUnion = itSelf ? "" : "AND m.id <> {userId} ";
		if (structureId != null && !structureId.trim().isEmpty()) {
			query.append("MATCH (n:User)-[:COMMUNIQUE*1..3]->m-[:DEPENDS*1..2]->(s:Structure {id:{schoolId}})"); //TODO manage leaf
			params.put("schoolId", structureId);
		} else {
			String myGroupQuery = (myGroup) ? "COLLECT(CASE WHEN g.users = 'BOTH' THEN g.id ELSE '' END)" : "[]";
			query.append(" MATCH (n:User {id: {userId}})-[:COMMUNIQUE]->(g:Group) ");
			query.append("WITH (REDUCE(acc=[], groups IN COLLECT(COALESCE(g.communiqueWith, [])) | acc+groups) + ")
					.append(myGroupQuery).append(") as comGroups ");
			query.append("MATCH p=(g:Group)<-[:DEPENDS*0..1]-cg-[:COMMUNIQUE*0..1]->m ");
			if (userProfile == null || "Student".equals(userProfile) || "Relative".equals(userProfile)) {
				union = new StringBuilder("MATCH p=(n:User)-[:COMMUNIQUE_DIRECT]->m " +
						"WHERE n.id = {userId} AND (NOT(HAS(m.blocked)) OR m.blocked = false) ");
			}
		}
		query.append("WHERE  g.id IN comGroups " +
				"AND (length(p) < 1 OR (length(p) < 2 AND g.id <> cg.id) OR (length(p) < 2 AND m:User)) " +
				"AND (NOT(HAS(m.blocked)) OR m.blocked = false) ");
		if (preFilter != null) {
			query.append(preFilter);
			if (union != null) {
				union.append(preFilter);
				union.append(conditionUnion);
			}
		}
		query.append(condition);
		if (expectedTypes != null && expectedTypes.size() > 0) {
			query.append("AND (");
			StringBuilder types = new StringBuilder();
			for (Object o: expectedTypes) {
				if (!(o instanceof String)) continue;
				String t = (String) o;
				types.append(" OR m:").append(t);
			}
			query.append(types.substring(4)).append(") ");
			if (union != null) {
				union.append("AND (").append(types.substring(4)).append(") ");
			}
		}
		String pcr = " ";
		String pr = "";
		if (profile) {
			query.append("OPTIONAL MATCH m-[:IN*0..1]->pgp-[:DEPENDS*0..1]->(pg:ProfileGroup)-[:HAS_PROFILE]->(profile:Profile) ");
			pcr = ", profile ";
			pr = "profile.name as type, ";
			if (union != null) {
				union.append("OPTIONAL MATCH m-[:IN*0..1]->pgp-[:DEPENDS*0..1]->(pg:ProfileGroup)-[:HAS_PROFILE]->(profile:Profile) ");
			}
		}
		if (customReturn != null && !customReturn.trim().isEmpty()) {
			query.append("WITH DISTINCT m as visibles").append(pcr);
			query.append(customReturn);
			if (union != null) {
				union.append("WITH DISTINCT m as visibles").append(pcr);
				union.append(customReturn);
			}
		} else {
			query.append("RETURN distinct m.id as id, m.name as name, "
					+ "m.login as login, m.displayName as username, ").append(pr)
					.append("m.lastName as lastName, m.firstName as firstName, m.profiles as profiles "
							+ "ORDER BY name, username ");
			if (union != null) {
				union.append("RETURN distinct m.id as id, m.name as name, "
						+ "m.login as login, m.displayName as username, ").append(pr)
						.append("m.lastName as lastName, m.firstName as firstName, m.profiles as profiles "
								+ "ORDER BY name, username ");
			}
		}
		params.put("userId", userId);
		if (additionnalParams != null) {
			params.mergeIn(additionnalParams);
		}
		String q;
		if (union != null) {
			q = query.append(" union ").append(union.toString()).toString();
		} else {
			q = query.toString();
		}
		neo4j.execute(q, params, validResultHandler(handler));
	}

}
