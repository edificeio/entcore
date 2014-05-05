/*
 * Copyright. Tous droits réservés. WebServices pour l’Education.
 */

package org.entcore.feeder.be1d;

import au.com.bytecode.opencsv.CSV;
import au.com.bytecode.opencsv.CSVReadProc;
import org.entcore.feeder.Feed;
import static org.entcore.feeder.dictionary.structures.DefaultProfiles.*;
import org.entcore.feeder.dictionary.structures.Importer;
import org.entcore.feeder.dictionary.structures.Structure;
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
			"address", "zipCode", "city", "country", "email", "homePhone", "workPhone",
			"#skip#", "mobile" };
	private static final String [] personnelHeader = new String[] { "title", "surname", "lastName", "firstName",
			"address", "zipCode", "city", "country", "email", "homePhone", "mobile", "#skip#" };
	public static final Pattern frenchDatePatter = Pattern.compile("^([0-9]{2})/([0-9]{2})/([0-9]{4})$");
	private final Vertx vertx;
	private final String path;
	private final String separator;
	private final Importer importer = Importer.getInstance();

	public Be1dFeeder(Vertx vertx, String path, String separator) {
		this.vertx = vertx;
		this.path = path;
		this.separator = separator;
	}

	@Override
	public void launch(Importer importer, final Handler<Message<JsonObject>> handler) throws Exception {
		if (importer.isFirstImport()) {
			importer.profileConstraints();
			importer.functionConstraints();
			importer.structureConstraints();
			importer.fieldOfStudyConstraints();
			importer.moduleConstraints();
			importer.userConstraints();
			importer.classConstraints();
			importer.groupConstraints();
			importer.persist(new Handler<Message<JsonObject>>() {
				@Override
				public void handle(Message<JsonObject> message) {
					if (message != null && "ok".equals(message.body().getString("status"))) {
						start(handler);
					} else {
						if (handler != null) {
							handler.handle(message);
						}
					}
				}
			});
		} else {
			start(handler);
		}
	}

	private void start(final Handler<Message<JsonObject>> handler) {
		importer.createOrUpdateProfile(STUDENT_PROFILE);
		importer.createOrUpdateProfile(RELATIVE_PROFILE);
		importer.createOrUpdateProfile(PERSONNEL_PROFILE);
		importer.createOrUpdateProfile(TEACHER_PROFILE);
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

	private void importSchool(String p, Handler<Message<JsonObject>> handler) {
		try {
			JsonObject structure = getStructure(p);
			if (importer.getStructure(structure.getString("externalId")) != null) {
				log.warn("Directory " + p + " is ignored.");
				handler.handle(new ResultMessage());
				return;
			}
			Structure s = importer.createOrUpdateStructure(structure);
			if (s == null) {
				log.error("Structure error with directory " + p + ".");
				handler.handle(new ResultMessage().error("Structure error with directory " + p + "."));
				return;
			}
			importStudent(s, p);
			importRelative(s, p);
			importPersonnel(s, p);
			importer.linkRelativeToClass(RELATIVE_PROFILE_EXTERNAL_ID);
			importer.linkRelativeToStructure(RELATIVE_PROFILE_EXTERNAL_ID);
			importer.persist(handler);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			handler.handle(null);
		}
	}

	private void importPersonnel(final Structure structure, String p) throws FileNotFoundException {
		String charset = "ISO-8859-1"; //detectCharset(csv);
		CSV csvParser = CSV
				.ignoreLeadingWhiteSpace()
				.separator(';')
				.skipLines(1)
				.charset(charset)
				.create();

		csvParser.read(p + File.separator + fileNames[1], new CSVReadProc() {

			@Override
			public void procRow(int rowIdx, String... values) {
				int i = 0;
				JsonObject props = new JsonObject();
				while (i < personnelHeader.length) {
					if (!"#skip#".equals(personnelHeader[i])) {
						if (values[i] != null && !values[i].trim().isEmpty()) {
							props.putString(personnelHeader[i], values[i].trim());
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
					String eId;
					try {
						eId = Hash.sha1((structure.getExternalId() + c).getBytes("UTF-8"));
					} catch (NoSuchAlgorithmException|UnsupportedEncodingException e) {
						log.error(e.getMessage(), e);
						continue;
					}
					structure.createClassIfAbsent(eId, c);
					classes.add(eId);
					cs[j - i][0] = structure.getExternalId();
					cs[j - i][1] = eId;
				}
				generateUserExternalId(props, String.valueOf(rowIdx), structure);
				importer.createOrUpdatePersonnel(props, TEACHER_PROFILE_EXTERNAL_ID, cs, null, true, true);
			}
		});
	}

	private void importRelative(final Structure structure, String p) throws FileNotFoundException {
		String charset = "ISO-8859-1"; //detectCharset(csv);
		CSV csvParser = CSV
				.ignoreLeadingWhiteSpace()
				.separator(';')
				.skipLines(1)
				.charset(charset)
				.create();
		//csv = csv.split("(;;;;;;;;;;;;;;;;;;;;|\n\n|\r\n\r\n)")[0];

		csvParser.read(p + File.separator + fileNames[2], new CSVReadProc() {

			@Override
			public void procRow(int rowIdx, String... values) {
				int i = 0;
				JsonObject props = new JsonObject();
				while (i < relativeHeader.length) {
					if (!"#skip#".equals(relativeHeader[i])) {
						if (values[i] != null && !values[i].trim().isEmpty()) {
							props.putString(relativeHeader[i], values[i].trim());
						}
					}
					i++;
				}
				JsonArray linkStudents = new JsonArray();
				for (i = 13; i < values.length; i += 4) {
					String mapping = structure.getExternalId()+values[i].trim()+
							values[i+1].trim()+values[i+2].trim()+values[i+3].trim();
					if (mapping.trim().isEmpty()) continue;
					try {
						linkStudents.add(Hash.sha1(mapping.getBytes("UTF-8")));
					} catch (NoSuchAlgorithmException|UnsupportedEncodingException e) {
						log.error(e.getMessage(), e);
					}
				}
				generateUserExternalId(props, String.valueOf(rowIdx), structure);
				importer.createOrUpdateUser(props, linkStudents);
			}
		});
	}

	private void importStudent(final Structure structure, String p)
			throws FileNotFoundException {
		String charset = "ISO-8859-1"; //detectCharset(csv);
		CSV csvParser = CSV
				.ignoreLeadingWhiteSpace()
				.separator(';')
				.skipLines(1)
				.charset(charset)
				.create();
		csvParser.read(p + File.separator + fileNames[0], new CSVReadProc() {

			@Override
			public void procRow(int rowIdx, String... values) {
				int i = 0;
				JsonObject props = new JsonObject();
				while (i < values.length && !"#break#".equals(studentHeader[i])) {
					if ("birthDate".equals(studentHeader[i]) && values[i] != null) {
						Matcher m;
						if (values[i] != null &&
								(m = frenchDatePatter.matcher(values[i])).find()) {
							props.putString(studentHeader[i], m.group(3) + "-" + m.group(2) + "-" + m.group(1));
						} else {
							props.putString(studentHeader[i], values[i].trim());
						}
					} else if (!"#skip#".equals(studentHeader[i])) {
						if (values[i] != null && !values[i].trim().isEmpty()) {
							props.putString(studentHeader[i], values[i].trim());
						}
					}
					i++;
				}
				props.putArray("structures", new JsonArray().add(structure.getExternalId()));
				String c = props.getString("classes");
				String[][] cs = null;
				if (c != null && !c.trim().isEmpty()) {
					cs = new String[1][2];
					try {
						String eId = Hash.sha1((structure.getExternalId() + c).getBytes("UTF-8"));
						structure.createClassIfAbsent(eId, c);
						cs[0][0] = structure.getExternalId();
						cs[0][1] = eId;
						props.putArray("classes", new JsonArray().add(eId));
					} catch (NoSuchAlgorithmException|UnsupportedEncodingException e) {
						log.error(e.getMessage(), e);
					}
				}
				generateUserExternalId(props, c, structure);
				importer.createOrUpdateStudent(props, STUDENT_PROFILE_EXTERNAL_ID, null, null, cs,
						null, null, true, true);
			}

		});
	}

	private void generateUserExternalId(JsonObject props, String c, Structure structure) {
		String mapping = structure.getExternalId()+props.getString("surname", "")+
				props.getString("lastName", "")+props.getString("firstName", "")+
				props.getString("email","")+props.getString("title","")+
				props.getString("homePhone","")+props.getString("mobile","")+c;
		try {
			props.putString("externalId", Hash.sha1(mapping.getBytes("UTF-8")));
		} catch (NoSuchAlgorithmException|UnsupportedEncodingException e) {
			log.error(e.getMessage(), e);
		}
	}

	private JsonObject getStructure(String p) throws UnsupportedEncodingException, NoSuchAlgorithmException {
		String dirName = p.substring(p.lastIndexOf(File.separatorChar) + 1);
		String [] n = dirName.split(separator);
		JsonObject structure = new JsonObject();
		if (n.length == 2) {
			structure.putString("name", n[0]);
			structure.putString("UAI", n[1]);
		} else {
			structure.putString("name", dirName);
		}
		structure.putString("externalId", Hash.sha1(dirName.getBytes("UTF-8")));
		return structure;
	}

}
