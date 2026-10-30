/**
 * Offline tests of the engine and of the DSL vocabulary, against a fake platform which "imports" the generated
 * AAF1D files in memory. The fake can reproduce the bug of ENABLING-1377 (a relative created by hand loses its
 * children at the next import) to check that the report of such a failure is understandable.
 *
 * Run with : node --experimental-transform-types --no-warnings --test _feeder-engine.test.ts
 */
import test from "node:test";
import assert from "node:assert/strict";
import { ScenarioBuilder } from "./_feeder-builder.ts";
import { cleanUp, runScenario } from "./_feeder-engine.ts";
import { FakePlatform, RecordingReporter } from "./_feeder-fake-gateway.ts";
import { FeederScenarioError } from "./_feeder-errors.ts";

const DATA = { seed: 123456 };

/** The scenario of ENABLING-1377. */
function foster(build: (s: ScenarioBuilder) => void = () => {}): ScenarioBuilder {
  const s = new ScenarioBuilder("ENABLING-1377");
  const parent = s.manualRelative("parent");
  s.given(s.aaf1d().withNbStructures(1).withNbClasses(1).withNbStudentsPerClass(2).withNbRelativesPerStudent(0))
    .whenImport("jour 1")
    .whenManual.createRelative(parent, { in: s.structure(0), children: [s.student(0)] })
    .thenRelative(parent).hasChildren(s.student(0))
    .whenImport("le lendemain")
    .thenRelative(parent).hasChildren(s.student(0));
  build(s);
  return s;
}

function play(scenario: ScenarioBuilder, platform: FakePlatform): RecordingReporter {
  const reporter = new RecordingReporter();
  runScenario(scenario.definition("Parent manuel conservé"), DATA, platform, reporter);
  return reporter;
}

test("a platform which keeps the relative created by hand satisfies the scenario", () => {
  const platform = new FakePlatform();
  const reporter = play(foster(), platform);
  assert.deepEqual(reporter.failed, []);
  assert.equal(reporter.passed.length, 6);
  assert.deepEqual(platform.calls.filter((c) => c !== "login"), [
    "upload AAF1D it-ENABLING-1377-123456", "import AAF1D",
    "upload AAF1D it-ENABLING-1377-123456", "import AAF1D",
  ]);
  assert.match(reporter.infos[reporter.infos.length - 1], /toutes les attentes sont satisfaites/);
});

test("ENABLING-1377 : the loss of the children is reported with the step, the person and the import", () => {
  const platform = new FakePlatform();
  platform.dropManualLinksOnReimport = true;
  const reporter = play(foster(), platform);

  assert.equal(reporter.failed.length, 1);
  const message = reporter.failed[0];
  assert.ok(!message.includes("\n"));
  assert.match(message, /^\[ENABLING-1377\] étape 6\/6 « thenRelative\(parent\)\.hasChildren\(student\(0\)\) »/);
  assert.match(message, /attendu : « Manuel PARENT » \(créé à la main, parent\) est le parent de « .+ » \(externalId it-ENABLING-1377-123456-\d+\)/);
  assert.match(message, /constaté : aucun enfant\. Dernier import/);
  assert.match(message, /Dernier import \(« le lendemain »\) : 0 erreur, 0 enregistrement ignoré\.$/);
  assert.equal(reporter.passed.length, 5, "the steps before the loss are reported as passing");
});

