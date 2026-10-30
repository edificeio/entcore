import { feederScenario } from "./_feeder-dsl.ts";

/**
 * The relative account of a foster family, imported with a CSV file, loses its children the day after.
 *
 * Seen on a real platform (ENABLING-1377) : the account has the source CSV, the child comes from the AAF1D files
 * and has two relatives there. The link written by the CSV import has no source, and the AAF1D import deletes the
 * links of a student without source whose relative is not in its file (Importer, createOrUpdateStudent).
 * Compare with parent-children-link-maintained.ts, where the relative is created by hand (its link has the source
 * MANUAL) for a child without relative in the files.
 */
const scenario = feederScenario("csv-relative-student-link-maintained", "Un parent importé par CSV garde ses enfants après un nouvel import AAF1D", (s) => {
  const parent = s.csvRelative("parent");
  const child = s.student(0);

  s.given(s.aaf1d().withNbStructures(1).withNbClasses(1).withNbStudentsPerClass(2).withNbRelativesPerStudent(2))
    .whenImport("jour 1")
    .whenCsv.importRelative(parent, { in: s.structure(0), children: [child] })
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
