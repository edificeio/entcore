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

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.neo4j.Neo4jUtils;
import org.entcore.common.neo4j.TransactionHelper;
import org.entcore.feeder.FeederLogger;
import org.entcore.feeder.ManualFeeder;
import org.entcore.feeder.dictionary.users.AbstractUser;
import org.entcore.feeder.dictionary.users.PersEducNat;
import org.entcore.feeder.utils.*;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

import fr.wseduc.webutils.Either;
import static fr.wseduc.webutils.Utils.getOrElse;
import static fr.wseduc.webutils.Utils.isNotEmpty;

public class Importer {

	private static final Logger log = LoggerFactory.getLogger(Importer.class);
	private ConcurrentMap<String, ImporterStructure> structures;
	private ConcurrentMap<String, Profile> profiles;
	private Set<String> userImportedExternalId = new HashSet<>();
	private Set<String> structuresImportedExternalId = new HashSet<>();
	private TransactionHelper transactionHelper;
	private final Validator structureValidator;
	private final Validator profileValidator;
	private final Validator studyValidator;
	private final Validator moduleValidator;
	private final Validator userValidator;
	private final Validator studentValidator;
	private PersEducNat persEducNat;
	private boolean firstImport = false;
	private AtomicBoolean isInUse = new AtomicBoolean(false);
	private String currentSource;
	private Neo4j neo4j;
	private ConcurrentMap<String, ImporterStructure> structuresByUAI;
	private ConcurrentHashMap<String, String> externalIdMapping;
	private ConcurrentHashMap<String, List<String>> groupClasses = new ConcurrentHashMap<>();
	private ConcurrentMap<String, String> fieldOfStudy= new ConcurrentHashMap<>();
	private ConcurrentMap<String, JsonObject> toSupportPerseducnat1D2D = new ConcurrentHashMap<>();
	private ConcurrentMap<String, JsonObject> studentsStructuresClassesGroups = new ConcurrentHashMap<>();
	private Set<String> blockedIne;
	private Report report;
	private JsonArray importsPrefixList;

	private enum CheckRelationshipsTypes { STRUCTURES, CLASSES, FOS, GROUPS, MODULES }

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

