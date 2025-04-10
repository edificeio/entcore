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

package org.entcore.feeder.timetable.edt;

import fr.wseduc.webutils.DefaultAsyncResult;
import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.security.Md5;

import org.entcore.common.storage.Storage;
import org.entcore.common.validation.StringValidation;
import org.entcore.common.utils.DateUtils;
import org.entcore.common.neo4j.TransactionHelper;
import org.entcore.feeder.dictionary.structures.PostImport;
import org.entcore.feeder.exceptions.TransactionException;
import org.entcore.feeder.exceptions.ValidationException;
import org.entcore.feeder.timetable.AbstractTimetableImporter;
import org.entcore.feeder.timetable.Slot;
import org.entcore.feeder.timetable.TimetableReport;
import org.entcore.feeder.utils.*;
import org.joda.time.DateTime;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.security.NoSuchAlgorithmException;
import java.util.*;

import static fr.wseduc.webutils.Utils.getOrElse;
import static fr.wseduc.webutils.Utils.isEmpty;
import static fr.wseduc.webutils.Utils.isNotEmpty;
import static org.entcore.feeder.dictionary.structures.DefaultProfiles.PERSONNEL_PROFILE_EXTERNAL_ID;
import static org.entcore.feeder.dictionary.structures.DefaultProfiles.TEACHER_PROFILE_EXTERNAL_ID;

public class EDTImporter extends AbstractTimetableImporter implements EDTReader {

	private static final String MATCH_PERSEDUCNAT_QUERY =
			"MATCH (:Structure {UAI : {UAI}})<-[:DEPENDS]-(:ProfileGroup)<-[:IN]-(u:User) " +
			"WHERE head(u.profiles) IN ['Teacher','Personnel'] AND LOWER(u.lastName) = {lastName} AND LOWER(u.firstName) = {firstName} " +
			"OPTIONAL MATCH (upn:User {IDPN: {IDPN}}) " +
			"WITH COLLECT(DISTINCT u) as user, COUNT(DISTINCT upn) as countUpn " +
			"WHERE countUpn = 0 AND LENGTH(user) = 1 " +
			"SET HEAD(user).IDPN = {IDPN} " +
			"RETURN DISTINCT HEAD(user).id as id, HEAD(user).IDPN as IDPN, {profile} as profile";
	private static final String STUDENTS_TO_GROUPS =
			"MATCH (u:User {id : {id}}), (fg:FunctionalGroup {externalId:{externalId}}) " +
			"MERGE u-[r:IN]->fg " +
			"SET r.lastUpdated = {now}, r.source = {source}, r.inDate = {inDate}, r.outDate = {outDate} ";
	private static final String CLEAN_IDPN =
			"MATCH (:Structure {UAI : {UAI}})<-[:DEPENDS]-(:ProfileGroup)<-[:IN]-(u:User) " +
			"WHERE HAS(u.IDPN) AND HEAD(u.profiles) IN ['Relative','Guest', 'Student'] " +
			"SET u.IDPN = null";
	private static final String CLEAN_IDPN_OTHER_STRUCTURE =
			"MATCH (u:User)-[:IN]->(:ProfileGroup)-[:DEPENDS]->(s:Structure) " +
			"WHERE u.IDPN  starts with {structureExternalId} " +
			"WITH u, collect(s.externalId) as struct " +
			"WHERE NOT({structureExternalId} IN struct) " +
			"SET u.IDPN = null";
	private final String mode;
	public static final String IDENT = "Ident";
	public static final String IDPN = "IDPN";
	public static final String EDT = "EDT";
	private final List<String> ignoreAttributes = Arrays.asList("Etiquette", "Periode", "PartieDeClasse");
	private final EDTUtils edtUtils;
	private final Map<String, JsonObject> notFoundPersEducNat = new HashMap<>();
	private final Map<String, String> equipments = new HashMap<>();
	private final Map<String, String> personnels = new HashMap<>();
	private final Map<String, String> students = new HashMap<>();
	private final Map<String, JsonObject> subClasses = new HashMap<>();
	private final Set<String> userImportedPronoteId = new HashSet<>();
	private final Map<String, String> idpnIdent = new HashMap<>();
	private final Map<String, TimetableReport.Teacher> teachersById = new HashMap<>();
	private final Map<String, TimetableReport.Subject> subjectsById = new HashMap<>();
	private int maxYearWeek;

