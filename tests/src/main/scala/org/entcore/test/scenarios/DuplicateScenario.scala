/*
 * Copyright © WebServices pour l'Éducation, 2015
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

import scala.collection.JavaConverters._
import net.minidev.json.{JSONObject, JSONArray, JSONValue}
import org.entcore.test.auth.Authenticate

object DuplicateScenario {

  val names = List("Devot", "Devost", "PIREZ", "PIRES", "MonjeUa", "MonjeUa")

  val scn = Authenticate.authenticateAdmin
    .exec(http("Create manual teacher")
    .post("""/directory/api/user""")
    .formParam("""classId""", """${classId}""")
    .formParam("""lastname""", "Devot")
    .formParam("""firstname""", """Julye""")
    .formParam("""type""", """Teacher""")
    .check(status.is(200)))
    .exec(http("Create manual teacher")
    .post("""/directory/api/user""")
    .formParam("""classId""", """${classId}""")
    .formParam("""lastname""", "PIREZ")
    .formParam("""firstname""", """Rachel""")
    .formParam("""type""", """Teacher""")
    .check(status.is(200)))
    .exec(http("Create manual student")
    .post("""/directory/api/user""")
    .formParam("""classId""", """${classId}""")
    .formParam("""lastname""", "MonjeUa")
    .formParam("""firstname""", """Lundy""")
    .formParam("""birthDate""", """1970-01-01""")
    .formParam("""type""", """Student""")
    .check(status.is(200)))
    .exec(http("Create manual parent")
    .post("""/directory/api/user""")
    .formParam("""classId""", """${classId}""")
    .formParam("""lastname""", "BLONDY")
    .formParam("""firstname""", """Astride""")
    .formParam("""type""", """Relative""")
    .formParam("""childrenIds""", """${childrenId}""")
    .check(status.is(200)))
    .pause(3)
    .exec(http("Create manual parent")
    .post("""/directory/api/user""")
    .formParam("""classId""", """${classId}""")
    .formParam("""lastname""", "FONTAINE-GOMES DE OLIVIER")
    .formParam("""firstname""", """Isabelle""")
    .formParam("""type""", """Relative""")
    .check(status.is(200)))
    .exec(http("Create manual Guest")
    .post("""/directory/api/user""")
    .formParam("""classId""", """${classId}""")
    .formParam("""lastname""", "TILMAN")
    .formParam("""firstname""", """LIZZYe""")
    .formParam("""type""", """Guest""")
    .check(status.is(200)))
    .exec(http("Create manual Guest")
    .post("""/directory/api/user""")
    .formParam("""structureId""", """${schoolId}""")
    .formParam("""lastname""", "Trur")
    .formParam("""firstname""", """Krysten""")
    .formParam("""type""", """Guest""")
    .check(status.is(200)))

    .exec(http("Mark duplicates")
    .post("""/directory/duplicates/mark""")
    .header("Content-Length", "0")
    .check(status.is(200)))
    .exec(http("List duplicates")
    .get("""/directory/duplicates""")
    .check(status.is(200), jsonPath("$").find.transformOption(_.map{ j =>
      JSONValue.parse(j).asInstanceOf[JSONArray].size()
    }).is(9)))

    .exec(Authenticate.authenticateUser("${teacherLogin}", "blipblop"))
    .exec(http("List duplicates")
    .get("""/directory/duplicates""")
    .check(status.is(401)))
    .exec(http("List duplicates")
    .get("""/directory/duplicates?structure=${parent-structure-id}""")
    .check(status.is(200), jsonPath("$").find.transformOption(_.map{ j =>
      JSONValue.parse(j).asInstanceOf[JSONArray].size()
    }).is(0)))
    .exec(http("List duplicates")
    .get("""/directory/duplicates?structure=${parent-structure-id}&inherit=true""")
    .check(status.is(200), jsonPath("$").find.transformOption(_.map{ j =>
      JSONValue.parse(j).asInstanceOf[JSONArray].size()}).is(9),
      jsonPath("$").find.transformOption(_.map{ j =>
        JSONValue.parse(j).asInstanceOf[JSONArray].asScala.toList
          .filter{i =>
            val j = i.asInstanceOf[JSONObject]
            val user1 = j.get("user1").asInstanceOf[JSONObject]
            val user2 = j.get("user2").asInstanceOf[JSONObject]
            names.contains(user2.get("lastName")) || names.contains(user1.get("lastName"))
          }
          .map{i =>
            val j = i.asInstanceOf[JSONObject]
            val user1 = j.get("user1").asInstanceOf[JSONObject]
            val user2 = j.get("user2").asInstanceOf[JSONObject]
            List(user1.get("id"), user1.get("lastName"), user2.get("id"), user2.get("lastName"))
          }
      }).saveAs("mergeDuplicates"),
      jsonPath("$").find.transformOption(_.map{ j =>
        JSONValue.parse(j).asInstanceOf[JSONArray].asScala.toList
          .filter{i =>
            val j = i.asInstanceOf[JSONObject]
            val user1 = j.get("user1").asInstanceOf[JSONObject]
            val user2 = j.get("user2").asInstanceOf[JSONObject]
            !names.contains(user2.get("lastName")) && !names.contains(user1.get("lastName"))
          }
          .map{i =>
            val j = i.asInstanceOf[JSONObject]
            List(j.get("user1").asInstanceOf[JSONObject].get("id"), j.get("user2").asInstanceOf[JSONObject].get("id"))
          }
      }).saveAs("ignoreDuplicates")))

    .foreach("${ignoreDuplicates}", "ignore") {
      exec(http("Ignore duplicate")
        .delete("""/directory/duplicate/ignore/${ignore(0)}/${ignore(1)}""")
        .check(status.is(200)))
    }
    .exec(http("List duplicates")
    .get("""/directory/duplicates?structure=${parent-structure-id}&inherit=true""")
    .check(status.is(200), jsonPath("$").find.transformOption(_.map{ j =>
      JSONValue.parse(j).asInstanceOf[JSONArray].size()
    }).is(1)))

    .foreach("${mergeDuplicates}", "merge") {
      doIf{session =>
        val lastName = session("merge").as[List[String]].lift(1).get
        lastName == "Devot" || lastName == "PIREZ"} {
          exec(http("Details")
            .get("""/directory/user/${merge(0)}""")
            .check(status.is(200),
              jsonPath("$.login").find.saveAs("dLogin"),
              jsonPath("$.activationCode").find.saveAs("dCode")))
        .exec(http("Activate account")
        .post("""/auth/activation""")
        .formParam("""login""", """${dLogin}""")
        .formParam("""activationCode""", """${dCode}""")
        .formParam("""password""", """blipblop""")
        .formParam("""confirmPassword""", """blipblop""")
        .formParam("""acceptCGU""", """true""")
        .check(status.is(302)))
      }
      .doIf{session =>
        val lastName = session("merge").as[List[String]].lift(3).get
        lastName == "Devot" || lastName == "PIREZ"} {
        exec(http("Details")
          .get("""/directory/user/${merge(2)}""")
          .check(status.is(200),
            jsonPath("$.login").find.saveAs("dLogin"),
            jsonPath("$.activationCode").find.saveAs("dCode")))
          .exec(http("Activate account")
          .post("""/auth/activation""")
          .formParam("""login""", """${dLogin}""")
          .formParam("""activationCode""", """${dCode}""")
          .formParam("""password""", """blipblop""")
          .formParam("""confirmPassword""", """blipblop""")
          .formParam("""acceptCGU""", """true""")
          .check(status.is(302)))
      }
        .doIf ("${merge(1)}", "MonjeUa") {
        exec(http("Details")
          .get("""/directory/user/${merge(0)}""")
          .check(status.is(200),
            jsonPath("$.login").find.saveAs("dLogin"),
            jsonPath("$.activationCode").find.saveAs("dCode")))
          .exec(http("Activate account")
          .post("""/auth/activation""")
          .formParam("""login""", """${dLogin}""")
          .formParam("""activationCode""", """${dCode}""")
          .formParam("""password""", """blipblop""")
          .formParam("""confirmPassword""", """blipblop""")
          .formParam("""acceptCGU""", """true""")
          .check(status.is(200)))
      }
      .doIf ("${merge(3)}", "MonjeUa") {
        exec(http("Details")
          .get("""/directory/user/${merge(2)}""")
          .check(status.is(200),
            jsonPath("$.login").find.saveAs("dLogin"),
            jsonPath("$.activationCode").find.saveAs("dCode")))
          .exec(http("Activate account")
          .post("""/auth/activation""")
          .formParam("""login""", """${dLogin}""")
          .formParam("""activationCode""", """${dCode}""")
          .formParam("""password""", """blipblop""")
          .formParam("""confirmPassword""", """blipblop""")
          .formParam("""acceptCGU""", """true""")
          .check(status.is(200)))
        }
      .doIfOrElse {session =>
        val lastName = session("merge").as[List[String]].lift(1).get
        lastName == "Devot" || lastName == "Devost" ||
          ((lastName == "PIREZ" || lastName == "PIRES" ) && session("rachelId").as[String].nonEmpty)
      } {
        exec(http("Merge duplicate")
          .put("""/directory/duplicate/merge/${merge(0)}/${merge(2)}""")
          .header("Content-Length", "0")
          .check(status.is(400)))
        .exec{session =>
          val cmc = session("countMergeConflict").asOption[Int].getOrElse(0)
          session.set("countMergeConflict", cmc + 1)
        }
      } {
        exec(http("Merge duplicate")
          .put("""/directory/duplicate/merge/${merge(0)}/${merge(2)}""")
          .header("Content-Length", "0")
          .check(status.is(200)))
      }
  }

//  .exec(http("List duplicates")
//  .get("""/directory/duplicates?structure=${parent-structure-id}&inherit=true""")
//  .check(status.is(200), jsonPath("$").find.transformOption(_.map{ j =>
//    JSONValue.parse(j).asInstanceOf[JSONArray].size()
//  }).is("${countMergeConflict}")))

  .exec(Authenticate.authenticateAdmin)
  .exec(http("Mark duplicates")
  .post("""/directory/duplicates/mark""")
  .header("Content-Length", "0")
  .check(status.is(200)))

//  .exec(http("List duplicates")
//  .get("""/directory/duplicates""")
//  .check(status.is(200), jsonPath("$").find.transformOption(_.map{ j =>
//    JSONValue.parse(j).asInstanceOf[JSONArray].size()}).is("${countMergeConflict}")))

}
