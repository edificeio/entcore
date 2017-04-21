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
import org.entcore.common.neo4j.Neo4jUtils;
import org.entcore.feeder.dictionary.users.AbstractUser;
import org.entcore.feeder.dictionary.users.PersEducNat;
import org.entcore.feeder.utils.*;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.logging.impl.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class Importer {

	private static final Logger log = LoggerFactory.getLogger(Importer.class);
	private ConcurrentMap<String, Structure> structures;
	private ConcurrentMap<String, Profile> profiles;
	private Set<String> userImportedExternalId = new HashSet<>();
	private TransactionHelper transactionHelper;
	private final Validator structureValidator;
	private final Validator profileValidator;
	private final Validator studyValidator;
	private final Validator moduleValidator;
	private final Validator userValidator;
	private final Validator studentValidator;
	private PersEducNat persEducNat;
	private boolean firstImport = false;
	private String currentSource;
	private Neo4j neo4j;
	private ConcurrentMap<String, Structure> structuresByUAI;
	private ConcurrentHashMap<String, String> externalIdMapping;
	private ConcurrentHashMap<String, List<String>> groupClasses = new ConcurrentHashMap<>();
	private ConcurrentMap<String, String> fieldOfStudy= new ConcurrentHashMap<>();
	private Report report;

	private Importer() {
		structureValidator = new Validator("dictionary/schema/Structure.json");
		profileValidator = new Validator("dictionary/schema/Profile.json");
		studyValidator = new Validator("dictionary/schema/FieldOfStudy.json");
		moduleValidator = new Validator("dictionary/schema/Module.json");
		userValidator = new Validator("dictionary/schema/User.json");

		studentValidator = new Validator("dictionary/schema/Student.json");
	}

	private static class StructuresHolder {
		private static final Importer instance = new Importer();
	}

	public static Importer getInstance() {
		return StructuresHolder.instance;
	}

	public void init(final Neo4j neo4j, final String source, String acceptLanguage, final Handler<Message<JsonObject>> handler) {
		this.neo4j = neo4j;
		this.currentSource = source;
		this.report = new Report(acceptLanguage);
		this.transactionHelper = new TransactionHelper(neo4j, 1000);
		GraphData.loadData(neo4j, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				firstImport = GraphData.getStructures().isEmpty();
				structures = GraphData.getStructures();
				structuresByUAI = GraphData.getStructuresByUAI();
				externalIdMapping = GraphData.getExternalIdMapping();
				profiles = GraphData.getProfiles();
				persEducNat = new PersEducNat(transactionHelper, externalIdMapping, userImportedExternalId, report, currentSource);
				if ("CSV".equals(source) && "ok".equals(event.body().getString("status"))) {
					loadFieldOfStudy(handler);
				} else {
					if (handler != null) {
						handler.handle(event);
					}
				}
			}
		});
	}

	private void loadFieldOfStudy(final Handler<Message<JsonObject>> handler) {
		Neo4j.getInstance().execute("MATCH (f:FieldOfStudy) return f.externalId as externalId, f.name as name", new JsonObject(),
				new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				final JsonArray res = event.body().getArray("result");
				if ("ok".equals(event.body().getString("status")) && res != null) {
					for (Object o : res) {
						if (!(o instanceof JsonObject)) continue;
						final JsonObject j = (JsonObject) o;
						fieldOfStudy.putIfAbsent(j.getString("name"), j.getString("externalId"));
					}
				}
				if (handler != null) {
					handler.handle(event);
				}
			}
		});
	}

	public TransactionHelper getTransaction() {
		return transactionHelper;
	}

	public void clear() {
		structures.clear();
		profiles.clear();
		userImportedExternalId.clear();
		groupClasses.clear();
		report = null;
		transactionHelper = null;
	}

	public boolean isReady() {
		return transactionHelper == null;
	}

	public void persist(final Handler<Message<JsonObject>> handler) {
		if (transactionHelper != null) {
			transactionHelper.commit(new Handler<Message<JsonObject>>() {
				@Override
				public void handle(Message<JsonObject> message) {
					transactionHelper = new TransactionHelper(neo4j, 1000);
					persEducNat.setTransactionHelper(transactionHelper);
					if (handler != null) {
						handler.handle(message);
					}
				}
			});
		}
		transactionHelper = null;
	}

	public void flush(Handler<Message<JsonObject>> handler) {
		if (transactionHelper != null) {
			transactionHelper.flush(handler);
		}
	}

	/**
	 * Warning : all data in old uncommitted transaction will be lost.
	 */
	public void reinitTransaction() {
		transactionHelper = new TransactionHelper(neo4j, 1000);
	}

	public Structure createOrUpdateStructure(JsonObject struct) {
		JsonArray groups = null;
		if (struct != null) {
			groups = struct.getArray("groups");
		}
		final String error = structureValidator.validate(struct);
		Structure s = null;
		if (error != null) {
			report.addIgnored("Structure", error, struct);
			log.warn(error);
		} else {
			final String externalId = struct.getString("externalId");
			if (groups != null) {
				for (Object gcMapping : groups) {
					if (!(gcMapping instanceof String)) continue;
					final String [] m = ((String) gcMapping).split("\\$");
					final String groupCode = m[0];
					if (groupCode == null || groupCode.isEmpty() || m.length < 3) continue;
					final List<String> classes = new LinkedList<>();
					for (int i = 2; i < m.length; i++) {
						classes.add(externalId + "$" + m[i]);
					}
					if (!classes.isEmpty()) {
						groupClasses.put(externalId + "$" + groupCode, classes);
					}
				}
			}
			s = structures.get(externalId);
			if (s != null) {
				s.update(struct);
			} else {
				String UAI = struct.getString("UAI");
				if (UAI != null) {
					s = structuresByUAI.get(UAI);
				}
				if (s != null) {
					structures.putIfAbsent(externalId, s);
					Object[] joinKeys = s.addJointure(externalId);
					if (joinKeys != null) {
						String origExternalId = s.getExternalId();
						for (Object key : joinKeys) {
							externalIdMapping.putIfAbsent(key.toString(), origExternalId);
						}
					}
				} else {
					try {
						s = new Structure(externalId, struct);
						structures.putIfAbsent(externalId, s);
						s.create();
					} catch (IllegalArgumentException e) {
						log.error(e.getMessage());
					}
				}
			}
		}
		return s;
	}

	public Profile createOrUpdateProfile(JsonObject profile) {
		final String error = profileValidator.validate(profile);
		Profile p = null;
		if (error != null) {
			report.addIgnored("Profile", error, profile);
			log.warn(error);
		} else {
			String externalId = profile.getString("externalId");
			p = profiles.get(externalId);
			if (p != null) {
				p.update(profile);
			} else {
				try {
					p = new Profile(externalId, profile);
					profiles.putIfAbsent(externalId, p);
					p.create();
				} catch (IllegalArgumentException e) {
					log.error(e.getMessage());
				}
			}
		}
		return p;
	}

	public void createOrUpdateFieldOfStudy(JsonObject object) {
		final String error = studyValidator.validate(object);
		if (error != null) {
			report.addIgnored("FielsOfStudy", error, object);
			log.warn(error);
		} else {
			String query;
			JsonObject params;
			if (!firstImport) {
				query =
						"MERGE (fos:FieldOfStudy { externalId : {externalId}}) " +
						"ON CREATE SET fos.id = {id} " +
						"WITH fos " +
						"WHERE fos.checksum IS NULL OR fos.checksum <> {checksum} " +
						"SET " + Neo4jUtils.nodeSetPropertiesFromJson("fos", object, "id", "externalId");
				params = object;
			} else {
				query = "CREATE (fos:FieldOfStudy {props}) ";
				params = new JsonObject().putObject("props", object);
			}
			transactionHelper.add(query, params);
		}
	}

	public void createOrUpdateModule(JsonObject object) {
		final String error = moduleValidator.validate(object);
		if (error != null) {
			report.addIgnored("Module", error, object);
			log.warn(error);
		} else {
			String query;
			JsonObject params;
			if (!firstImport) {
				query =
						"MERGE (m:Module { externalId : {externalId}}) " +
						"ON CREATE SET m.id = {id} " +
						"WITH m " +
						"WHERE m.checksum IS NULL OR m.checksum <> {checksum} " +
						"SET " + Neo4jUtils.nodeSetPropertiesFromJson("m", object, "id", "externalId");
				params = object;
			} else {
				query = "CREATE (m:Module {props}) ";
				params = new JsonObject().putObject("props", object);
			}
			transactionHelper.add(query, params);
		}
	}

	public void createOrUpdateUser(JsonObject object) {
		createOrUpdateUser(object, null);
	}

	public void createOrUpdateUser(JsonObject object, JsonArray linkStudent) {
		final String error = userValidator.validate(object);
		if (error != null) {
			report.addIgnored("Relative", error, object);
			log.warn(error);
		} else {
			object.putString("source", currentSource);
			userImportedExternalId.add(object.getString("externalId"));
			String query;
			JsonObject params;
			if (!firstImport) {
				query =
						"MERGE (u:User { externalId : {externalId}}) " +
						"ON CREATE SET u.id = {id}, u.login = {login}, u.activationCode = {activationCode}, " +
						"u.displayName = {displayName} " +
						"WITH u " +
						"WHERE u.checksum IS NULL OR u.checksum <> {checksum} " +
						"SET " + Neo4jUtils.nodeSetPropertiesFromJson("u", object,
								"id", "externalId", "login", "activationCode", "displayName", "email");
				params = object;
			} else {
				query = "CREATE (u:User {props}) ";
				params = new JsonObject().putObject("props", object);
			}
			transactionHelper.add(query, params);
			checkUpdateEmail(object);
			if (linkStudent != null && linkStudent.size() > 0) {
				String query2 =
						"START u0=node:node_auto_index(externalId={externalId}), " +
						"s=node:node_auto_index({studentExternalIds}) " +
						"MATCH u0-[:MERGED*0..1]->u " +
						"WHERE NOT(HAS(u.mergedWith)) " +
						"MERGE u<-[:RELATED]-s ";
				JsonObject p = new JsonObject()
						.putString("externalId", object.getString("externalId"))
						.putString("studentExternalIds",
								"externalId:" + Joiner.on(" OR externalId:").join(linkStudent));
				transactionHelper.add(query2, p);
			}
		}
	}

	public void createOrUpdateGuest(JsonObject object, String[][] linkClasses) {
		final String error = userValidator.validate(object);
		if (error != null) {
			report.addIgnored("Guest", error, object);
			log.warn(error);
		} else {
			object.putString("source", currentSource);
			final String externalId = object.getString("externalId");
			userImportedExternalId.add(externalId);
			String query;
			JsonObject params;
			if (!firstImport) {
				query =
						"MERGE (u:User { externalId : {externalId}}) " +
						"ON CREATE SET u.id = {id}, u.login = {login}, u.activationCode = {activationCode}, " +
						"u.displayName = {displayName} " +
						"WITH u " +
						"WHERE u.checksum IS NULL OR u.checksum <> {checksum} " +
						"SET " + Neo4jUtils.nodeSetPropertiesFromJson("u", object,
						"id", "externalId", "login", "activationCode", "displayName", "email");
				params = object;
			} else {
				query = "CREATE (u:User {props}) ";
				params = new JsonObject().putObject("props", object);
			}
			transactionHelper.add(query, params);
			checkUpdateEmail(object);
			JsonArray structures = getMappingStructures(object.getArray("structures"));
			if (externalId != null && structures != null && structures.size() > 0) {
				JsonObject p = new JsonObject().putString("userExternalId", externalId);
				String q1 = "MATCH (s:Structure)<-[:DEPENDS]-(g:ProfileGroup)-[:HAS_PROFILE]->(p:Profile), " +
							"(:User { externalId : {userExternalId}})-[:MERGED*0..1]->(u:User) " +
							"USING INDEX s:Structure(externalId) " +
							"USING INDEX p:Profile(externalId) " +
							"WHERE s.externalId IN {structuresAdmin} " +
							"AND p.externalId = {profileExternalId} AND NOT(HAS(u.mergedWith)) " +
							"MERGE u-[:IN]->g";
				p.putArray("structuresAdmin", structures)
						.putString("profileExternalId", DefaultProfiles.GUEST_PROFILE_EXTERNAL_ID);
				transactionHelper.add(q1, p);
				String qs =
						"MATCH (:User {externalId : {userExternalId}})-[r:IN|COMMUNIQUE]-(:Group)-[:DEPENDS]->(s:Structure) " +
						"WHERE NOT(s.externalId IN {structures}) AND (NOT(HAS(r.source)) OR r.source = {source}) " +
						"DELETE r";
				JsonObject ps = new JsonObject()
						.putString("userExternalId", externalId)
						.putString("source", currentSource)
						.putArray("structures", structures);
				transactionHelper.add(qs, ps);
			}
			if (externalId != null && linkClasses != null) {
				JsonArray classes = new JsonArray();
				for (String[] structClass : linkClasses) {
					if (structClass != null && structClass[0] != null && structClass[1] != null) {
						String q =
								"MATCH (s:Structure)<-[:BELONGS]-(c:Class)<-[:DEPENDS]-(g:ProfileGroup)" +
										"-[:DEPENDS]->(pg:ProfileGroup)-[:HAS_PROFILE]->(p:Profile), " +
										"(:User { externalId : {userExternalId}})-[:MERGED*0..1]->(u:User) " +
										"USING INDEX s:Structure(externalId) " +
										"USING INDEX p:Profile(externalId) " +
										"WHERE s.externalId = {structure} AND c.externalId = {class} " +
										"AND p.externalId = {profileExternalId} AND NOT(HAS(u.mergedWith)) " +
										"MERGE u-[:IN]->g";
						JsonObject p = new JsonObject()
								.putString("userExternalId", externalId)
								.putString("profileExternalId", DefaultProfiles.GUEST_PROFILE_EXTERNAL_ID)
								.putString("structure", structClass[0])
								.putString("class", structClass[1]);
						transactionHelper.add(q, p);
						classes.add(structClass[1]);
					}
				}
				String q =
						"MATCH (:User {externalId : {userExternalId}})-[r:IN|COMMUNIQUE]-(:Group)-[:DEPENDS]->(c:Class) " +
						"WHERE NOT(c.externalId IN {classes}) AND (NOT(HAS(r.source)) OR r.source = {source}) " +
						"DELETE r";
				JsonObject p = new JsonObject()
						.putString("userExternalId", externalId)
						.putString("source", currentSource)
						.putArray("classes", classes);
				transactionHelper.add(q, p);
			}
		}
	}

	private void checkUpdateEmail(JsonObject object) {
		AbstractUser.checkUpdateEmail(object, transactionHelper);
	}

	public JsonArray getMappingStructures(JsonArray structures) {
		return AbstractUser.getUserMappingStructures(structures, externalIdMapping);
	}

	public boolean isFirstImport() {
		return firstImport;
	}

	public void createOrUpdatePersonnel(JsonObject object, String profileExternalId, JsonArray structuresByFunctions,
			String[][] linkClasses, String[][] linkGroups, boolean nodeQueries, boolean relationshipQueries) {
		persEducNat.createOrUpdatePersonnel(object, profileExternalId, structuresByFunctions,
				linkClasses, linkGroups, nodeQueries, relationshipQueries);
	}

	public void createOrUpdateStudent(JsonObject object, String profileExternalId, String module, JsonArray fieldOfStudy,
			String[][] linkClasses, String[][] linkGroups, JsonArray relative, boolean nodeQueries,
			boolean relationshipQueries) {
		final String error = studentValidator.validate(object);
		if (error != null) {
			report.addIgnored("Student", error, object);
			log.warn(error);
		} else {
			if (nodeQueries) {
				object.putString("source", currentSource);
				userImportedExternalId.add(object.getString("externalId"));
				StringBuilder sb = new StringBuilder();
				JsonObject params;
				if (!firstImport) {
					sb.append("MERGE (u:`User` { externalId : {externalId}}) ");
					sb.append("ON CREATE SET u.id = {id}, u.login = {login}, u.activationCode = {activationCode}, ");
					sb.append("u.displayName = {displayName} ");
					sb.append("WITH u ");
					sb.append("WHERE u.checksum IS NULL OR u.checksum <> {checksum} ");
					sb.append("SET ").append(Neo4jUtils.nodeSetPropertiesFromJson("u", object,
							"id", "externalId", "login", "activationCode", "displayName", "email"));
					params = object;
				} else {
					sb.append("CREATE (u:User {props}) ");
					params = new JsonObject().putObject("props", object);
				}
				transactionHelper.add(sb.toString(), params);
				checkUpdateEmail(object);
			}
			if (relationshipQueries) {
				final String externalId = object.getString("externalId");
				JsonArray structures = getMappingStructures(object.getArray("structures"));
				if (externalId != null && structures != null && structures.size() > 0) {
					String query;
					JsonObject p = new JsonObject().putString("userExternalId", externalId);
					if (structures.size() == 1) {
						query = "MATCH (s:Structure)<-[:DEPENDS]-(g:ProfileGroup)-[:HAS_PROFILE]->(p:Profile), " +
								"(:User { externalId : {userExternalId}})-[:MERGED*0..1]->(u:User) " +
								"USING INDEX s:Structure(externalId) " +
								"USING INDEX p:Profile(externalId) " +
								"WHERE s.externalId = {structureAdmin} AND NOT(HAS(u.mergedWith)) " +
								"AND p.externalId = {profileExternalId} " +
								"MERGE u-[:ADMINISTRATIVE_ATTACHMENT]->s " +
								"WITH u, g " +
								"MERGE u-[:IN]->g";
						p.putString("structureAdmin", (String) structures.get(0))
								.putString("profileExternalId", profileExternalId);
					} else {
						query = "MATCH (s:Structure)<-[:DEPENDS]-(g:ProfileGroup)-[:HAS_PROFILE]->(p:Profile), " +
								"(:User { externalId : {userExternalId}})-[:MERGED*0..1]->(u:User) " +
								"USING INDEX s:Structure(externalId) " +
								"USING INDEX p:Profile(externalId) " +
								"WHERE s.externalId IN {structuresAdmin} AND NOT(HAS(u.mergedWith)) " +
								"AND p.externalId = {profileExternalId} " +
								"MERGE u-[:ADMINISTRATIVE_ATTACHMENT]->s " +
								"WITH u, g " +
								"MERGE u-[:IN]->g";
						p.putArray("structuresAdmin", structures)
								.putString("profileExternalId", profileExternalId);
					}
					transactionHelper.add(query, p);
					String qs =
							"MATCH (:User {externalId : {userExternalId}})-[r:IN|COMMUNIQUE]-(:Group)-[:DEPENDS]->(s:Structure) " +
							"WHERE NOT(s.externalId IN {structures}) AND (NOT(HAS(r.source)) OR r.source = {source}) " +
							"DELETE r";
					JsonObject ps = new JsonObject()
							.putString("userExternalId", externalId)
							.putString("source", currentSource)
							.putArray("structures", structures);
					transactionHelper.add(qs, ps);
				}
				if (externalId != null && linkClasses != null) {
					JsonArray classes = new JsonArray();
					for (String[] structClass : linkClasses) {
						if (structClass != null && structClass[0] != null && structClass[1] != null) {
							String query =
									"MATCH (s:Structure)<-[:BELONGS]-(c:Class)<-[:DEPENDS]-(g:ProfileGroup)" +
									"-[:DEPENDS]->(pg:ProfileGroup)-[:HAS_PROFILE]->(p:Profile), " +
									"(:User { externalId : {userExternalId}})-[:MERGED*0..1]->(u:User) " +
									"USING INDEX s:Structure(externalId) " +
									"USING INDEX p:Profile(externalId) " +
									"WHERE s.externalId = {structure} AND c.externalId = {class} " +
									"AND NOT(HAS(u.mergedWith))  AND p.externalId = {profileExternalId} " +
									"MERGE u-[:IN]->g";
							JsonObject p = new JsonObject()
									.putString("userExternalId", externalId)
									.putString("profileExternalId", profileExternalId)
									.putString("structure", structClass[0])
									.putString("class", structClass[1]);
							transactionHelper.add(query, p);
							classes.add(structClass[1]);
						}
					}
					String q =
							"MATCH (:User {externalId : {userExternalId}})-[r:IN|COMMUNIQUE]-(:Group)-[:DEPENDS]->(c:Class) " +
							"WHERE NOT(c.externalId IN {classes}) AND (NOT(HAS(r.source)) OR r.source = {source}) " +
							"DELETE r";
					JsonObject p = new JsonObject()
							.putString("userExternalId", externalId)
							.putString("source", currentSource)
							.putArray("classes", classes);
					transactionHelper.add(q, p);
				}
				final JsonArray groups = new JsonArray();
				if (externalId != null && linkGroups != null) {
					for (String[] structGroup : linkGroups) {
						if (structGroup != null && structGroup[0] != null && structGroup[1] != null) {
							String query =
									"MATCH (s:Structure)" +
									"<-[:DEPENDS]-(g:FunctionalGroup), " +
									"(:User { externalId : {userExternalId}})-[:MERGED*0..1]->(u:User) " +
									"USING INDEX s:Structure(externalId) " +
									"WHERE s.externalId = {structure} AND g.externalId = {group} " +
									"AND NOT(HAS(u.mergedWith)) " +
									"MERGE u-[:IN]->g";
							JsonObject p = new JsonObject()
									.putString("userExternalId", externalId)
									.putString("structure", structGroup[0])
									.putString("group", structGroup[1]);
							transactionHelper.add(query, p);
							groups.add(structGroup[1]);
						}
					}
				}
				if (externalId != null) {
					final String qdfg =
							"MATCH (:User {externalId : {userExternalId}})-[r:IN|COMMUNIQUE]-(g:FunctionalGroup) " +
							"WHERE NOT(g.externalId IN {groups}) AND (NOT(HAS(r.source)) OR r.source = {source}) " +
							"DELETE r";
					final JsonObject pdfg = new JsonObject()
							.putString("userExternalId", externalId)
							.putString("source", currentSource)
							.putArray("groups", groups);
					transactionHelper.add(qdfg, pdfg);
				}

				if (externalId != null && module != null) {
					String query =
							"START u=node:node_auto_index(externalId={userExternalId}), " +
							"m=node:node_auto_index(externalId={moduleStudent}) " +
							"CREATE UNIQUE u-[:FOLLOW]->m";
					JsonObject p = new JsonObject()
							.putString("userExternalId", externalId)
							.putString("moduleStudent", module);
					transactionHelper.add(query, p);
				}
				if (externalId != null && fieldOfStudy != null && fieldOfStudy.size() > 0) {
					for (Object o : fieldOfStudy) {
						if (!(o instanceof String)) continue;
						String query =
								"START u=node:node_auto_index(externalId={userExternalId}), " +
								"f=node:node_auto_index(externalId={fieldOfStudyStudent}) " +
								"CREATE UNIQUE u-[:COURSE]->f";
						JsonObject p = new JsonObject()
								.putString("userExternalId", externalId)
								.putString("fieldOfStudyStudent", (String) o);
						transactionHelper.add(query, p);
					}
				}
				if (externalId != null && relative != null && relative.size() > 0) {
					String query2 =
							"MATCH (:User {externalId:{userExternalId}})-[r:RELATED]->(p:User) " +
							"WHERE NOT(p.externalId IN {relatives}) " +
							"DELETE r ";
					JsonObject p2 = new JsonObject()
							.putString("userExternalId", externalId)
							.putArray("relatives", relative);
					transactionHelper.add(query2, p2);
					for (Object o : relative) {
						if (!(o instanceof String)) continue;
						String query =
								"START u=node:node_auto_index(externalId={userExternalId}), " +
								"r0=node:node_auto_index(externalId={user}) " +
								"MATCH r0-[:MERGED*0..1]->(r:User) " +
								"WHERE NOT(HAS(r.mergedWith)) " +
								"MERGE u-[:RELATED]->r " +
								"WITH r0, r, u " +
								"WHERE r0.id <> r.id " +
								"SET u.relative = coalesce(FILTER(eId IN u.relative WHERE eId <> (r.externalId + '$1$1$1$1$0')), []) + (r.externalId + '$1$1$1$1$0') ";
						JsonObject p = new JsonObject()
								.putString("userExternalId", externalId)
								.putString("user", (String) o);
						transactionHelper.add(query, p);
					}
				}
			}
		}
	}

	public void linkRelativeToStructure(String profileExternalId) {
		JsonObject j = new JsonObject().putString("profileExternalId", profileExternalId);
		String query =
				"MATCH (u:User)<-[:RELATED]-(s:User)-[:IN]->(scg:ProfileGroup)" +
				"-[:DEPENDS]->(c:Structure)<-[:DEPENDS]-(rcg:ProfileGroup)-[:HAS_PROFILE]->(p:Profile) " +
				"WHERE p.externalId = {profileExternalId} AND NOT((u)-[:IN]->(rcg)) " +
				"MERGE u-[:IN]->rcg";
		transactionHelper.add(query, j);
	}

	public void linkRelativeToClass(String profileExternalId) {
		JsonObject j = new JsonObject().putString("profileExternalId", profileExternalId);
		String query =
				"MATCH (u:User)<-[:RELATED]-(s:User)-[:IN]->(scg:ProfileGroup)" +
				"-[:DEPENDS]->(c:Class)<-[:DEPENDS]-(rcg:ProfileGroup)" +
				"-[:DEPENDS]->(pg:ProfileGroup)-[:HAS_PROFILE]->(p:Profile) " +
				"WHERE p.externalId = {profileExternalId} AND NOT((u)-[:IN]->(rcg)) " +
				"MERGE u-[:IN]->rcg";
		transactionHelper.add(query, j);
		String query2 =
				"MATCH (u:User)<-[:RELATED]-(:User)-[:IN]->(:ProfileGroup)-[:DEPENDS]->(c:Class) " +
				"WITH u, COLLECT(distinct c.id) as cIds " +
				"MATCH u-[r:IN|COMMUNIQUE]-(:Group)-[:DEPENDS]->(c:Class) " +
				"WHERE NOT(c.id IN cIds) " +
				"DELETE r";
		transactionHelper.add(query2, j);
		String query3 =
				"MATCH (u:User)<-[:RELATED]-(:User)-[:IN]->(:ProfileGroup)-[:DEPENDS]->(s:Structure) " +
				"WITH u, COLLECT(distinct s.id) as sIds " +
				"MATCH u-[r:IN|COMMUNIQUE]-(:Group)-[:DEPENDS]->(s:Structure) " +
				"WHERE NOT(s.id IN sIds) " +
				"DELETE r";
		transactionHelper.add(query3, j);
	}

	public void removeOldFunctionalGroup() {
		String query =
				"MATCH (g:FunctionalGroup)<-[:IN]-(:User) " +
				"WITH COLLECT(distinct g.id) as usedFunctionalGroup " +
				"MATCH (g:FunctionalGroup) " +
				"WHERE NOT(g.id IN usedFunctionalGroup) " +
				"OPTIONAL MATCH g-[r]-() " +
				"DELETE g, r ";
		transactionHelper.add(query, null);
		// prevent difference between relationships and properties
		String query2 =
				"MATCH (u:User) " +
				"WHERE NOT(HAS(u.deleteDate)) AND has(u.groups) AND LENGTH(u.groups) > 0 " +
				"AND NOT(u-[:IN]->(:FunctionalGroup)) " +
				"SET u.groups = [];";
		transactionHelper.add(query2, null);
		String query3 =
				"MATCH (u:User)-[:IN]->(g:FunctionalGroup) " +
				"WHERE has(u.groups) " +
				"WITH u, collect(g.externalId) as groups " +
				"SET u.groups = groups";
		transactionHelper.add(query3, null);
	}

	public void removeEmptyClasses() {
		String query =
				"MATCH (c:Class)<-[:DEPENDS]-(:Group)<-[:IN]-(:User) " +
				"WITH COLLECT(distinct c.id) as usedClasses " +
				"MATCH (c:Class)<-[r1:DEPENDS]-(g:Group) " +
				"WHERE NOT(c.id IN usedClasses) " +
				"OPTIONAL MATCH g-[r2]-(), c-[r3]-() " +
				"DELETE c, g, r1, r2, r3 ";
		transactionHelper.add(query, null);
		// prevent difference between relationships and properties
		String query2 =
				"MATCH (u:User)-[:IN]->(:ProfileGroup)-[:DEPENDS]->(c:Class) " +
				"WHERE has(u.classes) " +
				"WITH u, collect(c.externalId) as classes " +
				"SET u.classes = classes";
		transactionHelper.add(query2, null);
	}

	public void addRelativeProperties(String source) {
		String query =
				"MATCH (u:User {source: {source}})-[:RELATED]->(u2:User) " +
				"WHERE HEAD(u.profiles) = 'Student' AND NOT(HAS(u.relative)) " +
				"SET u.relative = coalesce(u.relative, []) + (u2.externalId + '$1$1$1$1$0')";
		transactionHelper.add(query, new JsonObject().putString("source", source));
	}

	public Structure getStructure(String externalId) {
		return structures.get(externalId);
	}

	public Profile getProfile(String externalId) {
		return profiles.get(externalId);
	}

	public void markMissingUsers(Handler<Void> handler) {
		markMissingUsers(null, handler);
	}

	public void markMissingUsers(String structureExternalId, final Handler<Void> handler) {
		markMissingUsers(structureExternalId, currentSource, userImportedExternalId, transactionHelper, handler);
	}

	public static void markMissingUsers(String structureExternalId, String currentSource,
			final Set<String> userImportedExternalId, final TransactionHelper transactionHelper,
			final Handler<Void> handler) {
		String query;
		JsonObject params = new JsonObject().putString("currentSource", currentSource);
		if (structureExternalId != null) {
			query = "MATCH (:Structure {externalId : {externalId}})<-[:DEPENDS]-(:ProfileGroup)<-[:IN]-(u:User) " +
					"WHERE u.source = {currentSource} " +
					"RETURN u.externalId as externalId";
			params.putString("externalId", structureExternalId);
		} else {
			query = "MATCH (u:User) WHERE u.source = {currentSource} RETURN u.externalId as externalId";
		}
		TransactionManager.getNeo4jHelper().execute(query, params, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> message) {
				JsonArray res = message.body().getArray("result");
				if ("ok".equals(message.body().getString("status")) && res != null) {
					Set<String> existingUser = new TreeSet<>();
					for (Object o : res) {
						if (!(o instanceof JsonObject)) continue;
						String externalId = ((JsonObject) o).getString("externalId");
						if (externalId != null) {
							existingUser.add(externalId);
						}
					}
					existingUser.removeAll(userImportedExternalId); // set difference
					String q = // mark missing users
							"START u=node:node_auto_index(externalId={externalId}) " +
									"WHERE NOT(HAS(u.disappearanceDate)) " +
									"SET u.disappearanceDate = {date} ";
					JsonObject p = new JsonObject()
							.putNumber("date", System.currentTimeMillis());
					for (String eId : existingUser) {
						transactionHelper.add(q, p.copy().putString("externalId", eId));
					}
					String q2 = // remove mark of imported users
							"START u=node:node_auto_index(externalId={externalId}) " +
									"WHERE HAS(u.disappearanceDate) " +
									"REMOVE u.disappearanceDate ";
					for (String eId : userImportedExternalId) {
						transactionHelper.add(q2, new JsonObject().putString("externalId", eId));
					}
				}
				handler.handle(null);
			}
		});
	}

	public void restorePreDeletedUsers() {
		restorePreDeletedUsers(currentSource, transactionHelper);
	}

	public static void restorePreDeletedUsers(String currentSource, TransactionHelper transactionHelper) {
		String query =
				"MATCH (u:User)-[:IN]->(:ProfileGroup)-[:DEPENDS]->(:Structure) " +
				"WHERE has(u.deleteDate) AND NOT(HAS(u.disappearanceDate)) AND u.source = {source} " +
				"REMOVE u.deleteDate " +
				"WITH u " +
				"MATCH (g:Group), u-[r:IN]->(:DeleteGroup), u-[r2:HAS_RELATIONSHIPS]->(b:Backup) " +
				"WHERE g.id IN b.IN_OUTGOING " +
				"CREATE UNIQUE u-[:IN]->g " +
				"DELETE r, r2, b";
		transactionHelper.add(query, new JsonObject().putString("source", currentSource));
	}

	public Report getReport() {
		return report;
	}

	public Set<String> getUserImportedExternalId() {
		return userImportedExternalId;
	}

	public ConcurrentHashMap<String, List<String>> getGroupClasses() {
		return groupClasses;
	}

	public PersEducNat getPersEducNat() {
		return persEducNat;
	}

	public ConcurrentMap<String, String> getFieldOfStudy() {
		return fieldOfStudy;
	}

}
