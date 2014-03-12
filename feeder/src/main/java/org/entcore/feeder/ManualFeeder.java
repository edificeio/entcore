package org.entcore.feeder;

import au.com.bytecode.opencsv.CSV;
import au.com.bytecode.opencsv.CSVReadProc;
import org.entcore.feeder.be1d.Be1dFeeder;
import org.entcore.feeder.utils.Hash;
import org.entcore.feeder.utils.Neo4j;
import org.entcore.feeder.utils.StatementsBuilder;
import org.entcore.feeder.utils.Validator;
import org.mozilla.universalchardet.UniversalDetector;
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

public class ManualFeeder extends BusModBase {

	private static final Validator structureValidator = new Validator("dictionary/schema/Structure.json");
	private static final Validator classValidator = new Validator("dictionary/schema/Class.json");
	private static final Map<String, Validator> profiles;
	private final Neo4j neo4j;

	static {
		Map<String, Validator> p = new HashMap<>();
		p.put("Personnel", new Validator("dictionary/schema/Personnel.json"));
		p.put("Teacher", new Validator("dictionary/schema/Personnel.json"));
		p.put("Student", new Validator("dictionary/schema/Student.json"));
		p.put("Relative", new Validator("dictionary/schema/User.json"));
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
		if (c.getString("externalId") == null) {
			c.putString("externalId", UUID.randomUUID().toString());
		}
		final String error = classValidator.validate(c);
		if (error != null) {
			logger.error(error);
			sendError(message, error);
		} else {
			String query =
					"MATCH (s:Structure { id : {structureId}}) " +
					"CREATE s<-[:BELONGS]-(c:Class {props})" +
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
					"SET " + Neo4j.nodeSetPropertiesFromJson("c", c) +
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
			JsonObject user, String profile, String structureId, JsonArray childrenIds) {
		String related = "";
		JsonObject params = new JsonObject()
				.putString("structureId", structureId)
				.putString("profile", profile)
				.putObject("props", user);
		if (childrenIds != null) {
			related =
					"WITH u " +
					"MATCH (student:User) " +
					"WHERE student.id IN {childrenIds} " +
					"CREATE student-[:RELATED]->u ";
			params.putArray("childrenIds", childrenIds);
		}
		String query =
				"MATCH (s:Structure { id : {structureId}})<-[:DEPENDS]-" +
				"(pg:ProfileGroup)-[:HAS_PROFILE]->(p:Profile { name : {profile}}) " +
				"CREATE UNIQUE pg<-[:IN]-(u:User {props}) " +
				related +
				"RETURN DISTINCT u.id as id";
		neo4j.execute(query, params, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> m) {
				message.reply(m.body());
			}
		});
	}

	private void createUserInClass(final Message<JsonObject> message,
			JsonObject user, String profile, String classId, JsonArray childrenIds) {
		String related = "";
		JsonObject params = new JsonObject()
				.putString("classId", classId)
				.putString("profile", profile)
				.putObject("props", user);
		if (childrenIds != null) {
			related =
					"WITH u " +
					"MATCH (student:User) " +
					"WHERE student.id IN {childrenIds} " +
					"CREATE student-[:RELATED]->u ";
			params.putArray("childrenIds", childrenIds);
		}
		String query =
				"MATCH (s:Class { id : {classId}})<-[:DEPENDS]-(cpg:ProfileGroup)-[:DEPENDS]->" +
				"(pg:ProfileGroup)-[:HAS_PROFILE]->(p:Profile { name : {profile}}) " +
				"CREATE UNIQUE pg<-[:IN]-(u:User {props}), cpg<-[:IN]-u " +
				related +
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
				if ("ok".equals(r.body().getString("status")) && res != null && res.size() == 1) {
					String profile = ((JsonObject) res.get(0)).getString("profile");
					Validator v = profiles.get(profile);
					if (v == null) {
						sendError(message, "Invalid profile : " + profile);
						return;
					}
					final String error = v.modifiableValidate(user);
					if (error != null) {
						logger.error(error);
						sendError(message, error);
					} else {
						String query =
								"MATCH (u:User { id : {userId}}) " +
								"SET " + Neo4j.nodeSetPropertiesFromJson("u", user) +
								"RETURN DISTINCT u.id as id ";
						JsonObject params = user.putString("userId", userId);
						neo4j.execute(query, params, new Handler<Message<JsonObject>>() {
							@Override
							public void handle(Message<JsonObject> m) {
								message.reply(m.body());
							}
						});
					}
				} else {
					sendError(message, "Invalid profile.");
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
											(m = Be1dFeeder.be1dDatePattern.matcher(values[i])).find()) {
										props.putString(Be1dFeeder.studentHeader[i],
												m.group(3) + "/" + m.group(2) + "/" + m.group(1));
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
							for (i = 13; i < values.length; i += 4) {
								String mapping = structureExternalId+values[i].trim()+
										values[i+1].trim()+values[i+2].trim()+values[i+3].trim();
								if (mapping.trim().isEmpty()) continue;
								try {
									linkStudents.add(Hash.sha1(mapping.getBytes("UTF-8")));
								} catch (NoSuchAlgorithmException|UnsupportedEncodingException e) {
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

	private String detectCharset(String csv) {
		UniversalDetector detector = new UniversalDetector(null);
		byte[] data = csv.getBytes();
		detector.handleData(data, 0, data.length);
		detector.dataEnd();
		String encoding = detector.getDetectedCharset();
		logger.debug(encoding);
		detector.reset();
		return encoding != null ? encoding : "ISO-8859-1";
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

}
