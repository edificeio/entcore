/**
 * k6 glue of the feeder scenarios : runs a scenario built with the DSL (see _feeder-builder.ts for its vocabulary)
 * and reports through k6 checks. See _template.ts for a scenario to copy and ENABLING-1377.ts for a real one.
 *
 *   const scenario = feederScenario("ENABLING-1377", "Parent manuel conservé après réimport", (s) => {
 *     const parent = s.manualRelative("parent");
 *     s.given(s.aaf1d().withNbStructures(1).withNbClasses(1).withNbStudentsPerClass(1).withNbRelativesPerStudent(0))
 *       .whenImport("jour 1")
 *       .whenManual.createRelative(parent, { in: s.structure(0), children: [s.student(0)] })
 *       .whenImport("le lendemain")
 *       .thenRelative(parent).hasChildren(s.student(0));
 *   });
 *   export const options = scenario.options;
 *   export const setup = scenario.setup;
 *   export default scenario.run;
 *   export const teardown = scenario.teardown;
 */
import { check, fail } from "k6";
import type { AAFFeeder } from "../directory/_aaf-utils.ts";
import { ScenarioBuilder } from "./_feeder-builder.ts";
import { cleanUp, runScenario } from "./_feeder-engine.ts";
import type { Reporter, ScenarioDefinition, ScenarioRunData } from "./_feeder-engine.ts";
import { FeederGateway, HttpFeederGateway } from "./_feeder-gateway.ts";

export { ScenarioBuilder } from "./_feeder-builder.ts";

/** Cleaning a feeder which received nothing is harmless, so a run does not need to remember which ones it used. */
const ALL_FEEDERS: AAFFeeder[] = ["AAF", "AAF1D"];

/** Runs the scenario in k6 : each message becomes the name of a k6 check, which is what the report of Jenkins shows. */
export const k6Reporter: Reporter = {
  pass: (name) => { check(true, { [name]: () => true }); },
  fail: (name) => { check(true, { [name]: () => false }); },
  info: (message) => console.log(message),
  abort: (message) => fail(message),
};

/** A scenario ready to be exported by a k6 script. */
export class FeederScenario {
  readonly options: any;

  constructor(private definition: ScenarioDefinition, private gateway: FeederGateway) {
    this.options = {
      setupTimeout: "5m",
      thresholds: { checks: ["rate == 1.00"] },
      scenarios: {
        feeder: {
          executor: "per-vu-iterations",
          vus: 1,
          iterations: 1,
          maxDuration: __ENV.MAX_DURATION || "30m",
          gracefulStop: "10s",
        },
      },
    };
  }

  private get skipped(): boolean {
    return !!this.definition.pendingFor && __ENV.FEEDER_RUN_PENDING !== "true";
  }

  /** The seed makes every run unique (the platform keeps what previous runs created) ; FEEDER_SEED replays one. */
  setup = (): ScenarioRunData => ({ seed: parseInt(__ENV.FEEDER_SEED || String((Date.now() % 900000000) + 100000)) });

  run = (data: ScenarioRunData): void => {
    if (this.skipped) {
      console.warn(`[${this.definition.ticket}] ignored : reproduce the bug ${this.definition.pendingFor} which has not yet been fixed (FEEDER_RUN_PENDING=true to play it)`);
      check(true, { [`[${this.definition.ticket}] ignored (bug ${this.definition.pendingFor} waiting for correction)`]: () => true });
      return;
    }
    runScenario(this.definition, data, this.gateway, k6Reporter);
  };

  teardown = (data: ScenarioRunData): void => {
    // FEEDER_KEEP_FILES=true : leave the uploaded files and the import directory on the platform, to inspect them
    if (!this.skipped && __ENV.FEEDER_KEEP_FILES !== "true") {
      cleanUp(this.definition, data, this.gateway, ALL_FEEDERS, k6Reporter);
    }
  };
}

/**
 * Declares a feeder scenario.
 * @param shortName Short name of the scenario. Used in every message and in the names of the import directories.
 * @param title What the scenario shows
 * @param define Describes the steps with the builder it is given
 */
export function feederScenario(shortName: string, title: string, define: (scenario: ScenarioBuilder) => void): FeederScenario {
  const builder = new ScenarioBuilder(shortName);
  define(builder);
  return new FeederScenario(builder.definition(title), new HttpFeederGateway());
}
