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

package org.entcore.feeder.timetable.edt;

import io.vertx.core.json.JsonObject;

public interface EDTReader
{
  abstract void addProfesseur(JsonObject currentEntity);
  abstract void addPersonnel(JsonObject currentEntity);
  abstract void addCourse(JsonObject currentEntity);
  abstract void addSubject(JsonObject currentEntity);
  abstract void addEleve(JsonObject currentEntity);
  abstract void addResponsable(JsonObject currentEntity);
  abstract void addClasse(JsonObject currentEntity);
  abstract void addGroup(JsonObject currentEntity);
  abstract void addRoom(JsonObject currentEntity);
  abstract void addEquipment(JsonObject currentEntity);
  abstract void initSchedule(JsonObject currentEntity);
  abstract void initSchoolYear(JsonObject currentEntity);
}