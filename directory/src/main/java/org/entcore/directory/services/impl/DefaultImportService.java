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

package org.entcore.directory.services.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.wseduc.mongodb.MongoDb;
import fr.wseduc.webutils.Either;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.http.HttpServerRequest;
import org.entcore.common.mongodb.MongoDbResult;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.user.UserInfos;
import org.entcore.directory.Directory;
import org.entcore.directory.pojo.ImportInfos;
import org.entcore.directory.services.ImportService;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;
import static fr.wseduc.webutils.Utils.defaultValidationParamsNull;
import static fr.wseduc.webutils.Utils.isNotEmpty;
import static org.entcore.common.user.DefaultFunctions.ADMIN_LOCAL;
import static org.entcore.common.user.DefaultFunctions.SUPER_ADMIN;

public class DefaultImportService implements ImportService {

	private static final Logger log = LoggerFactory.getLogger(DefaultImportService.class);
	private static final long TIMEOUT = 10 * 60 * 1000l;

	private static final String CONVERSATION_ADDRESS = "org.entcore.conversation";
	private final EventBus eb;
	private final Vertx vertx;
	private final Neo4j neo4j = Neo4j.getInstance();
	private static final ObjectMapper mapper = new ObjectMapper();
	private final MongoDb mongo = MongoDb.getInstance();
	private static final String IMPORTS = "imports";

	private String tempDemoHtml = "<html lang=\"fr\">\n" +
			"<head>\n" +
			"    <meta charset=\"UTF-8\">\n" +
			"    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
			"    <title>Bienvenue à notre école</title>\n" +
			"    <style>\n" +
			"        body {\n" +
			"            font-family: Arial, sans-serif;\n" +
			"            background-color: #f4f4f9;\n" +
			"            margin: 0;\n" +
			"            padding: 0;\n" +
			"            display: flex;\n" +
			"            justify-content: center;\n" +
			"            align-items: center;\n" +
			"            height: 100vh;\n" +
			"        }\n" +
			"        .container {\n" +
			"            background-color: #ffffff;\n" +
			"            padding: 20px;\n" +
			"            border-radius: 10px;\n" +
			"            box-shadow: 0 4px 8px rgba(0,0,0,0.1);\n" +
			"            max-width: 500px;\n" +
			"            text-align: center;\n" +
			"        }\n" +
			"        h1 {\n" +
			"            color: #333333;\n" +
			"        }\n" +
			"        p {\n" +
			"            color: #666666;\n" +
			"        }\n" +
			"        .password {\n" +
			"            font-weight: bold;\n" +
			"            color: #e74c3c;\n" +
			"            background-color: #f9e6e6;\n" +
			"            padding: 10px;\n" +
			"            border-radius: 5px;\n" +
			"            display: inline-block;\n" +
			"            margin-top: 10px;\n" +
			"        }\n" +
			"        .footer {\n" +
			"            margin-top: 20px;\n" +
			"            font-size: 0.9em;\n" +
			"            color: #aaaaaa;\n" +
			"        }\n" +
			"    </style>\n" +
			"</head>\n" +
			"<body>\n" +
			"    <div class=\"container\">\n" +
			"        <h1>Bienvenue, [Nom de l'étudiant] !</h1>\n" +
			"        <p>Nous sommes ravis de vous accueillir dans notre communauté scolaire. Voici votre mot de passe temporaire pour commencer :</p>\n" +
			"        <div class=\"password\">[Mot de passe]</div>\n" +
			"        <p>Veuillez changer votre mot de passe après votre première connexion.</p>\n" +
			"        <div class=\"footer\">\n" +
			"            &copy; 2024 Notre école. Tous droits réservés.\n" +
			"        </div>\n" +
			"    </div>\n" +
			"</body>\n" +
			"</html>\n";

	public DefaultImportService(Vertx vertx, EventBus eb) {
		this.eb = eb;
		this.vertx = vertx;
	}