	public void init(final Neo4j neo4j, final Vertx vertx, final String source, String acceptLanguage, boolean blockCreateByIne,
			boolean supportPersEducnat1D2D, boolean checkStudentsRelationships, final Handler<Message<JsonObject>> handler) {
		this.isInUse.set(true);
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

				Handler<Void> handlerCaller = new Handler<Void>()
				{
					@Override
					public void handle(Void v)
					{
						reinitTransaction(); // Resets the neo4j transaction timeout
						if(handler != null)
							handler.handle(event);
					}
				};
				Handler<Void> loginIniter = new Handler<Void>()
				{
					@Override
					public void handle(Void v)
					{
						if("CSV".equals(source))
							handlerCaller.handle(null);
						else
							Validator.initLogin(neo4j, vertx, handlerCaller);
					}
				};

				if ("ok".equals(event.body().getString("status"))) {
					final List<Future> futures = new ArrayList<>();
					if ("CSV".equals(source)) {
						futures.add(loadFieldOfStudy());
					}
					if (blockCreateByIne) {
						futures.add(loadUsedIne());
					}
					if (supportPersEducnat1D2D && "AAF1D".equals(source)) {
						futures.add(loadPersEducnat2D());
					}
					if (checkStudentsRelationships && ("AAF1D".equals(source) || "AAF".equals(source))) {
						studentsStructuresClassesGroups.clear();
						futures.add(loadStudentsStructuresClassesGroups("0"));
						futures.add(loadStudentsStructuresClassesGroups("1"));
						futures.add(loadStudentsStructuresClassesGroups("2"));
						futures.add(loadStudentsStructuresClassesGroups("3"));
						futures.add(loadStudentsStructuresClassesGroups("4"));
						futures.add(loadStudentsStructuresClassesGroups("5"));
						futures.add(loadStudentsStructuresClassesGroups("6"));
						futures.add(loadStudentsStructuresClassesGroups("7"));
						futures.add(loadStudentsStructuresClassesGroups("8"));
						futures.add(loadStudentsStructuresClassesGroups("9"));
						futures.add(loadStudentsStructuresClassesGroups("a"));
						futures.add(loadStudentsStructuresClassesGroups("b"));
						futures.add(loadStudentsStructuresClassesGroups("c"));
						futures.add(loadStudentsStructuresClassesGroups("d"));
						futures.add(loadStudentsStructuresClassesGroups("e"));
						futures.add(loadStudentsStructuresClassesGroups("f"));
					}
					if (!futures.isEmpty()) {
						CompositeFuture.all(futures).onComplete(ar -> {
							loginIniter.handle(null);
						});
					} else {
						loginIniter.handle(null);
					}
				} else {
					loginIniter.handle(null);
				}
			}
		});
	}

	private Future<Void> loadPersEducnat2D() {
		final Promise<Void> promise = Promise.promise();
		final String query =
			"MATCH (s:Structure {source:'AAF'})<-[:DEPENDS]-(:ProfileGroup)<-[r:IN]-(u:User) " +
			"WHERE u.source = 'AAF' and head(u.profiles) IN ['Personnel','Teacher'] and not(has(r.source)) " +
			"RETURN DISTINCT u.externalId as externalId, u.source as source, head(u.profiles) as profile, " +
			"COLLECT(distinct s.externalId) as structuresExternalIds";
		Neo4j.getInstance().execute(query, new JsonObject(), event -> {
			final JsonArray res = event.body().getJsonArray("result");
			if ("ok".equals(event.body().getString("status")) && res != null) {
				for (Object o : res) {
					if (!(o instanceof JsonObject)) continue;
					final JsonObject j = (JsonObject) o;
					toSupportPerseducnat1D2D.putIfAbsent(j.getString("externalId"), j);
				}
				promise.complete();
			} else {
				promise.fail("Error when load perseducnat 2D");
			}
		});
		return promise.future();
	}

	private Future<Void> loadStudentsStructuresClassesGroups(String suffix) {
		final Promise<Void> promise = Promise.promise();
		final String query =
			"MATCH (s:Structure {source:{source}})<-[:DEPENDS]-(:ProfileGroup)<-[r:IN]-(u:User) " +
			"where s.id ends with '" + suffix + "' " +
			"OPTIONAL MATCH u-[:IN]->(:ProfileGroup)-[:DEPENDS]->(c:Class) " +
			"OPTIONAL MATCH u-[:COURSE]->(f:FieldOfStudy) " +
			"OPTIONAL MATCH u-[:FOLLOW]->(m:Module) " +
			// "OPTIONAL MATCH u-[:IN]->(g:Group) " +
			"RETURN u.externalId as externalId, COLLECT(s.externalId) as structures, " +
			"COLLECT(f.externalId) as fos, COLLECT(m.externalId) as modules, " +
			// "COLLECT(g.externalId) as groups, " +
			"COLLECT(c.externalId) as classes";
			// "MATCH (s:Structure {source:{source}})<-[:DEPENDS]-(:ProfileGroup)<-[r:IN]-(u:User {source:{source}}) " +
			// "WHERE head(u.profiles) = 'Student' and not(has(r.source)) " +
			// "OPTIONAL MATCH u-[:IN]->(:ProfileGroup)-[:DEPENDS]->(c:Class) " +
			// "OPTIONAL MATCH u-[:COURSE]->(f:FieldOfStudy) " +
			// "OPTIONAL MATCH u-[:FOLLOW]->(m:Module) " +
			// "OPTIONAL MATCH u-[:IN]->(g:Group {source:{source}}) " +
			// "WHERE (g:FunctionalGroup OR g:FunctionGroup) " +
			// "RETURN DISTINCT u.externalId as externalId, " +
			// "COLLECT(distinct s.externalId) as structures, " +
			// "COLLECT(distinct f.externalId) as fos, " +
			// "COLLECT(distinct m.externalId) as modules, " +
			// "COLLECT(distinct g.externalId) as groups, " +
			// "COLLECT(distinct c.externalId) as classes";
		Neo4j.getInstance().execute(query, new JsonObject().put("source", currentSource), event -> {
			final JsonArray res = event.body().getJsonArray("result");
			if ("ok".equals(event.body().getString("status")) && res != null) {
				for (Object o : res) {
					if (!(o instanceof JsonObject)) continue;
					final JsonObject j = (JsonObject) o;
					studentsStructuresClassesGroups.putIfAbsent(j.getString("externalId"), j);
				}
				log.info("Users structures... loaded suffix " + suffix + ". Map numbers : " + studentsStructuresClassesGroups.size());
				promise.complete();
			} else {
				promise.fail("Error when load perseducnat 2D");
			}
		});
		return promise.future();
	}

	private Future<Void> loadFieldOfStudy() {
		final Future<Void> f = Future.future();
		final String query = "MATCH (f:FieldOfStudy) return f.externalId as externalId, f.name as name";
		Neo4j.getInstance().execute(query, new JsonObject(), event -> {
			final JsonArray res = event.body().getJsonArray("result");
			if ("ok".equals(event.body().getString("status")) && res != null) {
				for (Object o : res) {
					if (!(o instanceof JsonObject)) continue;
					final JsonObject j = (JsonObject) o;
					fieldOfStudy.putIfAbsent(j.getString("name"), j.getString("externalId"));
				}
			}
			f.complete();
		});
		return f;
	}

	private Future<Void> loadUsedIne() {
		final Future<Void> f = Future.future();
		final String query =
				"MATCH (u:User) " +
				"WHERE u.source IN {sources} AND HAS(u.ine) AND NOT(HAS(u.disappearanceDate)) AND NOT(HAS(u.deleteDate)) " +
				"RETURN COLLECT(DISTINCT u.ine) ines";
		final JsonArray sources = new JsonArray();
		for (Object o: sources) {
			if (currentSource.equals(o)) break;
			sources.add(o);
		}
		if (sources.isEmpty()) {
			f.complete();
			return f;
		}
		final JsonObject params = new JsonObject().put("sources", sources);
		Neo4j.getInstance().execute(query, params, event -> {
			final JsonArray res = event.body().getJsonArray("result");
			if ("ok".equals(event.body().getString("status")) && res != null && res.size() == 1) {
				final JsonArray a = getOrElse(res.getJsonObject(0).getJsonArray("ines"), new JsonArray());
				blockedIne = new HashSet<>(a.getList());
			}
			f.complete();
		});
		return f;
	}

	public TransactionHelper getTransaction() {
		return transactionHelper;
	}

	public void clear() {
		structures.clear();
		profiles.clear();
		userImportedExternalId.clear();
		structuresImportedExternalId.clear();
		groupClasses.clear();
		report = null;
		transactionHelper = null;
		this.isInUse.set(false);
	}

	public boolean isReady() {
		return this.isInUse.get() == false; //return transactionHelper == null;
	}

	public void persist(final Handler<Message<JsonObject>> handler) {
		if (transactionHelper != null) {
			transactionHelper.commit(new Handler<Message<JsonObject>>() {
				@Override
				public void handle(Message<JsonObject> message) {
					reinitTransaction();
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
		if(persEducNat != null)
			persEducNat.setTransactionHelper(transactionHelper);
	}

	public ImporterStructure createOrUpdateStructure(JsonObject struct) {
		JsonArray groups = null;
		if (struct != null) {
			groups = struct.getJsonArray("groups");
		}
		String name = struct.getString("name");
		if(name != null)
			struct.put("feederName", name); // This is used to reset manual names
		final String error = structureValidator.validate(struct);
		ImporterStructure s = null;
		if (error != null) {
			report.addIgnored("Structure", error, struct);
			log.warn(error);
		} else {
			struct.put("source", currentSource);
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
					s.update(struct);
				} else {
					try {
						s = new ImporterCachedStructure(externalId, struct);
						structures.putIfAbsent(externalId, s);
						s.create();
					} catch (IllegalArgumentException e) {
						log.error(e.getMessage());
					}
				}
			}
		}
		if(s != null)
			structuresImportedExternalId.add(s.getExternalId());
		return s;
	}

	public void forceStructureSource(ImporterStructure s)
	{
		s.setSource(currentSource);
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
				params = new JsonObject().put("props", object);
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
				params = new JsonObject().put("props", object);
			}
			transactionHelper.add(query, params);
		}
	}

	public void createOrUpdateUser(JsonObject object) {
		createOrUpdateUser(object, null);
	}

	public void createOrUpdateUser(JsonObject object, JsonArray linkStudent) {
		createOrUpdateUser(object, linkStudent, false);
	}

	public void createOrUpdateUser(JsonObject object, JsonArray linkStudent, boolean linkRelativeWithoutChild) {
		createOrUpdateUser(object, linkStudent, linkRelativeWithoutChild, null);
	}

	public void createOrUpdateUser(JsonObject object, JsonArray linkStudent, boolean linkRelativeWithoutChild, String[][] linkGroups) {
		final String error = userValidator.validate(object);
		if (error != null) {
			report.addIgnored("Relative", error, object);
			log.warn(error);
		} else {
			object.put("source", currentSource);
			userImportedExternalId.add(object.getString("externalId"));
			String query =
					"MERGE (u:User { externalId : {externalId}}) " +
					"ON CREATE SET u.id = {id}, u.login = {login}, u.activationCode = {activationCode}, " +
					"u.displayName = {displayName}, u.displayNameSearchField = {displayNameSearchField}, u.created = {created} " +
					"WITH u " +
					"WHERE u.checksum IS NULL OR u.checksum <> {checksum} " +
					"SET " + Neo4jUtils.nodeSetPropertiesFromJson("u", object,
							"id", "externalId", "login", "activationCode", "displayName", "displayNameSearchField", "email", "emailSearchField", "created");
			transactionHelper.add(query, object);
			checkUpdateEmail(object);
			if (linkStudent != null && linkStudent.size() > 0) {
				String query2 =
						"START u0=node:node_auto_index(externalId={externalId}), " +
						"s=node:node_auto_index({studentExternalIds}) " +
						"MATCH u0-[:MERGED*0..1]->u " +
						"WHERE NOT(HAS(u.mergedWith)) " +
						"MERGE u<-[:RELATED]-s ";
				JsonObject p = new JsonObject()
						.put("externalId", object.getString("externalId"))
						.put("studentExternalIds",
								"externalId:" + Joiner.on(" OR externalId:").join(linkStudent));
				transactionHelper.add(query2, p);
			} else if (linkRelativeWithoutChild) {
				final String externalId = object.getString("externalId");
				JsonArray structures = getMappingStructures(object.getJsonArray("structures"));
				if (externalId != null && structures != null && structures.size() > 0) {
					JsonObject p = new JsonObject().put("userExternalId", externalId);
					String q1 = "MATCH (s:Structure)<-[:DEPENDS]-(g:ProfileGroup)-[:HAS_PROFILE]->(p:Profile), " +
							"(:User { externalId : {userExternalId}})-[:MERGED*0..1]->(u:User) " +
							"USING INDEX s:Structure(externalId) " +
							"USING INDEX p:Profile(externalId) " +
							"WHERE s.externalId IN {structuresAdmin} " +
							"AND p.externalId = {profileExternalId} AND NOT(HAS(u.mergedWith)) " +
							"MERGE u-[:IN]->g";
					p.put("structuresAdmin", structures)
							.put("profileExternalId", DefaultProfiles.RELATIVE_PROFILE_EXTERNAL_ID);
					transactionHelper.add(q1, p);
				}
			}

			if (linkGroups != null && object.getString("externalId") != null) {
				final JsonArray groups = new JsonArray();
				for (String[] structGroup : linkGroups) {
					if (structGroup != null && structGroup[0] != null && structGroup[1] != null) {
						groups.add(structGroup[1]);
					}
				}
				String q2 = "MATCH (g:FunctionGroup), (u:User { externalId : {userExternalId}}) " +
						"WHERE g.externalId IN {groups} " +
						"AND NOT(HAS(u.mergedWith)) " +
						"AND g.source = {source} " +
						"MERGE u-[:IN]->g";
				JsonObject p = new JsonObject()
						.put("userExternalId", object.getString("externalId"))
						.put("source", currentSource)
						.put("groups", groups);
				transactionHelper.add(q2, p);
			}
		}
	}

	public void createOrUpdateGuest(JsonObject object, String[][] linkClasses) {
		final String error = userValidator.validate(object);
		if (error != null) {
			report.addIgnored("Guest", error, object);
			log.warn(error);
		} else {
			object.put("source", currentSource);
			final String externalId = object.getString("externalId");
			userImportedExternalId.add(externalId);
			String query =
					"MERGE (u:User { externalId : {externalId}}) " +
					"ON CREATE SET u.id = {id}, u.login = {login}, u.activationCode = {activationCode}, " +
					"u.displayName = {displayName}, u.displayNameSearchField = {displayNameSearchField}, u.created = {created} " +
					"WITH u " +
					"WHERE u.checksum IS NULL OR u.checksum <> {checksum} " +
					"SET " + Neo4jUtils.nodeSetPropertiesFromJson("u", object,
					"id", "externalId", "login", "activationCode", "displayName", "displayNameSearchField", "email", "emailSearchField", "created");
			transactionHelper.add(query, object);
			checkUpdateEmail(object);
			JsonArray structures = getMappingStructures(object.getJsonArray("structures"));
			if (externalId != null && structures != null && structures.size() > 0) {
				JsonObject p = new JsonObject().put("userExternalId", externalId);
				String q1 = "MATCH (s:Structure)<-[:DEPENDS]-(g:ProfileGroup)-[:HAS_PROFILE]->(p:Profile), " +
							"(:User { externalId : {userExternalId}})-[:MERGED*0..1]->(u:User) " +
							"USING INDEX s:Structure(externalId) " +
							"USING INDEX p:Profile(externalId) " +
							"WHERE s.externalId IN {structuresAdmin} " +
							"AND p.externalId = {profileExternalId} AND NOT(HAS(u.mergedWith)) " +
							"MERGE u-[:IN]->g";
				p.put("structuresAdmin", structures)
						.put("profileExternalId", DefaultProfiles.GUEST_PROFILE_EXTERNAL_ID);
				transactionHelper.add(q1, p);
				String qs =
						"MATCH (u:User {externalId : {userExternalId}})-[r:IN]-(g:Group)-[:DEPENDS]->(s:Structure) " +
						"WHERE NOT(s.externalId IN {structures}) AND (NOT(HAS(r.source)) OR r.source = {source}) " +
						"DELETE r ";
				JsonObject ps = new JsonObject()
						.put("userExternalId", externalId)
						.put("source", currentSource)
						.put("structures", structures);
				transactionHelper.add(qs, ps);
			}
			if (externalId != null && linkClasses != null) {
				JsonArray classes = new fr.wseduc.webutils.collections.JsonArray();
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
								.put("userExternalId", externalId)
								.put("profileExternalId", DefaultProfiles.GUEST_PROFILE_EXTERNAL_ID)
								.put("structure", structClass[0])
								.put("class", structClass[1]);
						transactionHelper.add(q, p);
						classes.add(structClass[1]);
					}
				}
				String q =
						"MATCH (u:User {externalId : {userExternalId}})-[r:IN]-(g:Group)-[:DEPENDS]->(c:Class) " +
						"WHERE NOT(c.externalId IN {classes}) AND (NOT(HAS(r.source)) OR r.source = {source}) " +
						"DELETE r ";
				JsonObject p = new JsonObject()
						.put("userExternalId", externalId)
						.put("source", currentSource)
						.put("classes", classes);
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
				object.put("source", currentSource);
				userImportedExternalId.add(object.getString("externalId"));
				String query =
					"MERGE (u:`User` { externalId : {externalId}}) " +
					"ON CREATE SET u.id = {id}, u.login = {login}, u.activationCode = {activationCode}, " +
					"u.displayName = {displayName}, u.displayNameSearchField = {displayNameSearchField}, u.created = {created} " +
					"WITH u " +
					"WHERE u.checksum IS NULL OR u.checksum <> {checksum} " +
					"SET " + Neo4jUtils.nodeSetPropertiesFromJson("u", object,
							"id", "externalId", "login", "activationCode", "displayName", "displayNameSearchField", "email", "emailSearchField", "created");
				transactionHelper.add(query, object);
				checkUpdateEmail(object);
			}
			if (relationshipQueries) {
				final String externalId = object.getString("externalId");
				JsonArray structures = getMappingStructures(object.getJsonArray("structures"));
				if (externalId != null && structures != null && structures.size() > 0) {
					String query;
					if (studentsRelationshipsNotExists(externalId, CheckRelationshipsTypes.STRUCTURES, structures)) {
						JsonObject p = new JsonObject().put("userExternalId", externalId);
						if (structures.size() == 1) {
							query = "MATCH (s:Structure {externalId : {structureAdmin}})<-[:DEPENDS]-(g:ProfileGroup)-[:HAS_PROFILE]->(p:Profile {externalId : {profileExternalId}}), " +
									"(u:User { externalId : {userExternalId}}) " +
									"WHERE NOT(HAS(u.mergedWith)) " +
									"MERGE u-[:ADMINISTRATIVE_ATTACHMENT]->s " +
									"WITH u, g " +
									"MERGE u-[:IN]->g";
							p.put("structureAdmin", structures.getString(0))
									.put("profileExternalId", profileExternalId);
						} else {
							query = "MATCH (s:Structure)<-[:DEPENDS]-(g:ProfileGroup)-[:HAS_PROFILE]->(p:Profile), " +
									"(u:User { externalId : {userExternalId}})) " +
									"WHERE s.externalId IN {structuresAdmin} AND NOT(HAS(u.mergedWith)) " +
									"AND p.externalId = {profileExternalId} " +
									"MERGE u-[:ADMINISTRATIVE_ATTACHMENT]->s " +
									"WITH u, g " +
									"MERGE u-[:IN]->g";
							p.put("structuresAdmin", structures)
									.put("profileExternalId", profileExternalId);
						}
						transactionHelper.add(query, p);
					}
					String qs =
							"MATCH (u:User {externalId : {userExternalId}})-[r:IN]-(g:Group)-[:DEPENDS]->(s:Structure) " +
							"WHERE NOT(s.externalId IN {structures}) AND (NOT(HAS(r.source)) OR r.source = {source}) " +
							"DELETE r ";
					JsonObject ps = new JsonObject()
							.put("userExternalId", externalId)
							.put("source", currentSource)
							.put("structures", structures);
					transactionHelper.add(qs, ps);
					final String daa =
							"MATCH (u:User {externalId : {userExternalId}})-[r:ADMINISTRATIVE_ATTACHMENT]->(s:Structure) " +
							"WHERE NOT(s.externalId IN {structures}) AND (NOT(HAS(r.source)) OR r.source = {source}) " +
							"DELETE r";
					transactionHelper.add(daa, ps);
				}
				JsonArray classes = new fr.wseduc.webutils.collections.JsonArray();
				if (externalId != null && linkClasses != null) {
					for (String[] structClass : linkClasses) {
						if (structClass != null && structClass[0] != null && structClass[1] != null) {
							classes.add(structClass[1]);
						}
					}
					if (studentsRelationshipsNotExists(externalId, CheckRelationshipsTypes.CLASSES, classes)) {
						String query =
								"MATCH (c:Class)<-[:DEPENDS]-(g:ProfileGroup)" +
								"-[:DEPENDS]->(:ProfileGroup)-[:HAS_PROFILE]->(:Profile {externalId : {profileExternalId}}), " +
								"(u:User { externalId : {userExternalId}}) " +
								"WHERE c.externalId IN {classes} AND NOT(HAS(u.mergedWith))  " +
								"MERGE u-[:IN]->g";
						JsonObject p0 = new JsonObject()
								.put("userExternalId", externalId)
								.put("profileExternalId", profileExternalId)
								.put("classes", classes);
						transactionHelper.add(query, p0);
					}
				}
				if (externalId != null) {
					String q =
							"MATCH (u:User {externalId : {userExternalId}})-[r:IN]-(g:Group)-[:DEPENDS]->(c:Class) " +
							"WHERE NOT(c.externalId IN {classes}) AND (NOT(HAS(r.source)) OR r.source = {source}) " +
							"DELETE r ";
					JsonObject p = new JsonObject()
							.put("userExternalId", externalId)
							.put("source", currentSource)
							.put("classes", classes);
					transactionHelper.add(q, p);
				}
				final JsonArray groups = new fr.wseduc.webutils.collections.JsonArray();
				if (externalId != null && linkGroups != null) {
					for (String[] structGroup : linkGroups) {
						if (structGroup != null && structGroup[0] != null && structGroup[1] != null) {
							groups.add(structGroup[1]);
						}
					}
					// if (studentsRelationshipsNotExists(externalId, CheckRelationshipsTypes.GROUPS, groups)) {
						String query =
								"MATCH (u:User { externalId: {userExternalId}}) " +
								"WHERE NOT(HAS(u.mergedWith)) " +
								"WITH u " +
								"MATCH (g:Group) " +
								"WHERE (g:FunctionalGroup OR g:FunctionGroup) AND g.externalId IN {groups} AND g.source = {source} " +
								"MERGE (u)-[:IN]->(g) ";
						JsonObject p = new JsonObject()
								.put("userExternalId", externalId)
								.put("source", currentSource)
								.put("groups", groups);
						transactionHelper.add(query, p);
					// }
				}
				if (externalId != null) {
					final String qdfg =
							"MATCH (u:User {externalId : {userExternalId}})-[r:IN]-(g:FunctionalGroup) " +
							"WHERE (NOT(HAS(r.source)) OR r.source = {source}) " +
							"AND (NOT(g.externalId IN {groups}) OR (g.source <> {source})) " +
							"DELETE r ";
					final JsonObject pdfg = new JsonObject()
							.put("userExternalId", externalId)
							.put("source", currentSource)
							.put("groups", groups);
					transactionHelper.add(qdfg, pdfg);
				}

				if (externalId != null && module != null &&
						studentsRelationshipsNotExists(externalId, CheckRelationshipsTypes.MODULES, new JsonArray().add(module))) {
					String query =
							"MATCH (u:User {externalId:{userExternalId}}), " +
							"(m:Module {externalId:{moduleStudent}}) " +
							"MERGE u-[:FOLLOW]->m";
					JsonObject p = new JsonObject()
							.put("userExternalId", externalId)
							.put("moduleStudent", module);
					transactionHelper.add(query, p);
				}
				if (externalId != null && fieldOfStudy != null && fieldOfStudy.size() > 0 &&
						studentsRelationshipsNotExists(externalId, CheckRelationshipsTypes.FOS, fieldOfStudy)) {
					String query =
								"MATCH (u:User {externalId:{userExternalId}}), (f:FieldOfStudy) " +
								"WHERE f.externalId IN {fieldOfStudyStudent} " +
								"MERGE u-[:COURSE]->f";
					JsonObject p = new JsonObject()
								.put("userExternalId", externalId)
								.put("fieldOfStudyStudent", fieldOfStudy);
					transactionHelper.add(query, p);
				}
				if (externalId != null && relative != null && relative.size() > 0) {
					String query2 =
							"MATCH (:User {externalId:{userExternalId}})-[r:RELATED|COMMUNIQUE_DIRECT]-(p:User) " +
							"WHERE NOT(p.externalId IN {relatives}) AND (NOT(HAS(r.source)) OR r.source = {source}) " +
							"DELETE r ";
					JsonObject p2 = new JsonObject()
							.put("userExternalId", externalId)
							.put("source", currentSource)
							.put("relatives", relative);
					transactionHelper.add(query2, p2);
					for (Object o : relative) {
						if (!(o instanceof String)) continue;
						String query =
								"MATCH (u:User {externalId:{userExternalId}}), " +
								"(:User {externalId:{user}})-[:MERGED*0..1]->(r:User) " +
								"WHERE NOT(HAS(r.mergedWith)) " +
								"MERGE u-[:RELATED]->r " +
								"WITH r, u " +
								"WHERE {user} <> r.externalId AND LENGTH(FILTER(eId IN u.relative WHERE eId STARTS WITH r.externalId)) = 0 " +
								"SET u.relative = coalesce(u.relative, []) + (r.externalId + '$10$1$1$0$0') ";
						JsonObject p = new JsonObject()
								.put("userExternalId", externalId)
								.put("user", (String) o);
						transactionHelper.add(query, p);
					}
				}
			}
		}
	}

	private boolean studentsRelationshipsNotExists(String externalId, CheckRelationshipsTypes checkType,
			JsonArray importElements) {
		final JsonObject user = studentsStructuresClassesGroups.get(externalId);
		if (user != null && importElements != null) {
			final JsonArray dbElements = user.getJsonArray(checkType.name().toLowerCase());
			for (Object e: importElements) {
				if (dbElements == null || !dbElements.contains(e)) {
					return true;
				}
			}
			return false;
		}
		return true;
	}

	public void linkRelativeToStructure(String profileExternalId) {
		linkRelativeToStructure(profileExternalId, null);
	}

	public void linkRelativeToStructure(String profileExternalId, String prefix) {
		linkRelativeToStructure(profileExternalId, prefix, null);
	}

	public void linkRelativeToStructure(String profileExternalId, String prefix, String structureExternalId) {
		JsonObject j = new JsonObject().put("profileExternalId", profileExternalId);
		String filter = "";
		if (isNotEmpty(prefix)) {
			filter = "AND u.externalId STARTS WITH {prefix} ";
			j.put("prefix", prefix);
		}
		if (isNotEmpty(structureExternalId)) {
			filter += "AND c.externalId = {structureExternalId} ";
			j.put("structureExternalId", structureExternalId);
		}
		String query =
				"MATCH (u:User)<-[:RELATED]-(s:User)-[:IN]->(scg:ProfileGroup)" +
				"-[:DEPENDS]->(c:Structure)<-[:DEPENDS]-(rcg:ProfileGroup)-[:HAS_PROFILE]->(p:Profile) " +
				"WHERE p.externalId = {profileExternalId} " + filter +
				"MERGE u-[:IN]->rcg";
		transactionHelper.add(query, j);
	}

	public void linkRelativeToClass(String profileExternalId) {
		linkRelativeToClass(profileExternalId, null);
	}

	public void linkRelativeToClass(String profileExternalId, String prefix) {
		linkRelativeToClass(profileExternalId, prefix, null);
	}

	public void linkRelativeToClass(String profileExternalId, String prefix, String structureExternalId) {
		JsonObject j = new JsonObject().put("profileExternalId", profileExternalId).put("source", currentSource);
		String filter = "";
		if (isNotEmpty(prefix)) {
			filter = "AND u.externalId STARTS WITH {prefix} ";
			j.put("prefix", prefix);
		}
		String filter3 = "(s:Structure) ";
		String additionalMatch = "";
		String additionalMatch2 = "";
		if (isNotEmpty(structureExternalId)) {
			additionalMatch = "MATCH (s:Structure {externalId : {structureExternalId}})<-[:BELONGS]-(c:Class) WITH c ";
			additionalMatch2 = "-[:BELONGS]->(s:Structure {externalId : {structureExternalId}}) ";
			filter3 = "(s:Structure {externalId : {structureExternalId}}) ";
			j.put("structureExternalId", structureExternalId);
		}
		String query =
				additionalMatch +
				"MATCH (u:User)<-[:RELATED]-(s:User)-[:IN]->(scg:ProfileGroup)" +
				"-[:DEPENDS]->(c:Class)<-[:DEPENDS]-(rcg:ProfileGroup)" +
				"-[:DEPENDS]->(pg:ProfileGroup)-[:HAS_PROFILE]->(p:Profile) " +
				"WHERE p.externalId = {profileExternalId} " + filter +
				"MERGE u-[:IN]->rcg";
		transactionHelper.add(query, j);
		String query2 =
				"MATCH (u:User)<-[:RELATED]-(:User)-[:IN]->(:ProfileGroup)-[:DEPENDS]->(c:Class) " + additionalMatch2 +
				"WITH u, COLLECT(distinct c.id) as cIds " +
				"MATCH u-[r:IN]-(g:Group)-[:DEPENDS]->(c:Class) " + additionalMatch2 +
				"WHERE NOT(c.id IN cIds) " + filter +
				"DELETE r ";
		transactionHelper.add(query2, j);
		String query3 =
				"MATCH (u:User)<-[:RELATED]-(:User)-[:IN]->(:ProfileGroup)-[:DEPENDS]->" + filter3 +
				"WITH u, COLLECT(distinct s.id) as sIds " +
				"MATCH u-[r:IN]-(g:Group)-[:DEPENDS]->" + filter3 +
				"WHERE NOT(s.id IN sIds) " + filter +
				"DELETE r ";
		transactionHelper.add(query3, j);
	}

	public void removeOldFunctionalGroup() {
		// transactionHelper.add("MATCH (g:Group) WHERE g:FunctionalGroup OR g:FunctionGroup OR g:HTGroup OR g:DirectionGroup set g.notEmptyGroup = false;", null);
		// transactionHelper.add("MATCH (g:Group)<-[:IN]-(:User) WHERE g:FunctionalGroup OR g:FunctionGroup OR g:HTGroup OR g:DirectionGroup with distinct g set g.notEmptyGroup = true;", null);
		// transactionHelper.add("MATCH (g:Group {notEmptyGroup:false}) WHERE g:FunctionalGroup OR g:FunctionGroup OR g:HTGroup OR g:DirectionGroup detach delete g;", null);

		transactionHelper.add("MATCH (g:Group {nbUsers:0}) WHERE g:FunctionalGroup OR g:FunctionGroup OR g:HTGroup OR g:DirectionGroup detach delete g;", null);
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
		// transactionHelper.add("MATCH (c:Class) set c.notEmptyClass = false;", null);
		// transactionHelper.add("MATCH (c:Class)<-[:DEPENDS]-(:Group)<-[:IN]-(:User) with distinct c set c.notEmptyClass = true;", null);
		// transactionHelper.add("MATCH (c:Class {notEmptyClass : false})<-[r1:DEPENDS]-(g:Group) DETACH DELETE c, g, r1", null);

		transactionHelper.add("MATCH (c:Class)<-[:DEPENDS]-(g:Group) with distinct c, sum(COALESCE(g.nbUsers, 4200)) as cNbUsers set c.cNbUsers = cNbUsers;", null);
		transactionHelper.add("MATCH (c:Class {cNbUsers:0})<-[r1:DEPENDS]-(g:Group) DETACH DELETE c, g, r1", null);

		// prevent difference between relationships and properties
		String query2 =
				"MATCH (u:User) " +
				"WHERE NOT(HAS(u.deleteDate)) AND has(u.classes) AND LENGTH(u.classes) > 0 " +
				"AND NOT(u-[:IN]->(:ProfileGroup)-[:DEPENDS]->(:Class)) " +
				"SET u.classes = [];";
		transactionHelper.add(query2, null);
		String query3 =
				"MATCH (u:User)-[:IN]->(:ProfileGroup)-[:DEPENDS]->(c:Class) " +
				"WHERE has(u.classes) " +
				"WITH u, collect(c.externalId) as classes " +
				"SET u.classes = classes";
		transactionHelper.add(query3, null);
	}

	protected static FeederLogger logger(final String method, final String prefix){
		return new FeederLogger(e -> method, e-> "prefix: "+prefix);
	}

	public void applyRemoveUsersFromStructure(String prefix, final Handler<Void> handler) {
		final FeederLogger log = logger("applyRemoveRelativesFromStructure" ,prefix);
		log.info(t -> "START to get relative removed from structure | source: "+currentSource, true);
		JsonObject params = new JsonObject().put("currentSource", currentSource);
		String filter = "";
		if (isNotEmpty(prefix)) {
			filter = "AND u.externalId STARTS WITH {prefix} ";
			params.put("prefix", prefix);
		}

		final String query =
				"MATCH (u:User) " +
				"WHERE u.source = {currentSource} AND HAS(u.removedFromStructures) AND u.removedFromStructures <> [] " + filter +
				"RETURN u.externalId as externalId";
		TransactionManager.getNeo4jHelper().execute(query, params, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> message) {
				JsonArray res = message.body().getJsonArray("result");
				if ("ok".equals(message.body().getString("status")) && res != null) {
					for (Object o : res) {
						if (!(o instanceof JsonObject)) continue;
						String externalId = ((JsonObject) o).getString("externalId");
						if (externalId != null) {
							ManualFeeder.applyRemoveUserFromStructure(null, externalId, null, null, transactionHelper);
						}
					}
					log.info(t -> "SUCCEED to get relative removed from structure | source: "+currentSource);
				} else {
					log.error(t -> "FAILED to get relative removed from structure | source: "+currentSource+" | detail:"+message.body().encode());
				}
				handler.handle(null);
			}
		});
	}

	public void addRelativeProperties(String source) {
		String query =
				"MATCH (u:User {source: {source}})-[:RELATED]->(u2:User) " +
				"WHERE HEAD(u.profiles) = 'Student' AND NOT(HAS(u.relative)) " +
				"SET u.relative = coalesce(u.relative, []) + (u2.externalId + '$10$1$1$0$0')";
		transactionHelper.add(query, new JsonObject().put("source", source));
	}

	public ImporterStructure getStructure(String externalId) {
		return structures.get(externalId);
	}

	public Profile getProfile(String externalId) {
		return profiles.get(externalId);
	}

	public void markMissingUsers(Handler<Void> handler) {
		markMissingUsers(null, handler);
	}

	public void markMissingUsers(String structureExternalId, String prefix, Handler<Void> handler) {
		markMissingUsers(structureExternalId, currentSource, userImportedExternalId, transactionHelper, prefix, handler);
	}

	public void markMissingUsers(String structureExternalId, final Handler<Void> handler) {
		markMissingUsers(structureExternalId, currentSource, userImportedExternalId, transactionHelper, null, handler);
	}

	public void markMissingUsers(String structureExternalId, String currentSource, String prefix, final Handler<Void> handler) {
		markMissingUsers(structureExternalId, currentSource, userImportedExternalId, transactionHelper, prefix, handler);
	}

	public static void markMissingUsers(String structureExternalId, String currentSource,
			final Set<String> userImportedExternalId, final TransactionHelper transactionHelper,
			final Handler<Void> handler) {
		markMissingUsers(structureExternalId, currentSource, userImportedExternalId, transactionHelper, null, handler);
	}

	public static void markMissingUsers(String structureExternalId, String currentSource,
			final Set<String> userImportedExternalId, final TransactionHelper transactionHelper, String prefix,
			final Handler<Void> handler) {
		final FeederLogger log = logger("markMissingUsers",prefix);
		log.info(t -> String.format("START mark missing user | source: %s | structureExternalId: %s ",currentSource, structureExternalId));
		String query;
		JsonObject params = new JsonObject().put("currentSource", currentSource);
		String filter = "";
		if (isNotEmpty(prefix)) {
			filter = "AND u.externalId STARTS WITH {prefix} ";
			params.put("prefix", prefix);
		}
		if (structureExternalId != null) {
			query = "MATCH (:Structure {externalId : {externalId}})<-[:DEPENDS]-(:ProfileGroup)<-[:IN]-(u:User) " +
					"WHERE u.source = {currentSource} " + filter +
					"RETURN u.externalId as externalId";
			params.put("externalId", structureExternalId);
		} else {
			query = "MATCH (u:User) WHERE u.source = {currentSource} " + filter + "RETURN u.externalId as externalId";
		}
		TransactionManager.getNeo4jHelper().execute(query, params, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> message) {
				JsonArray res = message.body().getJsonArray("result");
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
							.put("date", System.currentTimeMillis());
					for (String eId : existingUser) {
						transactionHelper.add(q, p.copy().put("externalId", eId));
					}
					String q2 = // remove mark of imported users
							"START u=node:node_auto_index(externalId={externalId}) " +
									"WHERE HAS(u.disappearanceDate) " +
									"REMOVE u.disappearanceDate " +
									"RETURN u.id AS id, u.login AS login";
					for (String eId : userImportedExternalId) {
						transactionHelper.add(q2, new JsonObject().put("externalId", eId), new Handler<Either<String, JsonArray>>()
						{
							@Override
							public void handle(Either<String, JsonArray> result)
							{
								if(result.isRight())
								{
									if(result.right().getValue().size() > 0)
									{
										JsonObject unmarkedUser = result.right().getValue().getJsonObject(0);
										DuplicateUsers.checkDuplicatesIntegrity(unmarkedUser.getString("id"), new Handler<Message<JsonObject>>()
										{
											@Override
											public void handle(Message<JsonObject> msg)
											{
												if("ok".equals(msg.body().getString("status")) == false)
													log.error(t -> String.format("FAILED check duplicates for returned user %s", unmarkedUser.getString("login")));
											}
										});
									}
								}
								else
									log.error(t -> String.format("FAILED unmark user %s", eId));
							}
						});
					}
					log.info(t -> String.format("SUCCEED mark missing user | source: %s | structureExternalId: %s ",currentSource, structureExternalId));
				}else{
					log.error(t -> String.format("FAILED mark missing user | source: %s | structureExternalId: %s | details: %s ",currentSource, structureExternalId, res.encode()));
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
				"REMOVE u.deleteDate ";
		transactionHelper.add(query, new JsonObject().put("source", currentSource));
		String query2 =
				"MATCH (u:User)-[r:IN]->(:DeleteGroup) " +
				"WHERE not(has(u.deleteDate)) " +
				"DELETE r ";
		transactionHelper.add(query2, new JsonObject());
		String query3 =
				"MATCH (u1:User)-[r:DUPLICATE]->(u2:User) " +
				"WHERE u1.source = {source} and u2.source = {source} AND NOT(HAS(u1.disappearanceDate)) and NOT(HAS(u2.disappearanceDate)) " +
				"DELETE r";
		transactionHelper.add(query3, new JsonObject().put("source", currentSource));
	}

	public void addStructureNameInGroups(String prefix) {
		addStructureNameInGroups(null, prefix);
	}

	public void addStructureNameInGroups(String structureExternalId, String prefix) {
		final JsonObject params = new JsonObject();
		final String filter;
		if (isNotEmpty(structureExternalId)) {
			filter = "AND s.externalId = {externalId} ";
			params.put("externalId", structureExternalId);
		} else if (isNotEmpty(prefix)) {
			filter = "AND s.externalId STARTS WITH {prefix} ";
			params.put("prefix", prefix);
		} else {
			filter = "AND s.source = {currentSource} ";
			params.put("currentSource", currentSource);
		}
		final String query =
				"MATCH (s:Structure)<-[:BELONGS]-(c:Class)<-[:DEPENDS]-(pg:ProfileGroup) " +
				"WHERE NOT(HAS(pg.structureName)) OR pg.structureName <> s.name " + filter +
				"SET pg.structureName = s.name";
		transactionHelper.add(query, params);
	}

	public void removeOldCommunicationRules(String prefix) {
		final String query =
				"MATCH (s:Structure)<-[:DEPENDS*1..2]-(g:Group)-[c:COMMUNIQUE]-(u:User) " +
				"WHERE s.externalId STARTS WITH {prefix} AND u.source = {currentSource} " +
				"AND (c.source IS NULL OR c.source <> 'MANUAL') AND NOT (u)-[:IN]->(g) " +
				"DELETE c";
		transactionHelper.add(query, new JsonObject().put("prefix", prefix).put("currentSource", currentSource));
	}

	public void countUsersInGroups() {
		User.countUsersInGroups(null, null, transactionHelper);
	}

	public void deleteOldProfileAttachments() {
		// Split in two query for performance reason
		final String query =
				"MATCH (u:User)-[r:IN|COMMUNIQUE]-(pg:ProfileGroup {filter:'Teacher'}) " +
				"WHERE head(u.profiles) = 'Personnel' AND pg.filter <> head(u.profiles) " +
				"DELETE r";
		transactionHelper.add(query, new JsonObject());
		final String query2 =
				"MATCH (u:User)-[r:IN|COMMUNIQUE]-(pg:ProfileGroup {filter:'Personnel'}) " +
				"WHERE head(u.profiles) = 'Teacher' AND pg.filter <> head(u.profiles) " +
				"DELETE r";
		transactionHelper.add(query2, new JsonObject());
	}

	public Report getReport() {
		return report;
	}

	public Set<String> getUserImportedExternalId() {
		return userImportedExternalId;
	}

	public Set<String> getStructureImportedExternalId() {
		return structuresImportedExternalId;
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

	public boolean blockedIne(JsonObject user) {
		return blockedIne != null && user != null && blockedIne.contains(user.getString("ine"));
	}

	public ConcurrentMap<String, JsonObject> getToSupportPerseducnat1D2D() {
		return toSupportPerseducnat1D2D;
	}

	public void setPrefixToImportList(JsonArray importSubDirectories) {
		this.importsPrefixList = importSubDirectories.copy();
	}

	public JsonArray getPrefixToImportList() {
		return this.importsPrefixList;
	}
}
