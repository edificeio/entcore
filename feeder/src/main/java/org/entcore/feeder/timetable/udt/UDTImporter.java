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

package org.entcore.feeder.timetable.udt;

import fr.wseduc.webutils.DefaultAsyncResult;
import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.Utils;
import org.entcore.common.neo4j.Neo4jUtils;
import org.entcore.common.storage.Storage;
import org.entcore.common.utils.FileUtils;
import org.entcore.common.validation.StringValidation;
import org.entcore.common.utils.DateUtils;
import org.entcore.feeder.dictionary.structures.PostImport;
import org.entcore.feeder.timetable.AbstractTimetableImporter;
import org.entcore.feeder.timetable.Slot;
import org.entcore.feeder.timetable.TimetableReport;
import org.entcore.feeder.utils.JsonUtil;
import org.entcore.feeder.utils.Report;
import org.entcore.feeder.utils.Validator;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static fr.wseduc.webutils.Utils.getOrElse;
import static fr.wseduc.webutils.Utils.isNotEmpty;
import static org.entcore.common.utils.StringUtils.isEmpty;
import static org.entcore.common.utils.StringUtils.padLeft;
import static org.entcore.feeder.dictionary.structures.DefaultProfiles.TEACHER_PROFILE_EXTERNAL_ID;

public class UDTImporter extends AbstractTimetableImporter {

	private static final String STUDENTS_TO_GROUPS =
			"MATCH (:Structure {externalId:{structureExternalId}})<-[:DEPENDS]-(:ProfileGroup)<-[:IN]-(u:User), " +
			"(fg:FunctionalGroup {externalId:{externalId}}) " +
			"WHERE lower(u.firstName) = {firstName} AND lower(u.lastName) = {lastName} AND u.birthDate = {birthDate} " +
			"MERGE u-[r:IN]->fg " +
			"SET r.lastUpdated = {now}, r.source = {source}, r.inDate = {inDate}, r.outDate = {outDate} ";
	private static final String PERSIST_USED_GROUPS =
			"MATCH (:Structure {externalId:{structureExternalId}})<-[:DEPENDS]-(fg:FunctionalGroup) " +
			"WHERE fg.idgpe IN {usedGroups} " +
			"SET fg.usedInCourses = true ";
	public static final String UDT = "UDT";
	public static final String CODE = "code";
	private final Pattern filenameWeekPatter;
	public static final String DATE_FORMAT = "dd/MM/yyyy";
	private int year;
	private long endStudents;
	private Map<String, Set<String>> coens = new HashMap<>();
	private Map<String, Set<String>> coensUDT = new HashMap<>();
	private Map<String, JsonObject> fichesT = new HashMap<>();
	private Map<String, JsonObject> regroup = new HashMap<>();
	private Map<String, List<JsonObject>> lfts = new HashMap<>();
	private HashMap<Integer, Integer> periods = new HashMap<>(); // key : start, value : end period
	private int maxYearWeek;
	private Vertx vertx;
	private DateTime startDateStudents;
	private BitSet holidayMask;
	private Set<DateTime> holidays = new HashSet<>();
	private Set<Integer> holidaysWeeks = new HashSet<>();
	private Map<String, JsonObject> eleves = new HashMap<>();
	private Map<String, String> codeGepDiv = new HashMap<>();
	private Set<String> usedGroupInCourses = new HashSet<>();
	private Map<String, JsonArray> aggregateRgmtCourses = new HashMap<>();
	private Set<String> coursesIds = new HashSet<>();
	private Map<String, List<TimetableReport.Teacher>> teachersBySubject = new HashMap<String, List<TimetableReport.Teacher>>();
	private Map<String, TimetableReport.Teacher> ttTeachersById = new HashMap<String, TimetableReport.Teacher>();
	private Map<String, TimetableReport.Subject> ttSubjects = new HashMap<String, TimetableReport.Subject>();
	private JsonArray parsedWeeks = new JsonArray();

	public UDTImporter(Vertx vertx, Storage storage, String uai, String path, String acceptLanguage,
						boolean authorizeUserCreation, boolean isManualImport, boolean updateGroups, boolean updateTimetable, Long forceTimestamp) {
		super(vertx, storage, uai, path, acceptLanguage, authorizeUserCreation, isManualImport, updateGroups, updateTimetable, forceTimestamp);
		this.vertx = vertx;
		filenameWeekPatter = Pattern.compile("(UDCal|udcal)_[0-9]{2}_([0-9]{2})\\.xml$");
	}

