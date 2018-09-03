/*
 * Copyright © WebServices pour l'Éducation, 2016
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
 */

package org.entcore.feeder.timetable.udt;

import fr.wseduc.swift.storage.DefaultAsyncResult;
import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.Utils;
import org.entcore.common.neo4j.Neo4jUtils;
import org.entcore.common.utils.FileUtils;
import org.entcore.common.validation.StringValidation;
import org.entcore.feeder.dictionary.structures.PostImport;
import org.entcore.feeder.timetable.AbstractTimetableImporter;
import org.entcore.feeder.timetable.Slot;
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
	private static final Pattern filenameWeekPatter = Pattern.compile("UDCal_[0-9]{2}_([0-9]{2})\\.xml$");
	public static final String DATE_FORMAT = "dd/MM/yyyy";
	private int year;
	private long endStudents;
	private Map<String, Set<String>> coens = new HashMap<>();
	private Map<String, JsonObject> fichesT = new HashMap<>();
	private Map<String, String> regroup = new HashMap<>();
	private Map<String, List<JsonObject>> lfts = new HashMap<>();
	private HashMap<Integer, Integer> periods = new HashMap<>(); // key : start, value : end period
	private int maxYearWeek;
	private Vertx vertx;
	private DateTime startDateStudents;
	private Set<DateTime> holidays = new HashSet<>();
	private Set<Integer> holidaysWeeks = new HashSet<>();
	private Map<String, JsonObject> eleves = new HashMap<>();
	private Map<String, String> codeGepDiv = new HashMap<>();
	private Set<String> usedGroupInCourses = new HashSet<>();
	private final boolean authorizeUserCreation;

	public UDTImporter(Vertx vertx, String uai, String path, String acceptLanguage, boolean authorizeUserCreation) {
		super(uai, path, acceptLanguage);
		this.vertx = vertx;
		this.authorizeUserCreation = authorizeUserCreation;
	}

	@Override
	public void launch(final Handler<AsyncResult<Report>> handler) throws Exception {
		init(new Handler<AsyncResult<Void>>() {
			@Override
			public void handle(AsyncResult<Void> event) {
				if (event.failed()) {
					handler.handle(new DefaultAsyncResult<Report>(event.cause()));
					return;
				}
				try {
					parse(basePath + "UDCal_24.xml");
					parse(basePath + "UDCal_00.xml");
					parse(basePath + "semaines.xml");
					parse(basePath + "UDCal_03.xml");
					parse(basePath + "UDCal_04.xml");
					parse(basePath + "UDCal_05.xml");
					parse(basePath + "UDCal_07.xml");
					parse(basePath + "UDCal_08.xml");
					parse(basePath + "UDCal_10.xml");
					parse(basePath + "UDCal_19.xml");
					parse(basePath + "UDCal_13.xml");
					parse(basePath + "UDCal_21.xml");
					parse(basePath + "UDCal_23.xml");
					parse(basePath + "UDCal_11.xml");
					parse(basePath + "UDCal_12.xml");
					generateCourses(startDateWeek1.getWeekOfWeekyear());
					vertx.fileSystem().readDir(basePath, "UDCal_12_[0-9]+.xml", new Handler<AsyncResult<List<String>>>() {
						@Override
						public void handle(AsyncResult<List<String>> event) {
							if (event.succeeded()) {
								try {
									for (String p : event.result()) {
										Matcher m = filenameWeekPatter.matcher(p);
										if (m.find()) {
											final int weekNumber = Integer.parseInt(m.group(1));
											if (periods.containsKey(weekNumber)) {
												parse(p);
												generateCourses(weekNumber);
											} else {
												log.warn("Ignore week : " + weekNumber);
											}
										}
									}
									persistUsedGroups();
									commit(handler);
								} catch (Exception e) {
									handler.handle(new DefaultAsyncResult<Report>(e));
								}
							}
						}
					});
				} catch (Exception e) {
					handler.handle(new DefaultAsyncResult<Report>(e));
				}
			}
		});
	}

	private void parse(String filePath) throws Exception {
		InputSource in = new InputSource(new FileInputStream(filePath));
		UDTHandler sh = new UDTHandler(this);
		XMLReader xr = XMLReaderFactory.createXMLReader();
		xr.setContentHandler(sh);
		xr.parse(in);
	}

	void setYear(String year) {
		if (this.year == 0) {
			this.year = Integer.parseInt(year);
		}
	}

	public void setEndStudents(String endStudents) {
		this.endStudents = DateTime.parse(endStudents, DateTimeFormat.forPattern(DATE_FORMAT)).getMillis();
	}

	public void setStartDateStudents(String startDateStudents) {
		this.startDateStudents = DateTime.parse(startDateStudents, DateTimeFormat.forPattern(DATE_FORMAT));
		maxYearWeek = this.startDateStudents.weekOfWeekyear().withMaximumValue().weekOfWeekyear().get();
	}

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

	void initSchedule(JsonObject e) {
		final String slotKey = e.getString("code_jour") + padLeft(e.getString(CODE), 2, '0') + e.getString("code_site");
		Slot s = new Slot(e.getString("hre_deb"), e.getString("hre_fin"), slotDuration);
		slots.put(slotKey, s);
	}

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


	void initHolidays(JsonObject currentEntity) {
		DateTime s = DateTime.parse(currentEntity.getString("debut"), DateTimeFormat.forPattern(DATE_FORMAT));
		DateTime e = DateTime.parse(currentEntity.getString("fin"), DateTimeFormat.forPattern(DATE_FORMAT));
		while (s.isBefore(e)) {
			holidays.add(s);
			s = s.plusDays(1);
			holidaysWeeks.add(s.getWeekOfWeekyear());
		}
		holidays.add(e);
	}

	void addRoom(JsonObject currentEntity) {
		rooms.put(currentEntity.getString(CODE), currentEntity.getString("nom"));
	}

	@Override
	protected String getTeacherMappingAttribute() {
		return "externalId";
	}

	@Override
	protected String getSource() {
		return UDT;
	}

	void addProfesseur(JsonObject currentEntity) {
		try {
			if (isEmpty(currentEntity.getString("code_matppl"))) {
				// Ignore prof without subject.
				// Often this case corresponds to personnel.
				return;
			}
			final String id = currentEntity.getString(CODE);
			String externalId = currentEntity.getString("epj");
			JsonObject p = persEducNat.applyMapping(currentEntity);
			p.put("profiles", new fr.wseduc.webutils.collections.JsonArray().add("Teacher"));
			if (isEmpty(externalId)) {
				externalId = JsonUtil.checksum(p, JsonUtil.HashAlgorithm.MD5);
			}
			p.put("externalId", externalId);
			userImportedExternalId.add(externalId);
			final String[] teacherId = teachersMapping.get(externalId);
			if (teacherId != null && isNotEmpty(teacherId[0])) {
				teachers.put(id, teacherId[0]);
				if (getSource().equals(teacherId[1]) && authorizeUserCreation) {
					updateUser(p);
				}
			} else {
				final String userId = UUID.randomUUID().toString();
				p.put("id", userId);
				p.put("structures", new JsonArray().add(structureExternalId));
				if (authorizeUserCreation) {
					persEducNat.createOrUpdatePersonnel(p, TEACHER_PROFILE_EXTERNAL_ID, structure, null, null, true, true);
				}
				teachers.put(id, userId);
			}
		} catch (Exception e) {
			report.addError(e.getMessage());
		}
	}

	void addSubject(JsonObject s) {
		final String code = s.getString(CODE);
		super.addSubject(code, new JsonObject().put("Code", code).put("Libelle", s.getString("libelle")));
	}

	void addClasse(JsonObject currentEntity) {
		final String id = currentEntity.getString(CODE);
		classes.put(id, currentEntity);
		final String codeGep =  currentEntity.getString("code_gep");
		if (isNotEmpty(codeGep)) {
			codeGepDiv.put(id, codeGep);
		}
		final String ocn = getOrElse(currentEntity.getString("libelle"), id, false);
		final String className = (classesMapping != null) ? getOrElse(classesMapping.getString(ocn), ocn, false) : ocn;
		currentEntity.put("className", className);
		if (className != null) {
			txXDT.add(UNKNOWN_CLASSES, new JsonObject().put("UAI", UAI).put("className", className));
		}
	}

	void addGroup(JsonObject currentEntity) {
		final String id = currentEntity.getString("code_div") + currentEntity.getString(CODE);
		groups.put(id, currentEntity);
		String name = currentEntity.getString("code_div") + " Gr " + currentEntity.getString(CODE);
		if (isEmpty(name)) {
			name = id;
		}
		currentEntity.put("code_gep", codeGepDiv.get(currentEntity.getString("code_div")));
		currentEntity.put("idgpe", currentEntity.remove("id"));
		final String set = "SET " + Neo4jUtils.nodeSetPropertiesFromJson("fg", currentEntity);
		txXDT.add(CREATE_GROUPS + set, currentEntity.put("structureExternalId", structureExternalId)
				.put("name", name).put("displayNameSearchField", Validator.sanitize(name))
				.put("externalId", structureExternalId + "$" + name)
				.put("id", UUID.randomUUID().toString()).put("source", getSource()));
	}

	void addGroup2(JsonObject currentEntity) {
		final String codeGroup = currentEntity.getString("code_gpe");
		final String name = currentEntity.getString("nom");
		if (isNotEmpty(codeGroup)) {
			final String groupId = currentEntity.getString("code_div") + codeGroup;
			JsonObject group = groups.get(groupId);
			if (group == null) {
				report.addError("unknown.group.mapping");
				return;
			}
			JsonArray groups = group.getJsonArray("groups");
			if (groups == null) {
				groups = new fr.wseduc.webutils.collections.JsonArray();
				group.put("groups", groups);
			}
			groups.add(name);
		} else {
			final String classId = currentEntity.getString("code_div");
			JsonObject classe = classes.get(classId);
			if (classe == null) {
				report.addError("unknown.class.mapping");
				return;
			}
			JsonArray groups = classe.getJsonArray("groups");
			if (groups == null) {
				groups = new fr.wseduc.webutils.collections.JsonArray();
				classe.put("groups", groups);
			}
			groups.add(name);
		}
		regroup.put(currentEntity.getString(CODE), name);
		txXDT.add(CREATE_GROUPS + "SET fg.idrgpmt = {idrgpmt} " , new JsonObject()
				.put("structureExternalId", structureExternalId)
				.put("name", name).put("displayNameSearchField", Validator.sanitize(name))
				.put("externalId", structureExternalId + "$" + name)
				.put("id", UUID.randomUUID().toString()).put("source", getSource())
				.put("idrgpmt", currentEntity.getString("id")));
	}

	private void persistUsedGroups() {
		txXDT.add(PERSIST_USED_GROUPS, new JsonObject().put("structureExternalId", structureExternalId)
				.put("usedGroups", new JsonArray(new ArrayList<>(usedGroupInCourses))));
	}

	void eleveMapping(JsonObject currentEntity) {
		eleves.put(currentEntity.getString(CODE), currentEntity);
	}

	void addEleve(JsonObject currentEntity) {
		if("0".equals(currentEntity.getString("theorique"))) {
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
		JsonArray groups;
		if (isNotEmpty(codeGroup)) {
			JsonObject group = this.groups.get(codeDiv + codeGroup);
			if (group == null) {
				report.addError("unknown.group.mapping");
				return;
			}
			final String name = group.getString("code_div") + " Gr " + group.getString(CODE);
			txXDT.add(STUDENTS_TO_GROUPS, new JsonObject()
					.put("firstName", eleve.getString("prenom", "").toLowerCase())
					.put("lastName", eleve.getString("nom", "").toLowerCase())
					.put("birthDate", StringValidation.convertDate(eleve.getString("naissance", "")))
					.put("externalId", structureExternalId + "$" + name)
					.put("structureExternalId", structureExternalId)
					.put("source", UDT)
					.put("inDate", importTimestamp)
					.put("outDate", endStudents)
					.put("now", importTimestamp));
			groups = group.getJsonArray("groups");

		} else {
			JsonObject classe = classes.get(codeDiv);
			if (classe == null) {
				report.addError("unknown.class.mapping");
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
						.put("externalId", structureExternalId + "$" + o2.toString())
						.put("structureExternalId", structureExternalId)
						.put("source", UDT)
						.put("inDate", importTimestamp)
						.put("outDate", endStudents)
						.put("now", importTimestamp));
			}
		}
	}

	void addCoens(JsonObject currentEntity) {
		final String clf = currentEntity.getString("lignefic");
		Set<String> teachers = coens.get(clf);
		if (teachers == null) {
			teachers = new HashSet<>();
			coens.put(clf, teachers);
		}
		final String externalId = currentEntity.getString("epj");
		String[] teacherId = null;
		if (isNotEmpty(externalId)) {
			teacherId = teachersMapping.get(externalId);
		}
		if (teacherId == null || isEmpty(teacherId[0])) {
			teacherId = new String[]{this.teachers.get(currentEntity.getString("prof")), getSource()};
		}
		if (teacherId != null && isNotEmpty(teacherId[0])) {
			teachers.add(teacherId[0]);
		}
	}

	void addFicheT(JsonObject currentEntity) {
		final String id = currentEntity.getString(CODE);
		fichesT.put(id, currentEntity);
	}

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
		return entity.getString("div") + entity.getString("mat") + entity.getString("prof") +
				entity.getString("rgpmt") + getOrElse(entity.getString("gpe"), "_", false);
	}

	private void generateCourses(int periodWeek) {
		for (Map.Entry<Integer, Integer> e : getNextHolidaysWeek(periodWeek).entrySet()) {
			for (List<JsonObject> c : lfts.values()) {
				Collections.sort(c, new LftComparator());
				String start = null;
				int current = 0;
				JsonObject previous = null;
				for (JsonObject j : c) {
					int val = Integer.parseInt(j.getString(CODE).substring(0, 3));
					if (start == null) {
						start = j.getString("fic");
						current = val;
					} else if ((++current) != val) {
						persistCourse(generateCourse(start, previous.getString("fic"), previous, e.getKey(), e.getValue()));
						start = j.getString("fic");
						current = val;
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

	private JsonObject generateCourse(String start, String end, JsonObject entity, int periodWeek, int endPeriodWeek) {
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
		while (holidays.contains(startDate)) {
			startDate = startDate.plusWeeks(1);
		}
		startDate = startDate.plusSeconds(slotStart.getStart());
		//final int epw = periods.get(periodWeek);
		final int cepw = (endPeriodWeek < startDateWeek1.getWeekOfWeekyear()) ? endPeriodWeek + maxYearWeek : endPeriodWeek;
		DateTime endDate = startDateWeek1.plusWeeks(cepw - startDateWeek1.getWeekOfWeekyear()).plusDays(day - 1);
		while (holidays.contains(endDate)) {
			endDate = endDate.minusWeeks(1);
		}
		endDate = endDate.plusSeconds(slotEnd.getEnd());
		if (endDate.isBefore(startDate)) {
			log.error("endDate before start date. cpw : " + cpw + ", cepw : " + cepw + ", startDateWeek1 : " + startDateWeek1 + ", endPeriodOfWeek : " + endPeriodWeek);
		}
		final Set<String> ce = coens.get(start);
		JsonArray teacherIds;
		if (ce != null && ce.size() > 0) {
			teacherIds = new fr.wseduc.webutils.collections.JsonArray(new ArrayList<>(ce));
		} else {
			teacherIds = new fr.wseduc.webutils.collections.JsonArray();
		}
		final String pId = teachers.get(entity.getString("prof"));
		if (isNotEmpty(pId)) {
			teacherIds.add(pId);
		}

		final JsonObject c = new JsonObject()
				.put("structureId", structureId)
				.put("startDate", startDate.toString())
				.put("endDate", endDate.toString())
				.put("dayOfWeek", day)
				.put("teacherIds", teacherIds);
		final String sId = subjects.get(entity.getString("mat"));
		if (isNotEmpty(sId)) {
			c.put("subjectId", sId);
		}
		final String rId = rooms.get(entity.getString("salle"));
		if (isNotEmpty(rId)) {
			c.put("roomLabels", new fr.wseduc.webutils.collections.JsonArray().add(rId));
		}
		final JsonObject cId = classes.get(entity.getString("div"));
		if (cId != null && isNotEmpty(cId.getString("className"))) {
			c.put("classes", new fr.wseduc.webutils.collections.JsonArray().add(cId.getString("className")));
		}

		JsonArray groups;
		if (isNotEmpty(entity.getString("rgpmt")) || isNotEmpty(entity.getString("gpe"))) {
			groups = new fr.wseduc.webutils.collections.JsonArray();
			c.put("groups", groups);
			String name = regroup.get(entity.getString("rgpmt"));
			if (isNotEmpty(name)) {
				groups.add(name);
			}
			String gName = entity.getString("gpe");
			if (isNotEmpty(gName)) {
				groups.add(entity.getString("div") + " Gr " + gName);
			}
		}
		try {
			c.put("_id", JsonUtil.checksum(c));
		} catch (NoSuchAlgorithmException e) {
			log.error("Error generating course checksum", e);
		}
		return c;
	}

	private class LftComparator implements Comparator<JsonObject> {
		@Override
		public int compare(JsonObject o1, JsonObject o2) {
			return o1.getString(CODE).compareTo(o2.getString(CODE));
		}
	}

	public static void launchImport(Vertx vertx, final Message<JsonObject> message, boolean udtUserCreation) {
		launchImport(vertx, message, null, udtUserCreation);
	}

	public static void launchImport(Vertx vertx, final Message<JsonObject> message, final PostImport postImport, boolean udtUserCreation) {
		final I18n i18n = I18n.getInstance();
		final String uai = message.body().getString("UAI");
		final String path = message.body().getString("path");
		final String acceptLanguage = message.body().getString("language", "fr");

		if (Utils.isEmpty(uai) || Utils.isEmpty(path) || Utils.isEmpty(acceptLanguage)) {
			JsonObject json = new JsonObject().put("status", "error").put("message",
					i18n.translate("invalid.params", I18n.DEFAULT_DOMAIN, acceptLanguage));
			message.reply(json);
		}

		try {
			final String parentPath = FileUtils.getParentPath(path);
			FileUtils.unzip(path, parentPath);
			new UDTImporter(vertx, uai, parentPath + File.separator, acceptLanguage, udtUserCreation).launch(new Handler<AsyncResult<Report>>() {
				@Override
				public void handle(AsyncResult<Report> event) {
					if (event.succeeded()) {
						message.reply(new JsonObject().put("status", "ok")
								.put("result", event.result().getResult()));
						if (postImport != null) {
							postImport.execute();
						}
					} else {
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
