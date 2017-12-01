/*
 * Copyright © WebServices pour l'Éducation, 2014
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

package org.entcore.test.scenarios

import io.gatling.core.Predef._
import io.gatling.http.Predef._

import net.minidev.json.{JSONArray, JSONValue}

object QuotaScenario {

	val scn =
    exec(http("Get quota and storage")
    .get("""/workspace/quota/user/${studentId}""")
    .check(status.is(200), jsonPath("$.quota").find.transformOption(_.map(res =>
      104857600l == res.toLong || 512144000l == res.toLong)).is(true),
      jsonPath("$.storage").find.is("136725")))

    .exec(http("Get quota and storage")
    .get("""/workspace/quota/user/${teacherId}""")
    .check(status.is(401)))

    .exec(http("Get structure quota and storage")
    .get("""/workspace/quota/structure/${schoolId}""")
    .check(status.is(401)))

    .exec(http("Get global quota and storage")
    .get("""/workspace/quota/global""")
    .check(status.is(401)))

    .exec(http("Update default quota")
    .put("""/workspace/quota/default/Teacher""")
    .header("Content-Type", "application/json")
    .body(StringBody("""{"defaultQuota": 272144000, "maxQuota" : 1073741824}"""))
    .check(status.is(401)))

    .exec(http("Update quota")
    .put("""/workspace/quota""")
    .header("Content-Type", "application/json")
    .body(StringBody("""{"users" : ["${teacherId}","${studentId}"], "quota" : 10073741824}"""))
    .check(status.is(401)))

    .exec(http("Login teacher")
    .post("""/auth/login""")
    .formParam("""email""", """${teacherLogin}""")
    .formParam("""password""", """blipblop""")
    .check(status.is(302)))

    .exec(http("Get quota and storage")
    .get("""/workspace/quota/user/${studentId}""")
    .check(status.is(200), jsonPath("$.quota").find.transformOption(_.map(res =>
    104857600l == res.toLong || 512144000l == res.toLong)).is(true),
      jsonPath("$.storage").find.is("136725")))

    .exec(http("Get quota and storage")
    .get("""/workspace/quota/user/${teacherId}""")
    .check(status.is(200), jsonPath("$.quota").find.transformOption(_.map(res =>
      104857600l == res.toLong || 1073741824l == res.toLong)).is(true),
      jsonPath("$.storage").find.lessThan("1048576")))

    .exec(http("Get structure quota and storage")
    .get("""/workspace/quota/structure/${schoolId}""")
    .check(status.is(200), jsonPath("$.quota").find.exists, jsonPath("$.storage").find.exists))

    .exec(http("Get global quota and storage")
    .get("""/workspace/quota/global""")
    .check(status.is(401)))

    .exec(http("Update default quota")
    .put("""/workspace/quota/default/Teacher""")
    .header("Content-Type", "application/json")
    .body(StringBody("""{"defaultQuota": 272144000, "maxQuota" : 1073741824}"""))
    .check(status.is(401)))

    .exec(http("Update quota")
    .put("""/workspace/quota""")
    .header("Content-Type", "application/json")
    .body(StringBody("""{"users" : ["${teacherId}","${studentId}"], "quota" : 1073741823}"""))
    .check(status.is(200), bodyString.find.transformOption(_.map(res =>
      JSONValue.parse(res).asInstanceOf[JSONArray].size())).is(2)))

    .exec(http("Login Admin")
    .post("""/auth/login""")
    .formParam("""email""", """tom.mate""")
    .formParam("""password""", """password""")
    .check(status.is(302)))

    .exec(http("Get quota and storage")
    .get("""/workspace/quota/user/${studentId}""")
    .check(status.is(200), jsonPath("$.quota").find.is("1073741823"),
      jsonPath("$.storage").find.is("136725")))

    .exec(http("Get quota and storage")
    .get("""/workspace/quota/user/${teacherId}""")
    .check(status.is(200), jsonPath("$.quota").find.is("1073741823"),
      jsonPath("$.storage").find.lessThan("1048576")))

    .exec(http("Get structure quota and storage")
    .get("""/workspace/quota/structure/${schoolId}""")
    .check(status.is(200), jsonPath("$.quota").find.exists, jsonPath("$.storage").find.exists))

    .exec(http("Get global quota and storage")
    .get("""/workspace/quota/global""")
    .check(status.is(200), jsonPath("$.quota").find.exists, jsonPath("$.storage").find.exists))

    .exec(http("Update default quota")
    .put("""/workspace/quota/default/Teacher""")
    .header("Content-Type", "application/json")
    .body(StringBody("""{"defaultQuota": 1073741824, "maxQuota" : 20073741824}"""))
    .check(status.is(200), jsonPath("$.id").find.exists))

    .exec(http("Update default quota")
    .put("""/workspace/quota/default/Student""")
    .header("Content-Type", "application/json")
    .body(StringBody("""{"defaultQuota": 512144000, "maxQuota" : 5073741824}"""))
    .check(status.is(200), jsonPath("$.id").find.exists))

    .exec(http("Update quota")
    .put("""/workspace/quota""")
    .header("Content-Type", "application/json")
    .body(StringBody("""{"users" : ["${teacherId}","${studentId}"], "quota" : 10073741824}"""))
    .check(status.is(200), bodyString.find.transformOption(_.map(res =>
      JSONValue.parse(res).asInstanceOf[JSONArray].size())).is(1)))

}
