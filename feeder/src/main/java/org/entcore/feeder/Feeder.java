package org.entcore.feeder;

import au.com.bytecode.opencsv.CSV;
import au.com.bytecode.opencsv.CSVReadProc;
import org.entcore.datadictionary.generation.ActivationCodeGenerator;
import org.entcore.datadictionary.generation.DisplayNameGenerator;
import org.entcore.datadictionary.generation.IdGenerator;
import org.entcore.datadictionary.generation.LoginGenerator;
import org.entcore.feeder.utils.Neo4j;
import org.entcore.feeder.utils.StatementsBuilder;
import org.mozilla.universalchardet.UniversalDetector;
import org.vertx.java.busmods.BusModBase;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.io.StringReader;
import java.text.Normalizer;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Feeder extends BusModBase implements Handler<Message<JsonObject>> {

	private Neo4j neo4j;
	private LoginGenerator loginGenerator;
	private DisplayNameGenerator displayNameGenerator;
	private ActivationCodeGenerator activationCodeGenerator;
	private IdGenerator idGenerator;
	private static final Pattern frenchDatePatter = Pattern.compile("^([0-9]{2})/([0-9]{2})/([0-9]{4})$");

	@Override
	public void start() {
		super.start();
		String neo4jAddress = container.config().getString("neo4j-address");
		if (neo4jAddress == null || neo4jAddress.trim().isEmpty()) {
			logger.fatal("Missing neo4j address.");
			return;
		}
		neo4j = new Neo4j(vertx.eventBus(), neo4jAddress);
		activationCodeGenerator = new ActivationCodeGenerator();
		displayNameGenerator = new DisplayNameGenerator();
		loginGenerator = new LoginGenerator();
		idGenerator = new IdGenerator();
		loadUsedLogin();
		vertx.eventBus().registerHandler(
				container.config().getString("address", "entcore.feeder"), this);
	}

	private void loadUsedLogin() {
		String query = "MATCH (u:User) RETURN u.login as login";
		neo4j.execute(query, new JsonObject(), new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				if ("ok".equals(event.body().getString("status")) && event.body().getArray("result") != null) {
					Set<String> login = new HashSet<>();
					for (Object o : event.body().getArray("result")) {
						if (!(o instanceof JsonObject)) continue;
						login.add(((JsonObject) o).getString("login", ""));
					}
					LoginGenerator.setUsedLogin(login);
				}
			}
		});
	}

	@Override
	public void handle(Message<JsonObject> message) {
		switch (message.body().getString("action", "")) {
			case "csvClassStudent": csvClassStudent(message);
				break;
			case "csvClassRelative" : csvClassRelative(message);
				break;
			default:
				sendError(message, "Invalid action.");
		}
	}

	private void csvClassStudent(final Message<JsonObject> message) {
		String classId = message.body().getString("classId");
		if (classId == null || classId.trim().isEmpty()) {
			sendError(message, "Invalid classId.");
			return;
		}
		String csv = message.body().getString("csv");
		if (csv == null || csv.trim().isEmpty()) {
			sendError(message, "Missing csv.");
			return;
		}

		String charset = detectCharset(csv);
		CSV csvParser = CSV
				.ignoreLeadingWhiteSpace()
				.separator(';')
				.skipLines(1)
				.charset(charset)
				.create();
		final String [] header = new String[] { "lastName", "surname", "firstName", "birthDate", "gender",
				"address", "zipCode", "city", "country", "#skip#", "#skip#", "#skip#", "#skip#",
				"sector", "level", "classes", "#break#" };
		final StatementsBuilder statementsBuilder = new StatementsBuilder();
		final JsonObject params = new JsonObject().putString("classId", classId);
		final String query =
				"MATCH (c:`Class` { id : {classId}})<-[:DEPENDS]-(csg:ClassStudentGroup)" +
				"-[:DEPENDS]->(ssg:SchoolStudentGroup)-[:DEPENDS]->(s:School) " +
				"CREATE c<-[:APPARTIENT]-(u:Student:User:Visible {props}), " +
				"csg<-[:APPARTIENT]-u, ssg<-[:APPARTIENT]-u, s<-[:APPARTIENT]-u " +
				"RETURN u.id as id";
		csvParser.readAndClose(new StringReader(csv), new CSVReadProc() {

			@Override
			public void procRow(int rowIdx, String... values) {
				int i = 0;
				JsonObject props = new JsonObject();
				while (i < values.length && !"#break#".equals(header[i])) {
					if ("classes".equals(header[i])) {
						props.putArray(header[i], new JsonArray().addString(values[i]));
					} else if ("lastName".equals(header[i])) {
						if (values[i] != null && !values[i].trim().isEmpty()) {
							props.putString(header[i], values[i].trim());
						} else {
							sendError(message, "invalid.lastName " + (rowIdx + 2));
							return;
						}
					} else if ("firstName".equals(header[i])) {
						if (values[i] != null && !values[i].trim().isEmpty()) {
							props.putString(header[i], values[i].trim());
						} else {
							sendError(message, "invalid.firstName " + (rowIdx + 2));
							return;
						}
					} else if ("birthDate".equals(header[i])) {
						Matcher m;
						if (values[i] != null && values[i].matches("^[0-9]{4}-[0-9]{2}-[0-9]{2}$")) {
							props.putString(header[i], values[i]);
						} else if (values[i] != null &&
								(m = frenchDatePatter.matcher(values[i])).find()) {
							props.putString(header[i], m.group(3) + "-" + m.group(2) + "-" + m.group(1));
						} else {
							sendError(message, "invalid.birthDate " + (rowIdx + 2));
							return;
						}
					} else if (!"#skip#".equals(header[i])) {
						props.putString(header[i], values[i]);
					}
					i++;
				}
				props.putString("id", UUID.randomUUID().toString())
						.putString("displayName", displayNameGenerator.generate(
								props.getString("firstName"), props.getString("lastName")))
						.putString("login", loginGenerator.generate(
								props.getString("firstName"), props.getString("lastName")))
						.putString("activationCode", activationCodeGenerator.generate())
						.putString("externalId", idGenerator.generate());
				statementsBuilder.add(query, params.copy().putObject("props", props));
			}

		});
		neo4j.executeTransaction(statementsBuilder.build(), null, true, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> res) {
				message.reply(res.body());
			}
		});
	}

	private void csvClassRelative(final Message<JsonObject> message) {
		String classId = message.body().getString("classId");
		if (classId == null || classId.trim().isEmpty()) {
			sendError(message, "Invalid classId.");
			return;
		}
		String csv = message.body().getString("csv");
		if (csv == null || csv.trim().isEmpty()) {
			sendError(message, "Missing csv.");
			return;
		}

		String charset = detectCharset(csv);
		CSV csvParser = CSV
				.ignoreLeadingWhiteSpace()
				.separator(';')
				.skipLines(1)
				.charset(charset)
				.create();
		csv = csv.split(";;;;;;;;;;;;;;;;;;;;")[0];
		final String [] header = new String[] { "title", "surname", "lastName", "firstName",
				"address", "zipCode", "city", "country", "email", "homePhone", "workPhone",
				"#skip#", "mobile" };

		final StatementsBuilder statementsBuilder = new StatementsBuilder();
		final JsonObject params = new JsonObject().putString("classId", classId);
		final String query =
				"MATCH (c:`Class` { id : {classId}}) " +
				"<-[:DEPENDS]-(csg:ClassRelativeGroup)-[:DEPENDS]->(ssg:SchoolRelativeGroup) " +
				"CREATE csg<-[:APPARTIENT]-(u:Relative:User:Visible {props}), " +
				"ssg<-[:APPARTIENT]-u " +
				"WITH u, c " +
				"MATCH (student:Student)-[:APPARTIENT]->(c) " +
				"WHERE student.login =~ {childrenLoginRegex} " +
				"CREATE student-[:EN_RELATION_AVEC]->u " +
				"RETURN DISTINCT u.id as id";
		csvParser.readAndClose(new StringReader(csv), new CSVReadProc() {

			@Override
			public void procRow(int rowIdx, String... values) {
				int i = 0;
				JsonObject props = new JsonObject();
				while (i < header.length) {
					if ("lastName".equals(header[i])) {
						if (values[i] != null && !values[i].trim().isEmpty()) {
							props.putString(header[i], values[i].trim());
						} else {
							sendError(message, "invalid.lastName " + (rowIdx + 2));
							return;
						}
					} else if ("firstName".equals(header[i])) {
						if (values[i] != null && !values[i].trim().isEmpty()) {
							props.putString(header[i], values[i].trim());
						} else {
							sendError(message, "invalid.firstName " + (rowIdx + 2));
							return;
						}
					} else if (!"#skip#".equals(header[i])) {
						props.putString(header[i], values[i]);
					}
					i++;
				}
				StringBuilder sb = new StringBuilder();
				for (int j = i; j < values.length; j += 4) {
					String login = (removeAccents(values[j+2]).replaceAll("\\s+", "-").toLowerCase()
							+ "." + removeAccents(values[j+1]).replaceAll("\\s+", "-").toLowerCase())
							.replaceAll("'", "");
					sb.append("|^").append(login).append("\\d*$");
				}
				props.putString("id", UUID.randomUUID().toString())
						.putString("displayName", displayNameGenerator.generate(
								props.getString("firstName"), props.getString("lastName")))
						.putString("login", loginGenerator.generate(
								props.getString("firstName"), props.getString("lastName")))
						.putString("activationCode", activationCodeGenerator.generate())
						.putString("externalId", idGenerator.generate());
				JsonObject p =  params.copy().putObject("props", props)
						.putString("childrenLoginRegex", sb.substring(1));
				statementsBuilder.add(query, p);
			}

		});
		neo4j.executeTransaction(statementsBuilder.build(), null, true, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> res) {
				message.reply(res.body());
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

	private static String removeAccents(String str) {
		return Normalizer.normalize(str, Normalizer.Form.NFD)
				.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
	}

}
