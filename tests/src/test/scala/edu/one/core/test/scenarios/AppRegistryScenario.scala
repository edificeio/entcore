package edu.one.core.test.scenarios

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import bootstrap._
import net.minidev.json.{JSONArray, JSONValue, JSONObject}
import scala.collection.JavaConverters._

object AppRegistryScenario {

  lazy val now = System.currentTimeMillis()

	val scn = exec(http("Get app-registry view")
			.get("""/appregistry/admin""")
		.check(status.is(200)))
		.exec(http("Find workflow habilitations")
			.get("""/appregistry/applications/actions?actionType=WORKFLOW""")
		.check(status.is(200), jsonPath("status").is("ok"),
      jsonPath("$.result").find.transform(_.map{res =>
        val json = JSONValue.parse(res).asInstanceOf[JSONObject]
        json.values.asScala.foldLeft[List[List[String]]](Nil){(acc, c) =>
          val app = c.asInstanceOf[JSONObject]
          lazy val actions: List[String] = app.get("actions").asInstanceOf[JSONArray].asScala.toList.map(
            _.asInstanceOf[JSONArray].get(0).asInstanceOf[String]
          )
          app.get("name").asInstanceOf[String] match {
            case "Blog" =>
              val tbt = List("blog-enseignants-" + now, actions.mkString(","))
              val fa = actions.filter(action => action.endsWith("list") || action.endsWith("blog"))
              val tbs = List("blog-eleves-" + now, fa.mkString(","))
              tbt :: tbs :: acc
            case "Espace documentaire" =>
              val twt = List("workspace-enseignants-" + now, actions.mkString(","))
              val fa = actions.filterNot(action =>
                action.toLowerCase.contains("share") || action.toLowerCase.contains("rack"))
              val tws = List("workspace-eleves-" + now, fa.mkString(","))
              twt :: tws :: acc
            case "Messagerie" =>
              val tmt = List("conversation-enseignants-" + now, actions.mkString(","))
              val tms = List("conversation-eleves-" + now, actions.mkString(","))
              tmt :: tms :: acc
            case _ => acc
          }
        }
      }).saveAs("roles")))
    .foreach("${roles}", "role") {
      exec(http("Create role ${role(0)}")
        .post("""/appregistry/role""")
        .param("""role""", """${role(0)}""")
        .param("""actions""", """${role(1)}""")
        .check(status.is(200), jsonPath("status").is("ok")))
    }
    .exec(http("Find roles with actions")
      .get("""/appregistry/roles/actions""")
      .check(status.is(200), jsonPath("status").is("ok")))
    .exec(http("Find roles")
      .get("""/appregistry/roles""")
      .check(status.is(200), jsonPath("status").is("ok"),
        jsonPath("$.result").find.transform(_.map{res =>
          val json = JSONValue.parse(res).asInstanceOf[JSONObject]
          json.values.asScala.foldLeft[List[List[String]]](List(Nil, Nil)){(acc, c) =>
            val app = c.asInstanceOf[JSONObject]
            if (app.get("name").asInstanceOf[String].contains("enseignants-" + now)) {
              List(app.get("id").asInstanceOf[String] :: acc.head, acc.last)
            } else if (app.get("name").asInstanceOf[String].contains("eleves-" + now)) {
              List(acc.head, app.get("id").asInstanceOf[String] :: acc.last)
            } else {
              acc
            }
          }
        }).saveAs("rolesIds")))
    .exec(http("Find profil groups with roles")
      .get("""/appregistry/groups/roles?schoolId=${schoolId}""")
      .check(status.is(200), jsonPath("status").is("ok"),
        jsonPath("$.result").find.transform(_.map{res =>
          val json = JSONValue.parse(res).asInstanceOf[JSONObject]
          json.values.asScala.foldLeft[List[String]](List("","")){(acc, c) =>
            val app = c.asInstanceOf[JSONObject]
            app.get("name").asInstanceOf[String] match {
              case "Ecole primaire Emile Zola_ENSEIGNANT" => List(app.get("id").asInstanceOf[String], acc.last)
              case "Ecole primaire Emile Zola_ELEVE" => List(acc.head, app.get("id").asInstanceOf[String])
              case _ => acc
            }
          }
        }).saveAs("profilGroupIds")))
    .exec(http("Link teacher profil groups with roles")
      .post("""/appregistry/authorize/group?schoolId=${schoolId}""")
      .param("""groupId""", """${profilGroupIds(0)}""")
      .multiValuedParam("""roleIds""", """${rolesIds(0)}""")
      .check(status.is(200), jsonPath("status").is("ok")))
    .exec(http("Link student profil groups with roles")
      .post("""/appregistry/authorize/group?schoolId=${schoolId}""")
      .param("""groupId""", """${profilGroupIds(1)}""")
      .multiValuedParam("""roleIds""", """${rolesIds(1)}""")
      .check(status.is(200), jsonPath("status").is("ok")))
    .exec(http("Create external applications")
    .post("""/appregistry/application/external""")
      .param("""grantType""", """authorization_code""")
      .param("""name""", "test" + now)
      .param("""secret""", "clientSecret")
      .check(status.is(200), jsonPath("status").is("ok")))

}
