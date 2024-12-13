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

package org.entcore.directory.controllers;

import fr.wseduc.rs.Delete;
import fr.wseduc.rs.Get;
import fr.wseduc.rs.Post;
import fr.wseduc.rs.Put;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.MfaProtected;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.DefaultAsyncResult;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.http.BaseController;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerFileUpload;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonObject;
import org.entcore.common.http.filter.AdminFilter;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.user.DefaultFunctions;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;
import org.entcore.directory.exceptions.ImportException;
import org.entcore.directory.pojo.ImportInfos;
import org.entcore.directory.security.TeacherOfClass;
import org.entcore.directory.services.ImportService;
import org.entcore.directory.services.SchoolService;

import java.io.File;
import java.util.UUID;

import static fr.wseduc.webutils.Utils.isNotEmpty;
import static fr.wseduc.webutils.request.RequestUtils.bodyToJson;
import static org.entcore.common.http.response.DefaultResponseHandler.*;
import static org.entcore.common.utils.FileUtils.deleteImportPath;


public class ImportController extends BaseController {

	private ImportService importService;
	private SchoolService schoolService;

	@Get("/wizard")
	@ResourceFilter(AdminFilter.class)
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@MfaProtected()
	public void view(HttpServerRequest request) {
		renderView(request);
	}

