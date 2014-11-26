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
        .post("""/directory/import""")
        .check(status.is(200))
      ).pause(10)
    }
    .exec(http("Auth : Logout user")
    .get("""/auth/logout""")
    .check(status.is(302)))

}