	public EDTImporter(Vertx vertx, Storage storage, EDTUtils edtUtils, String uai, String path, String acceptLanguage,
			String mode, boolean authorizeUserCreation, boolean isManualImport, boolean updateGroups, boolean updateTimetable, Long forceTimestamp) {
		super(vertx, storage, uai, path, acceptLanguage, authorizeUserCreation, isManualImport, updateGroups, updateTimetable, forceTimestamp);
		this.edtUtils = edtUtils;
		this.mode = mode;
	}

	public void launch(final Handler<AsyncResult<Report>> handler) throws Exception
	{
		final String content = edtUtils.getContent(basePath, mode, null);
		log.debug(content);
		EDTReader self = this;
		init(new Handler<AsyncResult<Void>>() {
			@Override
			public void handle(AsyncResult<Void> event) {
				if (event.succeeded()) {
					try {
						txXDT.setAutoSend(false);
						txXDT.add(CLEAN_IDPN, new JsonObject().put("UAI", UAI));
						txXDT.add(CLEAN_IDPN_OTHER_STRUCTURE, new JsonObject().put("structureExternalId", structureExternalId));
						edtUtils.parseContent(content, self, EDTHandler.Mode.PERS_EDUC_NAT_ONLY);
						if (txXDT.isEmpty()) {
							edtUtils.parseContent(content, self, EDTHandler.Mode.ALL_BUT_PERS_EDUC_NAT);
						} else {
							matchAndCreatePersEducNat(new Handler<AsyncResult<Void>>() {
								@Override
								public void handle(AsyncResult<Void> event) {
									if (event.succeeded()) {
										try {
											txXDT = TransactionManager.getTransaction();
											edtUtils.parseContent(content, self, EDTHandler.Mode.ALL_BUT_PERS_EDUC_NAT);
											userExternalId(new Handler<Void>(){
												@Override
												public void handle(Void v) {
													if(authorizeUpdateGroups == true)
														for(String group : functionalGroupExternalIdCopy.values())
															ttReport.groupDeleted(group);
													commit(handler);
												}
											});
										} catch (Exception e) {
											handler.handle(new DefaultAsyncResult<Report>(e));
										}
									} else {
										handler.handle(new DefaultAsyncResult<Report>(event.cause()));
									}
								}
							});
						}
					} catch (Exception e) {
						handler.handle(new DefaultAsyncResult<Report>(e));
					}
				} else {
					handler.handle(new DefaultAsyncResult<Report>(event.cause()));
				}
			}
		});
	}

	private void userExternalId(final Handler<Void> handler) {
		if (!userImportedPronoteId.isEmpty()) {
			final String query = "MATCH (u:User) where u.IDPN IN {pronoteIds} RETURN COLLECT(u.externalId) as externalIds";
			TransactionManager.getNeo4jHelper().execute(query, new JsonObject().put("pronoteIds",
					new JsonArray(new ArrayList<>(userImportedPronoteId))), new Handler<Message<JsonObject>>() {
				@Override
				public void handle(Message<JsonObject> event) {
					JsonArray res = event.body().getJsonArray("result");
					if ("ok".equals(event.body().getString("status")) && res.size() == 1) {
						JsonArray r = res.getJsonObject(0).getJsonArray("externalIds");
						if (r != null) {
							userImportedExternalId.addAll(r.getList());
						}
					}
					handler.handle(null);
				}
			});
		} else {
			handler.handle(null);
		}
	}

	public void initSchoolYear(JsonObject schoolYear) {
		startDateWeek1 = DateTime.parse(schoolYear.getString("DatePremierJourSemaine1"));
		maxYearWeek = startDateWeek1.weekOfWeekyear().withMaximumValue().weekOfWeekyear().get();
		byte week1 = (byte) startDateWeek1.getWeekOfWeekyear();
		for(byte i = 0; i < maxYearWeek; ++i)
			ttReport.addWeek(((week1 + i) % maxYearWeek) + 1);
	}

