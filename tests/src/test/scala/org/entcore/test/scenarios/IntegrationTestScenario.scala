package org.entcore.test.scenarios

import io.gatling.core.Predef._

object IntegrationTestScenario {

	val scn = scenario("Integration Test Scenario")
    .pause(10)
  .group("Import schools Scenario") {
    ImportScenario.scn
  }
    .pause(10)
  .group("Directory Scenario") {
    DirectoryScenario.scn
  }
  .group("App-registry Scenario") {
    AppRegistryScenario.scn
  }
  .group("Auth Scenario") {
    AuthScenario.scn
  }
  .group("Cas Scenario") {
    CasScenario.scn
  }
  .group("Timeline Scenario") {
    TimelineScenario.scn
  }
  .group("Communication Scenario") {
    CommunicationScenario.scn
  }
  .group("App-registry ADML Scenario") {
    AppRegistryAdmlScenario.scn
  }
  .group("Directory ADML Scenario") {
    DirectoryAdmlScenario.scn
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
  .group("Duplicate Scenario") {
    DuplicateScenario.scn
  }

}