	@Override
	public void launch(final Handler<AsyncResult<Report>> handler) throws Exception
	{
		final String parentPath = FileUtils.getParentPath(this.basePath);
		FileUtils.unzip(this.basePath, parentPath);
		String basePath = parentPath + File.separator;
		init(new Handler<AsyncResult<Void>>() {
			@Override
			public void handle(AsyncResult<Void> event) {
				if (event.failed()) {
					handler.handle(new DefaultAsyncResult<Report>(event.cause()));
					return;
				}
				try {
					parse(basePath + "UDCal_24.xml"); // Calendrier
					parse(basePath + "UDCal_00.xml"); // Paramètres
					parse(basePath + "semaines.xml"); // Période de publication
					parse(basePath + "UDCal_03.xml"); // Salles
					parse(basePath + "UDCal_04.xml"); // Professeurs
					parse(basePath + "UDCal_05.xml"); // Matières
					parse(basePath + "UDCal_07.xml"); // Demi-séquences
					parse(basePath + "UDCal_08.xml"); // Divisions
					parse(basePath + "UDCal_10.xml"); // Elèves
					parse(basePath + "UDCal_19.xml"); // Groupes
					parse(basePath + "UDCal_13.xml"); // Regroupements
					parse(basePath + "UDCal_21.xml"); // Appartenance des élèves dans les groupes
					parse(basePath + "UDCal_23.xml"); // Coenseignements
					parse(basePath + "UDCal_11.xml"); // Fiches-T
					parse(basePath + "UDCal_12.xml"); // Lignes de Fiches-T
					generateCourses(startDateWeek1.getWeekOfWeekyear(), true);
					final String UCal12Filter = "(UDCal|udcal)_12_[0-9]+.xml";
					final String UCal23Filter = "(UDCal|udcal)_23_[0-9]+.xml";
					vertx.fileSystem().readDir(basePath, UCal12Filter, new Handler<AsyncResult<List<String>>>() {
						@Override
						public void handle(AsyncResult<List<String>> event_12) {
							if (event.succeeded()) {
								vertx.fileSystem().readDir(basePath, UCal23Filter, new Handler<AsyncResult<List<String>>>() {
									@Override
									public void handle(AsyncResult<List<String>> event_23) {
										if (event.succeeded()) {
											try {
												for (String p : event_12.result())
												{
													Matcher m = filenameWeekPatter.matcher(p);
													if (m.find()) {
														final int weekNumber = Integer.parseInt(m.group(2));
														if (periods.containsKey(weekNumber)) {
															ttReport.addWeek(weekNumber);
															parse(p);
															parsedWeeks.add(weekNumber);

															boolean foundCoens = false;
															for(String coensFile : event_23.result())
															{
																Matcher mcoens = filenameWeekPatter.matcher(coensFile);
																if(mcoens.find())
																{
																	final int weekCoens = Integer.parseInt(mcoens.group(2));
																	if(weekNumber == weekCoens)
																	{
																		foundCoens = true;

																		Map<String, Set<String>> theoreticalCoens = coens;
																		Map<String, Set<String>> theoreticalCoensUDT = coensUDT;
																		coens = new HashMap<String, Set<String>>();
																		coensUDT = new HashMap<String, Set<String>>();

																		parse(coensFile);
																		generateCourses(weekNumber, false);

																		coens = theoreticalCoens;
																		coensUDT = theoreticalCoensUDT;

																		break;
																	}
																}
															}
															if(foundCoens == false)
																generateCourses(weekNumber, false);
														} else {
															log.warn("Ignore week : " + weekNumber);
														}
													}
												}
												persistUsedGroups();

												if(authorizeUpdateGroups == true)
													for(String group : functionalGroupExternalIdCopy.values())
														ttReport.groupDeleted(group);

												commit(handler);
											} catch (Exception e) {
												handler.handle(new DefaultAsyncResult<Report>(e));
											}
										}
									}
								});
							}
						}
					});
				} catch (Exception e) {
					handler.handle(new DefaultAsyncResult<Report>(e));
				}
			}
		});
	}

	private void parse(String filePath) throws Exception
	{
		InputSource in;
		try {
			in = new InputSource(new FileInputStream(filePath));
		}
		catch(FileNotFoundException e)
		{
			in = new InputSource(new FileInputStream(filePath.toLowerCase()));
		}
		UDTHandler sh = new UDTHandler(this);
		XMLReader xr = XMLReaderFactory.createXMLReader();
		xr.setContentHandler(sh);
		xr.parse(in);
	}

	// Origine: Calendrier
	void setYear(String year) {
		if (this.year == 0) {
			this.year = Integer.parseInt(year);
		}
	}

	// Origine: Calendrier
	public void setEndStudents(String endStudents) {
		this.endStudents = DateTime.parse(endStudents, DateTimeFormat.forPattern(DATE_FORMAT)).getMillis();
	}

	// Origine: Calendrier
	public void setStartDateStudents(String startDateStudents) {
		this.startDateStudents = DateTime.parse(startDateStudents, DateTimeFormat.forPattern(DATE_FORMAT));
		maxYearWeek = this.startDateStudents.weekOfWeekyear().withMaximumValue().weekOfWeekyear().get();
		this.holidayMask = new BitSet(maxYearWeek);
	}

	// Origine: Paramètres
	void initSchoolYear(JsonObject currentEntity) {
		startDateWeek1 = startDateStudents
				.withWeekOfWeekyear(Integer.parseInt(currentEntity.getString("premiere_semaine_ISO")))
				.withDayOfWeek(1);
		slotDuration = Integer.parseInt(currentEntity.getString("duree_seq")) / 2;
		DateTime h = startDateWeek1;
		while (h.isBefore(startDateStudents)) {
			holidays.add(h);
			h = h.plusDays(1);
			holidaysWeeks.add(h.getWeekOfWeekyear());
		}
	}

	// Origine: Demi-séquences
	void initSchedule(JsonObject e) {
		final String slotKey = e.getString("code_jour") + padLeft(e.getString(CODE), 2, '0') + e.getString("code_site");
		Slot s = new Slot(e.getString("hre_deb"), e.getString("hre_fin"), slotDuration);
		slots.put(slotKey, s);
	}

