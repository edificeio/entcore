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
import fr.wseduc.webutils.http.Renders;
import org.entcore.common.email.EmailFactory;
import org.entcore.common.http.request.JsonHttpServerRequest;
import org.entcore.common.http.response.JsonHttpResponse;
import org.entcore.feeder.exceptions.TransactionException;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.VoidHandler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.logging.impl.LoggerFactory;
import org.vertx.java.platform.Container;

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
		result = new JsonObject().putObject("errors", errors).putObject("files", files).putObject("ignored", ignored);
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
		JsonArray f = result.getObject("errors").getArray(file);
		if (f == null) {
			f = new JsonArray();
			result.getObject("errors").putArray(file, f);
		}
		String error = i18n.translate(key, I18n.DEFAULT_DOMAIN, acceptLanguage, errors);
		props.putString("error", error);
		f.addObject(props);
		log.error(error + " :\n" + Arrays.asList(props));
	}

	public void addErrorByFile(String filename, String key, String... errors) {
		final String file = "error." + filename;
		JsonArray f = result.getObject("errors").getArray(file);
		if (f == null) {
			f = new JsonArray();
			result.getObject("errors").putArray(file, f);
		}
		String error = i18n.translate(key, I18n.DEFAULT_DOMAIN, acceptLanguage, errors);
		f.addString(error);
		log.error(error);
	}

	public void addSoftErrorByFile(String file, String key, String... errors) {
		JsonObject softErrors = result.getObject("softErrors");
		if (softErrors == null) {
			softErrors = new JsonObject();
			result.putObject("softErrors", softErrors);
		}
		JsonArray f = softErrors.getArray(file);
		if (f == null) {
			f = new JsonArray();
			softErrors.putArray(file, f);
		}
		String error = i18n.translate(key, I18n.DEFAULT_DOMAIN, acceptLanguage, errors);
		f.addString(error);
		log.error(error);
	}

	public void addUser(String file, JsonObject props) {
		JsonArray f = result.getObject("files").getArray(file);
		if (f == null) {
			f = new JsonArray();
			result.getObject("files").putArray(file, f);
		}
		f.addObject(props);
	}

	public void addProfile(String profile) {
		JsonArray f = result.getArray(PROFILES);
		if (f == null) {
			f = new JsonArray();
			result.putArray(PROFILES, f);
		}
		f.addString(profile);
	}

	public void addIgnored(String file, String reason, JsonObject object) {
		JsonArray f = result.getObject("ignored").getArray(file);
		if (f == null) {
			f = new JsonArray();
			result.getObject("ignored").putArray(file, f);
		}
		f.addObject(new JsonObject().putString("reason", reason).putObject("object", object));
	}

	public String translate(String key, String... params) {
		return i18n.translate(key, I18n.DEFAULT_DOMAIN, acceptLanguage, params);
	}

	public JsonObject getResult() {
		return result;
	}

	public void setUsersExternalId(JsonArray usersExternalIds) {
		result.putArray("usersExternalIds", usersExternalIds);
	}

	public JsonArray getUsersExternalId() {
		final JsonArray res = new JsonArray();
		for (String f : result.getObject("files").getFieldNames()) {
			JsonArray a = result.getObject("files").getArray(f);
			if (a != null) {
				for (Object o : a) {
					if (!(o instanceof JsonObject)) continue;
					final String externalId = ((JsonObject) o).getString("externalId");
					if (externalId != null) {
						res.addString(externalId);
					}
				}
			}
		}
		return res;
	}

	public boolean containsErrors() {
		return result.getObject("errors", new JsonObject()).size() > 0;
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

	public void countDiff(final VoidHandler handler) {
		try {
			TransactionHelper tx = TransactionManager.getTransaction();
			JsonObject params = new JsonObject()
					.putString("source", source)
					.putNumber("start", startTime).putNumber("end", endTime)
					.putString("startTime", new DateTime(startTime).toString())
					.putString("endTime", new DateTime(endTime).toString());
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
					JsonArray results = event.body().getArray("results");
					if ("ok".equals(event.body().getString("status")) && results != null && results.size() == 3) {
						try {
							int created = results.<JsonArray>get(0).<JsonObject>get(0).getInteger("createdCount");
							int modified = results.<JsonArray>get(1).<JsonObject>get(0).getInteger("modifiedCount");
							int disappearance = results.<JsonArray>get(2).<JsonObject>get(0).getInteger("disappearanceCount");
							result.putObject("userCount", new JsonObject()
									.putNumber("created", created)
									.putNumber("modified", (modified - created))
									.putNumber("disappearance", disappearance)
							);
							result.putString("source", source);
							result.putString("startTime", new DateTime(startTime).toString());
							result.putString("endTime", new DateTime(endTime).toString());
							result.putArray("loadedFiles", new JsonArray(loadedFiles.toArray()));
							persist(new Handler<Message<JsonObject>>() {
								@Override
								public void handle(Message<JsonObject> event) {
									if (!"ok".equals(event.body().getString("status"))) {
										log.error("Error persist report : " + event.body().getString("message"));
									}
								}
							});
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

	public void emailReport(final Vertx vertx, final Container container) {
		final JsonObject sendReport = container.config().getObject("sendReport");
		if (sendReport == null || sendReport.getArray("to") == null || sendReport.getArray("to").size() == 0 ||
				sendReport.getArray("sources") == null || !sendReport.getArray("sources").contains(source) ) {
			return;
		}

		final JsonObject reqParams = new JsonObject()
				.putObject("headers", new JsonObject().putString("Accept-Language", acceptLanguage));
		EmailFactory emailFactory = new EmailFactory(vertx, container, container.config());
		emailFactory.getSender().sendEmail(
				new JsonHttpServerRequest(reqParams),
				sendReport.getArray("to").toList(),
				sendReport.getArray("cc") != null ? sendReport.getArray("cc").toList() : null,
				sendReport.getArray("bcc") != null ? sendReport.getArray("bcc").toList() : null,
				sendReport.getString("project", "") + i18n.translate("import.report", I18n.DEFAULT_DOMAIN, acceptLanguage) +
						" - " + DateTime.now().toString(DateTimeFormat.forPattern("yyyy-MM-dd")),
				"email/report.html",
				result,
				false,
				new Handler<Message<JsonObject>>() {
					@Override
					public void handle(Message<JsonObject> event) {
						if (!"ok".equals(event.body().getString("status"))) {
							log.error("Error sending report email : " + event.body().getString("message"));
						}
					}
				}
		);
	}


}
