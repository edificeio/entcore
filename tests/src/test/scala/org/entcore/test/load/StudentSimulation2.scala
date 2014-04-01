package org.entcore.test.load

import io.gatling.core.Predef._
import io.gatling.http.Predef._

import scala.concurrent.duration._

class StudentSimulation2 extends Simulation {

	val httpProtocol = http
		.baseURL("http://one")
		.acceptHeader("*/*")
		.acceptEncodingHeader("gzip, deflate")
		.acceptLanguageHeader("fr,fr-fr;q=0.8,en-us;q=0.5,en;q=0.3")
		.userAgentHeader("Mozilla/5.0 (X11; Linux i686; rv:17.0) Gecko/20131030 Firefox/17.0 Iceweasel/17.0.10")

	  setUp(
      TeacherScenario2.scn.inject(atOnce(1 user)),
      StudentScenario2.scn.inject(atOnce(1 user)),
      RelativeScenario2.scn.inject(atOnce(1 user))
    ).protocols(httpProtocol)
//  setUp(
//    TeacherScenario2.scn.inject(ramp(4 users) over (1 seconds)),
//    StudentScenario2.scn.inject(nothingFor(1 seconds), ramp(4 users) over (1 seconds)),
//    RelativeScenario2.scn.inject(ramp(4 users) over (1 seconds))
//  ).protocols(httpProtocol)
}