	// Origine: Période de publication
	void initPeriods(JsonObject currentEntity) {
		JsonArray weeks = currentEntity.getJsonArray("semaine");
		if (weeks != null && weeks.size() > 0) {
			int oldWeek = startDateWeek1.getWeekOfWeekyear();
			for (Object o : weeks) {
				if (o instanceof JsonObject) {
					int week = Integer.parseInt(((JsonObject) o).getString("num"));
					if (week != oldWeek) {
						periods.put(oldWeek, (week == 1 ? maxYearWeek : week - 1));
						oldWeek = week;
					}
				}
			}
			periods.put(oldWeek, new DateTime(endStudents).getWeekOfWeekyear());
			for (int i = new DateTime(endStudents).getWeekOfWeekyear() + 1; i < startDateWeek1.getWeekOfWeekyear(); i++) {
				holidaysWeeks.add(i);
			}
		}
	}

	// Origine: Calendrier
	void initHolidays(JsonObject currentEntity) {
		DateTime s = DateTime.parse(currentEntity.getString("debut"), DateTimeFormat.forPattern(DATE_FORMAT));
		DateTime e = DateTime.parse(currentEntity.getString("fin"), DateTimeFormat.forPattern(DATE_FORMAT));
		while (s.isBefore(e))
		{
			// Ce masque permet d'éviter l'exclusion des élèves de leurs groupes d'enseignements durant les vacances scolaires
			if(s.getDayOfWeek() == 1 && e.getDayOfWeek() == 7)
				holidayMask.set(s.getWeekOfWeekyear() - 1, true);

			holidays.add(s);
			s = s.plusDays(1);
			holidaysWeeks.add(s.getWeekOfWeekyear());
		}
		holidays.add(e);
	}

	// Origine: Salles
	void addRoom(JsonObject currentEntity) {
		rooms.put(currentEntity.getString(CODE), currentEntity.getString("nom"));
	}

	@Override
	protected String getTeacherMappingAttribute() {
		return "externalId";
	}

	@Override
	protected String getTimetableSource() {
		return UDT;
	}

	// Origine: Professeurs
	void addProfesseur(JsonObject currentEntity) {
		try {
			if (isEmpty(currentEntity.getString("code_matppl"))) {
				// Ignore prof without subject.
				// Often this case corresponds to personnel.
				//return;
			}
			final String id = currentEntity.getString(CODE);
			String externalId = currentEntity.getString("epj");
			final String firstName = currentEntity.getString("prenom");
			final String lastName = currentEntity.getString("nom");
			JsonObject p = persEducNat.applyMapping(currentEntity);
			p.put("profiles", new JsonArray().add("Teacher"));
			if (isEmpty(externalId)) {
				externalId = JsonUtil.checksum(p, JsonUtil.HashAlgorithm.MD5);
			}
			p.put("externalId", externalId);

			TimetableReport.Teacher ttTeacher = new TimetableReport.Teacher(firstName, lastName, p.getString("birthDate"));
			ttTeachersById.put(id, ttTeacher);

			userImportedExternalId.add(externalId);
			String[] teacherId = teachersMapping.get(externalId);
			if (teacherId == null) {
				teacherId = teachersCleanNameMapping.get(Validator.sanitize(firstName + lastName));
			}
			if (teacherId != null && isNotEmpty(teacherId[0])) {
				teachers.put(id, teacherId[0]);
				if (getTimetableSource().equals(teacherId[1]) && authorizeUserCreation) {
					updateUser(p);
				}
				foundTeachers.put(teacherId[0], new Boolean(true));
				this.ttReport.teacherFound();
			} else {
				final String userId = UUID.randomUUID().toString();
				p.put("id", userId);
				p.put("structures", new JsonArray().add(structureExternalId));
				if (authorizeUserCreation) {
					persEducNat.createOrUpdatePersonnel(p, TEACHER_PROFILE_EXTERNAL_ID, structure, null, null, true, true);
				}
				teachers.put(id, userId);
				this.ttReport.addUnknownTeacher(ttTeacher);
			}
			List<TimetableReport.Teacher> colleagues = teachersBySubject.get(currentEntity.getString("code_matppl"));
			if(colleagues == null)
			{
				colleagues = new ArrayList<TimetableReport.Teacher>();
				teachersBySubject.put(currentEntity.getString("code_matppl"), colleagues);
			}
			colleagues.add(ttTeacher);
		} catch (Exception e) {
			report.addError(e.getMessage());
		}
	}

	// Origine: Matières
	void addSubject(JsonObject s) {
		final String code = s.getString(CODE);
		JsonObject subject = new JsonObject().put("Code", code).put("Libelle", s.getString("libelle"))
													.put("mappingCode", getOrElse(s.getString("code_gep1"), code, false));
		super.addSubject(code, subject);

		TimetableReport.Subject ttSubject = new TimetableReport.Subject(code);
		this.ttSubjects.put(code, ttSubject);
		List<TimetableReport.Teacher> teachers = teachersBySubject.get(code);
		if(teachers == null)
			ttReport.addUserToSubject(null, ttSubject);
		else
			for(TimetableReport.Teacher t : teachers)
				ttReport.addUserToSubject(t, ttSubject);
	}

