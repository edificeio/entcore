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

package org.entcore.feeder.utils;

import fr.wseduc.mongodb.MongoDb;
import fr.wseduc.webutils.I18n;
import org.entcore.common.email.EmailFactory;
import org.entcore.common.http.request.JsonHttpServerRequest;
import org.entcore.feeder.exceptions.TransactionException;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class Report {

	public static final Logger log = LoggerFactory.getLogger(Report.class);
	public static final String FILES = "files";
	public static final String PROFILES = "profiles";
	public final JsonObject result;
	private final I18n i18n = I18n.getInstance();
	public final String acceptLanguage;
	private long endTime;
	private long startTime;
	private String source;
	private Set<Object> loadedFiles = new HashSet<>();
	public enum State { NEW, UPDATED, DELETED }

	public Report(String acceptLanguage) {
		this.acceptLanguage = acceptLanguage;
		final JsonObject errors = new JsonObject();
		final JsonObject files = new JsonObject();
		JsonObject ignored = new JsonObject();
		result = new JsonObject().put("errors", errors).put("files", files).put("ignored", ignored);
	}

	public Report addError(String error) {
		addErrorWithParams(error);
		return this;
	}

	public void addError(String file, String error) {
		addErrorByFile(file, error);
	}

	public void addErrorWithParams(String key, String... errors) {
		addErrorByFile("global", key, errors);
	}

	public void addFailedUser(String filename, String key, JsonObject props, String... errors) {
		final String file = "error." + filename;
		JsonArray f = result.getJsonObject("errors").getJsonArray(file);
		if (f == null) {
			f = new JsonArray();
			result.getJsonObject("errors").put(file, f);
		}
		String error = i18n.translate(key, I18n.DEFAULT_DOMAIN, acceptLanguage, errors);
		props.put("error", error);
		f.add(props);
		log.error(error + " :\n" + Arrays.asList(props));
	}

	public void addErrorByFile(String filename, String key, String... errors) {
		final String file = "error." + filename;
		JsonArray f = result.getJsonObject("errors").getJsonArray(file);
		if (f == null) {
			f = new JsonArray();
			result.getJsonObject("errors").put(file, f);
		}
		String error = i18n.translate(key, I18n.DEFAULT_DOMAIN, acceptLanguage, errors);
		f.add(error);
		log.error(error);
	}

	public void addSoftErrorByFile(String file, String key, String... errors) {
		JsonObject softErrors = result.getJsonObject("softErrors");
		if (softErrors == null) {
			softErrors = new JsonObject();
			result.put("softErrors", softErrors);
		}
		JsonArray f = softErrors.getJsonArray(file);
		if (f == null) {
			f = new JsonArray();
			softErrors.put(file, f);
		}
		String error = i18n.translate(key, I18n.DEFAULT_DOMAIN, acceptLanguage, errors);
		f.add(error);
		log.error(error);
	}

	public void addUser(String file, JsonObject props) {
		JsonArray f = result.getJsonObject("files").getJsonArray(file);
		if (f == null) {
			f = new JsonArray();
			result.getJsonObject("files").put(file, f);
		}
		f.add(props);
	}

	public void addProfile(String profile) {
		JsonArray f = result.getJsonArray(PROFILES);
		if (f == null) {
			f = new JsonArray();
			result.put(PROFILES, f);
		}
		f.add(profile);
	}

	public void addIgnored(String file, String reason, JsonObject object) {
		JsonArray f = result.getJsonObject("ignored").getJsonArray(file);
		if (f == null) {
			f = new JsonArray();
			result.getJsonObject("ignored").put(file, f);
		}
		f.add(new JsonObject().put("reason", reason).put("object", object));
	}

	public String translate(String key, String... params) {
		return i18n.translate(key, I18n.DEFAULT_DOMAIN, acceptLanguage, params);
	}

	public JsonObject getResult() {
		return result;
	}

	public void setUsersExternalId(JsonArray usersExternalIds) {
		result.put("usersExternalIds", usersExternalIds);
	}

	public JsonArray getUsersExternalId() {
		final JsonArray res = new JsonArray();
		for (String f : result.getJsonObject("files").fieldNames()) {
			JsonArray a = result.getJsonObject("files").getJsonArray(f);
			if (a != null) {
				for (Object o : a) {
					if (!(o instanceof JsonObject)) continue;
					final String externalId = ((JsonObject) o).getString("externalId");
					if (externalId != null) {
						res.add(externalId);
					}
				}
			}
		}
		return res;
	}

	public boolean containsErrors() {
		return result.getJsonObject("errors", new JsonObject()).size() > 0;
	}

	public void persist(Handler<Message<JsonObject>> handler) {
		cleanKeys();
		MongoDb.getInstance().save("imports", this.getResult(), handler);
	}

	protected void cleanKeys() {}

	public void setEndTime(long endTime) {
		this.endTime = endTime;
	}

	public void setStartTime(long startTime) {
		this.startTime = startTime;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public void loadedFile(String file) {
		loadedFiles.add(file);
	}

	public void countDiff(final Handler<Void> handler) {
		try {
			TransactionHelper tx = TransactionManager.getTransaction();
			JsonObject params = new JsonObject()
					.put("source", source)
					.put("start", startTime).put("end", endTime)
					.put("startTime", new DateTime(startTime).toString())
					.put("endTime", new DateTime(endTime).toString());
			tx.add(
					"MATCH (u:User {source:{source}}) " +
					"WHERE HAS(u.created) AND u.created >= {startTime} AND u.created < {endTime} " +
					"RETURN count(*) as createdCount", params);
			tx.add(
					"MATCH (u:User {source:{source}}) " +
					"WHERE HAS(u.modified) AND u.modified >= {startTime} AND u.modified < {endTime} " +
					"RETURN count(*) as modifiedCount", params);
			tx.add(
					"MATCH (u:User {source:{source}}) " +
					"WHERE HAS(u.disappearanceDate) AND u.disappearanceDate >= {start} AND u.disappearanceDate < {end} " +
					"RETURN count(*) as disappearanceCount", params);
			tx.commit(new Handler<Message<JsonObject>>() {
				@Override
				public void handle(Message<JsonObject> event) {
					JsonArray results = event.body().getJsonArray("results");
					if ("ok".equals(event.body().getString("status")) && results != null && results.size() == 3) {
						try {
							int created = results.getJsonArray(0).getJsonObject(0).getInteger("createdCount");
							int modified = results.getJsonArray(1).getJsonObject(0).getInteger("modifiedCount");
							int disappearance = results.getJsonArray(2).getJsonObject(0).getInteger("disappearanceCount");
							result.put("userCount", new JsonObject()
									.put("created", created)
									.put("modified", (modified - created))
									.put("disappearance", disappearance)
							);
							result.put("source", source);
							result.put("startTime", new DateTime(startTime).toString());
							result.put("endTime", new DateTime(endTime).toString());
							result.put("loadedFiles", new JsonArray(new ArrayList<>(loadedFiles)));
//							persist(new Handler<Message<JsonObject>>() {
//								@Override
//								public void handle(Message<JsonObject> event) {
//									if (!"ok".equals(event.body().getString("status"))) {
//										log.error("Error persist report : " + event.body().getString("message"));
//									}
//								}
//							});
						} catch (RuntimeException e) {
							log.error("Error parsing count diff response.", e);
						}
					} else {
						log.error("Error in count diff transaction.");
					}
					if (handler != null) {
						handler.handle(null);
					}
				}
			});
		} catch (TransactionException e) {
			log.error("Exception in count diff transaction.", e);
			if (handler != null) {
				handler.handle(null);
			}
		}
	}

	public void emailReport(final Vertx vertx, final JsonObject config) {
		final JsonObject sendReport = config.getJsonObject("sendReport");
		if (sendReport == null || sendReport.getJsonArray("to") == null || sendReport.getJsonArray("to").size() == 0 ||
				sendReport.getJsonArray("sources") == null || !sendReport.getJsonArray("sources").contains(source) ) {
			return;
		}

		final JsonObject reqParams = new JsonObject()
				.put("headers", new JsonObject().put("Accept-Language", acceptLanguage));
		EmailFactory emailFactory = new EmailFactory(vertx, config);
		emailFactory.getSender().sendEmail(
				new JsonHttpServerRequest(reqParams),
				sendReport.getJsonArray("to").getList(),
				sendReport.getJsonArray("cc") != null ? sendReport.getJsonArray("cc").getList() : null,
				sendReport.getJsonArray("bcc") != null ? sendReport.getJsonArray("bcc").getList() : null,
				sendReport.getString("project", "") + i18n.translate("import.report", I18n.DEFAULT_DOMAIN, acceptLanguage) +
						" - " + DateTime.now().toString(DateTimeFormat.forPattern("yyyy-MM-dd")),
				"email/report.html",
				result,
				false,
				ar -> {
					if (ar.failed()) {
						log.error("Error sending report email.", ar.cause());
					}
				}
		);
	}


}
