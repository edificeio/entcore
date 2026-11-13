/**
 * Offline tests of the pure parts of the feeder scenarios (messages, model comparison, AAF1D files).
 * Run with : node --experimental-transform-types --test _feeder-errors.test.ts
 * (the leading underscore keeps this file away from the scenarios discovered by the edifice cli)
 */
import test from "node:test";
import assert from "node:assert/strict";
import { AAFGeneratorBuilder } from "../directory/_aaf-generator.ts";
import {
  abortMessage,
  describeHttpFailure,
  describeImport,
  describeImportFailure,
  ExpectationFailed,
  failureMessage,
  oneLine,
  preview,
  stepPrefix,
  unknownReference,
  MAX_MESSAGE_LENGTH,
} from "./_feeder-errors.ts";
import { diffModel } from "./_feeder-verify.ts";
import type { GraphSnapshot, GraphUser } from "./_feeder-verify.ts";

function oneStructure1D(nbRelatives: number) {
  return new AAFGeneratorBuilder()
    .withFormat("AAF1D")
    .withIndexStart(500)
    .withNbStructures(1)
    .withNbClasses(1)
    .withNbStudentsPerClass(2)
    .withNbRelativesPerStudent(nbRelatives)
    .withNbTeachersPerStructure(1)
    .build()
    .generate();
}

/** The graph as it should be after importing the files. */
function perfectGraph(generated: ReturnType<typeof oneStructure1D>, prefix: string): GraphSnapshot {
  const { model } = generated;
  const user = (id: string, type: string, externalId: string, firstName: string, lastName: string, classes: string[] = []): GraphUser =>
    ({ id, type, externalId: `${prefix}${externalId}`, firstName, lastName, classes: classes.map((name) => ({ name })) });
  const users: GraphUser[] = [
    ...model.students.map((s) => user(`u-${s.externalId}`, "Student", s.externalId, s.firstName, s.lastName, [model.classes[s.classIndex].name])),
    ...model.teachers.map((t) => user(`u-${t.externalId}`, "Teacher", t.externalId, t.firstName, t.lastName)),
    ...model.relatives.map((r) => user(`u-${r.externalId}`, "Relative", r.externalId, r.firstName, r.lastName)),
  ];
  const parentsOf: Record<string, string[]> = {};
  model.students.forEach((s) => (parentsOf[`u-${s.externalId}`] = s.relativeLinks.map((l) => `u-${model.relatives[l.relativeIndex].externalId}`)));
  return { structures: [{ id: "s1", name: model.structures[0].name }], usersByStructure: [users], parentsOf };
}

test("preview shortens long lists and names the empty one", () => {
  assert.equal(preview([]), "aucun");
  assert.equal(preview(["a", "b"]), "a, b");
  assert.equal(preview(["a", "b", "c", "d"], 2), "a, b (+2 autres)");
});

test("oneLine flattens new lines and bounds the length", () => {
  assert.equal(oneLine("a\nb\r\nc"), "a ⏎ b ⏎ c");
  const long = oneLine("x".repeat(MAX_MESSAGE_LENGTH * 2));
  assert.equal(long.length, MAX_MESSAGE_LENGTH);
  assert.ok(long.endsWith("…"));
});

