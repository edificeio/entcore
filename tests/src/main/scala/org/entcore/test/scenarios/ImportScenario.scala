package org.entcore.test.scenarios

import io.gatling.core.Predef._
import io.gatling.http.Predef._


object ImportScenario {

  val scn = exec(http("Authenticate user")
    .post("""/auth/login""")
    .formParam("""callBack""", """http%3A%2F%2Flocalhost%3A8080%2Fadmin""")
    .formParam("""email""", """tom.mate""")
    .formParam("""password""", """password""")
    .check(status.is(302)))
    .exec(http("Directory : list Schools")
      .get("""/directory/api/ecole""")
      .check(status.is(200), jsonPath("$.status").is("ok"), jsonPath("$.result").find.saveAs("schools")))
    .doIf(session => session("schools").asOption[String].getOrElse("") == "{}") {
      exec(http("Directory : import schools")
        .post("""/directory/wizard/import""")
        .formParamMap(Map(
          "type" -> "CSV",
          "structureName" -> "Ecole primaire Emile Zola"
        ))
        .bodyPart(RawFileBodyPart("Teacher", "sample-be1d/EcoleprimaireEmileZola/CSVExtraction-enseignants.csv").fileName("CSVExtraction-enseignants.csv").transferEncoding("binary"))
        .bodyPart(RawFileBodyPart("Student", "sample-be1d/EcoleprimaireEmileZola/CSVExtraction-eleves.csv").fileName("CSVExtraction-eleves.csv").transferEncoding("binary"))
        .bodyPart(RawFileBodyPart("Relative", "sample-be1d/EcoleprimaireEmileZola/CSVExtraction-responsables.csv").fileName("CSVExtraction-responsables.csv").transferEncoding("binary"))
        .asMultipartForm
        .check(status.is(200))
      ).pause(10)
    }
    .exec(http("Auth : Logout user")
    .get("""/auth/logout""")
    .check(status.is(302)))

}

