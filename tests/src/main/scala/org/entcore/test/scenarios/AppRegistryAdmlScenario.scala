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

import net.minidev.json.{JSONValue, JSONObject, JSONArray}
import scala.collection.JavaConverters._
import org.entcore.test.auth.Authenticate

object AppRegistryAdmlScenario {

  lazy val now = System.currentTimeMillis()

  val scn =
    Authenticate.authenticateUser("${teacherLogin}", "blipblop")
      .exec(http("Create external application adml")
      .post("""/appregistry/application/external""")
      .header("Content-Type", "application/json")
      .body(StringBody("""{"grantType" : "authorization_code", "name":"testadml""" + now +
      """", "secret":"clientSecret", "address" : "http://localhost", "scope" : "userinfo"}"""))
      .check(status.is(401)))
      .exec(http("Create external application")
      .post("""/appregistry/application/external?structureId=${schoolId}""")
      .header("Content-Type", "application/json")
      .body(StringBody("""{"grantType" : "authorization_code", "name":"testadml""" + now +
      """", "secret":"clientSecret", "address" : "http://localhost"}"""))
      .check(status.is(201), jsonPath("$.id").find.saveAs("app-adml-id")))
      .exec(http("Create external application twice")
      .post("""/appregistry/application/external?structureId=${schoolId}""")
      .header("Content-Type", "application/json")
      .body(StringBody("""{"grantType" : "authorization_code", "name":"testadml""" + now +
      """", "secret":"clientSecret", "address" : "http://localhost", "scope" : "userinfo"}"""))
      .check(status.is(400)))
      .exec(http("Update external application")
      .put("""/appregistry/application/conf/${app-adml-id}""")
      .header("Content-Type", "application/json")
      .body(StringBody("""{"scope" : "userinfo"}"""))
      .check(status.is(200), jsonPath("$.id").find.exists))
      .exec(http("Find workflow habilitations")
      .get("""/appregistry/applications/actions?actionType=WORKFLOW""")
      .check(status.is(401)))
      .exec(http("Find workflow habilitations")
      .get("""/appregistry/applications/actions?actionType=WORKFLOW&structureId=${schoolId}""")
      .check(status.is(200),
        bodyString.find.transformOption(_.map{res =>
          val json = JSONValue.parse(res).asInstanceOf[JSONArray]
          json.asScala.foldLeft[List[List[String]]](Nil){(acc, c) =>
            val app = c.asInstanceOf[JSONObject]
            lazy val actions: List[String] = app.get("actions").asInstanceOf[JSONArray].asScala.toList.map(
              _.asInstanceOf[JSONArray].get(0).asInstanceOf[String]
            )
            if (("testadml" + now) == app.get("name").asInstanceOf[String]) {
              val twt = List("testadml" + now, actions.mkString("\",\""))
              twt :: acc
            } else {
              acc
            }
          }
        }).saveAs("roles")))
      .foreach("${roles}", "role") {
      exec(http("Create role ${role(0)}")
        .post("""/appregistry/role""")
        .header("Content-Type", "application/json")
        .body(StringBody("""{"role":"${role(0)}","actions":["${role(1)}"]}"""))
        .check(status.is(401)))
        .exec(http("Create role ${role(0)}")
        .post("""/appregistry/role?structureId=${schoolId}""")
        .header("Content-Type", "application/json")
        .body(StringBody("""{"role":"${role(0)}","actions":["${role(1)}"]}"""))
        .check(status.is(201)))
        .exec(http("Create role ${role(0)} twice")
        .post("""/appregistry/role?structureId=${schoolId}""")
        .header("Content-Type", "application/json")
        .body(StringBody("""{"role":"${role(0)}","actions":["${role(1)}"]}"""))
        .check(status.is(409)))
    }

      .exec(http("Find roles with actions")
      .get("""/appregistry/roles/actions""")
      .check(status.is(401)))
      .exec(http("Find roles with actions")
      .get("""/appregistry/roles/actions?structureId=${schoolId}""")
      .check(status.is(200)))
      .exec(http("Find roles")
      .get("""/appregistry/roles""")
      .check(status.is(401)))
      .exec(http("Find roles")
      .get("""/appregistry/roles?structureId=${schoolId}""")
      .check(status.is(200),
        bodyString.find.transformOption(_.map{res =>
          val json = JSONValue.parse(res).asInstanceOf[JSONArray]
          json.asScala.foldLeft[List[List[String]]](List(Nil, Nil)){(acc, c) =>
            val app = c.asInstanceOf[JSONObject]
            if (app.get("name").asInstanceOf[String].contains("testadml" + now)) {
              List(app.get("id").asInstanceOf[String] :: acc.head, acc.last)
            } else if (app.get("name").asInstanceOf[String].contains("testadml" + now)) {
              List(acc.head, app.get("id").asInstanceOf[String] :: acc.last)
            } else {
              acc
            }
          }.map(_.mkString("\",\""))
        }).saveAs("rolesIds")))
      .exec(http("Find profil groups with roles")
      .get("""/appregistry/groups/roles?structureId=${schoolId}""")
      .check(status.is(200),
        bodyString.find.transformOption(_.map{res =>
          val json = JSONValue.parse(res).asInstanceOf[JSONArray]
          json.asScala.foldLeft[List[String]](List("","")){(acc, c) =>
            val app = c.asInstanceOf[JSONObject]
            app.get("name").asInstanceOf[String] match {
              case "Enseignants du groupe CM2." => List(app.get("id").asInstanceOf[String], acc.last)
              case "Élèves du groupe CM2." => List(acc.head, app.get("id").asInstanceOf[String])
              case _ => acc
            }
          }
        }).saveAs("pgIds")))
      .exec(http("Link teacher profil groups with roles")
      .post("""/appregistry/authorize/group""")
      .header("Content-Type", "application/json")
      .body(StringBody("""{"groupId":"${pgIds(0)}", "roleIds":["${rolesIds(0)}"]}"""))
      .check(status.is(200)))
}
