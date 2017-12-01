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
import java.net.URLEncoder

object CasScenario {

	val scn = exec(http("Login cas")
    .get("""/cas/login?service=http%3A%2F%2Fperdu.com""")
    .check(status.is(302),
      header("Location").find.is("/auth/login?callback=%2Fcas%2Flogin%3Fservice%3Dhttp%253A%252F%252Fperdu.com")))
    .exec(http("User login for Cas")
    .post("""/auth/login""")
    .formParam("""email""", """${teacherLogin}""")
    .formParam("""password""", """blipblop""")
    .formParam("""callBack""", "%2Fcas%2Flogin%3Fservice%3Dhttp%253A%252F%252Fperdu.com")
    .check(status.is(302)))
    .exec(http("Login cas")
    .get("""/cas/login?service=http%3A%2F%2Fperdu.com""")
    .check(status.is(302),
      header("Location").find.transformOption(_.map{l => println(l);l.substring(l.indexOf("=")+1)}).exists.saveAs("casTicket")))
    .exec(http("Validate ticket")
    .get("""/cas/serviceValidate?service=http%3A%2F%2Fperdu.com&ticket=${casTicket}""")
    .check(status.is(200), bodyString.find.transformOption(_.map(_.contains("julie.devost"))).is(true)))
    .exec(http("Logout teacher user")
    .get("""/auth/logout""")
    .check(status.is(302)))
    .exec(http("Validate ticket")
    .get("""/cas/serviceValidate?service=http%3A%2F%2Fperdu.com&ticket=${casTicket}""")
    .check(status.is(200), bodyString.find.transformOption(_.map(_.contains("INVALID_TICKET"))).is(true)))

}