	// Origine: Divisions
	void addClasse(JsonObject currentEntity) {
		final String id = currentEntity.getString(CODE);
		classes.put(id, currentEntity);
		final String codeGep =  currentEntity.getString("code_gep");
		if (isNotEmpty(codeGep)) {
			codeGepDiv.put(id, codeGep);
		}
		final String className = (classesMapping != null) ? getOrElse(classesMapping.getString(id), id, false) : id;
		final String classExternalId = classNameExternalId.get(className);
		currentEntity.put("className", className);
		currentEntity.put("classExternalId", classExternalId);

		// The class won't be actually added to unknowns if it is auto-reconciliated: see the query for details
		txXDT.add(UNKNOWN_CLASSES, new JsonObject().put("UAI", UAI).put("source", this.getTimetableSource()).put("className", className));

		if(classExternalId != null)
			ttReport.classFound();
		else
			ttReport.addClassToReconciliate(
				new TimetableReport.SchoolClass(getOrElse(currentEntity.getString("libelle"), currentEntity.getString("className"), false)));
	}

	// Origine: Groupes
	void addGroup(JsonObject currentEntity)
	{
		String code = currentEntity.getString("code");
		String codeSTS = currentEntity.getString("code_sts");

		if(code != null && code.isEmpty() == false && codeSTS != null && codeSTS.isEmpty() == true)
			return;

		final String id = currentEntity.getString("code_div") + currentEntity.getString(CODE);
		groups.put(id, currentEntity);

		final String name = getOrElse(currentEntity.getString("code_sts"), id, false);
		final String mappedName = this.getMappedGroupName(name);
		final String externalId = this.getMappedGroupExternalId(name);

		currentEntity.put("externalId", externalId);
		currentEntity.put("code_gep", codeGepDiv.get(currentEntity.getString("code_div")));
		currentEntity.put("idgpe", currentEntity.remove("id"));
		currentEntity.put("modified", importDate);
		final String set = "SET " + Neo4jUtils.nodeSetPropertiesFromJson("fg", currentEntity);
		currentEntity.put("name", mappedName);

		if(authorizeUpdateGroups == true)
		{
			if(functionalGroupExternalId.containsKey(externalId) == false)
			{
				txXDT.add(CREATE_GROUPS + set, currentEntity.put("structureExternalId", structureExternalId)
					.put("name", name).put("displayNameSearchField", Validator.sanitize(name)).put("externalId", externalId)
					.put("id", UUID.randomUUID().toString()).put("source", getTimetableSource()).put("date", importDate));

				ttReport.temporaryGroupCreated(name);
			}
			else
			{
				txXDT.add("MATCH (fg:Group:FunctionalGroup {externalId:{externalId}}) " + set, currentEntity);

				functionalGroupExternalIdCopy.remove(externalId);
				ttReport.groupUpdated(name);
			}
		}
		else
		{
			// The group won't be actually added to unknowns if it is auto-reconciliated: see the query for details
			txXDT.add(UNKNOWN_GROUPS, new JsonObject().put("UAI", UAI).put("source", this.getTimetableSource()).put("groupExternalId", externalId).put("groupName", mappedName));
		}
	}

