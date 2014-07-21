package org.entcore.test.scenarios

import io.gatling.core.Predef._

object IntegrationTestScenario {

	val scn = scenario("Integration Test Scenario")
  .group("Import schools Scenario") {
    ImportScenario.scn
  }
  .group("Directory Scenario") {
    DirectoryScenario.scn
  }
  .group("App-registry Scenario") {
    AppRegistryScenario.scn
  }
  .group("Auth Scenario") {
    AuthScenario.scn
  }
  .group("Timeline Scenario") {
    TimelineScenario.scn
  }
  .group("Communication Scenario") {
    CommunicationScenario.scn
  }
  .group("Conversation Scenario") {
    ConversationScenario.scn
  }
  .group("Workspace Scenario") {
    WorkspaceScenario.scn
  }
  .group("Archive Scenario") {
    ArchiveScenario.scn
  }
  .group("Quota Scenario") {
    QuotaScenario.scn
  }

}
