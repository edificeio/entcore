/* Copyright © "Open Digital Education", 2014
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
 */

package org.entcore.feeder.dictionary.structures;

import org.entcore.common.neo4j.Neo4j;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;


public class GraphData {

	private static final ConcurrentMap<String, Structure> structures = new ConcurrentHashMap<>();
	private static final ConcurrentMap<String, Profile> profiles = new ConcurrentHashMap<>();
	private static final ConcurrentMap<String, Structure> structuresByUAI = new ConcurrentHashMap<>();
	private static final ConcurrentHashMap<String, String> externalIdMapping = new ConcurrentHashMap<>();

	static void loadData(final Neo4j neo4j, final Handler<Message<JsonObject>> handler) {
		String query =
				"MATCH (s:Structure) " +
				"OPTIONAL MATCH s<-[:DEPENDS]-(g:Group) " +
				"OPTIONAL MATCH s<-[:BELONGS]-(c:Class) " +
				"return s, collect(distinct g.externalId) as groups, collect(distinct c.externalId) as classes ";
		neo4j.execute(query, new JsonObject(), new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> message) {
				String query =
						"MATCH (p:Profile) " +
						"OPTIONAL MATCH p<-[:COMPOSE]-(f:Function) " +
						"return p, collect(distinct f.externalId) as functions ";
				final AtomicInteger count = new AtomicInteger(2);
				neo4j.execute(query, new JsonObject(), new Handler<Message<JsonObject>>() {
					@Override
					public void handle(Message<JsonObject> message) {
						JsonArray res = message.body().getJsonArray("result");
						if ("ok".equals(message.body().getString("status")) && res != null) {
							for (Object o : res) {
								if (!(o instanceof JsonObject)) continue;
								JsonObject r = (JsonObject) o;
								JsonObject p = r.getJsonObject("p", new JsonObject()).getJsonObject("data");
								profiles.putIfAbsent(p.getString("externalId"),
										new Profile(p, r.getJsonArray("functions")));
							}
						}
						if (handler != null && count.decrementAndGet() == 0) {
							handler.handle(message);
						}
					}
				});
				JsonArray res = message.body().getJsonArray("result");
				if ("ok".equals(message.body().getString("status")) && res != null) {
					for (Object o : res) {
						if (!(o instanceof JsonObject)) continue;
						JsonObject r = (JsonObject) o;
						JsonObject s = r.getJsonObject("s", new JsonObject()).getJsonObject("data");
						Structure structure = new Structure(s, r.getJsonArray("groups"), r.getJsonArray("classes"));
						String externalId = s.getString("externalId");
						structures.putIfAbsent(externalId, structure);
						String UAI = s.getString("UAI");
						if (UAI != null && !UAI.trim().isEmpty()) {
							structuresByUAI.putIfAbsent(UAI, structure);
						}
						JsonArray joinKeys = s.getJsonArray("joinKey");
						if (joinKeys != null && joinKeys.size() > 0) {
							for (Object key : joinKeys) {
								externalIdMapping.putIfAbsent(key.toString(), externalId);
							}
						}
					}
				}
				if (handler != null && count.decrementAndGet() == 0) {
					handler.handle(message);
				}
			}
		});
	}

	public static boolean isReady() {
		return structures.isEmpty() && profiles.isEmpty();
	}

	public static void clear() {
		structures.clear();
		profiles.clear();
		structuresByUAI.clear();
		externalIdMapping.clear();
	}

	public static ConcurrentMap<String, Profile> getProfiles() {
		return profiles;
	}

	public static ConcurrentMap<String, Structure> getStructures() {
		return structures;
	}

	public static ConcurrentMap<String, Structure> getStructuresByUAI() {
		return structuresByUAI;
	}

	public static ConcurrentHashMap<String, String> getExternalIdMapping() {
		return externalIdMapping;
	}

}
