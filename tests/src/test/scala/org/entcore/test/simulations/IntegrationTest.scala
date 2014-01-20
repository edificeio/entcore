package org.entcore.test.simulations

import org.entcore.test.scenarios._

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._
import assertions._

class IntegrationTest extends Simulation {

	val httpProtocol = http
		.baseURL("http://localhost:8090")
		.acceptHeader("*/*")
		.acceptEncodingHeader("gzip, deflate")
		.acceptLanguageHeader("fr,fr-fr;q=0.8,en-us;q=0.5,en;q=0.3")
    .disableFollowRedirect

	setUp(IntegrationTestScenario.scn.inject(nothingFor(15 seconds), atOnce(1 user)))
    .protocols(httpProtocol)
    .assertions(global.failedRequests.count.is(0))

}