	public void initSchedule(JsonObject currentEntity) {
		slotDuration = Integer.parseInt(currentEntity.getString("DureePlace")) * 60;
		for (Object o : currentEntity.getJsonArray("Place")) {
			if (o instanceof JsonObject) {
				JsonObject s = (JsonObject) o;
				slots.put(s.getString("Numero"), new Slot(
						s.getString("LibelleHeureDebut"), s.getString("LibelleHeureFin"), slotDuration));
			}
		}
	}

	public void addRoom(JsonObject currentEntity) {
		rooms.put(currentEntity.getString(IDENT), currentEntity.getString("Nom"));
	}

	public void addEquipment(JsonObject currentEntity) {
		equipments.put(currentEntity.getString(IDENT), currentEntity.getString("Nom"));
	}

	public void addSubject(JsonObject currentEntity) {
		super.addSubject(currentEntity.getString(IDENT),
				currentEntity.put("mappingCode", currentEntity.getString("Code")));
		TimetableReport.Subject s = new TimetableReport.Subject(currentEntity.getString("Code"));
		subjectsById.put(currentEntity.getString(IDENT), s);
		ttReport.addUserToSubject(null, s);
	}

	public void addGroup(JsonObject currentEntity) {
		final String id = currentEntity.getString(IDENT);
		groups.put(id, currentEntity);
		final JsonArray classes = currentEntity.getJsonArray("Classe");
		final JsonArray pcs = currentEntity.getJsonArray("PartieDeClasse");
		classInGroups(id, classes, this.classes);
		classInGroups(id, pcs, this.subClasses);

		final String name = currentEntity.getString("Nom");
		final String externalId = this.getMappedGroupExternalId(name);
		currentEntity.put("externalId", externalId);

		if(authorizeUpdateGroups == true)
		{
			if(functionalGroupExternalId.containsKey(externalId) == false)
			{
				txXDT.add(CREATE_GROUPS, new JsonObject().put("structureExternalId", structureExternalId)
					.put("name", name).put("displayNameSearchField", Validator.sanitize(name)).put("externalId", externalId)
					.put("id", UUID.randomUUID().toString()).put("source", getTimetableSource()).put("date", importDate));

				ttReport.temporaryGroupCreated(name);
			}
			else
			{
				functionalGroupExternalIdCopy.remove(externalId);
				ttReport.groupUpdated(name);
			}
		}
		else
		{
			// The group won't be actually added to unknowns if it is auto-reconciliated: see the query for details
			txXDT.add(UNKNOWN_GROUPS, new JsonObject().put("UAI", UAI).put("source", this.getTimetableSource()).put("groupExternalId", externalId).put("groupName", name));
		}
	}

	private void classInGroups(String id, JsonArray classes, Map<String, JsonObject> ref) {
		if (classes != null) {
			for (Object o : classes) {
				if (o instanceof JsonObject) {
					final JsonObject j = ref.get(((JsonObject) o).getString(IDENT));
					if (j != null) {
						JsonArray groups = j.getJsonArray("groups");
						if (groups == null) {
							groups = new JsonArray();
							j.put("groups", groups);
						}
						groups.add(id);
					}
				}
			}
		}
	}

	public void addClasse(JsonObject currentEntity) {
		final String id = currentEntity.getString(IDENT);
		classes.put(id, currentEntity);
		final JsonArray pcs = currentEntity.getJsonArray("PartieDeClasse");
		final String ocn = currentEntity.getString("Nom");
		final String className = (classesMapping != null && ocn != null) ? getOrElse(classesMapping.getString(ocn), ocn, false) : ocn;
		final String classExternalId = classNameExternalId.get(className);
		currentEntity.put("className", className);
		currentEntity.put("classExternalId", classExternalId == null ? "" : classExternalId);
		if (pcs != null) {
			for (Object o : pcs) {
				if (o instanceof JsonObject) {
					final String pcIdent = ((JsonObject) o).getString(IDENT);
					subClasses.put(pcIdent, ((JsonObject) o).put("className", className));
				}
			}
		}
		if (className != null) {
			txXDT.add(UNKNOWN_CLASSES, new JsonObject().put("UAI", UAI).put("source", this.getTimetableSource()).put("className", className));

			if(classExternalId != null)
				ttReport.classFound();
			else
				ttReport.addClassToReconciliate(new TimetableReport.SchoolClass(className));
		}
	}

