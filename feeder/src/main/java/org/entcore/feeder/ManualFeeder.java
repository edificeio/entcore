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

package org.entcore.feeder;

import au.com.bytecode.opencsv.CSV;
import au.com.bytecode.opencsv.CSVReadProc;
import org.entcore.common.events.EventStore;
import org.entcore.common.events.EventStoreFactory;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.neo4j.Neo4jUtils;
import org.entcore.common.user.UserInfos;
import org.entcore.feeder.be1d.Be1dFeeder;
import org.entcore.feeder.dictionary.structures.*;
import org.entcore.feeder.exceptions.TransactionException;
import org.entcore.feeder.exceptions.ValidationException;
import org.entcore.feeder.utils.*;
import org.vertx.java.busmods.BusModBase;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.impl.LoggerFactory;

import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;

import static org.entcore.feeder.utils.FeederHelper.detectCharset;

public class ManualFeeder extends BusModBase {

	private static final Validator structureValidator = new Validator("dictionary/schema/Structure.json");
	private static final Validator classValidator = new Validator("dictionary/schema/Class.json");
	public static final Map<String, Validator> profiles;
	private final Neo4j neo4j;
	private EventStore eventStore = EventStoreFactory.getFactory().getEventStore(Feeder.class.getSimpleName());
	public static final String SOURCE = "MANUAL";

	static {
		Map<String, Validator> p = new HashMap<>();
		p.put("Personnel", new Validator("dictionary/schema/Personnel.json"));
		p.put("Teacher", new Validator("dictionary/schema/Personnel.json"));
		p.put("Student", new Validator("dictionary/schema/Student.json"));
		p.put("Relative", new Validator("dictionary/schema/User.json"));
		p.put("Guest", new Validator("dictionary/schema/User.json"));
		profiles = Collections.unmodifiableMap(p);
	}

	public ManualFeeder(Neo4j neo4j) {
		this.neo4j = neo4j;
		this.logger = LoggerFactory.getLogger(ManualFeeder.class);
	}

	public void createStructure(final Message<JsonObject> message) {
		JsonObject struct = getMandatoryObject("data", message);
		if (struct == null) return;
		if (struct.getString("externalId") == null) {
			struct.putString("externalId", UUID.randomUUID().toString());
		}
		final String error = structureValidator.validate(struct);
		if (error != null) {
			logger.error(error);
			sendError(message, error);
		} else {
			String query =
					"CREATE (s:Structure {props}) " +
					"WITH s " +
					"MATCH (p:Profile) " +
					"CREATE p<-[:HAS_PROFILE]-(g:Group:ProfileGroup {name : s.name+'-'+p.name})-[:DEPENDS]->s " +
					"SET g.id = id(g)+'-'+timestamp() " +
					"RETURN DISTINCT s.id as id ";
			JsonObject params = new JsonObject()
					.putObject("props", struct);
			neo4j.execute(query, params, new Handler<Message<JsonObject>>() {
				@Override
				public void handle(Message<JsonObject> m) {
					message.reply(m.body());
				}
			});
		}
	}

	public void createClass(final Message<JsonObject> message) {
		JsonObject c = getMandatoryObject("data", message);
		if (c == null) return;
		String structureId = getMandatoryString("structureId", message);
		if (structureId == null) return;
		if (c.getString("externalId") == null || c.getString("externalId").isEmpty()) {
			c.putString("externalId", structureId + "$" + c.getString("name"));
		}
		final String error = classValidator.validate(c);
		if (error != null) {
			logger.error(error);
			sendError(message, error);
		} else {
			String query =
					"MATCH (s:Structure { id : {structureId}}) " +
					"CREATE s<-[:BELONGS]-(c:Class {props}) " +
					"SET c.externalId = s.externalId + '$' + c.name " +
					"WITH s, c " +
					"MATCH s<-[:DEPENDS]-(g:ProfileGroup)-[:HAS_PROFILE]->(p:Profile) " +
					"CREATE c<-[:DEPENDS]-(pg:Group:ProfileGroup {name : c.name+'-'+p.name})-[:DEPENDS]->g " +
					"SET pg.id = id(pg)+'-'+timestamp() " +
					"RETURN DISTINCT c.id as id ";
			JsonObject params = new JsonObject()
					.putString("structureId", structureId)
					.putObject("props", c);
			neo4j.execute(query, params, new Handler<Message<JsonObject>>() {
				@Override
				public void handle(Message<JsonObject> m) {
					message.reply(m.body());
				}
			});
		}
	}

