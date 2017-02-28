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

package org.entcore.feeder.be1d;

import au.com.bytecode.opencsv.CSV;
import au.com.bytecode.opencsv.CSVReadProc;
import org.entcore.feeder.Feed;
import static org.entcore.feeder.dictionary.structures.DefaultProfiles.*;

import org.entcore.feeder.dictionary.structures.DefaultFunctions;
import org.entcore.feeder.dictionary.structures.Importer;
import org.entcore.feeder.dictionary.structures.Structure;
import org.entcore.feeder.utils.CSVUtil;
import org.entcore.feeder.utils.Hash;
import org.entcore.feeder.utils.ResultMessage;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.VoidHandler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.logging.impl.LoggerFactory;

import java.io.*;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Be1dFeeder implements Feed {

	private static final Logger log = LoggerFactory.getLogger(Be1dFeeder.class);
	private static final String [] fileNames = new String[]
			{"CSVExtraction-eleves.csv", "CSVExtraction-enseignants.csv", "CSVExtraction-responsables.csv"};
	public static final String [] studentHeader = new String[] { "lastName", "surname", "firstName",
			"birthDate", "gender", "address", "zipCode", "city", "country", "#skip#", "#skip#",
			"#skip#", "#skip#", "sector", "level", "classes", "#break#" };
	public static final String [] relativeHeader = new String[] { "title", "surname", "lastName", "firstName",
			"address", "zipCode", "city", "country", "email", "homePhone", "workPhone", "mobile" };
	private static final String [] personnelHeader = new String[] { "title", "surname", "lastName", "firstName",
			"address", "zipCode", "city", "country", "email", "homePhone", "mobile", "#skip#" };
	public static final String [] studentUpdateHeader = new String[] { "externalId", "lastName", "surname", "firstName",
			"birthDate", "gender", "address", "zipCode", "city", "country", "#skip#", "#skip#",
			"#skip#", "#skip#", "sector", "level", "classes", "#break#" };
	public static final String [] relativeUpdateHeader = new String[] { "externalId", "title", "surname", "lastName",
			"firstName", "address", "zipCode", "city", "country", "email", "homePhone", "workPhone", "mobile" };
	private static final String [] personnelUpdateHeader = new String[] { "externalId", "title", "surname", "lastName",
			"firstName", "address", "zipCode", "city", "country", "email", "homePhone", "mobile", "#skip#" };
	public static final Pattern frenchDatePatter = Pattern.compile("^([0-9]{2})/([0-9]{2})/([0-9]{4})$");
	private final Vertx vertx;
	private final String path;
	private final Importer importer = Importer.getInstance();

	public Be1dFeeder(Vertx vertx, String path) {
		this.vertx = vertx;
		this.path = path;
	}

	@Override
	public void launch(Importer importer, final Handler<Message<JsonObject>> handler) throws Exception {
		launch(importer, path, handler);
	}

	@Override
	public void launch(Importer importer, final String path, final Handler<Message<JsonObject>> handler) throws Exception {
		start(path, handler);
	}

	@Override
	public String getSource() {
		return "BE1D";
	}

	private void start(final String path, final Handler<Message<JsonObject>> handler) {
		importer.createOrUpdateProfile(STUDENT_PROFILE);
		importer.createOrUpdateProfile(RELATIVE_PROFILE);
		importer.createOrUpdateProfile(PERSONNEL_PROFILE);
		importer.createOrUpdateProfile(TEACHER_PROFILE);
		importer.createOrUpdateProfile(GUEST_PROFILE);
		DefaultFunctions.createOrUpdateFunctions(importer);
		final String [] directories = vertx.fileSystem().readDirSync(path);
		final JsonArray errors = new JsonArray();
		final VoidHandler [] handlers = new VoidHandler[directories.length + 1];
		handlers[handlers.length - 1] = new VoidHandler() {
			@Override
			protected void handle() {
				if (handler != null) {
					ResultMessage m = new ResultMessage();
					if (errors.size() > 0) {
						m.put("errors", errors);
						m.error("Error during import.");
					}
					handler.handle(m);
				}
			}
		};
		for (int i = 0; i < directories.length; i++) {
			final int j = i;
			final String p = directories[i];
			handlers[j] = new VoidHandler() {
				@Override
				protected void handle() {
					for (String file : fileNames) {
						if (!vertx.fileSystem().existsSync(p + File.separator + file)) {
							log.error("Import " + file + " aborted. File  is missing in directory : " + p);
							errors.add("Import " + file + " aborted. File  is missing in directory : " + p);
							handlers[j + 1].handle(null);
							return;
						}
					}
					importSchool(p, new Handler<Message<JsonObject>>() {
						@Override
						public void handle(Message<JsonObject> message) {
							if (message == null) {
								log.error("Null return in directory " + p);
								errors.add("Null return in directory " + p);
							} else if (!"ok".equals(message.body().getString("status"))) {
								log.error("Error in directory " + p + " : " + message.body().encode());
								errors.add("Error in directory " + p + " : " + message.body().encode());
							}
							handlers[j + 1].handle(null);
						}
					});
				}
			};
		}
		handlers[0].handle(null);
	}

	private void importSchool(String p, final Handler<Message<JsonObject>> handler) {
		try {
			JsonObject structure = CSVUtil.getStructure(p);
			final boolean isUpdate = importer.getStructure(structure.getString("externalId")) != null;
			Structure s = importer.createOrUpdateStructure(structure);
			if (s == null) {
				log.error("Structure error with directory " + p + ".");
				handler.handle(new ResultMessage().error("Structure error with directory " + p + "."));
				return;
			}
			final long seed = System.currentTimeMillis();
			importStudent(s, p, isUpdate, seed);
			importRelative(s, p, isUpdate, seed);
			importPersonnel(s, p, isUpdate, seed);
			importer.linkRelativeToClass(RELATIVE_PROFILE_EXTERNAL_ID);
			importer.linkRelativeToStructure(RELATIVE_PROFILE_EXTERNAL_ID);
			importer.restorePreDeletedUsers();
			if (isUpdate) {
				importer.markMissingUsers(s.getExternalId(), new Handler<Void>() {
					@Override
					public void handle(Void event) {
						importer.persist(handler);
					}
				});
			} else {
				importer.persist(handler);
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			handler.handle(null);
		}
	}

	private void importPersonnel(final Structure structure, String p, final boolean isUpdate, final long seed)
			throws FileNotFoundException {
		String charset = CSVUtil.getCharsetSync(p + File.separator + fileNames[1]);
		CSV csvParser = CSV
				.ignoreLeadingWhiteSpace()
				.separator(';')
				.skipLines(1)
				.charset(charset)
				.create();
		final String [] header = isUpdate ? personnelUpdateHeader : personnelHeader;

		csvParser.read(p + File.separator + fileNames[1], new CSVReadProc() {

			@Override
			public void procRow(int rowIdx, String... values) {
				int i = 0;
				JsonObject props = new JsonObject();
				while (i < header.length) {
					if (!"#skip#".equals(header[i])) {
						if (values[i] != null && !values[i].trim().isEmpty()) {
							props.putString(header[i], values[i].trim());
						}
					}
					i++;
				}
				props.putArray("structures", new JsonArray().add(structure.getExternalId()));
				JsonArray classes = new JsonArray();
				props.putArray("classes", classes);
				String[][] cs = new String[values.length - i][2];
				for (int j = i; j < values.length; j++) {
					String c = values[j].trim();
					if (c.isEmpty()) continue;
					String eId = structure.getExternalId() + '$' + c;
					structure.createClassIfAbsent(eId, c);
					classes.add(eId);
					cs[j - i][0] = structure.getExternalId();
					cs[j - i][1] = eId;
				}
				String externalId = props.getString("externalId");
				if (externalId == null || externalId.trim().isEmpty()) {
					generateUserExternalId(props, String.valueOf(rowIdx), structure, seed);
				}
				props.putArray("profiles", new JsonArray().add("Teacher"));
				importer.createOrUpdatePersonnel(props, TEACHER_PROFILE_EXTERNAL_ID,
						props.getArray("structures"), cs, null, true, true);
			}
		});
	}

	private void importRelative(final Structure structure, String p, final boolean isUpdate, final long seed)
			throws FileNotFoundException {
		String charset = CSVUtil.getCharsetSync(p + File.separator + fileNames[2]);
		CSV csvParser = CSV
				.ignoreLeadingWhiteSpace()
				.separator(';')
				.skipLines(1)
				.charset(charset)
				.create();
		//csv = csv.split("(;;;;;;;;;;;;;;;;;;;;|\n\n|\r\n\r\n)")[0];
		final String [] header = isUpdate ? relativeUpdateHeader : relativeHeader;
		final int startClassesMapping = isUpdate ? 13 : 12;

		csvParser.read(p + File.separator + fileNames[2], new CSVReadProc() {

			@Override
			public void procRow(int rowIdx, String... values) {
				int i = 0;
				JsonObject props = new JsonObject();
				while (i < header.length && i < values.length) {
					if (!"#skip#".equals(header[i])) {
						if (values[i] != null && !values[i].trim().isEmpty()) {
							props.putString(header[i], values[i].trim());
						}
					}
					i++;
				}
				JsonArray linkStudents = new JsonArray();
				for (i = startClassesMapping; i < values.length; i += 4) {
					if (isUpdate && !values[i].trim().isEmpty() && values[i+1].trim().isEmpty() &&
							values[i+2].trim().isEmpty() && values[i+3].trim().isEmpty()) {
						linkStudents.add(values[i].trim());
					} else {
						String mapping = structure.getExternalId()+values[i].trim()+
								values[i+1].trim()+values[i+2].trim()+values[i+3].trim()+seed;
						if (mapping.trim().isEmpty()) continue;
						try {
							linkStudents.add(Hash.sha1(mapping.getBytes("UTF-8")));
						} catch (NoSuchAlgorithmException|UnsupportedEncodingException e) {
							log.error(e.getMessage(), e);
						}
					}
				}
				String externalId = props.getString("externalId");
				if (externalId == null || externalId.trim().isEmpty()) {
					generateUserExternalId(props, String.valueOf(rowIdx), structure, seed);
				}
				props.putArray("profiles", new JsonArray().add("Relative"));
				importer.createOrUpdateUser(props, linkStudents);
			}
		});
	}

	private void importStudent(final Structure structure, String p, final boolean isUpdate, final long seed)
			throws FileNotFoundException {
		String charset = CSVUtil.getCharsetSync(p + File.separator + fileNames[0]);
		CSV csvParser = CSV
				.ignoreLeadingWhiteSpace()
				.separator(';')
				.skipLines(1)
				.charset(charset)
				.create();
		final String [] header = isUpdate ? studentUpdateHeader : studentHeader;

		csvParser.read(p + File.separator + fileNames[0], new CSVReadProc() {

			@Override
			public void procRow(int rowIdx, String... values) {
				int i = 0;
				JsonObject props = new JsonObject();
				while (i < values.length && !"#break#".equals(header[i])) {
					if ("birthDate".equals(header[i]) && values[i] != null) {
						Matcher m;
						if (values[i] != null &&
								(m = frenchDatePatter.matcher(values[i])).find()) {
							props.putString(header[i], m.group(3) + "-" + m.group(2) + "-" + m.group(1));
						} else {
							props.putString(header[i], values[i].trim());
						}
					} else if (!"#skip#".equals(header[i])) {
						if (values[i] != null && !values[i].trim().isEmpty()) {
							props.putString(header[i], values[i].trim());
						}
					}
					i++;
				}
				props.putArray("structures", new JsonArray().add(structure.getExternalId()));
				String c = props.getString("classes");
				String[][] cs = null;
				if (c != null && !c.trim().isEmpty()) {
					cs = new String[1][2];
					String eId = structure.getExternalId() + '$' + c;
					structure.createClassIfAbsent(eId, c);
					cs[0][0] = structure.getExternalId();
					cs[0][1] = eId;
					props.putArray("classes", new JsonArray().add(eId));
				}
				String externalId = props.getString("externalId");
				if (externalId == null || externalId.trim().isEmpty()) {
					generateUserExternalId(props, c, structure, seed);
				}
				props.putArray("profiles", new JsonArray().add("Student"));
				importer.createOrUpdateStudent(props, STUDENT_PROFILE_EXTERNAL_ID, null, null, cs,
						null, null, true, true);
			}

		});
	}

	public static void generateUserExternalId(JsonObject props, String c, Structure structure, long seed) {
		String externalId = props.getString("externalId");
		if (externalId != null && !externalId.trim().isEmpty()) {
			return;
		}
		String mapping = structure.getExternalId()+props.getString("surname", "")+
				props.getString("lastName", "")+props.getString("firstName", "")+
				props.getString("email","")+props.getString("title","")+
				props.getString("homePhone","")+props.getString("mobile","")+c+seed;
		try {
			props.putString("externalId", Hash.sha1(mapping.getBytes("UTF-8")));
		} catch (NoSuchAlgorithmException|UnsupportedEncodingException e) {
			log.error(e.getMessage(), e);
		}
	}

}