	public void addProfesseur(JsonObject currentEntity) {
		final String id = currentEntity.getString(IDENT);
		final String idPronote = structureExternalId + "$" + currentEntity.getString(IDPN);
		final String firstName = currentEntity.getString("Prenom");
		final String lastName = currentEntity.getString("Nom");
		userImportedPronoteId.add(idPronote);

		String[] teacherId = teachersMapping.get(idPronote);
		if (teacherId == null) {
			teacherId = teachersCleanNameMapping.get(Validator.sanitize(firstName + lastName));
			findPersEducNat(currentEntity, idPronote, "Teacher");
		}

		TimetableReport.Teacher teacher = new TimetableReport.Teacher(firstName, lastName, currentEntity.getString("DateNaissance"));

		if (teacherId != null && isNotEmpty(teacherId[0])) {
			teachers.put(id, teacherId[0]);
			if (getTimetableSource().equals(teacherId[1])) {
				try {
					final JsonObject user = persEducNat.applyMapping(currentEntity.copy());
					updateUser(user.put(IDPN, idPronote));
					foundTeachers.put(teacherId[0], new Boolean(true));
					ttReport.teacherFound();
				} catch (ValidationException e) {
					ttReport.addUnknownTeacher(teacher);
					report.addError("update.user.error");
				}
			} else
			{
				foundTeachers.put(teacherId[0], new Boolean(true));
				ttReport.teacherFound();
			}
		} else {
			idpnIdent.put(idPronote, id);
			ttReport.addUnknownTeacher(teacher);
		}
		teachersById.put(id, teacher);
	}

	public void addPersonnel(JsonObject currentEntity) {
		final String id = currentEntity.getString(IDENT);
		try {
			final String idPronote =  structureExternalId + "$" + Md5.hash(currentEntity.getString("Nom") + currentEntity.getString("Prenom"));
			findPersEducNat(currentEntity, idPronote, "Personnel");
			userImportedPronoteId.add(idPronote);
		} catch (NoSuchAlgorithmException e) {
			log.error("Error hash personnel Id.", e);
		}
	}

	private void findPersEducNat(JsonObject currentEntity, String idPronote, String profile) {
		log.debug(currentEntity);
		try {
			JsonObject p = persEducNat.applyMapping(currentEntity);
			p.put("profiles", new JsonArray().add(profile));
			p.put("externalId", idPronote);
			p.put(IDPN, idPronote);
			if (isNotEmpty(p.getString("lastName")) && isNotEmpty(p.getString("firstName"))) {
				notFoundPersEducNat.put(idPronote, p);
				txXDT.add(MATCH_PERSEDUCNAT_QUERY, new JsonObject().put("UAI", UAI).put(IDPN, idPronote)
						.put("profile", profile)
						.put("lastName", p.getString("lastName").toLowerCase())
						.put("firstName", p.getString("firstName").toLowerCase()));
			} else if (authorizeUserCreation) {
				report.addErrorWithParams("empty.required.user.attribute", p.encode().replaceAll("\\$", ""));
			}
		} catch (Exception e) {
			report.addError(e.getMessage());
		}
	}

