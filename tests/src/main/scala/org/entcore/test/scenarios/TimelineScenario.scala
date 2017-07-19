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

import com.sun.org.apache.xerces.internal.impl.dv.util.Base64

object TimelineScenario {

	val scn =
    exec(http("Get OAuth2 Client Credentials token")
      .post("""/auth/oauth2/token""")
      .header("Authorization", "Basic " +
        Base64.encode(("MyExternalApp" + AppRegistryScenario.now + ":clientSecret").getBytes("UTF-8")))
      .header("Content-Type", "application/x-www-form-urlencoded")
      .header("Accept", "application/json; charset=UTF-8")
      .formParam("""grant_type""", """client_credentials""")
      .formParam("""scope""", "org.entcore.timeline.controllers.TimelineController|publish")
      .check(status.is(200), jsonPath("$.token_type").is("Bearer"),
        jsonPath("$.access_token").find.saveAs("clientCredentialsToken")))
    .exec(http("MyExternalApp publish on Timeline")
       .post("/timeline/publish")
       .header("Authorization", "Bearer ${clientCredentialsToken}")
       .body(StringBody(
         """{"message":"Lorem ipsum", "recipients" : [
             {"userId" : "${teacherId}", "unread" : 1},
             {"userId" : "${studentId}", "unread" : 1}
             ],
             "type" : "MY_EXTERNAL_APP_EVENT",
             "params": {}
         }"""))
       .check(status.is(201)))
    .exec(http("MyExternalApp list events on Timeline")
    .get("/timeline/lastNotifications")
       .header("Authorization", "Bearer ${clientCredentialsToken}")
       .check(status.is(401)))
    .exec(http("Login teacher")
    .post("""/auth/login""")
      .formParam("""email""", """${teacherLogin}""")
      .formParam("""password""", """blipblop""")
      .check(status.is(302)))
    .exec(http("MyExternalApp list events on Timeline")
    .get("/timeline/lastNotifications")
       .check(status.is(200), jsonPath("$.results[0].message").find.is("Lorem ipsum")))
    .exec(http("Logout teacher user")
      .get("""/auth/logout""")
      .check(status.is(302)))

}
