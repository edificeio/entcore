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

package org.entcore.directory.controllers;

import fr.wseduc.rs.Get;
import fr.wseduc.rs.Post;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.DefaultAsyncResult;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.http.BaseController;
import org.entcore.common.http.filter.AdminFilter;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.user.DefaultFunctions;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;
import org.entcore.directory.exceptions.ImportException;
import org.entcore.directory.pojo.ImportInfos;
import org.entcore.directory.services.ImportService;
import org.entcore.directory.services.impl.DefaultMappingService;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.VoidHandler;
import org.vertx.java.core.http.HttpServerFileUpload;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonObject;

import java.io.File;
import java.util.UUID;

import static fr.wseduc.webutils.request.RequestUtils.bodyToJson;


public class ImportController extends BaseController {

	private ImportService importService;
	private DefaultMappingService mappingService;
	private ImportInfos importInfos;

	@Get("/wizard")
	@ResourceFilter(AdminFilter.class)
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void view(HttpServerRequest request) {
		renderView(request);
	}

	@Post("/wizard/validate")
	@ResourceFilter(AdminFilter.class)
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void validateImport(final HttpServerRequest request) {
		uploadImport(request, new Handler<AsyncResult<ImportInfos>>() {
			@Override
			public void handle(AsyncResult<ImportInfos> event) {
				if (event.succeeded()) {
					importService.validate(event.result(), responseHandler(event.result().getPath(), request));
				} else {
					badRequest(request, event.cause().getMessage());
				}
			}
		});
	}

	@Post("/wizard/validateMapping/:profileName")
	@ResourceFilter(AdminFilter.class)
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void validateMappingImport(final HttpServerRequest request) {
		bodyToJson(request, new Handler<JsonObject>() {
			@Override
			public void handle(final JsonObject association) {
				String profileName = request.params().get("profileName");
				String path = association.getString("profile");
				final String importId = path;
				path = container.config().getString("wizard-path", "/tmp") + File.separator + importId;
				mappingService.mappingValidate( association, profileName, path, importInfos, new Handler<Either<String, JsonObject>>() {
					@Override
					public void handle(Either<String, JsonObject> event) {
						if (event.isRight()) {
							renderJson(request, event.right().getValue(), 200);
						} else {
							JsonObject error = new JsonObject()
									.putString("errors", event.left().getValue());
							renderJson(request, error, 400);
						}
					}
				});
			}
		});
	}

	@Get("/wizard/mapping")
	@ResourceFilter(AdminFilter.class)
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void getMappingInfos(final HttpServerRequest request) {
		String profile = request.params().get("profile");
		mappingService.getRequestedFieldsForProfile(profile, importInfos, new Handler<Either<String, JsonObject>>() {
			@Override
			public void handle(Either<String, JsonObject> event) {
				if (event.isRight()) {
					renderJson(request, event.right().getValue(), 200);
				} else {
					JsonObject error = new JsonObject()
							.putString("errors", event.left().getValue());
					renderJson(request, error, 400);
				}
			}
		});
	}

	private Handler<Either<JsonObject, JsonObject>> responseHandler(final String path, final HttpServerRequest request) {
		return new Handler<Either<JsonObject, JsonObject>>() {
			@Override
			public void handle(Either<JsonObject, JsonObject> event) {
				if (event.isRight()) {
					renderJson(request, event.right().getValue(), 200);
				} else {
					JsonObject error = new JsonObject()
							.putObject("errors", event.left().getValue());
					renderJson(request, error, 400);
				}
				//importService.deleteImportPath(path);
			}
		};
	}

	private void uploadImport(final HttpServerRequest request, final Handler<AsyncResult<ImportInfos>> handler) {
		request.pause();
		final String importId = UUID.randomUUID().toString();
		final String path = container.config().getString("wizard-path", "/tmp") + File.separator + importId;
		request.expectMultiPart(true);
		request.endHandler(new VoidHandler() {
			@Override
			protected void handle() {
				importInfos = new ImportInfos();
				importInfos.setId(importId);
				importInfos.setPath(path);
				importInfos.setStructureId(request.formAttributes().get("structureId"));
				importInfos.setStructureExternalId(request.formAttributes().get("structureExternalId"));
				importInfos.setPreDelete(paramToBoolean(request.formAttributes().get("predelete")));
				importInfos.setTransition(paramToBoolean(request.formAttributes().get("transition")));
				importInfos.setStructureName(request.formAttributes().get("structureName"));
				importInfos.setUAI(request.formAttributes().get("UAI"));
				importInfos.setLanguage(I18n.acceptLanguage(request));
				try {
					importInfos.setFeeder(request.formAttributes().get("type"));
				} catch (IllegalArgumentException | NullPointerException e) {
					handler.handle(new DefaultAsyncResult<ImportInfos>(new ImportException("invalid.import.type", e)));
					importService.deleteImportPath(path);
					return;
				}
				UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
					@Override
					public void handle(UserInfos user) {
						if (user == null) {
							handler.handle(new DefaultAsyncResult<ImportInfos>(new ImportException("invalid.admin")));
							importService.deleteImportPath(path);
							return;
						}
						importInfos.validate(user.getFunctions() != null && user.getFunctions()
								.containsKey(DefaultFunctions.SUPER_ADMIN), vertx, new Handler<AsyncResult<String>>() {
							@Override
							public void handle(AsyncResult<String> validate) {
								if (validate.succeeded()) {
									if (validate.result() == null) {
										handler.handle(new DefaultAsyncResult<>(importInfos));
									} else {
										handler.handle(new DefaultAsyncResult<ImportInfos>(new ImportException(validate.result())));
										importService.deleteImportPath(path);
									}
								} else {
									handler.handle(new DefaultAsyncResult<ImportInfos>(validate.cause()));
									log.error("Validate error", validate.cause());
									// no need to delete import path, we will make the mapping
									//importService.deleteImportPath(path);
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
				importService.deleteImportPath(path);
			}
		});
		request.uploadHandler(new Handler<HttpServerFileUpload>() {
			@Override
			public void handle(final HttpServerFileUpload upload) {
				final String filename = path + File.separator + upload.name();
				upload.endHandler(new Handler<Void>() {
					@Override
					public void handle(Void event) {
						log.info("File " + upload.filename() + " uploaded as " + upload.name());
					}
				});
				upload.streamToFileSystem(filename);
			}
		});
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
	}

	private boolean paramToBoolean(String param) {
		return "true".equalsIgnoreCase(param);
	}

	@Post("/wizard/import")
	@ResourceFilter(AdminFilter.class)
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void doImport(final HttpServerRequest request) {
		uploadImport(request, new Handler<AsyncResult<ImportInfos>>() {
			@Override
			public void handle(final AsyncResult<ImportInfos> event) {
				if (event.succeeded()) {
					importService.doImport(event.result(), responseHandler(event.result().getPath(), request));
				} else {
					badRequest(request, event.cause().getMessage());
				}
			}
		});
	}

	public void setImportService(ImportService importService) {
		this.importService = importService;
	}

	public void setDefaultMappingService(DefaultMappingService mappingService) {
		this.mappingService = mappingService;
	}

}