	private void matchAndCreatePersEducNat(final Handler<AsyncResult<Void>> handler) {
		txXDT.commit(new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				JsonArray res = event.body().getJsonArray("results");
				if ("ok".equals(event.body().getString("status")) && res != null) {
					for (Object o : res) {
						setUsersId(o);
					}
					if (!notFoundPersEducNat.isEmpty()) {
						try {
							TransactionHelper tx = TransactionManager.getTransaction();
							persEducNat.setTransactionHelper(tx);
							for (JsonObject p : notFoundPersEducNat.values()) {
								p.put("structures", new JsonArray().add(structureExternalId));
								final JsonObject p2 = p.copy();
								p2.remove(IDPN);
								if ("Teacher".equals(p.getJsonArray("profiles").getString(0)) &&
										authorizeUserCreation) {
									persEducNat.createOrUpdatePersonnel(p2, TEACHER_PROFILE_EXTERNAL_ID, structure, null, null, true, true);
								} else if (authorizeUserCreation) {
									persEducNat.createOrUpdatePersonnel(p2, PERSONNEL_PROFILE_EXTERNAL_ID, structure, null, null, true, true);
								}
							}
							tx.commit(new Handler<Message<JsonObject>>() {
								@Override
								public void handle(Message<JsonObject> event) {
									JsonArray res = event.body().getJsonArray("results");
									if ("ok".equals(event.body().getString("status")) && res != null) {
										for (Object o : res) {
											setUsersId(o);
										}
										if (notFoundPersEducNat.isEmpty()) {
											handler.handle(new DefaultAsyncResult<>((Void) null));
										} else {
											for (Map.Entry<String, JsonObject> e: notFoundPersEducNat.entrySet()) {
												log.warn("Not found PersEducNat : " +
														e.getKey() + " : " + e.getValue().encode());
											}
											//handler.handle(new DefaultAsyncResult<Void>(new ValidationException("not.found.users.not.empty")));
											handler.handle(new DefaultAsyncResult<>((Void) null));
										}
									} else {
										handler.handle(new DefaultAsyncResult<Void>(new TransactionException(event.body()
												.getString("message"))));
									}
								}
							});
						} catch (TransactionException e) {
							handler.handle(new DefaultAsyncResult<Void>(e));
						}
					} else {
						handler.handle(new DefaultAsyncResult<>((Void) null));
					}
				} else {
					handler.handle(new DefaultAsyncResult<Void>(new TransactionException(event.body().getString("message"))));
				}
			}

			private void setUsersId(Object o) {
				if ((o instanceof JsonArray) && ((JsonArray) o).size() > 0) {
					JsonObject j = ((JsonArray) o).getJsonObject(0);
					String idPronote = j.getString(IDPN);
					String id = j.getString("id");
					String profile = j.getString("profile");
					if (isNotEmpty(id) && isNotEmpty(idPronote) && isNotEmpty(profile)) {
						notFoundPersEducNat.remove(idPronote);
						if ("Teacher".equals(profile)) {
							teachersMapping.put(idPronote, new String[]{id, getTimetableSource()});
							final String ident = idpnIdent.get(idPronote);
							if (isNotEmpty(ident)) {
								teachers.put(ident, id);
							}
						} else {
							String[] ident = idPronote.split("\\$");
							if (ident.length == 2) {
								personnels.put(ident[1], id);
							}
						}
					}
				}
			}
		});

	}

	public void addEleve(JsonObject currentEntity) {
		final String idpn = currentEntity.getString("IDPN");
		if (isNotEmpty(idpn)) {
			final JsonArray classes = currentEntity.getJsonArray("Classe");
			final JsonArray pcs = currentEntity.getJsonArray("PartieDeClasse");

			String date = StringValidation.convertDate(currentEntity.getString("DateNaissance", ""));
			String idStr = currentEntity.getString("Prenom", "") + "$" + currentEntity.getString("Nom", "") + "$" + date;

			if(studentsIdStrings.containsKey(idStr) == true)
			{
				ttReport.userFound();
				String userId = studentsIdStrings.get(idStr);
				studentToGroups(userId, classes, this.classes);
				studentToGroups(userId, pcs, this.subClasses);
				students.put(currentEntity.getString(IDENT), userId);
			}
				else
				ttReport.addMissingUser(new TimetableReport.Student(currentEntity.getString("Prenom"), currentEntity.getString("Nom"), date));
		}
	}

	public void addResponsable(JsonObject currentEntity)
	{
		// Nothing to do
	}

	private void studentToGroups(String id, JsonArray classes, Map<String, JsonObject> ref) {
		if (classes != null && authorizeUpdateGroups == true) {
			for (Object o : classes) {
				if (o instanceof JsonObject) {
					final String inDate = ((JsonObject) o).getString("DateEntree");
					final String outDate = ((JsonObject) o).getString("DateSortie");
					final String ident = ((JsonObject) o).getString(IDENT);
					if (inDate == null || ident == null || outDate == null || DateTime.parse(inDate).isAfterNow()) continue;
					final JsonObject j = ref.get(ident);
					if (j != null) {
						JsonArray groups = j.getJsonArray("groups");
						if (groups != null) {
							for (Object o2: groups) {
								JsonObject group = this.groups.get(o2.toString());
								if (group != null) {
									String name = group.getString("Nom");
									txXDT.add(STUDENTS_TO_GROUPS, new JsonObject()
											.put("id", id)
											.put("externalId", this.getMappedGroupExternalId(name))
											.put("source", EDT)
											.put("inDate", DateTime.parse(inDate).getMillis())
											.put("outDate", DateTime.parse(outDate).getMillis())
											.put("now", importTimestamp));
									ttReport.validateGroupCreated(name);
								}
							}
						}
					}
				}
			}
		}
	}

	public void addCourse(JsonObject currentEntity) {
		final List<Long> weeks = new ArrayList<>();
		final List<JsonObject> items = new ArrayList<>();

		final String subject = currentEntity.getJsonArray("Matiere").getJsonObject(0).getString(IDENT);
		final JsonArray courseTeachers = currentEntity.getJsonArray("Professeur");

		if(courseTeachers != null)
		{
			for(Object o : courseTeachers)
			{
				if (!(o instanceof JsonObject)) continue;
				final JsonObject j = (JsonObject) o;
				ttReport.addUserToSubject(teachersById.get(j.getString(IDENT)), subjectsById.get(subject));
			}
		}

		for (String attr: currentEntity.fieldNames()) {
			if (!ignoreAttributes.contains(attr) && currentEntity.getValue(attr) instanceof JsonArray) {
				for (Object o: currentEntity.getJsonArray(attr)) {
					if (!(o instanceof JsonObject)) continue;
					final JsonObject j = (JsonObject) o;
					j.put("itemType", attr);
					String week = j.getString("Semaines");
					if(week == null)
						week = j.getString("SemainesInclusion");
					if (week != null) {
						weeks.add(Long.valueOf(week));
						items.add(j);
					}
				}
			}
		}

		if (log.isDebugEnabled() && currentEntity.containsKey("SemainesAnnulation")) {
			log.debug(currentEntity.encode());
		}
		final Long cancelWeek = (currentEntity.getString("SemainesAnnulation") != null) ?
				Long.valueOf(currentEntity.getString("SemainesAnnulation")) : null;

		for (int i = 1; i < maxYearWeek + 1; i++)
		{
			final BitSet currentWeek = new BitSet(weeks.size());
			boolean enabledCurrentWeek = false;
			for (int j = 0; j < weeks.size(); j++)
			{
				if (cancelWeek != null && ((1L << i) & cancelWeek) != 0) {
					currentWeek.set(j, false);
				} else {
					final Long week = weeks.get(j);
					currentWeek.set(j, ((1L << i) & week) != 0);
				}
				enabledCurrentWeek = enabledCurrentWeek | currentWeek.get(j);
			}
			if(enabledCurrentWeek)
				persistCourse(generateCourse(i, currentWeek, items, currentEntity));
		}
	}

	private JsonObject generateCourse(int courseWeek, BitSet enabledItems, List<JsonObject> items, JsonObject entity) {
		final int day = Integer.parseInt(entity.getString("Jour"));
		final int startPlace = Integer.parseInt(entity.getString("NumeroPlaceDebut"));
		final int placesNumber = Integer.parseInt(entity.getString("NombrePlaces"));
		DateTime startDate = startDateWeek1.plusWeeks(courseWeek - 1).plusDays(day - 1);
		DateTime endDate = startDate.plusWeeks(0); // Force a copy of startDate
		startDate = startDate.plusSeconds(slots.get(entity.getString("NumeroPlaceDebut")).getStart());
		endDate = endDate.plusSeconds(slots.get(String.valueOf((startPlace + placesNumber - 1))).getEnd());
		final JsonObject c = new JsonObject()
				.put("structureId", structureId)
//				.put("subjectId", subjects.get(entity.getJsonArray("Matiere").getJsonObject(0).getString(IDENT)))
				.put("startDate", startDate.toString())
				.put("endDate", endDate.toString())
				.put("dayOfWeek", startDate.getDayOfWeek());

			String recurrenceTeachers = "";
			String recurrenceClasses = "";
			String recurrenceGroups = "";
			String recurrenceSubject = "";

		final String codeMat = entity.getJsonArray("Matiere").getJsonObject(0).getString(IDENT);
		final String sId = subjects.get(codeMat);
		if (isNotEmpty(sId)) {
			c.put("timetableSubjectId", sId);
			recurrenceSubject = sId;
		}
		final String sBCNId = subjectsBCN.get(codeMat);
		if (isNotEmpty(sBCNId)) {
			c.put("subjectId", sBCNId);
		}

		for (int i = 0; i < enabledItems.size(); i++) {
			if (enabledItems.get(i)) {
				final JsonObject item = items.get(i);
				final String ident = item.getString(IDENT);
				switch (item.getString("itemType")) {
					case "Professeur":
						JsonArray teachersArray = c.getJsonArray("teacherIds");
						if (teachersArray == null) {
							teachersArray = new JsonArray();
							c.put("teacherIds", teachersArray);
						}
						final String tId = teachers.get(ident);
						if (isNotEmpty(tId)) {
							teachersArray.add(tId);
							List<String> l = (List<String>) teachersArray.getList();
							java.util.Collections.sort(l);
							recurrenceTeachers = "";
							for(Object o : l)
								recurrenceTeachers += (String) o;
						}
						break;
					case "Classe":
						JsonArray classesArray = c.getJsonArray("classes");
						JsonArray classesExternalIdsArray = c.getJsonArray("classesExternalIds");
						if (classesArray == null) {
							classesArray = new JsonArray();
							classesExternalIdsArray = new JsonArray();
							c.put("classes", classesArray);
							c.put("classesExternalIds", classesExternalIdsArray);
						}
						JsonObject ci = classes.get(ident);
						if (ci != null) {
							classesArray.add(ci.getString("className"));
							classesExternalIdsArray.add(ci.getString("classExternalId"));
							List<String> l = (List<String>) classesExternalIdsArray.getList();
							java.util.Collections.sort(l);
							recurrenceClasses = "";
							for(Object o : l)
								recurrenceClasses += (String) o;
						}
						break;
					case "Groupe":
						JsonArray groupsArray = c.getJsonArray("groups");
						JsonArray groupsExternalIds = c.getJsonArray("groupsExternalIds");
						if (groupsArray == null) {
							groupsArray = new JsonArray();
							groupsExternalIds = new JsonArray();
							c.put("groups", groupsArray);
							c.put("groupsExternalIds", groupsExternalIds);
						}
						JsonObject g = groups.get(ident);
						if (g != null) {
							groupsArray.add(g.getString("Nom"));
							groupsExternalIds.add(g.getString("externalId"));
							List<String> l = (List<String>) groupsExternalIds.getList();
							java.util.Collections.sort(l);
							recurrenceGroups = "";
							for(Object o : l)
								recurrenceGroups += (String) o;
						}
						break;
					case "Materiel":
						JsonArray equipmentsArray = c.getJsonArray("equipmentLabels");
						if (equipmentsArray == null) {
							equipmentsArray = new JsonArray();
							c.put("equipmentLabels", equipmentsArray);
						}
						final String eId = equipments.get(ident);
						if (isNotEmpty(eId)) {
							equipmentsArray.add(eId);
						}

						break;
					case "Salle":
						JsonArray roomsArray = c.getJsonArray("roomLabels");
						if (roomsArray == null) {
							roomsArray = new JsonArray();
							c.put("roomLabels", roomsArray);
						}
						final String rId = rooms.get(ident);
						if (isNotEmpty(rId)) {
							roomsArray.add(rId);
						}
						break;
					case "Personnel":
						JsonArray personnelsArray = c.getJsonArray("personnelIds");
						if (personnelsArray == null) {
							personnelsArray = new JsonArray();
							c.put("personnelIds", personnelsArray);
						}
						final String pId = personnels.get(ident);
						if (isNotEmpty(pId)) {
							personnelsArray.add(pId);
						}
						break;
					case "Eleve":
						final String studentId = students.get(ident);
						if (isNotEmpty(studentId)) {
							JsonArray studentsArray = c.getJsonArray("studentIds");
							if (studentsArray == null) {
								studentsArray = new JsonArray();
								c.put("studentIds", studentsArray);
							}
							studentsArray.add(studentId);
						}
						break;
				}
			}
		}
		if (c.getJsonArray("groups") != null && !c.getJsonArray("groups").isEmpty()) {
			c.put("classes", new JsonArray());
			c.put("classesExternalIds", new JsonArray());
		}
		try {
			c.put("recurrence", structureId + "_" + recurrenceSubject + "_" + recurrenceTeachers + "_" + recurrenceClasses + "_" + recurrenceGroups + "_" + day + "_" + startPlace + "_" + placesNumber);
			c.put("_id", JsonUtil.checksum(c));
		} catch (NoSuchAlgorithmException e) {
			log.error("Error generating course checksum", e);
		}
		return c;
	}

	@Override
	protected String getTimetableSource() {
		return EDT;
	}

	@Override
	protected String getTeacherMappingAttribute() {
		return "IDPN";
	}

	public static void launchImport(Vertx vertx, Storage storage, EDTUtils edtUtils, final Message<JsonObject> message, boolean edtUserCreation) {
		launchImport(vertx, storage, edtUtils, "prod", message, null, edtUserCreation, null);
	}

	public static void launchImport(Vertx vertx, Storage storage, EDTUtils edtUtils, final String mode, final Message<JsonObject> message, final PostImport postImport, boolean edtUserCreation, Long forceDateTimestamp) {
		final I18n i18n = I18n.getInstance();
		final String acceptLanguage = message.body().getString("language", "fr");
		if (edtUtils == null) {
			JsonObject json = new JsonObject().put("status", "error").put("message",
					i18n.translate("invalid.edt.key", I18n.DEFAULT_DOMAIN, acceptLanguage));
			message.reply(json);
			return;
		}
		final String uai = message.body().getString("UAI");
		final boolean updateGroups = message.body().getBoolean("updateGroups", true);
		final boolean updateTimetable = message.body().getBoolean("updateTimetable", true);
		final boolean isManualImport = message.body().getBoolean("isManualImport");
		final String path = message.body().getString("path");

		if (isEmpty(uai) || isEmpty(path) || isEmpty(acceptLanguage)) {
			JsonObject json = new JsonObject().put("status", "error").put("message",
					i18n.translate("invalid.params", I18n.DEFAULT_DOMAIN, acceptLanguage));
			message.reply(json);
		}

		try {
			String forceDateStr = (forceDateTimestamp == null) ? "" : " with forced date " + DateUtils.format(DateUtils.parseLongDate(forceDateTimestamp), "dd/MM/yyyy HH:mm:ss");
			final long start = System.currentTimeMillis();
			log.info("Launch EDT import : " + uai + forceDateStr);

			new EDTImporter(vertx, storage, edtUtils, uai, path, acceptLanguage, mode, edtUserCreation, isManualImport, updateGroups, updateTimetable, forceDateTimestamp)
			.launch(new Handler<AsyncResult<Report>>() {
				@Override
				public void handle(AsyncResult<Report> event) {
					if(event.succeeded()) {
						log.info("Import EDT : " + uai + " elapsed time " + (System.currentTimeMillis() - start) + " ms" + forceDateStr + ".");
						message.reply(new JsonObject().put("status", "ok")
								.put("result", event.result().getResult()));
						if (postImport != null && edtUserCreation) {
							postImport.execute();
						}
					} else {
						log.error("Error import EDT : " + uai + " elapsed time " +
								(System.currentTimeMillis() - start) + " ms" + forceDateStr + ".");
						log.error(event.cause().getMessage(), event.cause());
						JsonObject json = new JsonObject().put("status", "error")
								.put("message",
										i18n.translate(event.cause().getMessage(), I18n.DEFAULT_DOMAIN, acceptLanguage));
						message.reply(json);
					}
				}
			});
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			JsonObject json = new JsonObject().put("status", "error").put("message",
					i18n.translate(e.getMessage(), I18n.DEFAULT_DOMAIN, acceptLanguage));
			message.reply(json);
		}
	}
}
