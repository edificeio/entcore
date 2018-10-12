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

import fr.wseduc.bus.BusAddress;
import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Get;
import fr.wseduc.rs.Post;
import fr.wseduc.rs.Put;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.http.BaseController;
import fr.wseduc.webutils.request.RequestUtils;
import org.entcore.common.http.filter.AdminFilter;
import org.entcore.common.http.filter.AdmlOfStructure;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.http.filter.SuperAdminFilter;
import org.entcore.common.utils.StringUtils;
import org.entcore.directory.security.UserInStructure;
import org.entcore.directory.services.TimetableService;
import org.joda.time.DateTime;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerFileUpload;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.io.File;
import java.util.List;
import java.util.UUID;

import static fr.wseduc.webutils.Utils.getOrElse;
import static org.entcore.common.http.response.DefaultResponseHandler.*;
import static org.entcore.common.utils.FileUtils.deleteImportPath;

public class TimetableController extends BaseController {

	private TimetableService timetableService;

	@Get("/timetable")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@ResourceFilter(AdminFilter.class)
	public void timetable(HttpServerRequest request) {
		renderView(request);
	}

	@Get("/timetable/courses/:structureId")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@ResourceFilter(SuperAdminFilter.class)
	public void listCourses(HttpServerRequest request) {
		final String structureId = request.params().get("structureId");
		long lastDate;
		try {
			lastDate = Long.parseLong(getOrElse(request.params().get("lastDate"), "0", false));
		} catch (NumberFormatException e) {
			try {
				lastDate = DateTime.parse(request.params().get("lastDate")).getMillis();
			} catch (RuntimeException e2) {
				badRequest(request, "invalid.date");
				return;
			}
		}
		timetableService.listCourses(structureId, lastDate, arrayResponseHandler(request));
	}

	@Get("/timetable/courses/:structureId/:begin/:end")
	@ApiDoc("Get courses for a structure between two dates by optional teacher id and/or optional group name.")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@ResourceFilter(UserInStructure.class)
	public void listCoursesBetweenTwoDates(final HttpServerRequest request) {
		final String structureId = request.params().get("structureId");
		final String teacherId = request.params().get("teacherId");
		final List<String> groupNames = request.params().getAll("group");
		final String beginDate = request.params().get("begin");
		final String endDate = request.params().get("end");

		if (beginDate!=null && endDate != null &&
				beginDate.matches("\\d{4}-\\d{2}-\\d{2}") && endDate.matches("\\d{4}-\\d{2}-\\d{2}")) {
			timetableService.listCoursesBetweenTwoDates(structureId, teacherId, groupNames, beginDate, endDate, arrayResponseHandler(request));
		} else {
			badRequest(request, "timetable.invalid.dates");
		}
	}

	@Get("/timetable/subjects/:structureId")
	@ApiDoc("Get subject list of the structure by optional teacher identifiers and with the ability to display associated groups and classes.")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@ResourceFilter(UserInStructure.class)
	public void listSubjects(HttpServerRequest request) {
		final String structureId = request.params().get("structureId");
		final List<String> teachers = request.params().getAll("teacherId");
		final boolean classes = request.params().contains("classes");
		final boolean groups = request.params().contains("groups");

		timetableService.listSubjects(structureId, teachers, classes, groups, arrayResponseHandler(request));
	}

	@Get("/timetable/subjects/:structureId/group")
	@ApiDoc("Get subject list of the structure by external group id.")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@ResourceFilter(UserInStructure.class)
	public void listSubjectsByGroup(HttpServerRequest request) {
		final String structureId = request.params().get("structureId");
		final String externalGroupId = request.params().get("externalGroupId");

		timetableService.listSubjectsByGroup(structureId, externalGroupId, arrayResponseHandler(request));
	}

	@Put("/timetable/init/:structureId")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@ResourceFilter(AdmlOfStructure.class)
	public void initStructure(final HttpServerRequest request) {
		RequestUtils.bodyToJson(request, pathPrefix + "initTimetable", new Handler<JsonObject>() {
			@Override
			public void handle(JsonObject conf) {
				timetableService.initStructure(request.params().get("structureId"), conf, notEmptyResponseHandler(request));
			}
		});
	}

