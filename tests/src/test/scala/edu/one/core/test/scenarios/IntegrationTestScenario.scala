package edu.one.core.test.scenarios

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
  .group("Blog Scenario") {
    BlogScenario.scn
  }
  .group("Conversation Scenario") {
    ConversationScenario.scn
  }

}
