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
import fr.wseduc.rs.Put;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.http.BaseController;
import fr.wseduc.webutils.request.RequestUtils;
import org.entcore.common.http.filter.AdminFilter;
import org.entcore.common.http.filter.AdmlOfStructure;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.http.filter.SuperAdminFilter;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;
import org.entcore.directory.services.TimetableService;
import org.joda.time.DateTime;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.VoidHandler;
import org.vertx.java.core.http.HttpServerFileUpload;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonObject;

import java.io.File;
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


	@Get("/timetable/courses/teacher/:structureId")
	@SecuredAction(value = "", type = ActionType.AUTHENTICATED)
	public void listCoursesForTeacher(final HttpServerRequest request) {

		final String structureId = request.params().get("structureId");
		final String teacherId = request.params().get("teacherId");
		final String beginDate = request.params().get("begin");
		final String endDate = request.params().get("end");

		//grab connected user to control structure
		UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
					@Override
					public void handle(final UserInfos user) {
						if (user != null) {
							if (user.getStructures().contains(structureId)){
								timetableService.listCoursesForTeacher(structureId, teacherId, beginDate,endDate, arrayResponseHandler(request));
							}else{
								badRequest(request, "diary.invalid.structure.right");
							}
						} else {
							badRequest(request, "diary.invalid.login");
						}
					}
				}
		);

	}

	@Get("/timetable/subjects/:structureId")
	@SecuredAction(value = "", type = ActionType.AUTHENTICATED)
	public void listSubjects(HttpServerRequest request) {
		final String structureId = request.params().get("structureId");
		final boolean teachers = request.params().contains("teachers");
		final boolean classes = request.params().contains("classes");
		final boolean groups = request.params().contains("groups");
		timetableService.listSubjects(structureId, teachers, classes, groups, arrayResponseHandler(request));
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
		final String path = container.config().getString("timetable-path", "/tmp") + File.separator + importId;
		request.expectMultiPart(true);
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

	public void setTimetableService(TimetableService timetableService) {
		this.timetableService = timetableService;
	}

}
