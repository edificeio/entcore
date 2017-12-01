package org.entcore.test.load

import io.gatling.core.Predef._
import io.gatling.http.Predef._

import net.minidev.json.{JSONArray, JSONObject, JSONValue}
import scala.collection.JavaConverters._
import scala.collection.mutable.ArrayBuffer

class ConfigureSimulation extends Simulation {

	val httpProtocol = http
		.baseURL("http://one")
		.acceptHeader("*/*")
		.acceptEncodingHeader("gzip, deflate")
		.acceptLanguageHeader("fr-fr,pt-br;q=0.8,en-us;q=0.6,fr;q=0.4,en;q=0.2")
		.userAgentHeader("Mozilla/5.0 (X11; Linux x86_64; rv:24.0) Gecko/20140319 Firefox/24.0 Iceweasel/24.4.0")

	val scn = scenario("Configure plateform")
		.exec(Auth.login("tom.mate", "password"))
		.pause(1)
    .exec(http("List Schools")
    .get("""/directory/api/ecole""")
    .check(status.is(200), jsonPath("$.status").is("ok"),
      jsonPath("$.result..id").findAll.saveAs("schoolsIds"),
      jsonPath("$.result..name").findAll.saveAs("schoolsNames")))
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
              val r = List("workspace-all", actions.mkString("\",\""))
              r :: acc
            case "Messagerie" =>
              val r = List("conversation-all", actions.mkString("\",\""))
              r :: acc
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
    }
    .exec(http("Find roles")
    .get("""/appregistry/roles""")
    .check(status.is(200),
      bodyString.find.transformOption(_.map{res =>
        val json = JSONValue.parse(res).asInstanceOf[JSONArray]
        json.asScala.foldLeft[List[String]](Nil){(acc, c) =>
          val app = c.asInstanceOf[JSONObject]
          app.get("id").asInstanceOf[String] :: acc
        }.mkString("\",\"")
      }).saveAs("rolesIds")))
    .foreach("${schoolsIds}", "schoolId") {
    exec(http("Apply default communication rules")
      .put("/communication/rules/${schoolId}")
      .header("Content-Length", "0"))
      .pause(5)
    .exec(http("Find profile groups with roles")
    .get("""/appregistry/groups/roles?structureId=${schoolId}""")
    .check(status.is(200),
      bodyString.find.transformOption(_.map{res =>
        val json = JSONValue.parse(res).asInstanceOf[JSONArray]
        json.asScala.foldLeft[List[(String, String)]](Nil){(acc, c) =>
          val app = c.asInstanceOf[JSONObject]
          (app.get("id").asInstanceOf[String], app.get("name").asInstanceOf[String]) :: acc
        }
      }).saveAs("profileGroups")))
      .exec{session =>
        val schools = session("schoolsNames").as[ArrayBuffer[String]]
        val pg = session("profileGroups").as[List[(String, String)]]
        val pgu = pg.foldLeft[List[String]](Nil){(acc, c) =>
          val s = c._2.substring(0, c._2.lastIndexOf('-'))
          if (schools.contains(s)) {
            c._1::acc
          } else {
            acc
          }
        }
        session.set("profilGroupIds", pgu)
      }
      .foreach("${profilGroupIds}", "profilGroupId") {
      exec(http("Link profil groups with roles")
        .post("""/appregistry/authorize/group?schoolId=${schoolId}""")
        .header("Content-Type", "application/json")
        .body(StringBody("""{"groupId":"${profilGroupId}", "roleIds":["${rolesIds}"]}"""))
        .check(status.is(200)))
      }
    }

	setUp(scn.inject(atOnceUsers(1))).protocols(httpProtocol)
}