test("an unknown reference suggests the closest known one", () => {
  const message = unknownReference("responsable", "parnt", ["parent", "tuteur"]);
  assert.match(message, /inconnu\(e\) « parnt »/);
  assert.match(message, /connues : parent, tuteur/);
  assert.match(message, /Vouliez-vous dire « parent »/);
  assert.doesNotMatch(unknownReference("responsable", "zzzzzz", ["parent"]), /Vouliez-vous dire/);
  assert.match(unknownReference("responsable", "x", []), /aucune n'est déclarée/);
});

test("the description of an import tells errors and ignored records with their reason", () => {
  assert.equal(describeImport(undefined, undefined), "aucun import n'a encore eu lieu");
  assert.equal(
    describeImport("le lendemain", { status: "ok", result: { errors: {}, ignored: {}, files: {}, profiles: [] } }),
    "Dernier import (« le lendemain ») : 0 erreur, 0 enregistrement ignoré"
  );
  const withIgnored = describeImport(undefined, {
    status: "ok",
    result: {
      errors: {},
      files: {},
      profiles: [],
      ignored: { Eleve: [{ reason: "invalid.birthDate", object: {} }, { reason: "invalid.birthDate", object: {} }] },
      ignoredNotReturned: { Eleve: 3 },
    },
  });
  assert.equal(withIgnored, "Dernier import : 0 erreur, 5 enregistrements ignorés [Eleve: invalid.birthDate ×2]");
});

test("an import in error is explained with its first errors", () => {
  const message = describeImportFailure("jour 1", {
    status: "ok",
    result: { errors: { "error.Eleve": [{ translation: "Le fichier est illisible" }] }, ignored: {}, files: {}, profiles: [] },
  });
  assert.match(message, /l'import « jour 1 » est terminé avec des erreurs/);
  assert.match(message, /1 erreur/);
  assert.match(message, /error\.Eleve: Le fichier est illisible/);
});

test("a failure message is a single line telling scenario, step, expectation, finding and import", () => {
  const prefix = stepPrefix("ENABLING-1377", 4, 6, "thenRelative(parent).hasChildren(Hugo)");
  const message = failureMessage(prefix, new ExpectationFailed("parent a pour enfant Hugo", "aucun enfant"), "Dernier import : 0 erreur");
  assert.equal(
    message,
    "[ENABLING-1377] étape 4/6 « thenRelative(parent).hasChildren(Hugo) » ▸ attendu : parent a pour enfant Hugo ; constaté : aucun enfant. Dernier import : 0 erreur."
  );
  assert.ok(!message.includes("\n"));
  assert.match(abortMessage(prefix, "boom\nsuite"), /ARRÊT : boom ⏎ suite$/);
});

test("http failures come with what to do about them", () => {
  assert.match(describeHttpFailure("POST", "/directory/api/internal/upload-aaf", 403, "{}"), /ALLOW_UPLOAD_AAF_FILES=true/);
  assert.match(describeHttpFailure("POST", "/x", 409, ""), /import est déjà en cours/);
  assert.match(describeHttpFailure("POST", "/x", 0, ""), /n'a pas répondu/);
});

test("AAF1D files use the first degree attributes and class codes without '$'", () => {
  const { files, model } = oneStructure1D(1);
  const students = files["ENT_IT_Complet_Eleve_0001.xml"];
  const klass = model.classes[0];
  assert.ok(!klass.externalId.includes("$"));
  assert.ok(students.includes("ENTEleveAutoriteParentale"));
  assert.ok(!students.includes("ENTElevePersRelEleve"));
  assert.ok(!students.includes("ENTEleveClasses"));
  const classValue = /<attr name="ENTPersonClasses"><value>([^<]*)<\/value>/.exec(students)![1];
  const segments = classValue.split("$");
  assert.equal(segments.length, 5);
  assert.equal(segments[0], model.structures[0].externalId);
  assert.equal(segments[3], klass.externalId);
  assert.equal(segments[4], klass.name);
  assert.ok(files["ENT_IT_Complet_PersRelEleve_0001.xml"].includes("ENTPersRelEleveEleve"));
  assert.ok(files["ENT_IT_Complet_EtabEducNat_0001.xml"].includes("ENTEcoleMinistereTutelle"));
  assert.equal(files["ENT_IT_Complet_MefEducNat_0001.xml"], undefined);
});

test("AAF files of the second degree are left as they were", () => {
  const { files, model } = new AAFGeneratorBuilder().withIndexStart(500).withNbStudentsPerClass(1).build().generate();
  assert.ok(files["ENT_IT_Complet_Eleve_0001.xml"].includes("ENTEleveClasses"));
  assert.ok(model.classes[0].externalId.includes("$"));
});

test("the files are deterministic, which is what makes a next day import identical", () => {
  assert.deepEqual(oneStructure1D(2).files, oneStructure1D(2).files);
});

test("a graph holding what the files declare has no problem, whatever the prefix", () => {
  const generated = oneStructure1D(1);
  assert.deepEqual(diffModel(generated.model, perfectGraph(generated, ""), ""), []);
  assert.deepEqual(diffModel(generated.model, perfectGraph(generated, "it-sub-"), "it-sub-"), []);
});

test("what the platform holds beyond the files is not a problem", () => {
  const generated = oneStructure1D(1);
  const graph = perfectGraph(generated, "");
  graph.usersByStructure[0].push({ id: "manual", externalId: "", type: "Relative", firstName: "Famille", lastName: "Accueil", classes: [] });
  assert.deepEqual(diffModel(generated.model, graph, ""), []);
});

test("missing students, wrong classes and lost parent links are reported by category", () => {
  const generated = oneStructure1D(1);
  const graph = perfectGraph(generated, "");
  const [first, second] = generated.model.students;
  graph.usersByStructure[0] = graph.usersByStructure[0].filter((u) => u.externalId !== second.externalId);
  const firstUser = graph.usersByStructure[0].find((u) => u.externalId === first.externalId)!;
  firstUser.classes = [{ name: "AUTRE" }];
  graph.parentsOf[firstUser.id] = [];

  const problems = diffModel(generated.model, graph, "");
  assert.deepEqual(problems.map((p) => p.category), ["élèves", "classes", "liens parents"]);
  assert.match(problems[0].actual, new RegExp(`1 manquant\\(s\\) : « ${second.firstName} ${second.lastName} » \\(externalId ${second.externalId}\\)`));
  assert.match(problems[1].actual, /devrait être dans la classe « .* » \(classes : AUTRE\)/);
  assert.match(problems[2].actual, /n'a pas pour parent/);
});

test("a structure which was not found hides the problems of its users", () => {
  const generated = oneStructure1D(1);
  const problems = diffModel(generated.model, { structures: [undefined], usersByStructure: [[]], parentsOf: {} }, "");
  assert.deepEqual(problems.map((p) => p.category), ["structures"]);
});

test("the number of users the import read is told when the platform gives it", () => {
  assert.equal(
    describeImport("jour 1", { status: "ok", result: { errors: {}, ignored: {}, files: {}, profiles: [], usersImported: 12 } }),
    "Dernier import (« jour 1 ») : 0 erreur, 0 enregistrement ignoré, 12 utilisateurs lus"
  );
  assert.match(describeImport(undefined, { status: "ok", result: { errors: {}, ignored: {}, files: {}, profiles: [], usersImported: 1 } }), /1 utilisateur lu$/);
});
