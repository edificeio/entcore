/**
 * Plays the scenario of ENABLING-1377 in k6 against the in memory platform, with the bug reproduced, to see what
 * k6 and the report of Jenkins show when a scenario fails. No platform is needed :
 *
 *   k6 run -e ROOT_URL=http://localhost -e BUG=true _feeder-selftest.ts
 *
 * The leading underscore keeps this file away from the scenarios discovered by the edifice cli.
 */
import { ScenarioBuilder } from "./_feeder-builder.ts";
import { k6Reporter } from "./_feeder-dsl.ts";
import { runScenario } from "./_feeder-engine.ts";
import { FakePlatform } from "./_feeder-fake-gateway.ts";

export const options = {
  thresholds: { checks: ["rate == 1.00"] },
  scenarios: { selftest: { executor: "per-vu-iterations", vus: 1, iterations: 1 } },
};

export default function () {
  const s = new ScenarioBuilder("ENABLING-1377");
  const parent = s.manualRelative("parent");
  s.given(s.aaf1d().withNbStructures(1).withNbClasses(1).withNbStudentsPerClass(2).withNbRelativesPerStudent(0))
    .whenImport("jour 1")
    .whenManual.createRelative(parent, { in: s.structure(0), children: [s.student(0)] })
    .thenRelative(parent).hasChildren(s.student(0))
    .whenImport("le lendemain")
    .thenRelative(parent).hasChildren(s.student(0));

  const platform = new FakePlatform();
  platform.dropManualLinksOnReimport = __ENV.BUG === "true";
  runScenario(s.definition("Un parent créé à la main garde ses enfants"), { seed: 123456 }, platform, k6Reporter);
}
