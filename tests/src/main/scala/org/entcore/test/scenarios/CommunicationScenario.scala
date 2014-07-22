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
import org.entcore.test.auth.Authenticate
import net.minidev.json.{JSONObject, JSONArray, JSONValue}
import scala.collection.JavaConverters._

object CommunicationScenario {

  val scn = //Authenticate.authenticateAdmin

    // remove com rules
//    .exec(http("Remove all communication rules")
//      .delete("/communication/rules")
//      .check(status.is(200)))

   // .exec(Authenticate.logout)
    //.exec(Authenticate.authenticateUser("${teacherLogin}", "blipblop"))
    Authenticate.authenticateUser("${teacherLogin}", "blipblop")

    // List manageable groups
    .exec(http("List manageable groups")
      .get("/directory/group/admin/list")
      .check(status.is(200),
        bodyString.find.transform(_.map{res =>
          val json = JSONValue.parse(res).asInstanceOf[JSONArray]
          json.asScala.toList.filter(_.asInstanceOf[JSONObject].get("name") == "CM2-Teacher")
            .head.asInstanceOf[JSONObject].get("id")
        }).saveAs("com-group-0"),
        bodyString.find.transform(_.map{res =>
          val json = JSONValue.parse(res).asInstanceOf[JSONArray]
          json.asScala.toList.filter(_.asInstanceOf[JSONObject].get("name") == "CM1-Relative")
            .head.asInstanceOf[JSONObject].get("id")
        }).saveAs("com-group-1")))

    // Group communicate with nothing
    .exec(http("Group communicate with nothing")
      .get("/communication/group/${com-group-0}")
      .check(status.is(200), bodyString.find.transform(_.map(res => {
        val json = JSONValue.parse(res).asInstanceOf[JSONObject]
        json.get("communiqueWith").asInstanceOf[JSONArray].size().toString })).is("0")))

    // Add link between two groups
    .exec(http("Add link between two groups")
      .post("/communication/group/${com-group-0}/communique/${com-group-1}")
      .header("Content-Length", "0")
      .check(status.is(200), jsonPath("$.number").find.is("1")))

    // Add link inside group in both directions
    .exec(http("Add link inside group")
      .post("/communication/group/${com-group-0}")
      .header("Content-Length", "0")
      .check(status.is(200)))

    // Add link between relative and student in group
    .exec(http("Add link between relative and student without relative group")
      .post("/communication/relative/${com-group-0}")
      .header("Content-Length", "0")
      .check(status.is(200), jsonPath("$.number").find.is("0")))

    // Add link between relative and student in group
    .exec(http("Add link between relative and student")
      .post("/communication/relative/${com-group-1}")
      .header("Content-Length", "0")
      .check(status.is(200), jsonPath("$.number").find.not("0")))

    // Group communicate with one other group
    .exec(http("Group communicate with")
      .get("/communication/group/${com-group-0}")
      .check(status.is(200), jsonPath("$.communiqueWith[0].id").find.is("${com-group-1}"),
        jsonPath("$.communiqueUsers").find.is("BOTH")))

    .exec(http("Group communicate with")
      .get("/communication/group/${com-group-1}")
      .check(status.is(200), jsonPath("$.relativeCommuniqueStudent").find.is("BOTH")))

    // Remove link between two groups
    .exec(http("Remove link between two groups")
      .delete("/communication/group/${com-group-0}/communique/${com-group-1}")
      .check(status.is(200)))

    // Remove between group and users
    .exec(http("Remove between group and users")
      .delete("/communication/group/${com-group-0}?direction=INCOMING")
      .check(status.is(200)))

    // Remove link between relative and student in group
    .exec(http("Remove link between relative and student")
      .delete("/communication/relative/${com-group-1}?direction=OUTGOING")
      .check(status.is(200)))

    // Group communicate with one other group
    .exec(http("Group communicate with")
      .get("/communication/group/${com-group-0}")
      .check(status.is(200), bodyString.find.transform(_.map(res => {
        val json = JSONValue.parse(res).asInstanceOf[JSONObject]
        json.get("communiqueWith").asInstanceOf[JSONArray].size().toString })).is("0"),
        jsonPath("$.communiqueUsers").find.is("OUTGOING")))

    .exec(http("Group communicate with")
      .get("/communication/group/${com-group-1}")
      .check(status.is(200), jsonPath("$.relativeCommuniqueStudent").find.is("INCOMING")))

    // Remove between group and users
    .exec(http("Remove between group and users")
      .delete("/communication/group/${com-group-0}")
      .check(status.is(200)))

    // Remove link between relative and student in group
    .exec(http("Remove link between relative and student")
      .delete("/communication/relative/${com-group-1}")
      .check(status.is(200)))

    // Group communicate with one other group
    .exec(http("Group communicate with")
      .get("/communication/group/${com-group-0}")
      .check(status.is(200), bodyString.find.transform(_.map(res => {
      val json = JSONValue.parse(res).asInstanceOf[JSONObject]
      json.get("communiqueWith").asInstanceOf[JSONArray].size().toString })).is("0"),
        jsonPath("$.communiqueUsers").find.notExists))

    .exec(http("Group communicate with")
    .get("/communication/group/${com-group-1}")
      .check(status.is(200), jsonPath("$.relativeCommuniqueStudent").find.notExists))

    .exec(Authenticate.logout)
    .exec(Authenticate.authenticateUser("${studentLogin}", "blipblop"))

    .exec(http("Group communicate with bad user")
      .get("/communication/group/${com-group-0}")
      .check(status.is(401)))

    .exec(Authenticate.logout)
    .exec(Authenticate.authenticateAdmin)

    // Init defaut com rules
    .exec(http("Init default com rules")
      .put("/communication/init/rules")
      .header("Content-Type", "application/json")
      .body(StringBody("""{ "structures" : ["${schoolId}"] }"""))
      .check(status.is(200)))

    // Apply default com rules
    .exec(http("Apply default com rules")
      .put("/communication/rules/${schoolId}")
      .header("Content-Length", "0")
      .check(status.is(200)))

    .exec(Authenticate.logout)

}
