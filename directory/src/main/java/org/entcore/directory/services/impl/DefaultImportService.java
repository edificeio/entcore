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
import fr.wseduc.webutils.DefaultAsyncResult;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.I18n;
import io.vertx.core.*;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerFileUpload;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.mongodb.MongoDbResult;
import org.entcore.common.storage.Storage;
import org.entcore.common.user.DefaultFunctions;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;
import org.entcore.directory.Directory;
import org.entcore.directory.exceptions.ImportException;
import org.entcore.directory.pojo.ImportInfos;
import org.entcore.directory.services.ImportService;

import java.io.File;
import java.util.Map;
import java.util.UUID;

import static fr.wseduc.webutils.Utils.*;
import static org.entcore.common.user.DefaultFunctions.ADMIN_LOCAL;
import static org.entcore.common.user.DefaultFunctions.SUPER_ADMIN;
import static org.entcore.common.utils.FileUtils.deleteImportPath;

public class DefaultImportService implements ImportService {

	private static final Logger log = LoggerFactory.getLogger(DefaultImportService.class);
	private static final long TIMEOUT = 10 * 60 * 1000l;
	private final EventBus eb;
	private final Vertx vertx;
	private static final ObjectMapper mapper = new ObjectMapper();
	private final MongoDb mongo = MongoDb.getInstance();
	private static final String IMPORTS = "imports";
	private final Storage storage;
	private final JsonObject config;
	public DefaultImportService(Vertx vertx, EventBus eb, Storage storage, JsonObject config) {
		this.eb = eb;
		this.vertx = vertx;
		this.storage = storage;
		this.config = config;
	}

