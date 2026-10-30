import { feederScenario } from "./_feeder-dsl.ts";

/**
 * The relative account of a foster family, created by hand, loses its children the day after.
 *
 * The school sends no relative for the child (the child lives with a foster family), a person of the school
 * creates the relative account by hand and attaches the child to it. The next import of the same files must not
 * undo that.
 */
const scenario = feederScenario("relative-student-link-maintained", "Un parent créé à la main garde ses enfants après un nouvel import AAF1D", (s) => {
  const parent = s.manualRelative("parent");
  const child = s.student(0);

  s.given(s.aaf1d().withNbStructures(1).withNbClasses(1).withNbStudentsPerClass(2).withNbRelativesPerStudent(0))
    .whenImport("jour 1")
    .whenManual.createRelative(parent, { in: s.structure(0), children: [child] })
    .thenRelative(parent).hasChildren(child)
    .whenImport("le lendemain")
    .thenRelative(parent).hasChildren(child)
    .thenStudent(child).hasParents(parent);

  // the bug is not fixed yet : only played when FEEDER_RUN_PENDING=true
  //s.pending("ENABLING-1377");
});

export const options = scenario.options;
export const setup = scenario.setup;
export default scenario.run;
export const teardown = scenario.teardown;