test("the files are generated with the seed, so that the structure is unique to the run", () => {
  const platform = new FakePlatform();
  play(foster(), platform);
  assert.match(platform.structures[0].name, /^IT ENABLING-1377 123456 #0$/);
  assert.match(platform.uploaded[0]["ENT_IT_Complet_EtabEducNat_0001.xml"], /<value>1234560A<\/value>/);
  assert.deepEqual(platform.uploaded[0], platform.uploaded[1], "the next day files are the same files");
});

test("an import in error stops the scenario with the reason, without a cascade of failures", () => {
  const platform = new FakePlatform();
  platform.importSummary = { status: "ok", result: { errors: { "error.Eleve": [{ translation: "Fichier illisible" }] }, ignored: {}, files: {}, profiles: [] } };
  const reporter = new RecordingReporter();
  assert.throws(() => runScenario(foster().definition("t"), DATA, platform, reporter), /ABORT \[ENABLING-1377\] étape 2\/6 « whenImport\("jour 1"\) » ▸ ARRÊT : l'import « jour 1 » est terminé avec des erreurs/);
  assert.equal(reporter.failed.length, 1);
  assert.match(reporter.failed[0], /error\.Eleve: Fichier illisible/);
  assert.equal(platform.calls.filter((c) => c.startsWith("import")).length, 1);
});

test("an unknown relative is a scenario error with a suggestion", () => {
  const s = new ScenarioBuilder("T-1");
  s.given(s.aaf1d().withNbStructures(1).withNbStudentsPerClass(1).withNbRelativesPerStudent(0))
    .whenImport("jour 1")
    .whenManual.createRelative("parent", { in: s.structure(0), children: [s.student(0)] })
    .thenRelative("parnt").hasChildren(s.student(0));
  assert.throws(() => play(s, new FakePlatform()), /responsable créé à la main inconnu\(e\) « parnt » \(connues : parent\)\. Vouliez-vous dire « parent » \?/);
});

test("a student which does not exist is a scenario error with the valid indices", () => {
  const s = new ScenarioBuilder("T-1");
  s.given(s.aaf1d().withNbStructures(1).withNbStudentsPerClass(2).withNbRelativesPerStudent(0))
    .whenImport("jour 1")
    .whenManual.createRelative("parent", { in: s.structure(0), children: [s.student(7)] });
  assert.throws(() => play(s, new FakePlatform()), /student\(7\) n'existe pas, les fichiers en déclarent 2 \(indices 0\.\.1\)/);
});

test("using a student before any import tells what to do", () => {
  const s = new ScenarioBuilder("T-1");
  s.given(s.aaf1d()).whenManual.createRelative("parent", { in: s.structure(0), children: [s.student(0)] });
  assert.throws(() => play(s, new FakePlatform()), /aucun import n'a encore eu lieu/);
});

test("an import without data tells to declare them", () => {
  const s = new ScenarioBuilder("T-1");
  s.whenImport();
  assert.throws(() => play(s, new FakePlatform()), /déclarer given\(<données>\) avant whenImport\(\)/);
});

test("the links of the files are verified by every import, and what the files do not say is left alone", () => {
  const platform = new FakePlatform();
  const s = new ScenarioBuilder("T-2");
  s.given(s.aaf1d().withNbStructures(1).withNbStudentsPerClass(2).withNbRelativesPerStudent(1)).whenImport("jour 1");
  assert.deepEqual(play(s, platform).failed, []);

  // a parent link which disappears from the platform is caught by the next import
  const lost = new FakePlatform();
  const reporter = new RecordingReporter();
  const original = lost.importAndWait.bind(lost);
  lost.importAndWait = (feeder) => { const r = original(feeder); lost.users.forEach((u) => (u.parents = [])); return r; };
  runScenario(s.definition("t"), DATA, lost, reporter);
  assert.equal(reporter.failed.length, 1);
  assert.match(reporter.failed[0], /liens parents : chaque élève est lié à ses responsables ; constaté : 2 lien\(s\) absent\(s\) : « .+ » \(externalId .+\) n'a pas pour parent/);
});

test("thenImport tells about ignored records and errors", () => {
  const platform = new FakePlatform();
  platform.importSummary = { status: "ok", result: { errors: {}, ignored: { Eleve: [{ reason: "invalid.birthDate", object: {} }] }, files: {}, profiles: [] } };
  const s = new ScenarioBuilder("T-3");
  s.given(s.aaf1d().withNbStructures(1).withNbStudentsPerClass(1).withNbRelativesPerStudent(0)).whenImport("jour 1").thenImport.hasNoIgnoredRecord();
  const reporter = play(s, platform);
  assert.equal(reporter.failed.length, 1);
  assert.match(reporter.failed[0], /attendu : aucun enregistrement ignoré par le dernier import ; constaté : 1 enregistrement ignoré \(Eleve: invalid\.birthDate\)/);
});

test("the clean up removes the directory of the run and never fails", () => {
  const platform = new FakePlatform();
  const reporter = new RecordingReporter();
  cleanUp(foster().definition("t"), DATA, platform, ["AAF", "AAF1D"], reporter);
  assert.deepEqual(platform.calls, ["login", "clean AAF it-ENABLING-1377-123456", "clean AAF1D it-ENABLING-1377-123456"]);

  platform.cleanAAF = () => { throw new FeederScenarioError("403"); };
  cleanUp(foster().definition("t"), DATA, platform, ["AAF"], reporter);
  assert.match(reporter.infos[reporter.infos.length - 1], /nettoyage du dossier it-ENABLING-1377-123456 impossible : 403/);
});

/** ENABLING-1377 as seen on a real platform : a relative imported by CSV, for a child who has relatives in the files. */
function csvFoster(): ScenarioBuilder {
  const s = new ScenarioBuilder("ENABLING-1377");
  const parent = s.csvRelative("parent");
  s.given(s.aaf1d().withNbStructures(1).withNbClasses(1).withNbStudentsPerClass(2).withNbRelativesPerStudent(2))
    .whenImport("jour 1")
    .whenCsv.importRelative(parent, { in: s.structure(0), children: [s.student(0)] })
    .thenRelative(parent).hasChildren(s.student(0))
    .whenImport("le lendemain")
    .thenRelative(parent).hasChildren(s.student(0))
    .thenStudent(s.student(0)).hasParents(parent);
  return s;
}

test("a relative imported by CSV is attached to the generated students through their external ids", () => {
  const platform = new FakePlatform();
  const reporter = play(csvFoster(), platform);
  assert.deepEqual(reporter.failed, []);
  assert.deepEqual(platform.calls.filter((c) => c !== "login"), [
    "upload AAF1D it-ENABLING-1377-123456", "import AAF1D",
    "csv Relative",
    "upload AAF1D it-ENABLING-1377-123456", "import AAF1D",
  ]);
  const file = platform.csvImports[0].Relative!;
  const [header, row] = file.trim().split("\n");
  assert.equal(header, '"externalId";"Nom responsable";"Prénom responsable";"childExternalId"');
  assert.match(row, /^"it-ENABLING-1377-123456-csv-parent";"PARENT";"Csv";"it-ENABLING-1377-123456-\d+"$/);
});

test("ENABLING-1377 on CSV : the link without source is reported as lost, and the one created by hand is kept", () => {
  const platform = new FakePlatform();
  platform.dropSourcelessLinks = true;
  const reporter = play(csvFoster(), platform);
  assert.equal(reporter.failed.length, 2);
  assert.match(reporter.failed[0], /^\[ENABLING-1377\] étape 6\/7 « thenRelative\(parent\)\.hasChildren\(student\(0\)\) »/);
  assert.match(reporter.failed[0], /attendu : « Csv PARENT » \(importé par CSV, parent\) est le parent de/);
  assert.match(reporter.failed[0], /constaté : aucun enfant/);
  assert.match(reporter.failed[1], /étape 7\/7 « thenStudent\(student\(0\)\)\.hasParents\(parent\) »/);

  // same platform, same files, but the relative is created by hand : its link has a source, it stays
  const manual = new ScenarioBuilder("ENABLING-1377");
  const parent = manual.manualRelative("parent");
  manual.given(manual.aaf1d().withNbStructures(1).withNbClasses(1).withNbStudentsPerClass(2).withNbRelativesPerStudent(2))
    .whenImport("jour 1")
    .whenManual.createRelative(parent, { in: manual.structure(0), children: [manual.student(0)] })
    .whenImport("le lendemain")
    .thenRelative(parent).hasChildren(manual.student(0));
  const dropper = new FakePlatform();
  dropper.dropSourcelessLinks = true;
  assert.deepEqual(play(manual, dropper).failed, []);
});

test("a CSV relative whose structure has no external id stops the scenario with an explanation", () => {
  const platform = new FakePlatform();
  const original = platform.findStructureByName.bind(platform);
  platform.findStructureByName = (name) => {
    const found = original(name);
    return found && { id: found.id, name: found.name };
  };
  assert.throws(() => play(csvFoster(), platform), /nécessaire à l'import CSV/);
});

test("a CSV relative which was not imported is reported as such, with the known ones", () => {
  const s = new ScenarioBuilder("ENABLING-1377");
  s.given(s.aaf1d().withNbStructures(1).withNbClasses(1).withNbStudentsPerClass(1))
    .whenImport("jour 1")
    .thenRelative(s.csvRelative("parent")).hasNoChild();
  assert.throws(() => play(s, new FakePlatform()), /responsable importé par CSV inconnu\(e\) « parent » \(aucune n'est déclarée/);
});
