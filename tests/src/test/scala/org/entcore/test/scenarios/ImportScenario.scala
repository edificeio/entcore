package org.entcore.test.scenarios

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import bootstrap._

object ImportScenario {

  val scn = exec(http("Authenticate user")
    .post("""/auth/login""")
    .param("""callBack""", """http%3A%2F%2Flocalhost%3A8080%2Fadmin""")
    .param("""email""", """tom.mate""")
    .param("""password""", """password""")
    .check(status.is(302)))
    .exec(http("Directory : list Schools")
    .get("""/directory/api/ecole""")
    .check(status.is(200), jsonPath("$.status").is("ok"), jsonPath("$.result").find.saveAs("schools")))
    .doIf(session => session("schools").asOption.getOrElse("") == "{}") {
      exec(http("Directory : import schools")
        .get("""/directory/testbe1d""")
        .check(status.is(200))
      )
    }
    .exec(http("Auth : Logout user")
    .get("""/auth/logout""")
    .check(status.is(302)))

}
