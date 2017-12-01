/* Copyright © WebServices pour l'Éducation, 2014
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
				"OPTIONAL MATCH s<-[:DEPENDS]-(g:FunctionalGroup) " +
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
