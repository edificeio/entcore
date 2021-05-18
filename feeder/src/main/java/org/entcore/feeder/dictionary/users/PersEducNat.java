/*
 * Copyright © "Open Digital Education", 2016
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

package org.entcore.feeder.dictionary.users;

import org.entcore.common.neo4j.Neo4jUtils;
import org.entcore.feeder.timetable.edt.EDTImporter;
import org.entcore.feeder.utils.Report;
import org.entcore.feeder.utils.TransactionHelper;
import org.entcore.feeder.utils.Validator;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

import static fr.wseduc.webutils.Utils.getOrElse;
import static fr.wseduc.webutils.Utils.isNotEmpty;

import org.entcore.feeder.ManualFeeder;

public class PersEducNat extends AbstractUser {

	private final Validator personnelValidator = new Validator("dictionary/schema/Personnel.json");

	public PersEducNat(TransactionHelper transactionHelper, Report report, String currentSource) {
		super(transactionHelper, report, currentSource);
	}

	public PersEducNat(TransactionHelper transactionHelper, Map<String, String> externalIdMapping, Set<String> userImportedExternalId, Report report, String currentSource) {
		super(transactionHelper, externalIdMapping, userImportedExternalId, report, currentSource);
	}

	public void createOrUpdatePersonnel(JsonObject object, String profileExternalId, JsonArray structuresByFunctions,
				String[][] linkClasses, String[][] linkGroups, boolean nodeQueries, boolean relationshipQueries) {
		final String error = personnelValidator.validate(object);
		final Boolean isMultiEtab = object.getBoolean("multietab", false);
		if (error != null) {
			if (object.getJsonArray("profiles") != null && object.getJsonArray("profiles").size() == 1) {
				report.addIgnored(object.getJsonArray("profiles").getString(0), error, object);
			} else {
				report.addIgnored("Personnel", error, object);
			}
			log.warn(error);
		} else {
			if (nodeQueries) {
				object.put("source", currentSource);
				if (userImportedExternalId != null) {
					userImportedExternalId.add(object.getString("externalId"));
				}
				StringBuilder sb = new StringBuilder();
				JsonObject params;
				sb.append("MERGE (u:`User` { externalId : {externalId}}) ");
				sb.append("ON CREATE SET u.id = {id}, u.login = {login}, u.activationCode = {activationCode}, ");
				sb.append("u.displayName = {displayName}, u.displayNameSearchField = {displayNameSearchField}, u.created = {created} ");
				sb.append("WITH u ");
				if (!EDTImporter.EDT.equals(currentSource)) {
					sb.append("WHERE u.checksum IS NULL OR u.checksum <> {checksum} ");
				}

				if (Boolean.TRUE.equals(isMultiEtab)) {

					sb.append("SET ").append(Neo4jUtils.nodeSetPropertiesFromJson("u", object,
							"id", "externalId", "login", "activationCode", "displayName",
							"displayNameSearchField", "email", "emailSearchField", "created", "structures", "functions"));

					if (object.getJsonArray("structures") != null && object.getJsonArray("structures").size() > 0) {
						sb.append(", u.structures = FILTER (s IN coalesce(u.structures, []) WHERE NOT s IN {structures}) + {structures}");
					}
					if (object.getJsonArray("functions") != null && object.getJsonArray("functions").size() > 0) {
						sb.append(", u.functions = FILTER (f IN coalesce(u.functions, []) WHERE NOT f IN {functions}) + {functions}");
					}

				} else {
					sb.append("SET ").append(Neo4jUtils.nodeSetPropertiesFromJson("u", object,
							"id", "externalId", "login", "activationCode", "displayName",
							"displayNameSearchField", "email", "emailSearchField", "created"));
				}

				if (EDTImporter.EDT.equals(currentSource)) {
					sb.append("RETURN u.id as id, u.IDPN as IDPN, head(u.profiles) as profile");
				}
				params = object;
				transactionHelper.add(sb.toString(), params);
				checkUpdateEmail(object);
			}
			if (relationshipQueries) {
				final String externalId = object.getString("externalId");
				JsonArray structures = getMappingStructures(object.getJsonArray("structures"));
				if (externalId != null && structures != null && structures.size() > 0) {
					String query;
					JsonObject p = new JsonObject().put("userExternalId", externalId);
					if (structures.size() == 1) {
						query = "MATCH (s:Structure {externalId : {structureAdmin}}), (u:User {externalId : {userExternalId}}) " +
								"MERGE u-[:ADMINISTRATIVE_ATTACHMENT]->s ";
						p.put("structureAdmin", structures.getString(0));
					} else {
						query = "MATCH (s:Structure), (u:User {externalId : {userExternalId}}) " +
								"WHERE s.externalId IN {structuresAdmin} " +
								"MERGE u-[:ADMINISTRATIVE_ATTACHMENT]->s ";
						p.put("structuresAdmin", structures);
					}
					transactionHelper.add(query, p);
				}
				if (externalId != null && structuresByFunctions != null && structuresByFunctions.size() > 0) {
					String query;
					structuresByFunctions = getMappingStructures(structuresByFunctions);
					JsonObject p = new JsonObject().put("userExternalId", externalId);
					if (structuresByFunctions.size() == 1) {
						query = "MATCH (s:Structure {externalId : {structureAdmin}})<-[:DEPENDS]-(g:ProfileGroup)-[:HAS_PROFILE]->(p:Profile {externalId : {profileExternalId}}), " +
								"(u:User { externalId : {userExternalId}}) " +
								"WHERE NOT(HAS(u.mergedWith)) " +
								"MERGE u-[:IN]->g";
						p.put("structureAdmin", structuresByFunctions.getString(0))
								.put("profileExternalId", profileExternalId);
					} else {
						query = "MATCH (s:Structure)<-[:DEPENDS]-(g:ProfileGroup)-[:HAS_PROFILE]->(p:Profile), " +
								"(u:User { externalId : {userExternalId}}) " +
								"WHERE s.externalId IN {structuresAdmin} AND NOT(HAS(u.mergedWith)) " +
								"AND p.externalId = {profileExternalId} " +
								"MERGE u-[:IN]->g ";
						p.put("structuresAdmin", structuresByFunctions)
								.put("profileExternalId", profileExternalId);
					}
					transactionHelper.add(query, p);

					if (Boolean.FALSE.equals(isMultiEtab)) {
						String qs =
								"MATCH (:User {externalId : {userExternalId}})-[r:IN|COMMUNIQUE]-(:Group)-[:DEPENDS]->(s:Structure) " +
										"WHERE NOT(s.externalId IN {structures}) AND (NOT(HAS(r.source)) OR r.source = {source}) " +
										"DELETE r";
						JsonObject ps = new JsonObject()
								.put("userExternalId", externalId)
								.put("source", currentSource)
								.put("structures", structuresByFunctions);
						transactionHelper.add(qs, ps);
						final String daa =
								"MATCH (u:User {externalId : {userExternalId}})-[r:ADMINISTRATIVE_ATTACHMENT]->(s:Structure) " +
										"WHERE NOT(s.externalId IN {structures}) AND (NOT(HAS(r.source)) OR r.source = {source}) " +
										"DELETE r";
						transactionHelper.add(daa, ps.copy().put("structures", getOrElse(structures, new JsonArray())));
					}

				}
				final JsonObject fosm = new JsonObject();
				final JsonArray classes = new fr.wseduc.webutils.collections.JsonArray();
				final JsonObject fcm = new JsonObject();
				if (externalId != null && linkClasses != null) {
					for (String[] structClass : linkClasses) {
						if (structClass != null && structClass[0] != null && structClass[1] != null) {
							classes.add(structClass[1]);
							if (structClass.length > 2 && isNotEmpty(structClass[2])) {
								JsonArray fClasses = fcm.getJsonArray(structClass[2]);
								if (fClasses == null) {
									fClasses = new fr.wseduc.webutils.collections.JsonArray();
									fcm.put(structClass[2], fClasses);
								}
								fClasses.add(structClass[1]);
							}
						}
					}
					String query =
							"MATCH (c:Class)<-[:DEPENDS]-(g:ProfileGroup)" +
							"-[:DEPENDS]->(pg:ProfileGroup)-[:HAS_PROFILE]->(p:Profile {externalId : {profileExternalId}}), " +
							"(u:User { externalId : {userExternalId}}) " +
							"WHERE c.externalId IN {classes} AND NOT(HAS(u.mergedWith)) " +
							"MERGE u-[:IN]->g";
					JsonObject p0 = new JsonObject()
							.put("userExternalId", externalId)
							.put("profileExternalId", profileExternalId)
							.put("classes", classes);
					transactionHelper.add(query, p0);
					fosm.mergeIn(fcm);
				}
				if (externalId != null) {
					String q =
							"MATCH (:User {externalId : {userExternalId}})-[r:IN|COMMUNIQUE]-(:Group)-[:DEPENDS]->(c:Class) " +
							"WHERE NOT(c.externalId IN {classes}) AND (NOT(HAS(r.source)) OR r.source = {source}) " +
							"DELETE r";
					JsonObject p = new JsonObject()
							.put("userExternalId", externalId)
							.put("source", currentSource)
							.put("classes", classes);
					transactionHelper.add(q, p);
					final String removeOldFoslc =
							"MATCH (u:User {externalId : {userExternalId}})-[r:TEACHES_FOS]->(f:FieldOfStudy) " +
							"WHERE NOT(f.externalId IN {fos}) AND (NOT(HAS(r.source)) OR r.source = {source}) " +
							"SET r.classes = null ";
					transactionHelper.add(removeOldFoslc, p.copy().put("fos", new fr.wseduc.webutils.collections.JsonArray(new ArrayList<>(fcm.fieldNames()))));
					for (String fos: fcm.fieldNames()) {
						String q2 =
								"MATCH (u:User {externalId : {userExternalId}}), (f:FieldOfStudy {externalId:{feId}}) " +
								"MERGE u-[r:TEACHES_FOS]->f " +
								"SET r.classes = {classes} ";
						transactionHelper.add(q2, p.copy().put("classes", fcm.getJsonArray(fos)).put("feId", fos));
					}
				}
				final JsonArray groups = new fr.wseduc.webutils.collections.JsonArray();
				final JsonObject fgm = new JsonObject();
				if (externalId != null && linkGroups != null) {
					for (String[] structGroup : linkGroups) {
						if (structGroup != null && structGroup[0] != null && structGroup[1] != null) {
							groups.add(structGroup[1]);
							if (structGroup.length > 2 && isNotEmpty(structGroup[2])) {
								JsonArray fGroups = fgm.getJsonArray(structGroup[2]);
								if (fGroups == null) {
									fGroups = new fr.wseduc.webutils.collections.JsonArray();
									fgm.put(structGroup[2], fGroups);
								}
								fGroups.add(structGroup[1]);
							}
						}
					}
					String query =
							"MATCH (g:Group), (u:User {externalId : {userExternalId}}) " +
							"WHERE (g:FunctionalGroup OR g:FunctionGroup OR g:HTGroup OR g:DirectionGroup) AND g.externalId IN {groups} " +
							"MERGE u-[:IN]->g";
					JsonObject p = new JsonObject()
							.put("userExternalId", externalId)
							.put("groups", groups);
					transactionHelper.add(query, p);
				}
				if (externalId != null) {

					final JsonObject pdfg = new JsonObject()
							.put("userExternalId", externalId)
							.put("source", currentSource)
							.put("groups", groups);

					if (Boolean.FALSE.equals(isMultiEtab)) {
						final String qdfg =
								"MATCH (:User {externalId : {userExternalId}})-[r:IN|COMMUNIQUE]-(g:Group) " +
										"WHERE (g:FunctionalGroup OR g:FunctionGroup OR g:HTGroup OR g:DirectionGroup) AND " +
										"NOT(g.externalId IN {groups}) AND (NOT(HAS(r.source)) OR r.source = {source}) " +
										"DELETE r";

						transactionHelper.add(qdfg, pdfg);
					}

					fosm.mergeIn(fgm);
					final String deleteOldFoslg =
							"MATCH (u:User {externalId : {userExternalId}})-[r:TEACHES_FOS]->(f:FieldOfStudy) " +
							"WHERE NOT(f.externalId IN {fos}) AND (NOT(HAS(r.source)) OR r.source = {source}) " +
							"DELETE r";
					transactionHelper.add(deleteOldFoslg, pdfg.copy().put("fos", new fr.wseduc.webutils.collections.JsonArray(new ArrayList<>(fosm.fieldNames()))));
					final String removeOldFoslg =
							"MATCH (u:User {externalId : {userExternalId}})-[r:TEACHES_FOS]->(f:FieldOfStudy) " +
							"WHERE NOT(f.externalId IN {fos}) AND (NOT(HAS(r.source)) OR r.source = {source}) " +
							"SET r.groups = null ";
					transactionHelper.add(removeOldFoslg, pdfg.copy().put("fos", new fr.wseduc.webutils.collections.JsonArray(new ArrayList<>(fgm.fieldNames()))));
					for (String fos: fgm.fieldNames()) {
						String q2 =
								"MATCH (u:User {externalId : {userExternalId}}), (f:FieldOfStudy {externalId:{feId}}) " +
								"MERGE u-[r:TEACHES_FOS]->f " +
								"SET r.groups = {groups} ";
						transactionHelper.add(q2, pdfg.copy().put("groups", fgm.getJsonArray(fos)).put("feId", fos));
					}
				}
				ManualFeeder.applyRemoveUserFromStructure(null, externalId, null, null, transactionHelper);
			}
		}
	}

	public void createAndLinkSubjects() {
		createAndLinkSubjects(null);
	}

	public void createAndLinkSubjects(String structureExternalId) {
		final long now = System.currentTimeMillis();
		final JsonObject params = new JsonObject().put("now", now).put("source", currentSource);
		String filter = "";
		String filter2 = "";
		if (isNotEmpty(structureExternalId)) {
			filter = " {externalId : {structureExternalId}}";
			filter2 = " (:Structure {externalId : {structureExternalId}})<-[:SUBJECT]-";
			params.put("structureExternalId", structureExternalId);
		}
		final String query =
				"MATCH (f:FieldOfStudy)<-[r:TEACHES_FOS]-(u:User {source : {source}})-[:IN]->(:ProfileGroup)-[:DEPENDS]->(s:Structure" + filter + ") " +
				"MERGE s<-[:SUBJECT]-(sub:Subject {externalId: s.externalId + '$' + f.externalId}) " +
				"ON CREATE SET sub.code = f.externalId, sub.id = id(sub) + '-' + {now} " +
				"SET sub.label = f.name, sub.lastUpdated = {now}, sub.source = {source} " +
				"WITH r, sub, u, s.externalId as sExternalId " +
				"MERGE u-[r1:TEACHES]->sub " +
				"SET r1.aafClasses = FILTER(cId IN coalesce(r.classes, []) WHERE cId starts with sExternalId), " +
				"r1.aafGroups = FILTER(gId IN coalesce(r.groups, []) WHERE gId starts with sExternalId), " +
				"r1.lastUpdated = {now}, r1.source = {source} ";
		transactionHelper.add(query, params);
		final String deleteOldSubjects =
				"MATCH " + filter2 + "(sub:Subject {source : {source}}) WHERE sub.lastUpdated <> {now} detach delete sub";
		transactionHelper.add(deleteOldSubjects, params);
		final String deleteOldTeaches =
				"MATCH " + filter2 + "(sub:Subject)<-[r1:TEACHES {source : {source}}]-(u:User) " +
				"WHERE r1.lastUpdated <> {now} AND (NOT(HAS(r1.timetableClasses)) OR r1.timetableClasses = []) " +
				"AND (NOT(HAS(r1.timetableGroups)) OR r1.timetableGroups = []) " +
				"delete r1";
		transactionHelper.add(deleteOldTeaches, params);
		final String syncNotTimetableArrays =
				"MATCH (s:Structure" + filter + ")<-[:SUBJECT]-(sub:Subject)<-[r1:TEACHES {source : {source}}]-(u:User) " +
				"WHERE (NOT(HAS(s.timetable)) OR s.timetable = '' OR s.timetable = 'NOP') " +
				"SET r1.classes = r1.aafClasses, r1.groups = r1.aafGroups ";
		transactionHelper.add(syncNotTimetableArrays, params);
		mergeTeachesArrays(params, filter, transactionHelper);
		deleteEmptyTeaches(params, filter, transactionHelper);
	}

	public static void mergeTeachesArrays(JsonObject params, String filter, TransactionHelper transactionHelper) {
		final String syncTimetableArrays =
				"MATCH (s:Structure" + filter + ")<-[:SUBJECT]-(sub:Subject)<-[r1:TEACHES]-(u:User) " +
				"WHERE (s.timetable = 'EDT' OR s.timetable = 'UDT') " +
				"SET r1.tmpClasses = FILTER(gId IN coalesce(r1.classes, []) WHERE NOT(gId IN coalesce(r1.aafClasses, [])))" +
				" + coalesce(r1.aafClasses, []), " +
				"r1.classes = FILTER(gId IN coalesce(r1.tmpClasses, []) WHERE NOT(gId IN coalesce(r1.timetableClasses, [])))" +
				" + coalesce(r1.timetableClasses, []), " +
				"r1.groups = FILTER(gId IN coalesce(r1.groups, []) WHERE NOT(gId IN coalesce(r1.timetableGroups, [])))" +
				" + coalesce(r1.timetableGroups, []) ";
		transactionHelper.add(syncTimetableArrays, params);
	}

	public static void deleteEmptyTeaches(JsonObject params, String filter, TransactionHelper transactionHelper) {
		final String query =
				"MATCH (s:Structure" + filter + ")<-[:SUBJECT]-(sub:Subject)<-[r:TEACHES]-(:User) " +
				"WHERE (not(has(r.groups)) OR r.groups = []) and (not(has(r.classes)) OR r.classes = []) and " +
				"(NOT(HAS(r.source)) OR r.source <> 'MANUAL') " +
				"DELETE r ";
		transactionHelper.add(query, params);
	}

}
