package org.entcore.test.scenarios

import io.gatling.core.Predef._
import io.gatling.http.Predef._


import scala.concurrent.duration._

object ArchiveScenario {

  val scn =
    exec(http("Ask archive creation")
    .post("/archive/export")
    .body(StringBody("""{"apps":["rack","workspace"]}"""))
    .check(status.is(200), jsonPath("$.exportId").find.saveAs("exportId")))
    .exec(http("Verify archive")
    .get("/archive/export/verify/${exportId}")
    .check(status.is(200)))
    .exec(http("Get archive")
    .get("/archive/export/${exportId}")
    .check(status.is(200)))
//    .pause(100 milliseconds)
//    .exec(http("Try get anew")
//    .get("/archive/export/${exportId}")
//    .check(status.is(404)))

}