	public void updateClass(final Message<JsonObject> message) {
		JsonObject c = getMandatoryObject("data", message);
		if (c == null) return;
		String classId = getMandatoryString("classId", message);
		if (classId == null) return;
		final String error = classValidator.modifiableValidate(c);
		if (error != null) {
			logger.error(error);
			sendError(message, error);
		} else {
			String rename = "";
			if (c.getString("name") != null) {
				rename = "WITH c " +
						 "MATCH c<-[:DEPENDS]-(cpg:ProfileGroup)-[:DEPENDS]->" +
						 "(spg:ProfileGroup)-[:HAS_PROFILE]->(p:Profile) " +
						 "SET cpg.name = c.name+'-'+p.name ";
			}
			String query =
					"MATCH (c:`Class` { id : {classId}}) " +
					"SET " + Neo4jUtils.nodeSetPropertiesFromJson("c", c) +
					rename +
					"RETURN DISTINCT c.id as id ";
			JsonObject params = c.putString("classId", classId);
			neo4j.execute(query, params, new Handler<Message<JsonObject>>() {
				@Override
				public void handle(Message<JsonObject> m) {
					message.reply(m.body());
				}
			});
		}
	}

	public void createUser(final Message<JsonObject> message) {
		final JsonObject user = getMandatoryObject("data", message);
		if (user == null) return;
		if (user.getString("externalId") == null) {
			user.putString("externalId", UUID.randomUUID().toString());
		}
		final String profile = message.body().getString("profile", "");
		if (!profiles.containsKey(profile)) {
			sendError(message, "Invalid profile : " + profile);
			return;
		}
		JsonArray childrenIds = null;
		if ("Relative".equals(profile)) {
			childrenIds = user.getArray("childrenIds");
		}
		final String error = profiles.get(profile).validate(user);
		if (error != null) {
			logger.error(error);
			sendError(message, error);
			return;
		}
		user.putString("source", SOURCE);
		final String structureId = message.body().getString("structureId");
		if (structureId != null && !structureId.trim().isEmpty()) {
			createUserInStructure(message, user, profile, structureId, childrenIds);
			return;
		}
		final String classId = message.body().getString("classId");
		if (classId != null && !classId.trim().isEmpty()) {
			createUserInClass(message, user, profile, classId, childrenIds);
			return;
		}
		sendError(message, "structureId or classId must be specified");
	}

