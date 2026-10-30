// Copy this file to `<TICKET>.ts` in this directory (a name starting with "_" is not played) and describe the scenario.
//
// Vocabulary (see _feeder-builder.ts) :
//   data      s.aaf() / s.aaf1d()                 builders of the AAF files (see ../directory/_aaf-generator.ts)
//   handles   s.structure(i) s.student(i) s.relative(i)   what the files declare, by index (global, 0-based)
//             s.manualRelative("name")           a relative which will be created by hand
//   given     s.given(data)                        the data the next imports read (call it again to change them)
//   when      .whenImport("label", data?)          uploads the files, imports, then checks the files are on the platform
//             .whenManual.createRelative(ref, { in, children })  .linkRelative(ref, student)  .unlinkRelative(ref, student)
//   then      .thenRelative(ref).hasChildren(...) / .hasNoChild()
//             .thenStudent(student).hasParents(...)
//             .thenImport.hasNoError() / .hasNoIgnoredRecord()
//   bug       s.pending("TICKET")                  reproduces a bug which is not fixed yet : only played when FEEDER_RUN_PENDING=true
//
// Every failure is reported on one line : scenario, step, what was expected, what was found, what the last import said.
// To replay a run : FEEDER_SEED=<seed printed at the beginning of the run>.
import { feederScenario } from "./_feeder-dsl.ts";

const scenario = feederScenario("TICKET-0000", "What the scenario shows", (s) => {
  s.given(s.aaf1d().withNbStructures(1).withNbClasses(1).withNbStudentsPerClass(1).withNbRelativesPerStudent(1))
    .whenImport("jour 1")
    .thenStudent(s.student(0)).hasParents(s.relative(0));
});

export const options = scenario.options;
export const setup = scenario.setup;
export default scenario.run;
export const teardown = scenario.teardown;
