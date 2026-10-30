import { feederScenario } from "./_feeder-dsl.ts";

/**
 * Control scenario : importing the same AAF1D files twice, the day after, changes nothing and drops nothing.
 * It is the base every scenario of ENABLING-1377 type builds on : when it fails, the harness or the feeder is
 * broken, whatever the scenario one is interested in.
 */
const scenario = feederScenario("reimport-identique", "Un nouvel import des mêmes fichiers AAF1D ne change rien", (s) => {
  s.given(s.aaf1d().withNbStructures(2).withNbClasses(2).withNbStudentsPerClass(3).withNbRelativesPerStudent(2).withNbTeachersPerStructure(2))
    .whenImport("jour 1")
    .thenImport.hasNoError()
    .thenImport.hasNoIgnoredRecord()
    .whenImport("le lendemain")
    .thenImport.hasNoError()
    .thenImport.hasNoIgnoredRecord()
    .thenStudent(s.student(0)).hasParents(s.relative(0), s.relative(1));
});

export const options = scenario.options;
export const setup = scenario.setup;
export default scenario.run;
export const teardown = scenario.teardown;