	private void createUserInStructure(final Message<JsonObject> message,
			final JsonObject user, String profile, String structureId, JsonArray childrenIds) {
		String related = "";
		JsonObject params = new JsonObject()
				.putString("structureId", structureId)
				.putString("profile", profile)
				.putObject("props", user);
		if (childrenIds != null && childrenIds.size() > 0) {
			related =
					"WITH u " +
					"MATCH (student:User) " +
					"WHERE student.id IN {childrenIds} " +
					"CREATE student-[:RELATED]->u " +
					"SET student.relative = coalesce(student.relative, []) + (u.externalId + '$1$1$1$1$0') ";
			params.putArray("childrenIds", childrenIds);
		}
		String query =
				"MATCH (s:Structure { id : {structureId}})<-[:DEPENDS]-" +
				"(pg:ProfileGroup)-[:HAS_PROFILE]->(p:Profile { name : {profile}}) " +
				"CREATE UNIQUE pg<-[:IN]-(u:User {props}) " +
				"SET u.structures = [s.externalId] " +
				related +
				"RETURN DISTINCT u.id as id";
		neo4j.execute(query, params, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> m) {
				message.reply(m.body());
				if ("ok".equals(m.body().getString("status"))) {
					eventStore.createAndStoreEvent(Feeder.FeederEvent.CREATE_USER.name(),
							(UserInfos) null, new JsonObject().putString("new-user", user.getString("id")));
				}
			}
		});
	}

	public void addUser(final Message<JsonObject> message) {
		final String userId = getMandatoryString("userId", message);
		if (userId == null) return;

		final String structureId = message.body().getString("structureId");
		if (structureId != null && !structureId.trim().isEmpty()) {
			addUserInStructure(message, userId, structureId);
			return;
		}
		final String classId = message.body().getString("classId");
		if (classId != null && !classId.trim().isEmpty()) {
			addUserInClass(message, userId, classId);
			return;
		}
		sendError(message, "structureId or classId must be specified");
	}

	private void addUserInStructure(final Message<JsonObject> message,
			String userId, String structureId) {
		JsonObject params = new JsonObject()
				.putString("structureId", structureId)
				.putString("userId", userId);
		String query =
				"MATCH (u:User { id : {userId}})-[:IN]->(opg:ProfileGroup)-[:HAS_PROFILE]->(p:Profile) " +
				"WITH u, p " +
				"MATCH (s:Structure { id : {structureId}})<-[:DEPENDS]-(pg:ProfileGroup)-[:HAS_PROFILE]->p " +
				"CREATE UNIQUE pg<-[:IN {source:'MANUAL'}]-u " +
				"SET u.structures = CASE WHEN s.externalId IN u.structures THEN " +
				"u.structures ELSE coalesce(u.structures, []) + s.externalId END " +
				"RETURN DISTINCT u.id as id";
		neo4j.execute(query, params, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> m) {
				message.reply(m.body());
			}
		});
	}

	public void removeUser(final Message<JsonObject> message) {
		final String userId = getMandatoryString("userId", message);
		if (userId == null) return;

		final String structureId = message.body().getString("structureId");
		if (structureId != null && !structureId.trim().isEmpty()) {
			removeUserFromStructure(message, userId, structureId);
			return;
		}
		final String classId = message.body().getString("classId");
		if (classId != null && !classId.trim().isEmpty()) {
			removeUserFromClass(message, userId, classId);
			return;
		}
		sendError(message, "structureId or classId must be specified");
	}

	private void removeUserFromStructure(final Message<JsonObject> message,
			String userId, String structureId) {
		JsonObject params = new JsonObject()
				.putString("structureId", structureId)
				.putString("userId", userId);
		String query =
				"MATCH (u:User { id : {userId}})-[r:IN|COMMUNIQUE]-(cpg:ProfileGroup)-[:DEPENDS*0..1]->" +
				"(pg:ProfileGroup)-[:DEPENDS]->(s:Structure { id : {structureId}}), " +
				"pg-[:HAS_PROFILE]->(p:Profile), p<-[:HAS_PROFILE]-(dpg:DefaultProfileGroup) " +
				"CREATE UNIQUE dpg<-[:IN]-u " +
				"SET u.structures = FILTER(sId IN u.structures WHERE sId <> s.externalId), " +
				"u.classes = FILTER(cId IN u.classes WHERE NOT(cId =~ (s.externalId + '.*'))) " +
				"DELETE r " +
				"RETURN DISTINCT u.id as id";
		neo4j.execute(query, params, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> m) {
				message.reply(m.body());
			}
		});
	}

	private void createUserInClass(final Message<JsonObject> message,
			final JsonObject user, String profile, String classId, JsonArray childrenIds) {
		String related = "";
		JsonObject params = new JsonObject()
				.putString("classId", classId)
				.putString("profile", profile)
				.putObject("props", user);
		if (childrenIds != null && childrenIds.size() > 0) {
			related =
					"WITH u " +
					"MATCH (student:User) " +
					"WHERE student.id IN {childrenIds} " +
					"CREATE student-[:RELATED]->u " +
					"SET student.relative = coalesce(student.relative, []) + (u.externalId + '$1$1$1$1$0') ";
			params.putArray("childrenIds", childrenIds);
		}
		String query =
				"MATCH (s:Class { id : {classId}})<-[:DEPENDS]-(cpg:ProfileGroup)-[:DEPENDS]->" +
				"(pg:ProfileGroup)-[:HAS_PROFILE]->(p:Profile { name : {profile}}), s-[:BELONGS]->(struct:Structure) " +
				"CREATE UNIQUE pg<-[:IN]-(u:User {props}), cpg<-[:IN]-u " +
				"SET u.classes = [s.externalId], u.structures = [struct.externalId] " +
				related +
				"RETURN DISTINCT u.id as id";
		neo4j.execute(query, params, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> m) {
				message.reply(m.body());
				if ("ok".equals(m.body().getString("status"))) {
					eventStore.createAndStoreEvent(Feeder.FeederEvent.CREATE_USER.name(),
							(UserInfos) null, new JsonObject().putString("new-user", user.getString("id")));
				}
			}
		});
	}

	private void addUserInClass(final Message<JsonObject> message,
			String userId, String classId) {
		JsonObject params = new JsonObject()
				.putString("classId", classId)
				.putString("userId", userId);
		String query =
				"MATCH (u:User { id : {userId}})-[:IN]->(opg:ProfileGroup)-[:HAS_PROFILE]->(p:Profile) " +
				"WITH u, p " +
				"MATCH (s:Class { id : {classId}})<-[:DEPENDS]-(cpg:ProfileGroup)-[:DEPENDS]->" +
				"(pg:ProfileGroup)-[:HAS_PROFILE]->p, s-[:BELONGS]->(struct:Structure) " +
				"CREATE UNIQUE pg<-[:IN {source:'MANUAL'}]-u, cpg<-[:IN {source:'MANUAL'}]-u " +
				"SET u.classes = CASE WHEN s.externalId IN u.classes THEN " +
				"u.classes ELSE coalesce(u.classes, []) + s.externalId END, " +
				"u.structures = CASE WHEN struct.externalId IN u.structures THEN " +
				"u.structures ELSE coalesce(u.structures, []) + struct.externalId END " +
				"RETURN DISTINCT u.id as id";
		neo4j.execute(query, params, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> m) {
				message.reply(m.body());
			}
		});
	}

	private void removeUserFromClass(final Message<JsonObject> message,
								String userId, String classId) {
		JsonObject params = new JsonObject()
				.putString("classId", classId)
				.putString("userId", userId);
		String query =
				"MATCH (u:User { id : {userId}})-[r:IN|COMMUNIQUE]-(cpg:ProfileGroup)-[:DEPENDS]->" +
				"(c:Class  {id : {classId}}), cpg-[:DEPENDS]->(pg:ProfileGroup)-[:HAS_PROFILE]->(p:Profile), " +
				"p<-[:HAS_PROFILE]-(dpg:DefaultProfileGroup) " +
				"CREATE UNIQUE dpg<-[:IN]-u " +
				"SET u.classes = FILTER(cId IN u.classes WHERE cId <> c.externalId) " +
				"DELETE r " +
				"RETURN DISTINCT u.id as id";
		neo4j.execute(query, params, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> m) {
				message.reply(m.body());
			}
		});
	}

	public void updateUser(final Message<JsonObject> message) {
		final JsonObject user = getMandatoryObject("data", message);
		if (user == null) return;
		final String userId = getMandatoryString("userId", message);
		if (userId == null) return;
		String q =
				"MATCH (u:User { id : {userId}})-[:IN]->(pg:ProfileGroup)-[:HAS_PROFILE]->(p:Profile) " +
				"RETURN DISTINCT p.name as profile ";
		neo4j.execute(q, new JsonObject().putString("userId", userId), new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> r) {
				JsonArray res = r.body().getArray("result");
				if ("ok".equals(r.body().getString("status")) && res != null && res.size() > 0) {
					for (Object o : res) {
						if (!(o instanceof JsonObject)) continue;
						String profile = ((JsonObject) o).getString("profile");
						Validator v = profiles.get(profile);
						if (v == null) {
							sendError(message, "Invalid profile : " + profile);
							return;
						}
						final String error = v.modifiableValidate(user);
						if (error != null) {
							logger.error(error);
							sendError(message, error);
							return;
						}
					}
					String query =
							"MATCH (u:User { id : {userId}}) " +
							"SET " + Neo4jUtils.nodeSetPropertiesFromJson("u", user) +
							"RETURN DISTINCT u.id as id ";
					JsonObject params = user.putString("userId", userId);
					neo4j.execute(query, params, new Handler<Message<JsonObject>>() {
								@Override
								public void handle(Message<JsonObject> m) {
									message.reply(m.body());
								}
							});
				} else {
					sendError(message, "Invalid profile.");
				}
			}
		});
	}

	public void deleteUser(final Message<JsonObject> message) {
		final JsonArray users = message.body().getArray("users");
		if (users == null || users.size() == 0) {
			sendError(message, "Missing users.");
			return;
		}
		String query =
				"MATCH (u:User)" +
				"WHERE u.id IN {users} AND (u.source IN ['MANUAL', 'CSV', 'CLASS_PARAM', 'BE1D'] OR HAS(u.disappearanceDate)) " +
				"return count(*) as count ";
		neo4j.execute(query, new JsonObject().putArray("users", users), new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				JsonArray res = event.body().getArray("result");
				if ("ok".equals(event.body().getString("status")) && res != null && res.size() == 1) {
					JsonObject j = res.get(0);
					if (users.size() == j.getInteger("count", 0)) {
						executeTransaction(message, new VoidFunction<TransactionHelper>() {
							@Override
							public void apply(TransactionHelper tx) {
								for (Object o : users) {
									User.backupRelationship(o.toString(), tx);
									User.preDelete(o.toString(), tx);
								}
							}
						});
					} else {
						sendError(message, "unauthorized.user");
					}
				} else {
					message.reply(event.body());
				}
			}
		});
	}

	public void restoreUser(final Message<JsonObject> message) {
		final JsonArray users = message.body().getArray("users");
		if (users == null || users.size() == 0) {
			sendError(message, "Missing users.");
			return;
		}
		executeTransaction(message, new VoidFunction<TransactionHelper>() {
			@Override
			public void apply(TransactionHelper tx) {
				for (Object o : users) {
					User.restorePreDeleted(o.toString(), tx);
				}
			}
		});
	}

	public void csvClassStudent(final Message<JsonObject> message) {
		final String classId = message.body().getString("classId");
		if (classId == null || classId.trim().isEmpty()) {
			sendError(message, "invalid.class.id");
			return;
		}
		final String csv = message.body().getString("csv");
		if (csv == null || csv.trim().isEmpty()) {
			sendError(message, "missing.csv");
			return;
		}
		String q = "MATCH (c:Class {id : {id}})-[:BELONGS]->(s:Structure) " +
				   "RETURN c.externalId as cId, s.externalId as sId";
		neo4j.execute(q, new JsonObject().putString("id", classId), new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> r) {
				JsonArray result = r.body().getArray("result");
				if ("ok".equals(r.body().getString("status")) && result != null && result.size() == 1) {
					JsonObject j = result.get(0);
					final String structureExternalId = j.getString("sId");
					final String classExternalId = j.getString("cId");
					if (structureExternalId == null || classExternalId == null) {
						sendError(message, "invalid.class.id");
						return;
					}
					String charset = detectCharset(csv);
					CSV csvParser = CSV
							.ignoreLeadingWhiteSpace()
							.separator(';')
							.skipLines(1)
							.charset(charset)
							.create();
					final StatementsBuilder statementsBuilder = new StatementsBuilder();
					final JsonObject params = new JsonObject().putString("classId", classId);
					final String query =
							"MATCH (c:`Class` { id : {classId}})<-[:DEPENDS]-(csg:ProfileGroup)" +
							"-[:DEPENDS]->(ssg:ProfileGroup)-[:HAS_PROFILE]->(p:Profile { name : 'Student'}) " +
							"CREATE csg<-[:IN]-(u:User {props}), " +
							"ssg<-[:IN]-u " +
							"RETURN DISTINCT u.id as id";
					final Validator v = profiles.get("Student");
					final JsonArray errors = new JsonArray();
					csvParser.readAndClose(new StringReader(csv), new CSVReadProc() {

						@Override
						public void procRow(int rowIdx, String... values) {
							int i = 0;
							JsonObject props = new JsonObject();
							while (i < values.length && !"#break#".equals(Be1dFeeder.studentHeader[i])) {
								if ("birthDate".equals(Be1dFeeder.studentHeader[i]) && values[i] != null) {
									Matcher m;
									if (values[i] != null &&
											(m = Be1dFeeder.frenchDatePatter.matcher(values[i])).find()) {
										props.putString(Be1dFeeder.studentHeader[i],
												m.group(3) + "-" + m.group(2) + "-" + m.group(1));
									} else {
										props.putString(Be1dFeeder.studentHeader[i], values[i].trim());
									}
								} else if (!"#skip#".equals(Be1dFeeder.studentHeader[i])) {
									if (values[i] != null && !values[i].trim().isEmpty()) {
										props.putString(Be1dFeeder.studentHeader[i], values[i].trim());
									}
								}
								i++;
							}
							props.putArray("structures", new JsonArray().add(structureExternalId));
							String c = props.getString("classes");
							props.putArray("classes", new JsonArray().add(classExternalId));
							generateUserExternalId(props, c, structureExternalId);
							String error = v.validate(props);
							if (error != null) {
								String e = error + (rowIdx + 2);
								sendError(message, e);
								errors.add(e);
								return;
							}
							props.putString("source", "CLASS_PARAM");
							statementsBuilder.add(query, params.copy().putObject("props", props));
						}
					});
					if (errors.size() == 0) {
						neo4j.executeTransaction(statementsBuilder.build(), null, true,
								new Handler<Message<JsonObject>>() {
									@Override
									public void handle(Message<JsonObject> res) {
										message.reply(res.body());
									}
								});
					}
				} else {
					sendError(message, "invalid.class.id");
				}
			}
		});
	}

	public void csvClassRelative(final Message<JsonObject> message) {
		final String classId = message.body().getString("classId");
		if (classId == null || classId.trim().isEmpty()) {
			sendError(message, "invalid.class.id");
			return;
		}
		final String csv = message.body().getString("csv", "").split("(;;;;;;;;;;;;;;;;;;;;|\n\n|\r\n\r\n)")[0];
		if (csv == null || csv.trim().isEmpty()) {
			sendError(message, "missing.csv");
			return;
		}
		String q = "MATCH (c:Class {id : {id}})-[:BELONGS]->(s:Structure) " +
				   "RETURN c.externalId as cId, s.externalId as sId";
		neo4j.execute(q, new JsonObject().putString("id", classId), new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> r) {
				JsonArray result = r.body().getArray("result");
				if ("ok".equals(r.body().getString("status")) && result != null && result.size() == 1) {
					JsonObject j = result.get(0);
					final String structureExternalId = j.getString("sId");
					final String classExternalId = j.getString("cId");
					if (structureExternalId == null || classExternalId == null) {
						sendError(message, "invalid.class.id");
						return;
					}
					String charset = detectCharset(csv);
					CSV csvParser = CSV
							.ignoreLeadingWhiteSpace()
							.separator(';')
							.skipLines(1)
							.charset(charset)
							.create();

					final StatementsBuilder statementsBuilder = new StatementsBuilder();
					final JsonObject params = new JsonObject().putString("classId", classId);
					final String query =
							"MATCH (c:`Class` { id : {classId}})<-[:DEPENDS]-(csg:ProfileGroup)" +
							"-[:DEPENDS]->(ssg:ProfileGroup)-[:HAS_PROFILE]->(p:Profile { name : 'Relative'}) " +
							"CREATE csg<-[:IN]-(u:User {props}), " +
							"ssg<-[:IN]-u " +
							"WITH u, c " +
							"MATCH (student:User)-[:IN]->(pg:ProfileGroup)-[:DEPENDS]->(c) " +
							"WHERE student.externalId IN {linkStudents} " +
							"CREATE student-[:RELATED]->u " +
							"RETURN DISTINCT u.id as id";
					final Validator v = profiles.get("Relative");
					final JsonArray errors = new JsonArray();
					csvParser.readAndClose(new StringReader(csv), new CSVReadProc() {

						@Override
						public void procRow(int rowIdx, String... values) {
							int i = 0;
							JsonObject props = new JsonObject();
							while (i < Be1dFeeder.relativeHeader.length) {
								if (!"#skip#".equals(Be1dFeeder.relativeHeader[i])) {
									if (values[i] != null && !values[i].trim().isEmpty()) {
										props.putString(Be1dFeeder.relativeHeader[i], values[i].trim());
									}
								}
								i++;
							}
							JsonArray linkStudents = new JsonArray();
							for (i = 12; i < values.length; i += 4) {
								String mapping = structureExternalId + values[i].trim() +
										values[i + 1].trim() + values[i + 2].trim() + values[i + 3].trim();
								if (mapping.trim().isEmpty()) continue;
								try {
									linkStudents.add(Hash.sha1(mapping.getBytes("UTF-8")));
								} catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
									logger.error(e.getMessage(), e);
									errors.add(e.getMessage());
									sendError(message, e.getMessage());
									return;
								}
							}
							generateUserExternalId(props, String.valueOf(rowIdx), structureExternalId);
							String error = v.validate(props);
							if (error != null) {
								String e = error + (rowIdx + 2);
								sendError(message, e);
								errors.add(e);
								return;
							}
							props.putString("source", "CLASS_PARAM");
							statementsBuilder.add(query, params.copy()
									.putObject("props", props).putArray("linkStudents", linkStudents));
						}
					});
					if (errors.size() == 0) {
						neo4j.executeTransaction(statementsBuilder.build(), null, true,
								new Handler<Message<JsonObject>>() {
									@Override
									public void handle(Message<JsonObject> res) {
										message.reply(res.body());
									}
								});
					}
				} else {
					sendError(message, "invalid.class.id");
				}
			}
		});
	}

	private void generateUserExternalId(JsonObject props, String c, String structureExternalId) {
		String mapping = structureExternalId+props.getString("surname", "")+
				props.getString("lastName", "")+props.getString("firstName", "")+
				props.getString("email","")+props.getString("title","")+
				props.getString("homePhone","")+props.getString("mobile","")+c;
		try {
			props.putString("externalId", Hash.sha1(mapping.getBytes("UTF-8")));
		} catch (NoSuchAlgorithmException|UnsupportedEncodingException e) {
			logger.error(e.getMessage(), e);
		}
	}

	public void createFunction(final Message<JsonObject> message) {
		final JsonObject function = getMandatoryObject("data", message);
		if (function == null) return;
		final String profile = message.body().getString("profile", "");
		if (!profiles.containsKey(profile)) {
			sendError(message, "Invalid profile : " + profile);
			return;
		}
		executeTransaction(message, new VoidFunction<TransactionHelper>() {
			@Override
			public void apply(TransactionHelper tx) {
				Profile.createFunction(profile, null, function, tx);
			}
		});
	}

	private TransactionHelper getTransaction(Message<JsonObject> message) {
		TransactionHelper tx = null;
		try {
			tx = TransactionManager.getInstance().begin();
		} catch (TransactionException e) {
			logger.error(e.getMessage(), e);
			sendError(message, e.getMessage());
		}
		return tx;
	}

	private void executeTransaction(final Message<JsonObject> message, VoidFunction<TransactionHelper> f) {
		TransactionHelper tx;
		try {
			tx = TransactionManager.getInstance().begin();
			f.apply(tx);
			tx.commit(new Handler<Message<JsonObject>>() {
				@Override
				public void handle(Message<JsonObject> event) {
					message.reply(event.body());
				}
			});
		} catch (TransactionException | ValidationException e) {
			sendError(message, e.getMessage(), e);
		}
	}

	public void deleteFunction(final Message<JsonObject> message) {
		final String functionCode = getMandatoryString("functionCode", message);
		if (functionCode == null) return;
		executeTransaction(message, new VoidFunction<TransactionHelper>() {
			@Override
			public void apply(TransactionHelper tx) {
				Profile.deleteFunction(functionCode, tx);
			}
		});
	}

	public void createFunctionGroup(Message<JsonObject> message) {
		final JsonArray functions = message.body().getArray("functions");
		if (functions == null || functions.size() == 0) {
			sendError(message, "missing.functions");
			return;
		}
		final String name = message.body().getString("name");
		if (name == null || name.trim().isEmpty()) {
			sendError(message, "missing.name");
			return;
		}
		final String externalId = message.body().getString("externalId");
		if (externalId == null || externalId.trim().isEmpty()) {
			sendError(message, "missing.externalId");
			return;
		}
		executeTransaction(message, new VoidFunction<TransactionHelper>() {
			@Override
			public void apply(TransactionHelper tx) {
				Profile.createFunctionGroup(functions, name, externalId, tx);
			}
		});
	}

	public void deleteFunctionGroup(Message<JsonObject> message) {
		final String groupId = getMandatoryString("groupId", message);
		if (groupId == null) return;
		executeTransaction(message, new VoidFunction<TransactionHelper>() {
			@Override
			public void apply(TransactionHelper tx) {
				Profile.deleteFunctionGroup(groupId, tx);
			}
		});
	}

	public void addUserFunction(final Message<JsonObject> message) {
		final String userId = getMandatoryString("userId", message);
		final String function = message.body().getString("function");
		if (userId == null || function == null) return;
		final JsonArray scope = message.body().getArray("scope");
		String inherit =  message.body().getString("inherit", "");
		if (scope != null && ("s".equals(inherit) || "sc".equals(inherit))) {
			String query;
			if ("sc".equals(inherit)) {
				query = "MATCH (s:Structure)<-[:HAS_ATTACHMENT*0..]-(:Structure)<-[:BELONGS*0..1]-(scope) " +
						"WHERE s.id IN {scope} " +
						"RETURN COLLECT(scope.id) as ids ";
			} else {
				query = "MATCH (s:Structure)<-[:HAS_ATTACHMENT*0..]-(scope:Structure) " +
						"WHERE s.id IN {scope} " +
						"RETURN COLLECT(scope.id) as ids ";
			}
			neo4j.execute(query, new JsonObject().putArray("scope", scope), new Handler<Message<JsonObject>>() {
				@Override
				public void handle(Message<JsonObject> event) {
					JsonArray result = event.body().getArray("result");
					if ("ok".equals(event.body().getString("status")) && result != null && result.size() == 1) {
						final JsonArray s = result.<JsonObject>get(0).getArray("ids");
						executeTransaction(message, new VoidFunction<TransactionHelper>() {
							@Override
							public void apply(TransactionHelper tx) {
								User.addFunction(userId, function, s, tx);
							}
						});
					} else {
						sendError(message, "invalid.scope");
					}
				}
			});
		} else {
			executeTransaction(message, new VoidFunction<TransactionHelper>() {
				@Override
				public void apply(TransactionHelper tx) {
					User.addFunction(userId, function, scope, tx);
				}
			});
		}
	}

	public void removeUserFunction(Message<JsonObject> message) {
		final String userId = getMandatoryString("userId", message);
		final String function = message.body().getString("function");
		if (userId == null || function == null) return;
		executeTransaction(message, new VoidFunction<TransactionHelper>() {
			@Override
			public void apply(TransactionHelper tx) {
				User.removeFunction(userId, function, tx);
			}
		});
	}

	public void addUserGroup(Message<JsonObject> message) {
		final String userId = getMandatoryString("userId", message);
		final String groupId = message.body().getString("groupId");
		if (userId == null || groupId == null) return;
		executeTransaction(message, new VoidFunction<TransactionHelper>() {
			@Override
			public void apply(TransactionHelper tx) {
				User.addGroup(userId, groupId, tx);
			}
		});
	}

	public void removeUserGroup(Message<JsonObject> message) {
		final String userId = getMandatoryString("userId", message);
		final String groupId = message.body().getString("groupId");
		if (userId == null || groupId == null) return;
		executeTransaction(message, new VoidFunction<TransactionHelper>() {
			@Override
			public void apply(TransactionHelper tx) {
				User.removeGroup(userId, groupId, tx);
			}
		});
	}

	public void createOrUpdateTenant(Message<JsonObject> message) {
		final JsonObject tenant = getMandatoryObject("data", message);
		if (tenant == null) return;
		executeTransaction(message, new VoidFunction<TransactionHelper>() {
			@Override
			public void apply(TransactionHelper tx) throws ValidationException {
				Tenant.createOrUpdate(tenant, tx);
			}
		});
	}

	public void createGroup(Message<JsonObject> message) {
		final JsonObject group = message.body().getObject("group");
		if (group == null || group.size() == 0) {
			sendError(message, "missing.group");
			return;
		}
		final String structureId = message.body().getString("structureId");
		final String classId = message.body().getString("classId");
		executeTransaction(message, new VoidFunction<TransactionHelper>() {
			@Override
			public void apply(TransactionHelper tx) throws ValidationException {
				Group.manualCreateOrUpdate(group, structureId, classId, tx);
			}
		});
	}

	public void deleteGroup(Message<JsonObject> message) {
		final String groupId = getMandatoryString("groupId", message);
		if (groupId == null) return;
		executeTransaction(message, new VoidFunction<TransactionHelper>() {
			@Override
			public void apply(TransactionHelper tx) {
				Group.manualDelete(groupId, tx);
			}
		});
	}

	public void structureAttachment(Message<JsonObject> message) {
		final String structureId = getMandatoryString("structureId", message);
		final String parentStructureId = getMandatoryString("parentStructureId", message);
		executeTransaction(message, new VoidFunction<TransactionHelper>() {
			@Override
			public void apply(TransactionHelper tx) throws ValidationException {
				Structure.addAttachment(structureId, parentStructureId, tx);
			}
		});
	}

	public void structureDetachment(Message<JsonObject> message) {
		final String structureId = getMandatoryString("structureId", message);
		final String parentStructureId = getMandatoryString("parentStructureId", message);
		executeTransaction(message, new VoidFunction<TransactionHelper>() {
			@Override
			public void apply(TransactionHelper tx) throws ValidationException {
				Structure.removeAttachment(structureId, parentStructureId, tx);
			}
		});
	}

	public void updateStructure(final Message<JsonObject> message) {
		JsonObject s = getMandatoryObject("data", message);
		if (s == null) return;
		String structureId = getMandatoryString("structureId", message);
		if (structureId == null) return;
		final String error = structureValidator.modifiableValidate(s);
		if (error != null) {
			logger.error(error);
			sendError(message, error);
		} else {
			String rename = "SET ";
			if (s.getString("name") != null) {
				rename =
						"WITH s, s.name as old " +
						"OPTIONAL MATCH s<-[:DEPENDS]-(spg:ProfileGroup) " +
						"OPTIONAL MATCH s<-[:DEPENDS]-(fg:FunctionGroup) " +
						"SET spg.name = replace(spg.name, old, {name}), fg.name = replace(fg.name, old, {name}), ";
			}
			String query =
					"MATCH (s:`Structure` { id : {structureId}}) " +
					rename + Neo4jUtils.nodeSetPropertiesFromJson("s", s) +
					"RETURN DISTINCT s.id as id ";
			JsonObject params = s.putString("structureId", structureId);
			neo4j.execute(query, params, new Handler<Message<JsonObject>>() {
				@Override
				public void handle(Message<JsonObject> m) {
					message.reply(m.body());
				}
			});
		}
	}

	public void relativeStudent(Message<JsonObject> message) {
		final String relativeId = getMandatoryString("relativeId", message);
		final String studentId = getMandatoryString("studentId", message);
		executeTransaction(message, new VoidFunction<TransactionHelper>() {
			@Override
			public void apply(TransactionHelper tx) throws ValidationException {
				User.relativeStudent(relativeId, studentId, tx);
			}
		});
	}

	public void unlinkRelativeStudent(Message<JsonObject> message) {
		final String relativeId = getMandatoryString("relativeId", message);
		final String studentId = getMandatoryString("studentId", message);
		executeTransaction(message, new VoidFunction<TransactionHelper>() {
			@Override
			public void apply(TransactionHelper tx) throws ValidationException {
				User.unlinkRelativeStudent(relativeId, studentId, tx);
			}
		});
	}
}
