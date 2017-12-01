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

package org.entcore.directory.services;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.Arrays;
import java.util.List;

public interface TimetableService {

	List<String> TIMETABLE_TYPES = Arrays.asList("EDT", "UDT");

	void listCourses(String structureId, long lastDate, Handler<Either<String,JsonArray>> handler);

	/**
	 * Get the list course between two dates
	 *
	 * @param structureId The structure ID
	 * @param teacherId The teacher ID
	 * @param begin From the begin date
	 * @param end To the begin date
	 * @param handler
	 */
	void listCoursesBetweenTwoDates(String structureId, String teacherId, String group, String begin, String end, Handler<Either<String,JsonArray>> handler);

	void listSubjects(String structureId, List<String> teachers, boolean classes, boolean groups,
	                  Handler<Either<String, JsonArray>> handler);

	/**
	 * Get subject list of one strcture for an external classe id or group id
	 * @param structureId The structure ID
	 * @param externalGroupId The external classe or group ID
	 * @param handler
     */
	void listSubjectsByGroup(String structureId, String externalGroupId,
	                               Handler<Either<String, JsonArray>> handler);

	void initStructure(String structureId, JsonObject conf, Handler<Either<String,JsonObject>> handler);

	void classesMapping(String structureId, Handler<Either<String,JsonObject>> handler);

	void updateClassesMapping(String structureId, JsonObject mapping, Handler<Either<String,JsonObject>> handler);

	void importTimetable(String structureId, String path, String domain, String acceptLanguage,
			Handler<Either<JsonObject,JsonObject>> handler);

}
