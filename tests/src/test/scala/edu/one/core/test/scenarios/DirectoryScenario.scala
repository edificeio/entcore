package edu.one.core.test.scenarios

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import bootstrap._
import net.minidev.json.{JSONValue, JSONObject}
import scala.collection.JavaConverters._

object DirectoryScenario {

	val scn = exec(http("Get admin page")
			.get("""/admin""")
		.check(status.is(302)))
		.exec(http("Authenticate admin user")
			.post("""/auth/login""")
			.param("""callBack""", """http%3A%2F%2Flocalhost%3A8080%2Fadmin""")
			.param("""email""", """tom.mate""")
			.param("""password""", """password""")
		.check(status.is(302)))
		.exec(http("Get admin page")
			.get("""/directory/admin""")
		.check(status.is(200)))
		.exec(http("List Schools")
			.get("""/directory/api/ecole""")
		.check(status.is(200), jsonPath("status").is("ok"),
      jsonPath("$.result.0.id").find.saveAs("schoolId")))
		.exec(http("List classes")
			.get("""/directory/api/classes?id=${schoolId}""")
		.check(status.is(200), jsonPath("status").is("ok"),
      jsonPath("$.result.0.classId").find.saveAs("classId")))
    .exec(http("List students in class")
      .get("""/directory/api/personnes?id=${classId}&type=ELEVE""")
    .check(status.is(200), jsonPath("status").is("ok"),
      jsonPath("$.result.0.userId").find.saveAs("childrenId")))
		.exec(http("Create manual teacher")
			.post("""/directory/api/user""")
			.param("""classId""", """${classId}""")
			.param("""lastname""", "Devost")
			.param("""firstname""", """Julie""")
			.param("""type""", """ENSEIGNANT""")
		.check(status.is(200), jsonPath("status").is("ok")))
    .exec(http("Create manual student")
      .post("""/directory/api/user""")
      .param("""classId""", """${classId}""")
      .param("""lastname""", "Monjeau")
      .param("""firstname""", """Lundy""")
      .param("""type""", """ELEVE""")
    .check(status.is(200), jsonPath("status").is("ok")))
    .exec(http("Create manual parent")
      .post("""/directory/api/user""")
      .param("""classId""", """${classId}""")
      .param("""lastname""", "Bondy")
      .param("""firstname""", """Astrid""")
      .param("""type""", """PERSRELELEVE""")
      .param("""childrenIds""", """${childrenId}""")
    .check(status.is(200), jsonPath("status").is("ok")))
    .exec(http("List persons in class")
      .get("""/directory/api/personnes?id=${classId}""")
      .check(status.is(200), jsonPath("status").is("ok"),
      jsonPath("$.result").find.transform(_.map(res => {
        val json = JSONValue.parse(res).asInstanceOf[JSONObject]
        json.values.asScala.foldLeft[List[(String, String)]](Nil){(acc, c) =>
          val user = c.asInstanceOf[JSONObject]
          user.get("lastName").asInstanceOf[String] match {
            case "Devost" | "Monjeau" if !user.get("code").asInstanceOf[String].isEmpty() =>
              (user.get("type").asInstanceOf[String], user.get("userId").asInstanceOf[String]) :: acc
            case _ => acc
          }
        }.toMap
      })).saveAs("createdUserIds")))
    .exec{session =>
      val uIds = session("createdUserIds").as[Map[String, String]]
      session.set("teacherId", uIds.get("ENSEIGNANT").get).set("studentId", uIds.get("ELEVE").get)
    }
    .exec(http("Teacher details")
      .get("""/directory/api/details?id=${teacherId}""")
      .check(status.is(200), jsonPath("status").is("ok"),
        jsonPath("$.result.0.login").find.saveAs("teacherLogin"),
        jsonPath("$.result.0.code").find.saveAs("teacherCode")))
    .exec(http("Student details")
    .get("""/directory/api/details?id=${studentId}""")
      .check(status.is(200), jsonPath("status").is("ok"),
        jsonPath("$.result.0.login").find.saveAs("studentLogin"),
        jsonPath("$.result.0.code").find.saveAs("studentCode")))

}