	// Origine: Regroupements
	void addGroup2(JsonObject currentEntity)
	{
		final String codeGroup = currentEntity.getString("code_gpe");
		final String name = this.getMappedGroupName(currentEntity.getString("nom"));
		final String externalId = this.getMappedGroupExternalId(name);

		if (isNotEmpty(codeGroup))
		{
			final String groupId = currentEntity.getString("code_div") + codeGroup;
			JsonObject group = groups.get(groupId);

			if (group == null) {
				log.warn("addGroup2 : unknown.group.mapping");
				return;
			}

			JsonArray groups = group.getJsonArray("groups");
			if (groups == null) {
				groups = new JsonArray();
				group.put("groups", groups);
			}
			groups.add(name);

			JsonArray aggClasses = aggregateRgmtCourses.get(name);
			if (aggClasses == null) {
				aggClasses = new JsonArray();
				aggregateRgmtCourses.put(name, aggClasses);
			}
			aggClasses.add(currentEntity.getString("code_div"));
		}
		else
		{
			final String classId = currentEntity.getString("code_div");
			JsonObject classe = classes.get(classId);

			if (classe == null) {
				log.warn("addGroup2 : unknown.class.mapping");
				return;
			}

			JsonArray groups = classe.getJsonArray("groups");
			if (groups == null) {
				groups = new JsonArray();
				classe.put("groups", groups);
			}
			groups.add(name);
		}

		regroup.put(currentEntity.getString(CODE), new JsonObject().put("name", name).put("externalId", externalId));

		if(authorizeUpdateGroups == true)
		{
			if(functionalGroupExternalId.containsKey(externalId) == false)
			{
				txXDT.add(CREATE_GROUPS + "SET fg.idrgpmt = {idrgpmt} " , new JsonObject()
					.put("structureExternalId", structureExternalId)
					.put("name", name).put("displayNameSearchField", Validator.sanitize(name))
					.put("externalId", externalId)
					.put("id", UUID.randomUUID().toString()).put("source", getTimetableSource())
					.put("date", importDate)
					.put("idrgpmt", currentEntity.getString("id")));

				ttReport.temporaryGroupCreated(name);
			}
			else
			{
				txXDT.add("MATCH (fg:Group:FunctionalGroup {externalId:{externalId}}) SET fg.idrgpmt = {idrgpmt}, fg.modified = {date}",
					currentEntity.put("externalId", externalId).put("idrgpmt", currentEntity.getString("id")).put("date", importDate));

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

	private void persistUsedGroups() {
		txXDT.add(PERSIST_USED_GROUPS, new JsonObject().put("structureExternalId", structureExternalId)
				.put("usedGroups", new JsonArray(new ArrayList<>(usedGroupInCourses))));
	}

	// Origine: Elèves
	void eleveMapping(JsonObject currentEntity) {
		eleves.put(currentEntity.getString(CODE), currentEntity);

		String date = StringValidation.convertDate(currentEntity.getString("naissance", ""));
		String idStr = currentEntity.getString("prenom", "") + "$" + currentEntity.getString("nom", "") + "$" + date;

		if(studentsIdStrings.containsKey(idStr) == true)
			ttReport.userFound();
		else
			ttReport.addMissingUser(new TimetableReport.Student(currentEntity.getString("prenom"), currentEntity.getString("nom"), date));
	}

	// Origine: Appartenance des élèves dans les groupes
	void addEleve(JsonObject currentEntity) {
		if(authorizeUpdateGroups == false) {
			return;
		}
		final String ele = currentEntity.getString("ele");
		if (isEmpty(ele)) {
			report.addErrorWithParams("invalid.epj", currentEntity.encode());
			return;
		}
		JsonObject eleve = eleves.get(ele);
		if (eleve == null) {
			report.addErrorWithParams("missing.student", currentEntity.encode());
			return;
		}
		final String codeGroup = currentEntity.getString("gpe");
		final String codeDiv = currentEntity.getString("code_div");
		final String semaines =  currentEntity.getString("semaines");

		String date = StringValidation.convertDate(eleve.getString("naissance", ""));
		String idStr = eleve.getString("prenom", "") + "$" + eleve.getString("nom", "") + "$" + date;

		// If the student is missing, don't try to link them to groups
		if(studentsIdStrings.containsKey(idStr) == false)
			return;

		DateTime inDate = new DateTime(importTimestamp);
		DateTime outDate = new DateTime(endStudents);

		if(semaines != null && semaines.isEmpty() == false && "0".equals(semaines) == false)
		{
			try
			{
				long weeks = Long.valueOf(semaines);
				BitSet weekBits = new BitSet(maxYearWeek);
				int currentWeek = new DateTime(importTimestamp).weekOfWeekyear().get() - 1;
				DateTime refWeek = new DateTime().withYear(year).withWeekOfWeekyear(1).withDayOfWeek(1).withTimeAtStartOfDay();
				int borderWeek = startDateStudents.weekOfWeekyear().get();

				for (int i = 0; i < maxYearWeek; i++)
					weekBits.set(i, ((1L << i) & weeks) != 0);

				for(int x = 0; x++ < 2;) // We need to loop twice to handle the case where vacations start before the new year and end after
					for(int i = 0; i < maxYearWeek; ++i)
					{
						if(holidayMask.get(i) == true)
						{
							int previous = (i - 1 + maxYearWeek) % maxYearWeek;
							if(weekBits.get(previous) == true) // Keep children in their groups during holidays
								weekBits.set(i, true);
						}
					}

				for(int x = 0; x++ < 2;) // // We need to loop twice to handle the case where vacations start before the new year and end after
					for(int i = maxYearWeek; i-- > 0;)
					{
						if(holidayMask.get(i) == true)
						{
							int next = (i + 1) % maxYearWeek;
							if(weekBits.get(next) == true) // Add children in their groups during holidays
								weekBits.set(i, true);
						}
					}

				if(weekBits.get(currentWeek) == true)
				{
					for(int i = 0; i < maxYearWeek; ++i)
					{
						int prev = (currentWeek - i + maxYearWeek) % maxYearWeek;
						if(weekBits.get(prev) == false)
						{
							int start = (prev + 1) % maxYearWeek;
							if((start + 1) < borderWeek) // +1 because BitSets are zero-indexed but weeks are one-indexed
								inDate = refWeek.plusYears(1).plusWeeks(start).withDayOfWeek(1);
							else
								inDate = refWeek.plusWeeks(start).withDayOfWeek(1);
							break;
						}
					}

					for(int i = 0; i < maxYearWeek; ++i)
					{
						int next = (currentWeek + i) % maxYearWeek;
						if(weekBits.get(next) == false)
						{
							int end = (next - 1 + maxYearWeek) % maxYearWeek;
							if((end + 1) < borderWeek) // +1 because BitSets are zero-indexed but weeks are one-indexed
								outDate = refWeek.plusYears(1).plusWeeks(end).withDayOfWeek(7).plusDays(1).plusSeconds(-1);
							else
								outDate = refWeek.plusWeeks(end).withDayOfWeek(7).plusDays(1).plusSeconds(-1);
							break;
						}
					}
				}
				else
				{
					// Force leaving the group
					outDate = new DateTime(0);
				}
			}
			catch(NumberFormatException e)
			{
				// Ignore
			}
		}

		long inLong = inDate.getMillis() < startDateStudents.getMillis() ? startDateStudents.getMillis() : inDate.getMillis();
		long outLong = outDate.getMillis() > endStudents ? endStudents : outDate.getMillis();

		JsonArray groups;
		if (isNotEmpty(codeGroup)) {
			JsonObject group = this.groups.get(codeDiv + codeGroup);
			if (group == null) {
				log.warn("addEleve : unknown.group.mapping");
				return;
			}
			final String name = getOrElse(group.getString("code_sts"), group.getString("code_div") + group.getString(CODE), false);
			txXDT.add(STUDENTS_TO_GROUPS, new JsonObject()
					.put("firstName", eleve.getString("prenom", "").toLowerCase())
					.put("lastName", eleve.getString("nom", "").toLowerCase())
					.put("birthDate", StringValidation.convertDate(eleve.getString("naissance", "")))
					.put("externalId", this.getMappedGroupExternalId(name))
					.put("structureExternalId", structureExternalId)
					.put("source", UDT)
					.put("inDate", inLong)
					.put("outDate", outLong)
					.put("now", importTimestamp));
			groups = group.getJsonArray("groups");

			ttReport.validateGroupCreated(name);
		} else {
			JsonObject classe = classes.get(codeDiv);
			if (classe == null) {
				log.warn("addEleve : unknown.class.mapping");
				return;
			}
			groups = classe.getJsonArray("groups");
		}
		if (groups != null) {
			for (Object o2: groups) {
				txXDT.add(STUDENTS_TO_GROUPS, new JsonObject()
						.put("firstName", eleve.getString("prenom", "").toLowerCase())
						.put("lastName", eleve.getString("nom", "").toLowerCase())
						.put("birthDate", StringValidation.convertDate(eleve.getString("naissance", "")))
						.put("externalId", this.getMappedGroupExternalId(o2.toString()))
						.put("structureExternalId", structureExternalId)
						.put("source", UDT)
						.put("inDate", inLong)
						.put("outDate", outLong)
						.put("now", importTimestamp));

				ttReport.validateGroupCreated(o2.toString());
			}
		}
	}

	// Origine: Coenseignements
	void addCoens(JsonObject currentEntity) {
		final String clf = currentEntity.getString("lignefic");
		Set<String> teachers = coens.get(clf);
		Set<String> teachersWithUDTIds = coensUDT.get(clf);
		if (teachers == null) {
			teachers = new HashSet<>();
			teachersWithUDTIds = new HashSet<>();
			coens.put(clf, teachers);
			coensUDT.put(clf, teachers);
		}
		final String externalId = currentEntity.getString("epj");
		String[] teacherId = null;
		if (isNotEmpty(externalId)) {
			teacherId = teachersMapping.get(externalId);
		}
		if (teacherId == null || isEmpty(teacherId[0])) {
			teacherId = new String[]{this.teachers.get(currentEntity.getString("prof")), getTimetableSource()};
		}
		if (teacherId != null && isNotEmpty(teacherId[0])) {
			teachers.add(teacherId[0]);
			teachersWithUDTIds.add(currentEntity.getString("prof"));
		}
	}

	// Origine: Fiches-T
	void addFicheT(JsonObject currentEntity) {
		final String id = currentEntity.getString(CODE);
		fichesT.put(id, currentEntity);
	}

	// Origine: Lignes de Fiches-T
	void addCourse(JsonObject entity) {
		final String div = entity.getString("div");
		if (isEmpty(div)) {
			return;
		}
		final String fic = entity.getString("fic");
		if (isEmpty(fic)) {
			report.addError("invalid.fic");
			return;
		}
		final String idgpe = entity.getString("idgpe");
		if ("0".equals(entity.getString("rgpmt")) && isNotEmpty(idgpe) && isNotEmpty(entity.getString("gpe"))) {
			usedGroupInCourses.add(idgpe);
		}
		final String tmpId = calculateTmpId(entity);
		List<JsonObject> l = lfts.get(tmpId);
		if (l == null) {
			l = new ArrayList<>();
			lfts.put(tmpId, l);
		}
		l.add(entity);
	}

	private String calculateTmpId(JsonObject entity) {
		return entity.getString("div") + "$" + entity.getString("mat") + entity.getString("prof") +
				entity.getString("rgpmt") + getOrElse(entity.getString("gpe"), "_", false);
	}

	private String rewriteTmpIdToGroups(String tmpId, JsonArray groups)
	{
		List<String> gStrings = (List<String>) groups.getList();

		String groupsId = "";
		Collections.sort(gStrings);
		for(String s : gStrings)
			groupsId += s;

		int ix = tmpId.indexOf("$");
		return groupsId + tmpId.substring(ix);
	}

	private void generateCourses(int periodWeek, boolean theoretical) {
		for (Map.Entry<Integer, Integer> e : getNextHolidaysWeek(periodWeek).entrySet()) {
			if (!theoretical && e.getKey() != periodWeek) continue;
			final int startPeriodWeek = e.getKey();
			final int endPeriodWeek = theoretical ? e.getValue() : startPeriodWeek;
			for (Map.Entry<String, List<JsonObject>> le : lfts.entrySet()) {
				List<JsonObject> c = le.getValue();
				Collections.sort(c, new LftComparator());
				String start = null;
				String startCode = null;
				int current = 0;
				JsonObject previous = null;
				int count = 0;
				for (JsonObject j : c) {
					int val = Integer.parseInt(j.getString(CODE).substring(0, 3));
					count++;
					if (start == null) {
						start = j.getString("fic");
						startCode = j.getString(CODE);
						current = val;
					}
					else
					{
						boolean follows = (++current) == val;
						if (follows == false) {
							for(int week = startPeriodWeek; week <= endPeriodWeek; ++week)
								persistCourse(generateCourse(start, previous.getString("fic"), startCode,
									previous, week, le.getKey(), theoretical));
							start = j.getString("fic");
							startCode = j.getString(CODE);
							current = val;
						}

						if (count == c.size()) {
							for(int week = startPeriodWeek; week <= endPeriodWeek; ++week)
								persistCourse(generateCourse(start, j.getString("fic"), startCode,
									j, week, le.getKey(), theoretical));
						}
					}
					previous = j;
				}
			}
		}
		lfts.clear();
	}

	private Map<Integer, Integer> getNextHolidaysWeek(int periodWeek) {
		int endPeriod = periods.get(periodWeek);
		int e = (endPeriod < startDateWeek1.getWeekOfWeekyear()) ? endPeriod + maxYearWeek : endPeriod;
		int startPeriod = periodWeek;
		Map<Integer, Integer> p = new HashMap<>();
		for (int i = periodWeek; i < e; i++) {
			int j = (i > maxYearWeek) ? i - maxYearWeek : i;
			if (startPeriod < 1 && !holidaysWeeks.contains(j)) {
				startPeriod = j;
			}
			if (startPeriod > 0 && (holidaysWeeks.contains(j) && startPeriod != j)) {
				p.put(startPeriod, j);
				startPeriod = 0;

			}
		}
		if (startPeriod > 0) {
			p.put(startPeriod, endPeriod);
		}
		return p;
	}

	@Override
	protected JsonObject getDeletionQuery(JsonObject baseQuery)
	{
		JsonObject defaultQuery = super.getDeletionQuery(baseQuery);

		JsonObject includeTheoreticals = new JsonObject().put("periodWeek", new JsonObject().put("$exists", false));
		JsonObject includeCurrentWeeks = new JsonObject().put("periodWeek", new JsonObject().put("$in", this.parsedWeeks));

		JsonObject saveOtherWeeks = new JsonObject().put("$or", new JsonArray().add(includeTheoreticals).add(includeCurrentWeeks));
		return new JsonObject().put("$and", new JsonArray().add(defaultQuery).add(saveOtherWeeks));
	}

	private JsonObject generateCourse(String start, String end, String startCode, JsonObject entity, int periodWeek, String ligneFicheTTmpId, boolean theoretical) {
		JsonObject ficheTStart = fichesT.get(start);
		JsonObject ficheTEnd = fichesT.get(end);
		if (ficheTStart == null || ficheTEnd == null) {
			report.addError("invalid.ficheT");
			return null;
		}
		final Slot slotStart = slots.get(ficheTStart.getString("jour") +
				padLeft(ficheTStart.getString("demi_seq"), 2, '0') + ficheTStart.getString("site"));
		final Slot slotEnd = slots.get(ficheTEnd.getString("jour") +
				padLeft(ficheTEnd.getString("demi_seq"), 2, '0') + ficheTEnd.getString("site"));
		if (slotStart == null || slotEnd == null) {
			report.addError("invalid.slot");
			return null;
		}
		final int day = Integer.parseInt(ficheTStart.getString("jour"));
		final int cpw = (periodWeek < startDateWeek1.getWeekOfWeekyear()) ? periodWeek + maxYearWeek : periodWeek;
		DateTime startDate = startDateWeek1.plusWeeks(cpw - startDateWeek1.getWeekOfWeekyear()).plusDays(day - 1);
		boolean startWasHoliday = false;
		while (holidays.contains(startDate)) {
			startDate = startDate.plusWeeks(1);
			startWasHoliday = true;
		}
		startDate = startDate.plusSeconds(slotStart.getStart());
		//final int epw = periods.get(periodWeek);
		final int cepw = (periodWeek < startDateWeek1.getWeekOfWeekyear()) ? periodWeek + maxYearWeek : periodWeek;
		DateTime endDate = startDateWeek1.plusWeeks(cepw - startDateWeek1.getWeekOfWeekyear()).plusDays(day - 1);
		boolean endWasHoliday = false;
		while (holidays.contains(endDate)) {
			endDate = endDate.minusWeeks(1);
			endWasHoliday = true;
		}
		endDate = endDate.plusSeconds(slotEnd.getEnd());
		if (endDate.isBefore(startDate)) {
			if(startWasHoliday == false || endWasHoliday == false)
				log.error("endDate before start date. cpw : " + cpw + ", cepw : " + cepw + ", startDateWeek1 : " + startDateWeek1);
			return null;
		}

		String subject = entity.getString("mat");
		TimetableReport.Subject ttSubject = ttSubjects.get(subject);
		final Set<String> ce = coens.get(startCode);
		JsonArray teacherIds;
		if (ce != null && ce.size() > 0) {
			teacherIds = new JsonArray(new ArrayList<>(ce));
		} else {
			teacherIds = new JsonArray();
		}
		final String pId = teachers.get(entity.getString("prof"));
		if (isNotEmpty(pId)) {
			teacherIds.add(pId);
			ttReport.addUserToSubject(ttTeachersById.get(entity.getString("prof")), ttSubject);
		}
		Set<String> ceUDT = coensUDT.get(startCode);
		if(ceUDT != null)
			for(String prof : ceUDT)
				ttReport.addUserToSubject(ttTeachersById.get(prof), ttSubject);

		final JsonObject c = new JsonObject()
				.put("structureId", structureId)
				.put("startDate", startDate.toString())
				.put("endDate", endDate.toString())
				.put("dayOfWeek", day)
				.put("teacherIds", teacherIds)
				.put("recurrence", structureId + "_" + ligneFicheTTmpId + "_" + start.substring(0, 3) + end.substring(0, 3))
				.put("theoretical", theoretical);
		if (!theoretical) {
			c.put("periodWeek", periodWeek);
		}
		final String sId = subjects.get(subject);
		if (isNotEmpty(sId)) {
			c.put("timetableSubjectId", sId);
		}
		final String sBCNId = subjectsBCN.get(subject);
		if (isNotEmpty(sBCNId)) {
			c.put("subjectId", sBCNId);
		}

		final String rId = rooms.get(entity.getString("salle"));
		if (isNotEmpty(rId)) {
			c.put("roomLabels", new JsonArray().add(rId));
		}
		final JsonObject cId = classes.get(entity.getString("div"));
		if (cId != null && isNotEmpty(cId.getString("className"))) {
			c.put("classes", new JsonArray().add(cId.getString("className")));
			c.put("classesExternalIds", new JsonArray().add(cId.getString("classExternalId")));
		}

		JsonArray groups;
		JsonArray groupsExternalIds;
		if (isNotEmpty(entity.getString("rgpmt")) || isNotEmpty(entity.getString("gpe"))) {
			groups = new JsonArray();
			groupsExternalIds = new JsonArray();
			c.put("groups", groups);
			c.put("groupsExternalIds", groupsExternalIds);
			JsonObject regp = regroup.get(entity.getString("rgpmt"));
			if (regp != null) {
				String name = regp.getString("name");
				groups.add(name);
				groupsExternalIds.add(regp.getString("externalId"));
				final JsonArray aggClasses = aggregateRgmtCourses.get(name);
				if (aggClasses != null && !aggClasses.isEmpty()) {
					final TreeSet<String> cclasses = new TreeSet<>();
					final JsonArray cc = c.getJsonArray("classes");
					if (cc != null) {
						cclasses.addAll(cc.getList());
					}
					cclasses.addAll(aggClasses.getList());
					c.put("classes", new JsonArray(new ArrayList<>(cclasses)));
				}
			} else {
				String gName = entity.getString("gpe");
				if (isNotEmpty(gName)) {
					JsonObject g = this.groups.get(entity.getString("div") + gName);
					if (g != null) {
						groups.add(g.getString("name"));
						groupsExternalIds.add(g.getString("externalId"));
					} else {
						groups.add(entity.getString("div") + gName);
						groupsExternalIds.add((String)null);
					}
				}
			}
			if (!groups.isEmpty()) {
				c.put("recurrence", structureId + "_" + rewriteTmpIdToGroups(ligneFicheTTmpId, groups) + "_" + start.substring(0, 3) + end.substring(0, 3));
				c.put("classes", new JsonArray());
				c.put("classesExternalIds", new JsonArray());
			}
		}
		try {
			final String check = JsonUtil.checksum(c);
			if (coursesIds.add(check)) {
				c.put("_id", check);
				return c;
			}
		} catch (NoSuchAlgorithmException e) {
			log.error("Error generating course checksum", e);
		}
		return null;
	}

	@Override
	protected void removeUselessGroups(JsonObject baseParams)
	{
		super.removeUselessGroups(baseParams);
		baseParams.put("date", importDate);
		txXDT.add(get_DELETE_GROUPS("WHERE HEAD(u.profiles) = 'Teacher' OR g.created <> {date}"), baseParams);
	}

	private class LftComparator implements Comparator<JsonObject> {
		@Override
		public int compare(JsonObject o1, JsonObject o2) {
			return o1.getString(CODE).compareTo(o2.getString(CODE));
		}
	}

	public static void launchImport(Vertx vertx, Storage storage, final Message<JsonObject> message, boolean udtUserCreation) {
		launchImport(vertx, storage, message, null, udtUserCreation, null);
	}

	public static void launchImport(Vertx vertx, Storage storage, final Message<JsonObject> message, final PostImport postImport, boolean udtUserCreation, Long forceDateTimestamp) {
		final I18n i18n = I18n.getInstance();
		final String uai = message.body().getString("UAI");
		final boolean updateGroups = message.body().getBoolean("updateGroups", true);
		final boolean updateTimetable = message.body().getBoolean("updateTimetable", true);
		final boolean isManualImport = message.body().getBoolean("isManualImport");
		final String path = message.body().getString("path");
		final String acceptLanguage = message.body().getString("language", "fr");

		if (Utils.isEmpty(uai) || Utils.isEmpty(path) || Utils.isEmpty(acceptLanguage)) {
			JsonObject json = new JsonObject().put("status", "error").put("message",
					i18n.translate("invalid.params", I18n.DEFAULT_DOMAIN, acceptLanguage));
			message.reply(json);
		}

		try {
			String forceDateStr = (forceDateTimestamp == null) ? "" : " with forced date " + DateUtils.format(DateUtils.parseLongDate(forceDateTimestamp), "dd/MM/yyyy HH:mm:ss");
			final long start = System.currentTimeMillis();
			log.info("Launch UDT import : " + uai + forceDateStr);

			new UDTImporter(vertx, storage, uai, path, acceptLanguage, udtUserCreation, isManualImport, updateGroups, updateTimetable, forceDateTimestamp)
			.launch(new Handler<AsyncResult<Report>>() {
				@Override
				public void handle(AsyncResult<Report> event) {
					if (event.succeeded()) {
						log.info("Import UDT : " + uai + " elapsed time " + (System.currentTimeMillis() - start) + " ms" + forceDateStr + ".");
						message.reply(new JsonObject().put("status", "ok")
								.put("result", event.result().getResult()));
						if (postImport != null && udtUserCreation) {
							postImport.execute();
						}
					} else {
						log.error("Error import UDT : " + uai + " elapsed time " +
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
