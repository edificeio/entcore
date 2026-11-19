// referential POC, step 4 in fallback mode B: the feeder notifies referential of the entities touched by manual
// actions (option referential-notify), referential rereads them in Neo4j. Needs referential running (REFERENTIAL_URL,
// default http://localhost:8108) on the same Neo4j, PostgreSQL and NATS as the platform.
import { feederScenario } from "./_feeder-dsl.ts";

const parent = "parent manuel";

const scenario = feederScenario("RBACK-313", "referential relit les actions manuelles du feeder", (s) => {
  s.given(s.aaf1d().withNbStructures(1).withNbClasses(1).withNbStudentsPerClass(2).withNbRelativesPerStudent(1))
    .whenImport("jour 1")
    .whenReferentialReadsImport()
    .whenManual.createRelative(s.manualRelative(parent), { in: s.structure(0), children: [s.student(0)] })
    .thenReferential(s.student(0), s.manualRelative(parent)).isInSync()
    .whenManual.linkRelative(s.manualRelative(parent), s.student(1))
    .thenReferential(s.student(1), s.manualRelative(parent)).isInSync()
    .whenManual.unlinkRelative(s.manualRelative(parent), s.student(0))
    .thenReferential(s.student(0), s.student(1), s.manualRelative(parent)).isInSync();
});

export const options = scenario.options;
export const setup = scenario.setup;
export default scenario.run;
export const teardown = scenario.teardown;