	@Override
	public void validate(ImportInfos importInfos, UserInfos user, final Handler<Either<JsonObject, JsonObject>> handler) {
		try {
			final AdmlValidate admlValidate = new AdmlValidate(user, handler).invoke();
			if (admlValidate.is()) return;
			final JsonObject action = new JsonObject(mapper.writeValueAsString(importInfos))
					.put("action", "validate")
					.put("adml-structures", admlValidate.getAdmlStructures());
			eb.send(Directory.FEEDER, action, new DeliveryOptions().setSendTimeout(TIMEOUT), handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
				@Override
				public void handle(Message<JsonObject> res) {
					if ("ok".equals(res.body().getString("status"))) {
						JsonObject r = res.body().getJsonObject("result", new JsonObject());
						if (r.getJsonObject("errors", new JsonObject()).size() > 0) {
							handler.handle(new Either.Left<JsonObject, JsonObject>(r.getJsonObject("errors")));
						} else {
							JsonObject f = r.getJsonObject("files");
							if(r.getJsonObject("softErrors") != null) {
								f.put("softErrors", r.getJsonObject("softErrors"));
							}
							if (isNotEmpty(r.getString("_id"))) {
								f.put("importId", r.getString("_id"));
							}
							handler.handle(new Either.Right<JsonObject, JsonObject>(f));
						}
					} else {
						handler.handle(new Either.Left<JsonObject, JsonObject>(
								new JsonObject().put("global",
								new fr.wseduc.webutils.collections.JsonArray().add(res.body().getString("message", "")))));
					}
				}
			}));
		} catch (JsonProcessingException e) {
			handler.handle(new Either.Left<JsonObject, JsonObject>(new JsonObject()
					.put("global", new fr.wseduc.webutils.collections.JsonArray().add("unexpected.error"))));
			log.error(e.getMessage(), e);
		}
	}

	@Override
	public void validate(String importId, UserInfos user, final Handler<Either<JsonObject, JsonObject>> handler) {
		final AdmlValidate admlValidate = new AdmlValidate(user, handler).invoke();
		if (admlValidate.is()) return;
		final JsonObject action = new JsonObject()
				.put("action", "validateWithId")
				.put("id", importId)
				.put("adml-structures", admlValidate.getAdmlStructures());
		sendCommand(handler, action);
	}

	@Override
	public void doImport(ImportInfos importInfos, final Handler<Either<JsonObject, JsonObject>> handler) {
		try {
			JsonObject action = new JsonObject(mapper.writeValueAsString(importInfos))
					.put("action", "import");
			eb.send("entcore.feeder", action, new DeliveryOptions().setSendTimeout(TIMEOUT), handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
				@Override
				public void handle(Message<JsonObject> event) {
					if ("ok".equals(event.body().getString("status"))) {
						JsonObject r = event.body().getJsonObject("result", new JsonObject());
						if (r.getJsonObject("errors", new JsonObject()).size() > 0) {
							handler.handle(new Either.Left<JsonObject, JsonObject>(r.getJsonObject("errors")));
						} else {
							handler.handle(new Either.Right<JsonObject, JsonObject>(r.getJsonObject("ignored")));
						}
					} else {
						handler.handle(new Either.Left<JsonObject, JsonObject>(new JsonObject().put("global",
								new fr.wseduc.webutils.collections.JsonArray().add(event.body().getString("message", "")))));
					}
			}
			}));
		} catch (JsonProcessingException e) {
			handler.handle(new Either.Left<JsonObject, JsonObject>(new JsonObject()
					.put("global", new fr.wseduc.webutils.collections.JsonArray().add("unexpected.error"))));
			log.error(e.getMessage(), e);
		}
	}

	@Override
	public void doImport(String importId, final Handler<Either<JsonObject, JsonObject>> handler) {
		JsonObject action = new JsonObject().put("action", "importWithId").put("id", importId);
		sendCommand(handler, action);
	}

	@Override
	public void columnsMapping(ImportInfos importInfos, final Handler<Either<JsonObject, JsonObject>> handler) {
		try {
			JsonObject action = new JsonObject(mapper.writeValueAsString(importInfos))
					.put("action", "columnsMapping");
			eb.send("entcore.feeder", action, handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
				@Override
				public void handle(Message<JsonObject> event) {
					if ("ok".equals(event.body().getString("status"))) {
						JsonObject r = event.body();
						r.remove("status");
						if (r.getJsonObject("errors", new JsonObject()).size() > 0) {
							handler.handle(new Either.Left<JsonObject, JsonObject>(r));
						} else {
							handler.handle(new Either.Right<JsonObject, JsonObject>(r));
						}
					} else {
						handler.handle(new Either.Left<JsonObject, JsonObject>(new JsonObject().put("global",
								new JsonArray().add(event.body().getString("message", "")))));
					}
				}
			}));
		} catch (JsonProcessingException e) {
			handler.handle(new Either.Left<JsonObject, JsonObject>(new JsonObject()
					.put("global", new JsonArray().add("unexpected.error"))));
			log.error(e.getMessage(), e);
		}
	}

	@Override
	public void asmColumnsMapping(ImportInfos importInfos, final Handler<Either<JsonObject, JsonObject>> handler) {
		try {
			JsonObject action = new JsonObject(mapper.writeValueAsString(importInfos))
					.put("action", "asmColumnsMapping");
			eb.send("entcore.feeder", action, handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
				@Override
				public void handle(Message<JsonObject> event) {

					if ("ok".equals(event.body().getString("status"))) {
						JsonObject r = event.body();
						r.remove("status");
						if (r.getJsonObject("errors", new JsonObject()).size() > 0) {
							handler.handle(new Either.Left<JsonObject, JsonObject>(r));
						} else {
							handler.handle(new Either.Right<JsonObject, JsonObject>(r));
						}
					} else {
						handler.handle(new Either.Left<JsonObject, JsonObject>(new JsonObject().put("global",
								new JsonArray().add(event.body().getString("message", "")))));
					}
				}
			}));
		} catch (JsonProcessingException e) {
			handler.handle(new Either.Left<JsonObject, JsonObject>(new JsonObject()
					.put("global", new JsonArray().add("unexpected.error"))));
			log.error(e.getMessage(), e);
		}
	}

	@Override
	public void validateMassMessaging(HttpServerRequest request, Handler<Either<JsonObject, JsonArray>> handler) {
		log.info("initiating Validate and Populate");
		request.bodyHandler(new Handler<Buffer>() {
			@Override
			public void handle(Buffer buffer) {

				JsonObject body = buffer.toJsonObject();
				JsonArray data = body.getJsonArray("mappings");
				JsonArray requiredFields = body.getJsonArray("required");
				String[] headersArray = data.getJsonArray(0).getString(0).split(";");

				ArrayList<String> requiredHeaders = requiredFields.stream().map(header -> header.toString().toLowerCase())
						.collect(Collectors.toCollection(ArrayList::new));

				ArrayList allHeaders = Arrays.stream(headersArray)
						.map(String::trim)
						.map(String::toLowerCase)
						.collect(Collectors.toCollection(ArrayList::new));

				if(!allHeaders.containsAll(requiredHeaders)){
					JsonObject message = new JsonObject();
					message.put("message", "Required header not found. Required header: " + requiredHeaders + " Given: " + allHeaders );
					handler.handle(new Either.Left<>(message));
				} else {
					JsonArray head = new JsonArray(allHeaders);
					handler.handle(new Either.Right<>(head));
				}
			}
		});
	}

	@Override
	public void publishMassMessages(HttpServerRequest request, JsonObject messageConfig, Handler<Either<String,JsonObject>> handler) {
		log.info("publishing messages");

		request.bodyHandler(new Handler<Buffer>() {
			@Override
			public void handle(Buffer buffer) {
				JsonObject body = buffer.toJsonObject();
				JsonArray rows = body.getJsonArray("rows");
				JsonArray headers = body.getJsonArray("headers");
				String template = body.getString("template");
				String messageSubject = body.getString("messageSubject");
				JsonArray loginList = new JsonArray();
				String senderId = messageConfig.getString("sender-id");

				rows.stream().map(row -> ((JsonObject)row).getJsonObject("login").getString("value")).forEach(loginList::add);

				String query = "MATCH (u:User)-[:IN]->(:ProfileGroup)-[:DEPENDS]->(s:Structure) " +
						"WHERE u.login IN {logins} "+
						"RETURN DISTINCT u.id as id, u.login as login";

				JsonObject params = new JsonObject().put("logins", loginList);
				neo4j.execute(query, params, new Handler<Message<JsonObject>>(){
					@Override
					public void handle(Message<JsonObject> event) {
						Map<String, JsonObject> rowMap = new HashMap<>();

						//Getting from map is O[1] better than doing a nested loop
						rows.forEach(row -> {
							JsonObject obj = (JsonObject) row;
							String key = ((JsonObject)obj.getValue("login")).getString("value");
							if(!key.isEmpty()) {
								rowMap.put(key, obj);
							}
						});
						JsonArray res = event.body().getJsonArray("result");
						AtomicInteger failureCounter = new AtomicInteger();
						if ("ok".equals(event.body().getString("status")) && res != null) {
							res.stream().forEach(userid -> {
								String loginKey = ((JsonObject) userid).getString("login");
									if(rowMap.containsKey((loginKey))) {
										JsonObject newValue = rowMap.get(loginKey).put("userId", ((JsonObject) userid).getValue("id"));
										rowMap.put(loginKey, newValue);
										sendMassMessaging(rowMap.get(loginKey), headers, messageSubject, senderId,template, handler);
									} else {
										failureCounter.getAndIncrement();
									}
								});
							log.debug("number of logins ignored:" + failureCounter);
						} else {
							handler.handle(new Either.Left<>("failed to send all messages"));
						}
					}
				});
			}
		});
	}

	private void sendMassMessaging(JsonObject row, JsonArray headers, String messageSubject, String senderId,String template,Handler<Either<String, JsonObject>> handler) {
		JsonObject mail = new JsonObject();

		for (Object headerObject : headers) {
			JsonObject header = (JsonObject) headerObject;
			String token = header.getString("token");
			String key = header.getString("field");
			String field = row.getJsonObject(key).getString("value");

			Pattern pattern = Pattern.compile(Pattern.quote(token), Pattern.CASE_INSENSITIVE);

			// Replace all occurrences of token with field in template
			template = pattern.matcher(template).replaceAll(field);
		}
		String loginUsername = row.getJsonObject("login").getString("value");

		JsonArray toArray = new JsonArray().add(row.getString("userId"));
		mail.put("to", toArray);
		mail.put("subject", messageSubject);
		mail.put("body", template);

		JsonObject message = new JsonObject();
		message.put("action", "send");
		message.put("userId", senderId);
		message.put("username", loginUsername);
		message.put("message", mail);

		eb.send(CONVERSATION_ADDRESS, message, handlerToAsyncHandler(new io.vertx.core.Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				if ("ok".equals(event.body().getString("status"))) {
					JsonObject resultSuccess = new JsonObject();
					resultSuccess.put("message", "message sent successfully");

					handler.handle(new Either.Right<>(resultSuccess));
				} else {
					handler.handle(new Either.Left<>("failed to send messages"));
				}
			}
		}));
	}

	@Override
	public void classesMapping(ImportInfos importInfos, final Handler<Either<JsonObject, JsonObject>> handler) {
		try {
			JsonObject action = new JsonObject(mapper.writeValueAsString(importInfos))
					.put("action", "classesMapping");
			sendCommand(handler, action);
		} catch (JsonProcessingException e) {
			handler.handle(new Either.Left<JsonObject, JsonObject>(new JsonObject()
					.put("global", new JsonArray().add("unexpected.error"))));
			log.error(e.getMessage(), e);
		}
	}

	@Override
	public void addLine(String importId, String profile, JsonObject line, Handler<Either<String, JsonObject>> handler) {
		final JsonObject query = new JsonObject().put("_id", importId);
		final JsonObject update = new JsonObject().put("$push", new JsonObject().put("files." + profile, line));
		mongo.update(IMPORTS, query, update, MongoDbResult.validActionResultHandler(handler));
	}

	@Override
	public void updateLine(String importId, String profile, JsonObject line, Handler<Either<String, JsonObject>> handler) {
		Integer lineId = line.getInteger("line");
		if (defaultValidationParamsNull(handler, lineId)) return;
		JsonObject item = new JsonObject();
		for (String attr : line.fieldNames()) {
			if ("line".equals(attr)) continue;
			item.put("files." + profile + ".$." + attr, line.getValue(attr));
		}
//		db.imports.update({"_id" : "8ff9a53f-a216-49f2-97cf-7ccc41c6e2b6", "files.Relative.line" : 147}, {$set : {"files.Relative.$.state" : "bla"}})
		final JsonObject query = new JsonObject().put("_id", importId).put("files." + profile + ".line", lineId);
		final JsonObject update = new JsonObject().put("$set", item);
		mongo.update(IMPORTS, query, update, MongoDbResult.validActionResultHandler(handler));
	}

	@Override
	public void deleteLine(String importId, String profile, Integer line, Handler<Either<String, JsonObject>> handler) {
		final JsonObject query = new JsonObject().put("_id", importId).put("files." + profile + ".line", line);
		final JsonObject update = new JsonObject().put("$pull", new JsonObject()
				.put("files." + profile, new JsonObject().put("line", line)));
		mongo.update(IMPORTS, query, update, MongoDbResult.validActionResultHandler(handler));
	}

	public void findById(String importId, Handler<Either<String,JsonObject>> handler) {
		mongo.findOne("imports",
				new JsonObject().put("_id", importId),
				MongoDbResult.validActionResultHandler(handler));
	}

	protected void sendCommand(final Handler<Either<JsonObject, JsonObject>> handler, JsonObject action) {
		eb.send("entcore.feeder", action, new DeliveryOptions().setSendTimeout(600000L), handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				if ("ok".equals(event.body().getString("status"))) {
					JsonObject r = event.body().getJsonObject("result", new JsonObject());
					r.remove("status");
					if (r.getJsonObject("errors", new JsonObject()).size() > 0) {
						handler.handle(new Either.Left<JsonObject, JsonObject>(r));
					} else {
						handler.handle(new Either.Right<JsonObject, JsonObject>(r));
					}
				} else {
					handler.handle(new Either.Left<JsonObject, JsonObject>(new JsonObject().put("global",
							new JsonArray().add(event.body().getString("message", "")))));
				}
			}
		}));
	}

	private class AdmlValidate {
		private boolean myResult;
		private UserInfos user;
		private Handler<Either<JsonObject, JsonObject>> handler;
		private JsonArray admlStructures;

		public AdmlValidate(UserInfos user, Handler<Either<JsonObject, JsonObject>> handler) {
			this.user = user;
			this.handler = handler;
		}

		boolean is() {
			return myResult;
		}

		public JsonArray getAdmlStructures() {
			return admlStructures;
		}

		public AdmlValidate invoke() {
			Map<String, UserInfos.Function> functions = user.getFunctions();
			if (functions == null || functions.isEmpty()) {
				handler.handle(new Either.Left<>(new JsonObject()
						.put("global", new JsonArray().add("not.admin.user"))));
				myResult = true;
				return this;
			}
			if (functions.containsKey(SUPER_ADMIN)) {
				admlStructures = null;
			} else {
				final UserInfos.Function adminLocal = functions.get(ADMIN_LOCAL);
				if (adminLocal != null && adminLocal.getScope() != null) {
					admlStructures = new JsonArray(adminLocal.getScope());
				} else {
					handler.handle(new Either.Left<>(new JsonObject()
							.put("global", new JsonArray().add("not.admin.user"))));
					myResult = true;
					return this;
				}
			}
			myResult = false;
			return this;
		}
	}

}
