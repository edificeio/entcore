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

import org.entcore.feeder.utils.Joiner;
import org.entcore.feeder.utils.Neo4j;
import org.entcore.feeder.utils.TransactionHelper;
import org.entcore.feeder.utils.Validator;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.logging.impl.LoggerFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
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
	private final Validator personnelValidator;
	private final Validator studentValidator;
	private boolean firstImport = false;
	private Neo4j neo4j;

	private Importer() {
		structureValidator = new Validator("dictionary/schema/Structure.json");
		profileValidator = new Validator("dictionary/schema/Profile.json");
		studyValidator = new Validator("dictionary/schema/FieldOfStudy.json");
		moduleValidator = new Validator("dictionary/schema/Module.json");
		userValidator = new Validator("dictionary/schema/User.json");
		personnelValidator = new Validator("dictionary/schema/Personnel.json");
		studentValidator = new Validator("dictionary/schema/Student.json");
	}

	private static class StructuresHolder {
		private static final Importer instance = new Importer();
	}

	public static Importer getInstance() {
		return StructuresHolder.instance;
	}

	public void init(final Neo4j neo4j, final Handler<Message<JsonObject>> handler) {
		this.neo4j = neo4j;
		this.transactionHelper = new TransactionHelper(neo4j, 1000);
		GraphData.loadData(neo4j, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				firstImport = GraphData.getStructures().isEmpty();
				structures = GraphData.getStructures();
				profiles = GraphData.getProfiles();
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
		final String error = structureValidator.validate(struct);
		Structure s = null;
		if (error != null) {
			log.warn(error);
		} else {
			String externalId = struct.getString("externalId");
			s = structures.get(externalId);
			if (s != null) {
				s.update(struct);
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
		return s;
	}

	public Profile createOrUpdateProfile(JsonObject profile) {
		final String error = profileValidator.validate(profile);
		Profile p = null;
		if (error != null) {
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
						"SET " + Neo4j.nodeSetPropertiesFromJson("fos", object, "id", "externalId");
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
						"SET " + Neo4j.nodeSetPropertiesFromJson("m", object, "id", "externalId");
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
			log.warn(error);
		} else {
			userImportedExternalId.add(object.getString("externalId"));
			String query;
			JsonObject params;
			if (!firstImport) {
				query =
						"MERGE (u:User { externalId : {externalId}}) " +
						"ON CREATE SET u.id = {id}, u.login = {login}, u.activationCode = {activationCode} " +
						"WITH u " +
						"WHERE u.checksum IS NULL OR u.checksum <> {checksum} " +
						"SET " + Neo4j.nodeSetPropertiesFromJson("u", object,
								"id", "externalId", "login", "activationCode");
				params = object;
			} else {
				query = "CREATE (u:User {props}) ";
				params = new JsonObject().putObject("props", object);
			}
			transactionHelper.add(query, params);
			if (linkStudent != null && linkStudent.size() > 0) {
				String query2 =
						"START u=node:node_auto_index(externalId={externalId}), " +
						"s=node:node_auto_index({studentExternalIds}) " +
						"MERGE u<-[:RELATED]-s ";
				JsonObject p = new JsonObject()
						.putString("externalId", object.getString("externalId"))
						.putString("studentExternalIds",
								"externalId:" + Joiner.on(" OR externalId:").join(linkStudent));
				transactionHelper.add(query2, p);
			}
		}
	}


	public boolean isFirstImport() {
		return firstImport;
	}

	public void structureConstraints() {
		JsonObject j = new JsonObject();
		transactionHelper.add("CREATE CONSTRAINT ON (structure:Structure) ASSERT structure.id IS UNIQUE;", j);
		transactionHelper.add("CREATE CONSTRAINT ON (structure:Structure) ASSERT structure.externalId IS UNIQUE;", j);
		transactionHelper.add("CREATE CONSTRAINT ON (structure:Structure) ASSERT structure.UAI IS UNIQUE;", j);
	}

	public void fieldOfStudyConstraints() {
		JsonObject j = new JsonObject();
		transactionHelper.add("CREATE CONSTRAINT ON (fos:FieldOfStudy) ASSERT fos.id IS UNIQUE;", j);
		transactionHelper.add("CREATE CONSTRAINT ON (fos:FieldOfStudy) ASSERT fos.externalId IS UNIQUE;", j);
	}

	public void moduleConstraints() {
		JsonObject j = new JsonObject();
		transactionHelper.add("CREATE CONSTRAINT ON (module:Module) ASSERT module.id IS UNIQUE;", j);
		transactionHelper.add("CREATE CONSTRAINT ON (module:Module) ASSERT module.externalId IS UNIQUE;", j);
	}

	public void userConstraints() {
		JsonObject j = new JsonObject();
		transactionHelper.add("CREATE CONSTRAINT ON (user:User) ASSERT user.id IS UNIQUE;", j);
		transactionHelper.add("CREATE CONSTRAINT ON (user:User) ASSERT user.externalId IS UNIQUE;", j);
		transactionHelper.add("CREATE CONSTRAINT ON (user:User) ASSERT user.login IS UNIQUE;", j);
	}

	public void profileConstraints() {
		JsonObject j = new JsonObject();
		transactionHelper.add("CREATE CONSTRAINT ON (profile:Profile) ASSERT profile.id IS UNIQUE;", j);
		transactionHelper.add("CREATE CONSTRAINT ON (profile:Profile) ASSERT profile.externalId IS UNIQUE;", j);
		transactionHelper.add("CREATE CONSTRAINT ON (profile:Profile) ASSERT profile.name IS UNIQUE;", j);
	}

	public void functionConstraints() {
		JsonObject j = new JsonObject();
		transactionHelper.add("CREATE CONSTRAINT ON (function:Function) ASSERT function.id IS UNIQUE;", j);
		transactionHelper.add("CREATE CONSTRAINT ON (function:Function) ASSERT function.externalId IS UNIQUE;", j);
		transactionHelper.add("CREATE CONSTRAINT ON (function:Functions) ASSERT function.id IS UNIQUE;", j);
		transactionHelper.add("CREATE CONSTRAINT ON (function:Functions) ASSERT function.externalId IS UNIQUE;", j);
	}

	public void classConstraints() {
		JsonObject j = new JsonObject();
		transactionHelper.add("CREATE CONSTRAINT ON (class:Class) ASSERT class.id IS UNIQUE;", j);
		transactionHelper.add("CREATE CONSTRAINT ON (class:Class) ASSERT class.externalId IS UNIQUE;", j);
	}

	public void groupConstraints() {
		JsonObject j = new JsonObject();
		transactionHelper.add("CREATE CONSTRAINT ON (group:Group) ASSERT group.id IS UNIQUE;", j);
		transactionHelper.add("CREATE CONSTRAINT ON (group:Group) ASSERT group.externalId IS UNIQUE;", j);
	}

	public void createOrUpdatePersonnel(JsonObject object, String profileExternalId, String[][] linkClasses,
			String[][] linkGroups, boolean nodeQueries, boolean relationshipQueries) {
		final String error = personnelValidator.validate(object);
		if (error != null) {
			log.warn(error);
		} else {
			if (nodeQueries) {
				userImportedExternalId.add(object.getString("externalId"));
				StringBuilder sb = new StringBuilder();
				JsonObject params;
				if (!firstImport) {
					sb.append("MERGE (u:`User` { externalId : {externalId}}) ");
					sb.append("ON CREATE SET u.id = {id}, u.login = {login}, u.activationCode = {activationCode} ");
					sb.append("WITH u ");
					sb.append("WHERE u.checksum IS NULL OR u.checksum <> {checksum} ");
					sb.append("SET ").append(Neo4j.nodeSetPropertiesFromJson("u", object,
							"id", "externalId", "login", "activationCode"));
					params = object;
				} else {
					sb.append("CREATE (u:User {props}) ");
					params = new JsonObject().putObject("props", object);
				}
				transactionHelper.add(sb.toString(), params);
			}
			if (relationshipQueries) {
				final String externalId = object.getString("externalId");
				JsonArray structures = object.getArray("structures");
				if (externalId != null && structures != null && structures.size() > 0) {
					String query;
					JsonObject p = new JsonObject().putString("userExternalId", externalId);
					if (structures.size() == 1) {
						query = "MATCH (s:Structure)<-[:DEPENDS]-(g:ProfileGroup)-[:HAS_PROFILE]->(p:Profile), " +
								"(u:User) " +
								"USING INDEX s:Structure(externalId) " +
								"USING INDEX u:User(externalId) " +
								"USING INDEX p:Profile(externalId) " +
								"WHERE s.externalId = {structureAdmin} AND u.externalId = {userExternalId} " +
								"AND p.externalId = {profileExternalId} " +
								"MERGE u-[:ADMINISTRATIVE_ATTACHMENT]->s " +
								"WITH u, g " +
								"MERGE u-[:IN]->g";
						p.putString("structureAdmin", (String) structures.get(0))
								.putString("profileExternalId", profileExternalId);
					} else {
						query = "MATCH (s:Structure)<-[:DEPENDS]-(g:ProfileGroup)-[:HAS_PROFILE]->(p:Profile), " +
								"(u:User) " +
								"USING INDEX s:Structure(externalId) " +
								"USING INDEX u:User(externalId) " +
								"USING INDEX p:Profile(externalId) " +
								"WHERE s.externalId IN {structuresAdmin} AND u.externalId = {userExternalId} " +
								"AND p.externalId = {profileExternalId} " +
								"MERGE u-[:ADMINISTRATIVE_ATTACHMENT]->s " +
								"WITH u, g " +
								"MERGE u-[:IN]->g";
						p.putArray("structuresAdmin", structures)
								.putString("profileExternalId", profileExternalId);
					}
					transactionHelper.add(query, p);
				}
				if (externalId != null && linkClasses != null) {
					for (String[] structClass : linkClasses) {
						if (structClass != null && structClass[0] != null && structClass[1] != null) {
							String query =
									"MATCH (s:Structure)<-[:BELONGS]-(c:Class)<-[:DEPENDS]-(g:ProfileGroup)" +
									"-[:DEPENDS]->(pg:ProfileGroup)-[:HAS_PROFILE]->(p:Profile), (u:User) " +
									"USING INDEX s:Structure(externalId) " +
									"USING INDEX u:User(externalId) " +
									"USING INDEX p:Profile(externalId) " +
									"WHERE s.externalId = {structure} AND c.externalId = {class} " +
									"AND u.externalId = {userExternalId} AND p.externalId = {profileExternalId} " +
									"CREATE UNIQUE u-[:IN]->g";
							JsonObject p = new JsonObject()
									.putString("userExternalId", externalId)
									.putString("profileExternalId", profileExternalId)
									.putString("structure", structClass[0])
									.putString("class", structClass[1]);
							transactionHelper.add(query, p);
						}
					}
				}
				if (externalId != null && linkGroups != null) {
					for (String[] structGroup : linkGroups) {
						if (structGroup != null && structGroup[0] != null && structGroup[1] != null) {
							String query =
									"MATCH (s:Structure)" +
									"<-[:DEPENDS]-(g:FunctionalGroup), " +
									"(u:User) " +
									"USING INDEX s:Structure(externalId) " +
									"USING INDEX u:User(externalId) " +
									"WHERE s.externalId = {structure} AND g.externalId = {group} AND u.externalId = {userExternalId} " +
									"CREATE UNIQUE u-[:IN]->g";
							JsonObject p = new JsonObject()
									.putString("userExternalId", externalId)
									.putString("structure", structGroup[0])
									.putString("group", structGroup[1]);
							transactionHelper.add(query, p);
						}
					}
				}
			}
		}
	}

	public void createOrUpdateStudent(JsonObject object, String profileExternalId, String module, JsonArray fieldOfStudy,
			String[][] linkClasses, String[][] linkGroups, JsonArray relative, boolean nodeQueries,
			boolean relationshipQueries) {
		final String error = studentValidator.validate(object);
		if (error != null) {
			log.warn(error);
		} else {
			if (nodeQueries) {
				userImportedExternalId.add(object.getString("externalId"));
				StringBuilder sb = new StringBuilder();
				JsonObject params;
				if (!firstImport) {
					sb.append("MERGE (u:`User` { externalId : {externalId}}) ");
					sb.append("ON CREATE SET u.id = {id}, u.login = {login}, u.activationCode = {activationCode} ");
					sb.append("WITH u ");
					sb.append("WHERE u.checksum IS NULL OR u.checksum <> {checksum} ");
					sb.append("SET ").append(Neo4j.nodeSetPropertiesFromJson("u", object,
							"id", "externalId", "login", "activationCode"));
					params = object;
				} else {
					sb.append("CREATE (u:User {props}) ");
					params = new JsonObject().putObject("props", object);
				}
				transactionHelper.add(sb.toString(), params);
			}
			if (relationshipQueries) {
				final String externalId = object.getString("externalId");
				JsonArray structures = object.getArray("structures");
				if (externalId != null && structures != null && structures.size() > 0) {
					String query;
					JsonObject p = new JsonObject().putString("userExternalId", externalId);
					if (structures.size() == 1) {
						query = "MATCH (s:Structure)<-[:DEPENDS]-(g:ProfileGroup)-[:HAS_PROFILE]->(p:Profile), " +
								"(u:User) " +
								"USING INDEX s:Structure(externalId) " +
								"USING INDEX u:User(externalId) " +
								"USING INDEX p:Profile(externalId) " +
								"WHERE s.externalId = {structureAdmin} AND u.externalId = {userExternalId} " +
								"AND p.externalId = {profileExternalId} " +
								"MERGE u-[:ADMINISTRATIVE_ATTACHMENT]->s " +
								"WITH u, g " +
								"MERGE u-[:IN]->g";
						p.putString("structureAdmin", (String) structures.get(0))
								.putString("profileExternalId", profileExternalId);
					} else {
						query = "MATCH (s:Structure)<-[:DEPENDS]-(g:ProfileGroup)-[:HAS_PROFILE]->(p:Profile), " +
								"(u:User) " +
								"USING INDEX s:Structure(externalId) " +
								"USING INDEX u:User(externalId) " +
								"USING INDEX p:Profile(externalId) " +
								"WHERE s.externalId IN {structuresAdmin} AND u.externalId = {userExternalId} " +
								"AND p.externalId = {profileExternalId} " +
								"MERGE u-[:ADMINISTRATIVE_ATTACHMENT]->s " +
								"WITH u, g " +
								"MERGE u-[:IN]->g";
						p.putArray("structuresAdmin", structures)
								.putString("profileExternalId", profileExternalId);
					}
					transactionHelper.add(query, p);
				}
				if (externalId != null && linkClasses != null) {
					for (String[] structClass : linkClasses) {
						if (structClass != null && structClass[0] != null && structClass[1] != null) {
							String query =
									"MATCH (s:Structure)<-[:BELONGS]-(c:Class)<-[:DEPENDS]-(g:ProfileGroup)" +
									"-[:DEPENDS]->(pg:ProfileGroup)-[:HAS_PROFILE]->(p:Profile), (u:User) " +
									"USING INDEX s:Structure(externalId) " +
									"USING INDEX u:User(externalId) " +
									"USING INDEX p:Profile(externalId) " +
									"WHERE s.externalId = {structure} AND c.externalId = {class} " +
									"AND u.externalId = {userExternalId} AND p.externalId = {profileExternalId} " +
									"CREATE UNIQUE u-[:IN]->g";
							JsonObject p = new JsonObject()
									.putString("userExternalId", externalId)
									.putString("profileExternalId", profileExternalId)
									.putString("structure", structClass[0])
									.putString("class", structClass[1]);
							transactionHelper.add(query, p);
						}
					}
				}
				if (externalId != null && linkGroups != null) {
					for (String[] structGroup : linkGroups) {
						if (structGroup != null && structGroup[0] != null && structGroup[1] != null) {
							String query =
									"MATCH (s:Structure)" +
									"<-[:DEPENDS]-(g:FunctionalGroup), " +
									"(u:User) " +
									"USING INDEX s:Structure(externalId) " +
									"USING INDEX u:User(externalId) " +
									"WHERE s.externalId = {structure} AND g.externalId = {group} " +
									"AND u.externalId = {userExternalId} " +
									"CREATE UNIQUE u-[:IN]->g";
							JsonObject p = new JsonObject()
									.putString("userExternalId", externalId)
									.putString("structure", structGroup[0])
									.putString("group", structGroup[1]);
							transactionHelper.add(query, p);
						}
					}
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
					for (Object o : relative) {
						if (!(o instanceof String)) continue;
						String query =
								"START u=node:node_auto_index(externalId={userExternalId}), " +
								"r=node:node_auto_index(externalId={user}) " +
								"CREATE UNIQUE u-[:RELATED]->r";
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
		String query;
		JsonObject params = new JsonObject();
		if (structureExternalId != null) {
			query = "MATCH (:Structure {externalId : {externalId}})<-[:DEPENDS]-(:ProfileGroup)<-[:IN]-(u:User) " +
					"WHERE NOT(HAS(u.manual)) " +
					"RETURN u.externalId as externalId";
			params.putString("externalId", structureExternalId);
		} else {
			query = "MATCH (u:User) WHERE NOT(HAS(u.manual)) RETURN u.externalId as externalId";
		}
		neo4j.execute(query, params, new Handler<Message<JsonObject>>() {
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

}
