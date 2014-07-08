package org.entcore.test.appregistry

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import bootstrap._
import net.minidev.json.{JSONArray, JSONObject, JSONValue}
import scala.collection.JavaConverters._
import org.entcore.test.auth.Authenticate

object Role {

  def createAndSetRole(application: String) = {
    val now = System.currentTimeMillis()
    Authenticate.authenticateAdmin
    .exec(http("Find workflow habilitations")
      .get("""/appregistry/applications/actions?actionType=WORKFLOW""")
      .check(status.is(200),
        bodyString.find.transform(_.map{res =>
          val json = JSONValue.parse(res).asInstanceOf[JSONArray]
          json.asScala.foldLeft[List[List[String]]](Nil){(acc, c) =>
            val app = c.asInstanceOf[JSONObject]
            lazy val actions: List[String] = app.get("actions").asInstanceOf[JSONArray].asScala.toList.map(
              _.asInstanceOf[JSONArray].get(0).asInstanceOf[String]
            )
            if (app.get("name").asInstanceOf[String] == application) {
                val twt = List(application + "-all-" + now, actions.mkString("\",\""))
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
        .check(status.is(201)))
    }
      .exec(http("Find roles with actions")
      .get("""/appregistry/roles/actions""")
      .check(status.is(200)))
      .exec(http("Find roles")
      .get("""/appregistry/roles""")
      .check(status.is(200),
        bodyString.find.transform(_.map{res =>
          val json = JSONValue.parse(res).asInstanceOf[JSONArray]
          json.asScala.foldLeft[List[String]](Nil){(acc, c) =>
            val app = c.asInstanceOf[JSONObject]
              app.get("id").asInstanceOf[String] :: acc
          }.mkString("\",\"")
        }).saveAs("rolesIds")))
      .exec(http("Find profil groups with roles")
      .get("""/appregistry/groups/roles?structureId=${schoolId}""")
      .check(status.is(200),
        bodyString.find.transform(_.map{res =>
          val json = JSONValue.parse(res).asInstanceOf[JSONArray]
          json.asScala.foldLeft[List[String]](Nil){(acc, c) =>
            val app = c.asInstanceOf[JSONObject]
            app.get("id").asInstanceOf[String] :: acc
          }
        }).saveAs("profilGroupIds")))
      .foreach("${profilGroupIds}", "profilGroupId") {
      exec(http("Link profil groups with roles")
      .post("""/appregistry/authorize/group?schoolId=${schoolId}""")
      .header("Content-Type", "application/json")
      .body(StringBody("""{"groupId":"${profilGroupId}", "roleIds":["${rolesIds}"]}"""))
      .check(status.is(200)))
    }
  }

}
