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

package org.entcore.feeder.be1d;

import au.com.bytecode.opencsv.CSV;
import au.com.bytecode.opencsv.CSVReadProc;
import fr.wseduc.webutils.collections.Joiner;
import org.entcore.feeder.utils.CSVUtil;
import org.entcore.feeder.utils.Report;
import org.entcore.feeder.ImportValidator;
import org.entcore.feeder.utils.Hash;
import org.entcore.feeder.utils.Validator;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.logging.impl.LoggerFactory;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Be1dValidator extends Report implements ImportValidator {

	private static final Logger log = LoggerFactory.getLogger(Be1dValidator.class);
	public static final String [] fileNames = new String[]
			{"CSVExtraction-eleves.csv", "CSVExtraction-enseignants.csv", "CSVExtraction-responsables.csv"};
	public static final String [] studentHeader = new String[] { "lastName", "surname", "firstName",
			"birthDate", "gender", "address", "zipCode", "city", "country", "#skip#", "#skip#",
			"#skip#", "#skip#", "sector", "level", "classes", "#break#" };
	public static final String [] relativeHeader = new String[] { "title", "surname", "lastName", "firstName",
			"address", "zipCode", "city", "country", "email", "homePhone", "workPhone", "mobile" };
	private static final String [] personnalHeader = new String[] { "title", "surname", "lastName", "firstName",
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
	private final String separator;
	private final Validator structureValidator;
	private final Validator userValidator;
	private final Validator personnelValidator;
	private final Validator studentValidator;
	private final Set<String> classes = new HashSet<>();
	private final Set<String> students = new HashSet<>();
	private final Map<String, String> classesNamesMapping = new HashMap<>();
	private final MappingFinder mappingFinder;

	public Be1dValidator(Vertx vertx, String separator, String acceptLanguage) {
		super(acceptLanguage);
		this.vertx = vertx;
		this.separator = separator;
		structureValidator = new Validator("dictionary/schema/Structure.json", true);
		userValidator = new Validator("dictionary/schema/User.json", true);
		personnelValidator = new Validator("dictionary/schema/Personnel.json", true);
		studentValidator = new Validator("dictionary/schema/Student.json", true);
		mappingFinder = new MappingFinder(vertx);
	}

	public void validate(String path, Handler<JsonObject> handler) {
		final String [] directories = vertx.fileSystem().readDirSync(path);

		for (String p : directories) {
			for (String file : fileNames) {
				if (!vertx.fileSystem().existsSync(p + File.separator + file)) {
					addErrorWithParams("file.missing.in.directory", file, p);
					handler.handle(result);
					return;
				}
			}
			validateSchool(p, result, handler);
		}
	}

	private void validateSchool(final String p, final JsonObject result, final Handler<JsonObject> handler) {
		try {
			final JsonObject structure = getStructure(p);
			final String sId = structure.getString("externalId");
			mappingFinder.structureExists(sId, new Handler<Boolean>() {
				@Override
				public void handle(final Boolean isUpdate) {
					if (isUpdate) {
						mappingFinder.generateFilesWithExternalIds(p, sId, new Handler<String>() {
							@Override
							public void handle(String error) {
								if (error == null) {
									validate(isUpdate, handler);
								} else {
									addError("error.find.externalIds");
									handler.handle(result);
								}
							}
						});
					} else {
						validate(isUpdate, handler);
					}
				}

				private void validate(boolean isUpdate, Handler<JsonObject> handler) {
					String error = structureValidator.validate(structure);
					if (error != null) {
						addErrorWithParams("structure.contains.errors", structure.encode(), error);
					}
					try {
						validateStudent(sId, p, isUpdate);
						validateRelative(sId, p, isUpdate);
						validatePersonnel(sId, p, isUpdate);
					} catch (FileNotFoundException e) {
						log.error(e.getMessage(), e);
						addError("validate.error");
						handler.handle(result);
					}
					handler.handle(result);
				}
			});
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			addError("validate.error");
			handler.handle(result);
		}
	}

	private void validatePersonnel(final String structure, String p, boolean isUpdate) throws FileNotFoundException {
		String charset = CSVUtil.getCharsetSync(p + File.separator + fileNames[1]);
		CSV csvParser = CSV
				.ignoreLeadingWhiteSpace()
				.separator(';')
				.skipLines(1)
				.charset(charset)
				.create();

		final String [] header = isUpdate ? personnelUpdateHeader : personnalHeader;
		csvParser.read(p + File.separator + fileNames[1], new CSVReadProc() {

			@Override
			public void procRow(int rowIdx, String... values) {
				int i = 0;
				JsonObject props = new JsonObject();
				try {
					while (i < header.length) {
						if (!"#skip#".equals(header[i])) {
							if (values[i] != null && !values[i].trim().isEmpty()) {
								props.putString(header[i], values[i].trim());
							}
						}
						i++;
					}
				} catch (ArrayIndexOutOfBoundsException ae) {
					log.error("unknown.error.line" + (rowIdx + 2) + " : " + Joiner.on("; ").join(values), ae);
					addErrorByFile(fileNames[1], "unknown.error.line", Integer.toString(rowIdx + 2));
					return;
				}
				props.putArray("structures", new JsonArray().add(structure));
				final JsonArray classes = new JsonArray();
				final JsonArray classesNames = new JsonArray();
				props.putArray("classes", classes);
				if (values.length < i) {
					addErrorByFile(fileNames[1], "missing.columns", Integer.toString(rowIdx + 2));
					return;
				}
				String[][] cs = new String[values.length - i][2];
				for (int j = i; j < values.length; j++) {
					String c = values[j].trim();
					if (c.isEmpty()) continue;
					String eId;
					try {
						eId = Hash.sha1((structure + c).getBytes("UTF-8"));
					} catch (NoSuchAlgorithmException|UnsupportedEncodingException e) {
						log.error(e.getMessage(), e);
						continue;
					}
					if (!Be1dValidator.this.classes.contains(eId)) {
						addErrorByFile(fileNames[1], "invalid.class.line", Integer.toString(rowIdx + 2), c);
					}
					classes.add(eId);
					classesNames.addString(c);
					cs[j - i][0] = structure;
					cs[j - i][1] = eId;
				}
				String externalId = props.getString("externalId");
				final State state;
				if (externalId == null || externalId.trim().isEmpty()) {
					generateUserExternalId(props, String.valueOf(rowIdx), structure);
					state = State.NEW;
				} else {
					state = State.UPDATED;
				}

				String error = personnelValidator.validate(props, acceptLanguage);
				if (error != null) {
					addErrorByFile(fileNames[1], "error.line", Integer.toString(rowIdx + 2), error);
				} else {
					addUser(fileNames[1], props.putString("state", translate(state.name()))
							.putString("translatedProfile", translate("Teacher"))
							.putString("classesStr", Joiner.on(", ").join(classesNames)));
				}
			}
		});
	}

	private void validateRelative(final String structure, final String p, final boolean isUpdate) throws FileNotFoundException {
		String charset = CSVUtil.getCharsetSync(p + File.separator + fileNames[2]);
		final CSV csvParser = CSV
				.ignoreLeadingWhiteSpace()
				.separator(';')
				.skipLines(1)
				.charset(charset)
				.create();
		//csv = csv.split("(;;;;;;;;;;;;;;;;;;;;|\n\n|\r\n\r\n)")[0];

		final String [] header = isUpdate ? relativeUpdateHeader : relativeHeader;
		final int startClassesMapping = isUpdate ? 13 : 12;
		final List<String> lines = new ArrayList<>();

		csvParser.read(p + File.separator + fileNames[2], new CSVReadProc() {

			@Override
			public void procRow(int rowIdx, String... values) {
				int i = 0;
				JsonObject props = new JsonObject();
				final JsonArray classesNames = new JsonArray();
				try {
					while (i < header.length) { // && i < values.length) {
						if (!"#skip#".equals(header[i])) {
							if (values[i] != null && !values[i].trim().isEmpty()) {
								props.putString(header[i], values[i].trim());
							}
						}
						i++;
					}
				} catch (ArrayIndexOutOfBoundsException e) {
					log.error("error line " + rowIdx, e);
					log.error(Joiner.on("").join(values));
					log.error(values.length);
					if (lines.isEmpty()) {
						try {
							lines.addAll(Files.readAllLines(Paths.get(p + File.separator + fileNames[2]),
									Charset.forName("ISO-8859-1")));
						} catch (IOException e1) {
							log.error(e1.getMessage(), e1);
							addErrorByFile(fileNames[2], "unknown.error.line", Integer.toString(rowIdx + 2));
						}
					}
					if (!lines.isEmpty() && lines.size() > rowIdx + 1) {
						try {
							String line = lines.get(rowIdx + 1);
							log.info(line);
							String[] vals = line.replaceAll("\"", "").split(";");
							while (i < header.length) { // && i < values.length) {
								if (!"#skip#".equals(header[i])) {
									if (vals[i] != null && !vals[i].trim().isEmpty()) {
										props.putString(header[i], vals[i].trim());
									}
								}
								i++;
							}
						} catch (RuntimeException e2) {
							log.error(e2.getMessage(), e2);
							addErrorByFile(fileNames[2], "unknown.error.line", Integer.toString(rowIdx + 2));
							return;
						}
					} else {
						addErrorByFile(fileNames[2], "unknown.error.line", Integer.toString(rowIdx + 2));
					}
				}
				JsonArray linkStudents = new JsonArray();
				try {
					for (i = startClassesMapping; i < values.length; i += 4) {
						String mapping;
						if (isUpdate && !values[i].trim().isEmpty() && values[i + 1].trim().isEmpty() &&
								values[i + 2].trim().isEmpty() && values[i + 3].trim().isEmpty()) {
							mapping = values[i].trim();

						} else {
							mapping = structure + values[i].trim() +
									values[i + 1].trim() + values[i + 2].trim() + values[i + 3].trim();
							try {
								mapping = Hash.sha1(mapping.getBytes("UTF-8"));
							} catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
								log.error(e.getMessage(), e);
							}
						}
						if (classesNamesMapping.get(mapping) != null) {
							classesNames.addString(classesNamesMapping.get(mapping));
						}
//					String mapping = structure+values[i].trim()+
//							values[i+1].trim()+values[i+2].trim()+values[i+3].trim();
						if (mapping.trim().isEmpty()) continue;
						if (values[i] != null && !values[i].trim().isEmpty() && !students.contains(mapping)) {
							addErrorByFile(fileNames[2], "invalid.student.line", Integer.toString(rowIdx + 2),
									values[i].trim() + " " + values[i + 1].trim() + " " + values[i + 2]);
						}
					}
				} catch (ArrayIndexOutOfBoundsException ae) {
					log.error("unknown.error.line" + (rowIdx + 2) + " : " + Joiner.on("; ").join(values), ae);
					addErrorByFile(fileNames[2], "unknown.error.line", Integer.toString(rowIdx + 2));
					return;
				}
				String externalId = props.getString("externalId");
				final State state;
				if (externalId == null || externalId.trim().isEmpty()) {
					generateUserExternalId(props, String.valueOf(rowIdx), structure);
					state = State.NEW;
				} else {
					state = State.UPDATED;
				}
				String error = userValidator.validate(props, acceptLanguage);
				if (error != null) {
					addErrorByFile(fileNames[2], "error.line", Integer.toString(rowIdx + 2), error);
				} else {
					addUser(fileNames[2], props.putString("state", translate(state.name()))
							.putString("translatedProfile", translate("Relative"))
							.putString("classesStr", Joiner.on(", ").join(classesNames)));
				}
			}
		});
	}

	private void validateStudent(final String structure, String p, final boolean isUpdate)
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
				try {
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
				} catch (ArrayIndexOutOfBoundsException ae) {
					log.error("unknown.error.line" + (rowIdx + 2) + " : " + Joiner.on("; ").join(values), ae);
					addErrorByFile(fileNames[0], "unknown.error.line", Integer.toString(rowIdx + 2));
					return;
				}
				props.putArray("structures", new JsonArray().add(structure));
				final JsonArray classesNames = new JsonArray();
				String c = props.getString("classes");
				String[][] cs = null;
				if (c != null && !c.trim().isEmpty()) {
					cs = new String[1][2];
					try {
						String eId = Hash.sha1((structure + c).getBytes("UTF-8"));
						classes.add(eId);
						cs[0][0] = structure;
						cs[0][1] = eId;
						props.putArray("classes", new JsonArray().add(eId));
						classesNames.addString(c);
					} catch (NoSuchAlgorithmException|UnsupportedEncodingException e) {
						log.error(e.getMessage(), e);
					}
				}
				String externalId = props.getString("externalId");
				final State state;
				if (externalId == null || externalId.trim().isEmpty()) {
					generateUserExternalId(props, c, structure);
					state = State.NEW;
				} else {
					state = State.UPDATED;
				}
				students.add(props.getString("externalId"));
				final String error = studentValidator.validate(props, acceptLanguage);
				if (error != null) {
					addErrorByFile(fileNames[0], "error.line", Integer.toString(rowIdx + 2), error);
				} else {
					final String classesStr = Joiner.on(", ").join(classesNames);
					classesNamesMapping.put(props.getString("externalId"), classesStr);
					addUser(fileNames[0], props.putString("state", translate(state.name()))
							.putString("translatedProfile", translate("Student"))
							.putString("classesStr", classesStr));
				}
			}

		});
	}

	private void generateUserExternalId(JsonObject props, String c, String structure) {
		String mapping = structure+props.getString("surname", "")+
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
		int idx = n[0].indexOf("@");
		if (idx >= 0) {
			structure.putString("name", n[0].substring(0, idx));
			structure.putString("externalId", n[0].substring(idx + 1));
		} else {
			structure.putString("name", n[0]);
			structure.putString("externalId", Hash.sha1(dirName.getBytes("UTF-8")));
		}
		if (n.length == 2) {
			structure.putString("UAI", n[1]);
		}
		return structure;
	}

}
