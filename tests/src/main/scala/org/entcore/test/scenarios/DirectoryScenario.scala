package org.entcore.test.scenarios

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
      .get("""/directory/api/personnes?id=${classId}&type=Student""")
    .check(status.is(200), jsonPath("status").is("ok"),
      jsonPath("$.result.0.userId").find.saveAs("childrenId")))
		.exec(http("Create manual teacher")
			.post("""/directory/api/user""")
			.param("""classId""", """${classId}""")
			.param("""lastname""", "Devost")
			.param("""firstname""", """Julie""")
			.param("""type""", """Teacher""")
		.check(status.is(200)))
    .exec(http("Create manual student")
      .post("""/directory/api/user""")
      .param("""classId""", """${classId}""")
      .param("""lastname""", "Monjeau")
      .param("""firstname""", """Lundy""")
      .param("""birthDate""", """1970-01-01""")
      .param("""type""", """Student""")
    .check(status.is(200)))
    .exec(http("Create manual parent")
      .post("""/directory/api/user""")
      .param("""classId""", """${classId}""")
      .param("""lastname""", "Bondy")
      .param("""firstname""", """Astrid""")
      .param("""type""", """Relative""")
      .param("""childrenIds""", """${childrenId}""")
    .check(status.is(200)))
    .exec(http("List persons in class")
      .get("""/directory/api/personnes?id=${classId}""")
      .check(status.is(200), jsonPath("status").is("ok"),
      jsonPath("$.result").find.transform(_.map(res => {
        val json = JSONValue.parse(res).asInstanceOf[JSONObject]
        json.values.asScala.foldLeft[List[(String, String)]](Nil){(acc, c) =>
          val user = c.asInstanceOf[JSONObject]
          user.get("lastName").asInstanceOf[String] match {
            case "Devost" | "Monjeau" if user.get("code") != null =>
              (user.get("type").asInstanceOf[String], user.get("userId").asInstanceOf[String]) :: acc
            case _ => acc
          }
        }.toMap
      })).saveAs("createdUserIds")))
    .exec{session =>
      val uIds = session("createdUserIds").as[Map[String, String]]
      session.set("teacherId", uIds.get("Teacher").get).set("studentId", uIds.get("Student").get)
        .set("now", System.currentTimeMillis())
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

    // create function
    .exec(http("Create function")
      .post("""/directory/function/Teacher""")
      .header("Content-Type", "application/json")
      .body(StringBody("""{"externalId": "ADMIN_LOCAL_${now}", "name": "AdminLocal"}"""))
      .check(status.is(201), jsonPath("$.id").find.saveAs("function-id")))

    .exec(http("Create function")
      .post("""/directory/function/Teacher""")
      .header("Content-Type", "application/json")
      .body(StringBody("""{"externalId": "CLASS_ADMIN_${now}", "name": "Class Admin"}"""))
      .check(status.is(201), jsonPath("$.id").find.saveAs("function-id2")))

    .exec(http("Create function")
      .post("""/directory/function/Teacher""")
      .header("Content-Type", "application/json")
      .body(StringBody("""{"externalId": "DELETE_${now}", "name": "To delete"}"""))
      .check(status.is(201), jsonPath("$.id").find.saveAs("function-delete")))

    // create function group
    .exec(http("Create function group")
      .post("""/directory/functiongroup""")
      .header("Content-Type", "application/json")
      .body(StringBody("""{"functionsCodes": ["CLASS_ADMIN_${now}"], "classes": ["${classId}"]}"""))
      .check(status.is(201), jsonPath("$.id").find.saveAs("function-group-id")))

    .exec(http("Create function group")
      .post("""/directory/functiongroup""")
      .header("Content-Type", "application/json")
      .body(StringBody("""{"functionsCodes": ["DELETE_${now}"], "classes": ["${schoolId}"]}"""))
      .check(status.is(201), jsonPath("$.id").find.saveAs("function-group-id-delete")))

    // add user to group
    .exec(http("add user to group")
      .post("""/directory/user/group/${teacherId}/${function-group-id}""")
      .header("Content-Length", "0")
      .check(status.is(200)))

    .exec(http("add user to group")
      .post("""/directory/user/group/${teacherId}/${function-group-id-delete}""")
      .header("Content-Length", "0")
      .check(status.is(200)))

    // add user function
    .exec(http("User add function ")
      .post("""/directory/user/function/${teacherId}""")
      .header("Content-Type", "application/json")
      .body(StringBody("""{"functionCode": "DELETE_${now}", "classes": ["${classId}"]}"""))
      .check(status.is(200)))

    .exec(http("User add function ")
      .post("""/directory/user/function/${teacherId}""")
      .header("Content-Type", "application/json")
      .body(StringBody("""{"functionCode": "ADMIN_LOCAL_${now}", "structures": ["${schoolId}"]}"""))
      .check(status.is(200)))

    .exec(http("User add function ")
      .post("""/directory/user/function/${teacherId}""")
      .header("Content-Type", "application/json")
      .body(StringBody("""{"functionCode": "ADMIN_LOCAL", "structures": ["${schoolId}"]}"""))
      .check(status.is(200)))

    // remove user from group
//    .exec(http("Remove user from group")
//      .delete("""/directory/user/group/${teacherId}/${function-group-id-delete}""")
//      .header("Content-Length", "0")
//      .check(status.is(200)))
//
//    // remove user function
//    .exec(http("Remove user function")
//      .delete("""/directory/user/function/${teacherId}/DELETE_${now}""")
//      .check(status.is(200)))
//
//    // Delete function group
//    .exec(http("Delete function group")
//      .delete("""/directory/functiongroup/${function-group-id-delete}""")
//      .check(status.is(204)))

    // Delete function
//    .exec(http("Delete function")
//      .delete("""/directory/function/DELETE_${now}""")
//      .check(status.is(204)))

}
