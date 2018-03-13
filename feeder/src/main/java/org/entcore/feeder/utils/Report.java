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

package org.entcore.feeder.utils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonValue;
import org.apache.commons.lang3.StringUtils;
import org.entcore.common.email.EmailFactory;
import org.entcore.common.http.request.JsonHttpServerRequest;
import org.entcore.feeder.exceptions.TransactionException;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;

import fr.wseduc.mongodb.MongoDb;
import fr.wseduc.webutils.I18n;
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
import java.util.UUID;

public class Report {

	public static final Logger log = LoggerFactory.getLogger(Report.class);
	public static final String FILES = "files";
	public static final String PROFILES = "profiles";
	private static final String MAPPINGS = "mappings";
	public static final String KEYS_CLEANED = "keysCleaned";
	public final JsonObject result;
	private final I18n i18n = I18n.getInstance();
	public final String acceptLanguage;
	private long endTime;
	private long startTime;
	private Set<String> loadedFiles = new HashSet<>();

	public enum State {
		NEW, UPDATED, DELETED
	}

	public Report(String acceptLanguage) {
		this.acceptLanguage = acceptLanguage;
		final JsonObject errors = new JsonObject();
		final JsonObject files = new JsonObject();
		JsonObject ignored = new JsonObject();
		result = new JsonObject().put("_id", UUID.randomUUID().toString()).put("created", MongoDb.now())
				.put("errors", errors).put(FILES, files).put("ignored", ignored)
				.put("source", getSource());
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
			f = new fr.wseduc.webutils.collections.JsonArray();
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
			f = new fr.wseduc.webutils.collections.JsonArray();
			result.getJsonObject("errors").put(file, f);
		}
		String error = i18n.translate(key, I18n.DEFAULT_DOMAIN, acceptLanguage, errors);
		f.add(error);
		log.error(error);
	}

	public void addSoftErrorByFile(String file, String key, String lineNumber, String... errors) {
    JsonObject softErrors = result.getJsonObject("softErrors");
		if (softErrors == null) {
			softErrors = new JsonObject();
			result.put("softErrors", softErrors);
		}
		JsonArray reasons = softErrors.getJsonArray("reasons");
		if (reasons == null) {
			reasons = new JsonArray();
			softErrors.put("reasons", reasons);
		}
		if (!reasons.contains(key)) {
			reasons.add(key);
		}

		JsonArray fileErrors = softErrors.getJsonArray(file);
		if (fileErrors == null) {
			fileErrors = new JsonArray();
			softErrors.put(file, fileErrors);
		}
		JsonObject error = new JsonObject().copy()
				.put("line",lineNumber)
				.put("reason", key)
				.put("attribute", errors.length > 0 ? errors[0] : "")
				.put("value", errors.length > 1 ? errors[1] : "");

		List<String> errorContext = new ArrayList<>(Arrays.asList(errors)); // Hack to support "add" operation
		errorContext.add(0, lineNumber);
		String translation = i18n.translate(key, I18n.DEFAULT_DOMAIN, acceptLanguage, errorContext.toArray(new String[errorContext.size()]));
		error.put("translation", translation);

		fileErrors.add(error);
		log.error(translation);
		//String cleanKey = key.replace('.','-'); // Mongo don't support '.' characters in document field's name
	}

	public void addUser(String file, JsonObject props) {
		JsonArray f = result.getJsonObject(FILES).getJsonArray(file);
		if (f == null) {
			f = new fr.wseduc.webutils.collections.JsonArray();
			result.getJsonObject(FILES).put(file, f);
		}
		f.add(props);
	}

	public void addProfile(String profile) {
		JsonArray f = result.getJsonArray(PROFILES);
		if (f == null) {
			f = new fr.wseduc.webutils.collections.JsonArray();
			result.put(PROFILES, f);
		}
		f.add(profile);
	}

	public void addIgnored(String file, String reason, JsonObject object) {
		JsonArray f = result.getJsonObject("ignored").getJsonArray(file);
		if (f == null) {
			f = new fr.wseduc.webutils.collections.JsonArray();
			result.getJsonObject("ignored").put(file, f);
		}
		f.add(new JsonObject().put("reason", reason).put("object", object));
	}

	public String translate(String key, String... params) {
		return i18n.translate(key, I18n.DEFAULT_DOMAIN, acceptLanguage, params);
	}

	public JsonObject getResult() {
		return result.copy();
	}

	public void setUsersExternalId(JsonArray usersExternalIds) {
		result.put("usersExternalIds", usersExternalIds);
	}

	public JsonArray getUsersExternalId() {
		final JsonArray res = new fr.wseduc.webutils.collections.JsonArray();
		for (String f : result.getJsonObject(FILES).fieldNames()) {
			JsonArray a = result.getJsonObject(FILES).getJsonArray(f);
			if (a != null) {
				for (Object o : a) {
					if (!(o instanceof JsonObject))
						continue;
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

	public void updateErrors(Handler<Message<JsonObject>> handler) {
		boolean cleaned = updateCleanKeys();
		JsonObject modif = new JsonObject()
				.put("errors", result.getJsonObject("errors"))
				.put("softErrors", result.getJsonObject("softErrors"));
		if (cleaned) {
			modif.put(KEYS_CLEANED, true);
		}
		MongoDb.getInstance().update("imports", new JsonObject().put("_id", result.getString("_id")),
				new JsonObject().put("$set", modif), handler);

	}

	protected void cleanKeys() {}

	public void setEndTime(long endTime) {
		this.endTime = endTime;
	}

	public void setStartTime(long startTime) {
		this.startTime = startTime;
	}

	public void loadedFile(String file) {
		loadedFiles.add(file);
	}

	private JsonObject cloneAndFilterResults(Optional<String> prefixAcademy) {
		JsonObject results = this.result.copy();
		if (prefixAcademy.isPresent()) {
			// filter each ignored object by externalId starting with academy name
			String prefix = prefixAcademy.get();
			JsonObject ignored = results.getJsonObject("ignored");
			Set<String> domains = ignored.fieldNames();
			for (String domain : domains) {
				JsonArray filtered = ignored.getJsonArray(domain, new JsonArray()).stream().filter(ig -> {
					if (ig instanceof JsonObject && ((JsonObject) ig).containsKey("object")) {
						JsonObject object = ((JsonObject) ig).getJsonObject("object");
						String externalId = object.getString("externalId");
						return StringUtils.startsWithIgnoreCase(externalId, prefix);
					} else {
						// keep in list because it is not a concerned object
						return true;
					}
				}).collect(JsonArray::new, JsonArray::add, JsonArray::addAll);//
				ignored.put(domain, filtered);
			}
			// userExternalIds FIltered
			JsonArray usersExternalIdsFiltered = results.getJsonArray("usersExternalIds", new JsonArray()).stream()
					.filter(value -> {
						return (value instanceof String && StringUtils.startsWithIgnoreCase((String) value, prefix));
					}).collect(JsonArray::new, JsonArray::add, JsonArray::addAll);//
			results.put("usersExternalIds", usersExternalIdsFiltered);
		}
		return results;
	}

	private JsonArray cloneAndFilterFiles(Optional<String> academyPrefix) {
		List<String> filtered = null;
		if (academyPrefix.isPresent()) {
			String pattern = academyPrefix.get();
			filtered = loadedFiles.stream().filter(file -> StringUtils.contains(file, "/" + pattern + "/")).sorted()
					.collect(Collectors.toList());
		} else {
			filtered = loadedFiles.stream().sorted().collect(Collectors.toList());
		}
		return new fr.wseduc.webutils.collections.JsonArray(filtered);
	}

	private void countDiff(Optional<String> prefixAcademy, String source, final Handler<JsonObject> handler) {
		try {
			TransactionHelper tx = TransactionManager.getTransaction();
			JsonObject params = new JsonObject().put("source", source).put("start", startTime).put("end", endTime)
					.put("startTime", new DateTime(startTime).toString())
					.put("endTime", new DateTime(endTime).toString());
			if (prefixAcademy.isPresent()) {
				params.put("prefixAcademy", prefixAcademy.get());
			}
			tx.add("MATCH (u:User {source:{source}}) "
					+ "WHERE HAS(u.created) AND u.created >= {startTime} AND u.created < {endTime} "
					+ (prefixAcademy.isPresent() ? " AND u.externalId STARTS WITH {prefixAcademy} " : "")//
					+ "RETURN count(*) as createdCount", params);
			tx.add("MATCH (u:User {source:{source}}) "
					+ "WHERE HAS(u.modified) AND u.modified >= {startTime} AND u.modified < {endTime} "
					+ (prefixAcademy.isPresent() ? " AND u.externalId STARTS WITH {prefixAcademy} " : "")//
					+ "RETURN count(*) as modifiedCount", params);
			tx.add("MATCH (u:User {source:{source}}) "
					+ "WHERE HAS(u.disappearanceDate) AND u.disappearanceDate >= {start} AND u.disappearanceDate < {end} "
					+ (prefixAcademy.isPresent() ? " AND u.externalId STARTS WITH {prefixAcademy} " : "")//
					+ "RETURN count(*) as disappearanceCount", params);
			tx.commit(new Handler<Message<JsonObject>>() {
				@Override
				public void handle(Message<JsonObject> event) {
					JsonArray results = event.body().getJsonArray("results");
					if ("ok".equals(event.body().getString("status")) && results != null && results.size() == 3) {
						try {
							final JsonObject result = cloneAndFilterResults(prefixAcademy);
							int created = results.getJsonArray(0).getJsonObject(0).getInteger("createdCount");
							int modified = results.getJsonArray(1).getJsonObject(0).getInteger("modifiedCount");
							int disappearance = results.getJsonArray(2).getJsonObject(0)
									.getInteger("disappearanceCount");
							result.put("userCount", new JsonObject().put("created", created)
									.put("modified", (modified - created)).put("disappearance", disappearance));
							result.put("source", source);
							result.put("startTime", new DateTime(startTime).toString());
							result.put("endTime", new DateTime(endTime).toString());
							result.put("loadedFiles", cloneAndFilterFiles(prefixAcademy));
							handler.handle(result);
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
							handler.handle(null);
						}
					} else {
						log.error("Error in count diff transaction.");
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

	private void emailReport(final Vertx vertx, final EmailFactory emailFactory, final JsonObject sendReport,
			final JsonObject result) {
		final JsonObject reqParams = new JsonObject().put("headers",
				new JsonObject().put("Accept-Language", acceptLanguage));
		emailFactory.getSender().sendEmail(new JsonHttpServerRequest(reqParams),
				sendReport.getJsonArray("to").getList(),
				sendReport.getJsonArray("cc") != null ? sendReport.getJsonArray("cc").getList() : null,
				sendReport.getJsonArray("bcc") != null ? sendReport.getJsonArray("bcc").getList() : null,
				sendReport.getString("project", "")
						+ i18n.translate("import.report", I18n.DEFAULT_DOMAIN, acceptLanguage) + " - "
						+ DateTime.now().toString(DateTimeFormat.forPattern("yyyy-MM-dd")),
				"email/report.html", result, false, ar -> {
					if (ar.failed()) {
						log.error("Error sending report email.", ar.cause());
					}
				});
	}

	public void sendEmails(final Vertx vertx, final JsonObject config, String source) {
		final JsonArray sendReport = config.getJsonArray("sendReport");
		if (sendReport == null) {
			//log.error("Cannot send reports because of empty config: " + sendReport);
			return;
		}
		int count = sendReport.size();
		EmailFactory emailFactory = new EmailFactory(vertx, config);
		for (Object o : sendReport) {
			JsonObject currentSendReport = (JsonObject) o;
			if (currentSendReport.getJsonArray("to") == null //
					|| currentSendReport.getJsonArray("to").size() == 0 //
					|| currentSendReport.getJsonArray("sources") == null//
					|| !currentSendReport.getJsonArray("sources").contains(source)) {
				// log.error("Cannot send report because of missing infos: " + currentSendReport);
				continue;
			}
			if (count == 1) {
				this.countDiff(Optional.empty(), source, countEvent -> {
					if (countEvent != null) {
						this.emailReport(vertx, emailFactory, currentSendReport, countEvent);
					}
				});
			} else {
				String prefixAcademy = currentSendReport.getString("academyPrefix");
				this.countDiff(Optional.ofNullable(prefixAcademy), source, countEvent -> {
					if (countEvent != null) {
						this.emailReport(vertx, emailFactory, currentSendReport, countEvent);
					}
				});
			}
		}
	}

	public void addMapping(String profile, JsonObject mappping) {
		JsonObject mappings = result.getJsonObject(MAPPINGS);
		if (mappings == null) {
			mappings = new JsonObject();
			result.put(MAPPINGS, mappings);
		}
		mappings.put(profile, mappping);
	}

	public JsonObject getMappings() {
		return result.getJsonObject(MAPPINGS);
	}

	public void setMappings(JsonObject mappings) {
		if (mappings != null && mappings.size() > 0) {
			result.put(MAPPINGS, mappings);
		}
	}

	protected boolean updateCleanKeys() { return false; }

	protected int cleanAttributeKeys(JsonObject attribute) {
		int count = 0;
		if (attribute != null) {
			for (String attr : attribute.fieldNames()) {
				Object j = attribute.getValue(attr);
				if (j != null){
					if (j instanceof JsonObject) {
						JsonObject jo = (JsonObject)j;
						for (String attr2 : jo.copy().fieldNames()) {
							if (attr2.contains(".")) {
								count++;
								jo.put(attr2.replaceAll("\\.", "_|_"), (String) jo.remove(attr2));
							}
						}
					} else if (j instanceof JsonArray && attr.contains(".")) {
						attribute.put(attr.replaceAll("\\.", "_|_"), (JsonArray) j);
						attribute.remove(attr);
						count++;
					}
				}
			}
		}
		return count;
	}

	protected void uncleanAttributeKeys(JsonObject attribute) {
		if (attribute != null) {
			for (String attr : attribute.fieldNames()) {
				Object j = attribute.getValue(attr);
				if (j != null) {
					if (j instanceof JsonObject) {
						JsonObject jo = (JsonObject)j;
						for (String attr2 : jo.copy().fieldNames()) {
							if (attr2.contains("_|_")) {
								jo.put(attr2.replaceAll("_\\|_", "."), (String) jo.remove(attr2));
							}
						}
					} else if (j instanceof JsonArray && attr.contains("_|_")) {
						attribute.put(attr.replaceAll("_\\|_", "."), (JsonArray) j);
						attribute.remove(attr);
					}
				}
			}
		}
	}

	public String getSource() {
		return "REPORT";
	}

}
