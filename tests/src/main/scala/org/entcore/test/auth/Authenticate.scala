package org.entcore.test.auth

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import bootstrap._

object Authenticate {

  val authenticateAdmin = exec(http("Authenticate admin")
    .post("""/auth/login""")
    .param("""email""", """tom.mate""")
    .param("""password""", """password""")
    .check(status.is(302)))

  def authenticateUser(login:String, password:String) =  exec(http("Authenticate user")
    .post("""/auth/login""")
    .param("""email""", login)
    .param("""password""", password)
    .check(status.is(302)))

  val logout = exec(http("Logout").get("""/auth/logout""").check(status.is(302)))

}