	@Get("/timetable/classes/:structureId")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@ResourceFilter(AdmlOfStructure.class)
	public void classesMapping(final HttpServerRequest request) {
		timetableService.classesMapping(request.params().get("structureId"), defaultResponseHandler(request));
	}

	@Put("/timetable/classes/:structureId")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@ResourceFilter(AdmlOfStructure.class)
	public void updateClassesMapping(final HttpServerRequest request) {
		RequestUtils.bodyToJson(request, new Handler<JsonObject>() {
			@Override
			public void handle(JsonObject mapping) {
				timetableService.updateClassesMapping(request.params().get("structureId"), mapping, defaultResponseHandler(request));
			}
		});
	}

	@Post("/timetable/import/:structureId")
	@ResourceFilter(AdminFilter.class)
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void importTimetable(final HttpServerRequest request) {
		request.pause();
		final String importId = UUID.randomUUID().toString();
		final String path = config.getString("timetable-path", "/tmp") + File.separator + importId;
		request.setExpectMultipart(true);
		request.exceptionHandler(new Handler<Throwable>() {
			@Override
			public void handle(Throwable event) {
				badRequest(request, event.getMessage());
				deleteImportPath(vertx, path);
			}
		});
		request.uploadHandler(new Handler<HttpServerFileUpload>() {
			@Override
			public void handle(final HttpServerFileUpload upload) {
				final String filename = path + File.separator + upload.filename();
				upload.endHandler(new Handler<Void>() {
					@Override
					public void handle(Void event) {
						timetableService.importTimetable(request.params().get("structureId"), filename,
								getHost(request), I18n.acceptLanguage(request),
								reportResponseHandler(vertx, path, request));
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
					badRequest(request, "mkdir.error");
				}
			}
		});
	}

	@BusAddress("timetable")
	@SuppressWarnings("unchecked")
	public void getTimetable(final Message<JsonObject> message){
		final String action = message.body().getString("action");

		if (action == null) {
			log.warn("[@BusAddress](timetable) Invalid action.");
			message.reply(new JsonObject().put("status", "error")
					.put("message", "Invalid action."));
			return;
		}

		final String structureId = message.body().getString("structureId");

		switch(action){
			case "get.course":
				final String teacherId = message.body().getString("teacherId");
				final List<String> groupNames = message.body().getJsonArray("group", new JsonArray()).getList();
				final String beginDate = message.body().getString("begin");
				final String endDate = message.body().getString("end");

				if (beginDate!=null && endDate != null &&
						beginDate.matches("\\d{4}-\\d{2}-\\d{2}") && endDate.matches("\\d{4}-\\d{2}-\\d{2}")) {

					timetableService.listCoursesBetweenTwoDates(structureId, teacherId, groupNames, beginDate, endDate, getBusResultHandler(message));
				} else {
					message.reply(new JsonObject()
							.put("status", "error")
							.put("message", "timetable.invalid.dates"));
				}
				break;
			case "get.subjects":
				final List<String> teachers = message.body().getJsonArray("teacherIds", new fr.wseduc.webutils.collections.JsonArray()).getList();
				final String externalGroupId = message.body().getString("externalGroupId");
				final boolean classes = message.body().getBoolean("classes", false);
				final boolean groups = message.body().getBoolean("groups", false);

				if (StringUtils.isEmpty(externalGroupId)) {
					timetableService.listSubjects(structureId, teachers, classes, groups, getBusResultHandler(message));
				} else {
					timetableService.listSubjectsByGroup(structureId, externalGroupId, getBusResultHandler(message));
				}

				break;
			default:
				message.reply(new JsonObject().put("status", "error")
						.put("message", "Invalid action."));
				break;
		}
	}

	private Handler<Either<String, JsonArray>> getBusResultHandler(final Message<JsonObject> message) {
		return new Handler<Either<String, JsonArray>>() {
            @Override
            public void handle(Either<String, JsonArray> result) {
                if (result.isRight()) {
                    message.reply(new JsonObject()
                            .put("status", "ok")
                            .put("results", result.right().getValue()));
                } else {
                    message.reply(new JsonObject()
                            .put("status", "error")
                            .put("message", result.left().getValue()));
                }
            }
        };
	}

	public void setTimetableService(TimetableService timetableService) {
		this.timetableService = timetableService;
	}

}
