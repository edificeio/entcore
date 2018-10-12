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
	void listCoursesBetweenTwoDates(String structureId, String teacherId, List<String> groupNames, String begin, String end, Handler<Either<String,JsonArray>> handler);


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
