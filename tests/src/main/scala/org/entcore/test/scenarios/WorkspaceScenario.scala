package org.entcore.test.scenarios

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import bootstrap._

import org.entcore.test.workspace.Workspace._

object WorkspaceScenario {

  val scn =
    exec(uploadDocument("RecordSimulation_request_202.txt"))
    .exec(uploadDocument("RecordSimulation_request_202.txt"))
    .exec(uploadDocument("RecordSimulation_request_202.txt"))

}