	@Override
	public void validate(ImportInfos importInfos, UserInfos user, final Handler<Either<JsonObject, JsonObject>> handler) {
		try {
			final AdmlValidate admlValidate = new AdmlValidate(user, handler).invoke();
			if (admlValidate.is()) return;
			final JsonObject action = new JsonObject(mapper.writeValueAsString(importInfos))
					.put("action", "validate")
					.put("adml-structures", admlValidate.getAdmlStructures());
			eb.request(Directory.FEEDER, action, new DeliveryOptions().setSendTimeout(TIMEOUT), handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
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
								new JsonArray().add(res.body().getString("message", "")))));
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
			eb.request("entcore.feeder", action, new DeliveryOptions().setSendTimeout(TIMEOUT), handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
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
	public void doImport(String importId, final Handler<Either<JsonObject, JsonObject>> handler) {
		JsonObject action = new JsonObject().put("action", "importWithId").put("id", importId);
		sendCommand(handler, action);
	}

	@Override
	public void columnsMapping(ImportInfos importInfos, final Handler<Either<JsonObject, JsonObject>> handler) {
		try {
			JsonObject action = new JsonObject(mapper.writeValueAsString(importInfos))
					.put("action", "columnsMapping");
			eb.request("entcore.feeder", action, handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
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
		eb.request("entcore.feeder", action, new DeliveryOptions().setSendTimeout(600000L), handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
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

	@Override
	public void uploadImport(final HttpServerRequest request, final Handler<AsyncResult<ImportInfos>> handler) {
		request.pause();
		final String importId = UUID.randomUUID().toString();
		String path = config.getString("wizard-path", "/tmp") + File.separator + importId;
		request.setExpectMultipart(true);
		request.endHandler(new Handler<Void>() {
			@Override
			public void handle(Void v) {
				final ImportInfos importInfos = new ImportInfos();
				importInfos.setId(importId);
				importInfos.setPath(path);
				importInfos.setStructureId(request.formAttributes().get("structureId"));
				importInfos.setStructureExternalId(request.formAttributes().get("structureExternalId"));
				importInfos.setPreDelete(paramToBoolean(request.formAttributes().get("predelete")));
				importInfos.setTransition(paramToBoolean(request.formAttributes().get("transition")));
				importInfos.setStructureName(request.formAttributes().get("structureName"));
				importInfos.setUAI(request.formAttributes().get("UAI").equals("null") ? null : request.formAttributes().get("UAI"));
				importInfos.setLanguage(I18n.acceptLanguage(request));
				if (isNotEmpty(request.formAttributes().get("classExternalId"))) {
					importInfos.setOverrideClass(request.formAttributes().get("classExternalId"));
				}

				if (isNotEmpty(request.formAttributes().get("columnsMapping")) ||
						isNotEmpty(request.formAttributes().get("classesMapping"))) {
					try {
						if (isNotEmpty(request.formAttributes().get("columnsMapping"))) {
							importInfos.setMappings(new JsonObject(request.formAttributes().get("columnsMapping")));
						}
						if (isNotEmpty(request.formAttributes().get("classesMapping"))) {
							importInfos.setClassesMapping(new JsonObject(request.formAttributes().get("classesMapping")));
						}
					} catch (DecodeException e) {
						handler.handle(new DefaultAsyncResult<ImportInfos>(new ImportException("invalid.columns.mapping", e)));
						deleteImportPath(vertx, storage, path);
						return;
					}
				}
				try {
					importInfos.setFeeder(request.formAttributes().get("type"));
				} catch (IllegalArgumentException | NullPointerException e) {
					handler.handle(new DefaultAsyncResult<ImportInfos>(new ImportException("invalid.import.type", e)));
					deleteImportPath(vertx, storage, path);
					return;
				}
				UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
					@Override
					public void handle(UserInfos user) {
						if (user == null) {
							handler.handle(new DefaultAsyncResult<ImportInfos>(new ImportException("invalid.admin")));
							deleteImportPath(vertx, storage, path);
							return;
						}
						importInfos.validate(
								user.getFunctions() != null && user.getFunctions().containsKey(DefaultFunctions.SUPER_ADMIN),
								vertx,
								storage,
								new Handler<AsyncResult<String>>() {
									@Override
									public void handle(AsyncResult<String> validate) {
										if (validate.succeeded()) {
											if (validate.result() == null) {
												handler.handle(new DefaultAsyncResult<>(importInfos));
											} else {
												handler.handle(new DefaultAsyncResult<ImportInfos>(new ImportException(validate.result())));
												deleteImportPath(vertx, storage, path);
											}
										} else {
											handler.handle(new DefaultAsyncResult<ImportInfos>(validate.cause()));
											log.error("Validate error", validate.cause());
											deleteImportPath(vertx, storage, path);
										}
									}
								});
					}
				});
			}
		});
		request.exceptionHandler(new Handler<Throwable>() {
			@Override
			public void handle(Throwable event) {
				handler.handle(new DefaultAsyncResult<ImportInfos>(event));
				deleteImportPath(vertx, storage, path);
			}
		});
		request.uploadHandler(new Handler<HttpServerFileUpload>() {
			@Override
			public void handle(final HttpServerFileUpload upload) {
				if (!upload.filename().toLowerCase().endsWith(".csv")) {
					handler.handle(new DefaultAsyncResult<ImportInfos>(
							new ImportException("invalid.file.extension")));
					return;
				}
				final String filename = path + File.separator + upload.name();
				upload.streamToFileSystem(filename)
						.onSuccess(event -> log.info("File " + upload.filename() + " uploaded as " + upload.name()))
						.onFailure(th -> log.error("Cannot import " + upload.filename(), th));
				request.resume();
			}
		});

		deleteImportPath(vertx, storage, path,res->{
			vertx.fileSystem().mkdir(path, new Handler<AsyncResult<Void>>() {
				@Override
				public void handle(AsyncResult<Void> event) {
					if (event.succeeded()) {
						request.resume();
					} else {
						handler.handle(new DefaultAsyncResult<ImportInfos>(
								new ImportException("mkdir.error", event.cause())));
					}
				}
			});
		});
	}

	private boolean paramToBoolean(String param) {
		return "true".equalsIgnoreCase(param);
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
