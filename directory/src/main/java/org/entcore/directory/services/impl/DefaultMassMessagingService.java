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

import com.opencsv.CSVReader;
import fr.wseduc.webutils.Either;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.directory.pojo.ImportInfos;
import org.entcore.directory.services.MassMessagingService;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;
public class DefaultMassMessagingService implements MassMessagingService {

	private static final Logger log = LoggerFactory.getLogger(DefaultMassMessagingService.class);

	private static final String CONVERSATION_ADDRESS = "org.entcore.conversation";
	private final EventBus eb;
	private final Vertx vertx;
	private final Neo4j neo4j = Neo4j.getInstance();


	public DefaultMassMessagingService(Vertx vertx, EventBus eb) {
		this.eb = eb;
		this.vertx = vertx;
	}

	@Override
	public void csvColumnsMapping(ImportInfos importInfos, final Handler<Either<JsonObject, JsonObject>> handler) {

			String path = importInfos.getPath();
		vertx.fileSystem().readDir(path, new Handler<AsyncResult<List<String>>>() {
			@Override
			public void handle(AsyncResult<List<String>> event) {
				if (event.succeeded() && event.result().size() == 1) {
					final String path = event.result().get(0);
					vertx.fileSystem().readDir(path, new Handler<AsyncResult<List<String>>>() {
						@Override
						public void handle(AsyncResult<List<String>> event) {
							final List<String> importFiles = event.result();


							List<List<String>> records = new ArrayList<List<String>>();
							try (CSVReader csvReader = new CSVReader(new FileReader(importFiles.get(0)));) {
								String[] values = null;
								while ((values = csvReader.readNext()) != null) {
									List<String> row = Arrays.asList(values);
									if (!row.isEmpty() && row.stream().anyMatch(value -> value != null && !value.trim().isEmpty())) {
										records.add(Arrays.asList(values));
									}
								}
							} catch (FileNotFoundException e) {
								handler.handle(new Either.Left<>(new JsonObject().put("error", "File not found")));
							} catch (IOException e) {
								handler.handle(new Either.Left<>(new JsonObject().put("error", "io exception")));
							}
							handler.handle(new Either.Right<>(new JsonObject().put("asmRecords", records)));
						}
					});
				} else {
					handler.handle(new Either.Left<>(new JsonObject().put("error", "Failed reading from Path")));

				}
			}
		});

	}

	@Override
	public void getSenderDisplayName(HttpServerRequest request, JsonObject messageConfig, Handler<Either<JsonObject, String>> handler) {

		String senderId = messageConfig.getString("sender-id");
		String query = "MATCH (v:Visible) WHERE v.id = {id} RETURN v.displayName AS displayName";
		JsonObject param = new JsonObject().put("id", senderId);

		neo4j.execute(query, param, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {

				JsonArray result = event.body().getJsonArray("result");
				if (result.getJsonObject(0) != null) {
					JsonObject name = result.getJsonObject(0);
					if (name != null) {
						String displayName = name.getString("displayName");
						handler.handle(new Either.Right<>(displayName));
					}
				} else {
					handler.handle(new Either.Left<>(new JsonObject().put("error", "Query execution failed")));
				}
			}
		});
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

				//To remove Byte Order Mark in order to make comparison more accurate
				headersArray[0] = headersArray[0].replaceAll("[\\uFEFF-\\uFFFF]", "").trim();

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

				List<String> loginsFailed = rows.stream()
											.map(row -> ((JsonObject)row).getJsonObject("login").getString("value"))
											.collect(Collectors.toList());

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
										loginsFailed.remove(loginKey);
									} else {
										failureCounter.getAndIncrement();
									}
								});
							log.debug("number of logins ignored:" + failureCounter);
							JsonObject result = new JsonObject();
							result.put("remainingLogins", new JsonArray(loginsFailed));
							result.put("loginsSucceeded", res.size());
							handler.handle(new Either.Right<>(result));
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
		message.put("userId", senderId);// the FROM field of the email
		message.put("username", loginUsername);
		message.put("message", mail);

		eb.request(CONVERSATION_ADDRESS, message, handlerToAsyncHandler(event -> {
      if ("ok".equals(event.body().getString("status"))) {
        JsonObject resultSuccess = new JsonObject();
        resultSuccess.put("message", "message sent successfully");

        handler.handle(new Either.Right<>(resultSuccess));
      } else {
        handler.handle(new Either.Left<>("failed to send messages"));
      }
    }));
	}

}