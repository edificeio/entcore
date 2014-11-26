package org.entcore.test.load

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

class AllUsersSimulation extends Simulation {

	val httpProtocol = http
		.baseURL("http://one")
		.disableFollowRedirect
		.acceptHeader("*/*")
		.acceptEncodingHeader("gzip, deflate")
		.acceptLanguageHeader("fr,fr-fr;q=0.8,en-us;q=0.5,en;q=0.3")
		.userAgentHeader("Mozilla/5.0 (X11; Linux i686; rv:17.0) Gecko/20131030 Firefox/17.0 Iceweasel/17.0.10")

  setUp(
    TeacherScenario.scn.inject(rampUsers(30) over (600 seconds)),
    StudentScenario.scn.inject(nothingFor(300 seconds), rampUsers(600) over (600 seconds))
  ).protocols(httpProtocol)

}
