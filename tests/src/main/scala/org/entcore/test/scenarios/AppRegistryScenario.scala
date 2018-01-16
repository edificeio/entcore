package org.entcore.test.scenarios

import io.gatling.core.Predef._
import io.gatling.http.Predef._

import net.minidev.json.{JSONValue, JSONObject, JSONArray}
import scala.collection.JavaConverters._

object AppRegistryScenario {

  lazy val now = System.currentTimeMillis()

	val scn = exec(http("Get app-registry view")
			.get("""/appregistry/admin-console""")
		.check(status.is(200)))
		.exec(http("Find workflow habilitations")
			.get("""/appregistry/applications/actions?actionType=WORKFLOW""")
		.check(status.is(200),
      bodyString.find.transformOption(_.map{res =>
        val json = JSONValue.parse(res).asInstanceOf[JSONArray]
        json.asScala.foldLeft[List[List[String]]](Nil){(acc, c) =>
          val app = c.asInstanceOf[JSONObject]
          lazy val actions: List[String] = app.get("actions").asInstanceOf[JSONArray].asScala.toList.map(
            _.asInstanceOf[JSONArray].get(0).asInstanceOf[String]
          )
          app.get("name").asInstanceOf[String] match {
            case "Espace documentaire" =>
              val twt = List("workspace-enseignants-" + now, actions.mkString("\",\""))
              val fa = actions.filterNot(action =>
                action.toLowerCase.contains("share") || action.toLowerCase.contains("rack"))
              val tws = List("workspace-eleves-" + now, fa.mkString("\",\""))
              twt :: tws :: acc
            case "Messagerie" =>
              val tmt = List("conversation-enseignants-" + now, actions.mkString("\",\""))
              val tms = List("conversation-eleves-" + now, actions.mkString("\",\""))
              tmt :: tms :: acc
            case "Archive" =>
              val tat = List("archive-enseignants-" + now, actions.mkString("\",\""))
              val tas = List("archive-eleves-" + now, actions.mkString("\",\""))
              tat :: tas :: acc
            case _ => acc
          }
        }
      }).saveAs("roles")))
    .foreach("${roles}", "role") {
      exec(http("Create role ${role(0)}")
        .post("""/appregistry/role""")
        .header("Content-Type", "application/json")
        .body(StringBody("""{"role":"${role(0)}","actions":["${role(1)}"]}"""))
        .check(status.is(201)))
      .exec(http("Create role ${role(0)} twice")
        .post("""/appregistry/role""")
        .header("Content-Type", "application/json")
        .body(StringBody("""{"role":"${role(0)}","actions":["${role(1)}"]}"""))
        .check(status.is(409)))
    }
    .exec{session =>
      val roles = session("roles").as[List[List[String]]]
      session.set("testRole", roles.head)
    }
    .exec(http("Create role ${testRole(0)}-test")
    .post("""/appregistry/role""")
    .header("Content-Type", "application/json")
    .body(StringBody("""{"role":"${testRole(0)}-test","actions":["${testRole(1)}"]}"""))
    .check(status.is(201), jsonPath("$.id").find.saveAs("test-id")))
    .exec(http("Update role ${testRole(0)}-test")
    .put("""/appregistry/role/${test-id}""")
    .header("Content-Type", "application/json")
    .body(StringBody("""{"role":"${testRole(0)}-bla-test"}"""))
    .check(status.is(200), jsonPath("$.id").find.exists))
//    .exec(http("Delete role ${testRole(0)}-test")
//    .delete("/appregistry/role/${test-id}")
//    .check(status.is(204)))
    .exec(http("Find roles with actions")
      .get("""/appregistry/roles/actions""")
      .check(status.is(200)))
    .exec(http("Find roles")
      .get("""/appregistry/roles""")
      .check(status.is(200),
        bodyString.find.transformOption(_.map{res =>
          val json = JSONValue.parse(res).asInstanceOf[JSONArray]
          json.asScala.foldLeft[List[List[String]]](List(Nil, Nil)){(acc, c) =>
            val app = c.asInstanceOf[JSONObject]
            if (app.get("name").asInstanceOf[String].contains("enseignants-" + now)) {
              List(app.get("id").asInstanceOf[String] :: acc.head, acc.last)
            } else if (app.get("name").asInstanceOf[String].contains("eleves-" + now)) {
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
              case "Tous les enseignants du groupe Ecole primaire Emile Zola." | "Tous les enseignants du groupe E.Zola renamed." => List(app.get("id").asInstanceOf[String], acc.last)
              case "Tous les élèves du groupe Ecole primaire Emile Zola." | "Tous les élèves du groupe E.Zola renamed." => List(acc.head, app.get("id").asInstanceOf[String])
              case _ => acc
            }
          }
        }).saveAs("profilGroupIds")))
    .exec(http("Link teacher profil groups with roles")
      .post("""/appregistry/authorize/group?schoolId=${schoolId}""")
      .header("Content-Type", "application/json")
      .body(StringBody("""{"groupId":"${profilGroupIds(0)}", "roleIds":["${rolesIds(0)}"]}"""))
      .check(status.is(200)))
    .exec(http("Link student profil groups with roles")
      .post("""/appregistry/authorize/group?schoolId=${schoolId}""")
      .header("Content-Type", "application/json")
      .body(StringBody("""{"groupId":"${profilGroupIds(1)}", "roleIds":["${rolesIds(1)}"]}"""))
      .check(status.is(200)))
    .exec(http("Create external application with additionnal parameter")
      .post("""/appregistry/application/external""")
      .header("Content-Type", "application/json")
      .body(StringBody("""{"grantType" : "authorization_code", "name":"test""" + now +
      """", "secret":"clientSecret", "address" : "http://localhost", "bla":"bla"}"""))
      .check(status.is(400)))
    .exec(http("Create external application with invalid parameter")
      .post("""/appregistry/application/external""")
      .header("Content-Type", "application/json")
      .body(StringBody("""{"grantType" : "authorization_code", "name":"test""" + now +
      """", "secret":"clientSecret", "address" : ["http://localhost"]}"""))
      .check(status.is(400)))
    .exec(http("Create external application with missing parameter")
      .post("""/appregistry/application/external""")
      .header("Content-Type", "application/json")
      .body(StringBody("""{"grantType" : "authorization_code",
      "secret":"clientSecret", "address" : "http://localhost"}"""))
      .check(status.is(400)))
    .exec(http("Create external application")
    .post("""/appregistry/application/external?structureId=${schoolId}""")
      .header("Content-Type", "application/json")
      .body(StringBody("""{"grantType" : "authorization_code", "name":"test""" + now +
          """", "secret":"clientSecret", "address" : "http://localhost"}"""))
      .check(status.is(201), jsonPath("$.id").find.saveAs("app-id")))
    .exec(http("Create external applications with client_credentials")
    .post("""/appregistry/application/external?structureId=${schoolId}""")
      .header("Content-Type", "application/json")
      .body(StringBody("""{"grantType" : "client_credentials", "name":"MyExternalApp""" + now +
          """", "secret":"clientSecret",  "address" : "http://localhost", "scope" : "org.entcore.timeline.controllers.TimelineController|publish"}"""))
      .check(status.is(201)))
    .exec(http("Create external application twice")
      .post("""/appregistry/application/external?structureId=${schoolId}""")
      .header("Content-Type", "application/json")
      .body(StringBody("""{"grantType" : "authorization_code", "name":"test""" + now +
        """", "secret":"clientSecret", "address" : "http://localhost", "scope" : "userinfo"}"""))
      .check(status.is(400)))
    .exec(http("Update external application")
      .put("""/appregistry/application/conf/${app-id}""")
      .header("Content-Type", "application/json")
      .body(StringBody("""{"scope" : "userinfo"}"""))
      .check(status.is(200), jsonPath("$.id").find.exists))
}