    @Post("/wizard/column/mapping")
    @ResourceFilter(AdminFilter.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @MfaProtected()
    public void columnsMapping(final HttpServerRequest request) {
        uploadImport(request, new Handler<AsyncResult<ImportInfos>>() {
            @Override
            public void handle(AsyncResult<ImportInfos> event) {
                if (event.succeeded()) {
                    importService.columnsMapping(event.result(), reportResponseHandler(vertx, event.result().getPath(), request));
                } else {
                    badRequest(request, event.cause().getMessage());
                }
            }
        });
    }

	@Post("/wizard/classes/mapping")
	@ResourceFilter(AdminFilter.class)
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@MfaProtected()
	public void classesMapping(final HttpServerRequest request) {
		uploadImport(request, new Handler<AsyncResult<ImportInfos>>() {
			@Override
			public void handle(AsyncResult<ImportInfos> event) {
				if (event.succeeded()) {
					importService.classesMapping(event.result(), reportResponseHandler(vertx, event.result().getPath(), request));
				} else {
					badRequest(request, event.cause().getMessage());
				}
			}
		});
	}


    @Post("/wizard/validate")
    @ResourceFilter(AdminFilter.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @MfaProtected()
    public void validateImport(final HttpServerRequest request) {
        uploadImport(request, new Handler<AsyncResult<ImportInfos>>() {
            @Override
            public void handle(AsyncResult<ImportInfos> event) {
                if (event.succeeded()) {
                    UserUtils.getUserInfos(eb, request, user -> {
                        if (user != null) {
                            importService.validate(event.result(), user,
                                    reportResponseHandler(vertx, event.result().getPath(), request));
                        } else {
                            unauthorized(request, "invalid.user");
                        }
                    });
                } else {
                    badRequest(request, event.cause().getMessage());
                }
            }
        });
    }

	@Put("/wizard/validate/:id")
	@ResourceFilter(AdminFilter.class) // TODO add import owner and check
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@MfaProtected()
	public void validateWithId(final HttpServerRequest request) {
		String importId = request.params().get("id");
		UserUtils.getUserInfos(eb, request, user -> {
			if (user != null) {
				importService.validate(importId, user, reportResponseHandler(vertx,
						config.getString("wizard-path", "/tmp") + File.separator + importId, request));
			} else {
				unauthorized(request, "invalid.user");
			}
		});
	}

	private void uploadImport(final HttpServerRequest request, final Handler<AsyncResult<ImportInfos>> handler) {
		request.pause();
		final String importId = UUID.randomUUID().toString();
		final String path = config.getString("wizard-path", "/tmp") + File.separator + importId;
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
				importInfos.setUAI(request.formAttributes().get("UAI"));
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
						deleteImportPath(vertx, path);
						deleteImportPath(vertx, path);
						return;
					}
				}
				try {
					importInfos.setFeeder(request.formAttributes().get("type"));
				} catch (IllegalArgumentException | NullPointerException e) {
					handler.handle(new DefaultAsyncResult<ImportInfos>(new ImportException("invalid.import.type", e)));
					deleteImportPath(vertx, path);
					return;
				}
				UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
					@Override
					public void handle(UserInfos user) {
						if (user == null) {
							handler.handle(new DefaultAsyncResult<ImportInfos>(new ImportException("invalid.admin")));
							deleteImportPath(vertx, path);
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
										deleteImportPath(vertx, path);
									}
								} else {
									handler.handle(new DefaultAsyncResult<ImportInfos>(validate.cause()));
									log.error("Validate error", validate.cause());
									deleteImportPath(vertx, path);
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
				deleteImportPath(vertx, path);
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
		deleteImportPath(vertx, path,res->{
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

	@Get("/wizard/import/:id")
	@ResourceFilter(AdminFilter.class)
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@MfaProtected()
	public void findImportDraft(final HttpServerRequest request) {
		importService.findById(request.params().get("id"), defaultResponseHandler(request));
	}

	@Post("/wizard/import")
	@ResourceFilter(AdminFilter.class)
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@MfaProtected()
	public void doImport(final HttpServerRequest request) {
		uploadImport(request, new Handler<AsyncResult<ImportInfos>>() {
			@Override
			public void handle(final AsyncResult<ImportInfos> event) {
				if (event.succeeded()) {
					importService.doImport(event.result(), reportResponseHandler(vertx, event.result().getPath(), request));
				} else {
					badRequest(request, event.cause().getMessage());
				}
			}
		});
	}

	@Post("/import/:userType/class/:classId")
	@ResourceFilter(TeacherOfClass.class)
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void doClassImport(final HttpServerRequest request) {
		request.pause();
		schoolService.getByClassId(request.params().get("classId"), new Handler<Either<String, JsonObject>>() {
			@Override
			public void handle(Either<String, JsonObject> s) {
				JsonObject structure = new JsonObject();
				if (s.isRight()) {
					structure = s.right().getValue();
				}

				if(structure.isEmpty() == false)
				{
					request.setExpectMultipart(true);
					request.formAttributes().add("structureId", structure.getString("id"));
					request.formAttributes().add("structureExternalId", structure.getString("externalId"));
					request.formAttributes().add("predelete", "false");
					request.formAttributes().add("transition", "false");
					request.formAttributes().add("structureName", structure.getString("name"));
					if(structure.getString("UAI") != null)
						request.formAttributes().add("UAI", structure.getString("UAI"));
					request.formAttributes().add("type", "CSV");
					request.resume();
					uploadImport(request, new Handler<AsyncResult<ImportInfos>>() {
						@Override
						public void handle(final AsyncResult<ImportInfos> event) {
							if (event.succeeded()) {
								importService.doImport(event.result(), reportResponseHandler(vertx, event.result().getPath(), request));
							} else {
								badRequest(request, event.cause().getMessage());
							}
						}
					});
				} else {
					notFound(request, "class.not.found");
				}
			}
		});
	}

	@Put("/wizard/import/:id")
	@ResourceFilter(AdminFilter.class) // TODO add import owner and check
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@MfaProtected()
	public void launchImport(final HttpServerRequest request) {
		String importId = request.params().get("id");
		importService.doImport(importId, reportResponseHandler(vertx,
				config.getString("wizard-path", "/tmp") + File.separator + importId, request));
	}

	@Post("/wizard/update/:id/:profile")
	@ResourceFilter(AdminFilter.class) // TODO add import owner and check
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@MfaProtected()
	public void createLine(final HttpServerRequest request) {
		final String importId = request.params().get("id");
		final String profile = request.params().get("profile");
		bodyToJson(request, new Handler<JsonObject>() { // TODO add json validator
			@Override
			public void handle(JsonObject line) {
				importService.addLine(importId, profile, line, notEmptyResponseHandler(request));
			}
		});
	}

	@Put("/wizard/update/:id/:profile")
	@ResourceFilter(AdminFilter.class) // TODO add import owner and check
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@MfaProtected()
	public void updateLine(final HttpServerRequest request) {
		final String importId = request.params().get("id");
		final String profile = request.params().get("profile");
		bodyToJson(request, new Handler<JsonObject>() { // TODO add json validator
			@Override
			public void handle(JsonObject line) {
				importService.updateLine(importId, profile, line, notEmptyResponseHandler(request));
			}
		});
	}

	@Delete("/wizard/update/:id/:profile/:line")
	@ResourceFilter(AdminFilter.class) // TODO add import owner and check
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@MfaProtected()
	public void deleteLine(final HttpServerRequest request) {
		final String importId = request.params().get("id");
		final String profile = request.params().get("profile");
		try {
			final Integer line = Integer.parseInt(request.params().get("line"));
			importService.deleteLine(importId, profile, line, notEmptyResponseHandler(request));
		} catch (NumberFormatException e) {
			badRequest(request, "invalid.line");
		}
	}

	public void setImportService(ImportService importService) {
		this.importService = importService;
	}

	public void setSchoolService(SchoolService schoolService) {
		this.schoolService = schoolService;
	}

}
