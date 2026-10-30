/**
 * Engine of the feeder scenarios : plays the steps declared with the DSL (_feeder-dsl.ts) against a
 * {@link FeederGateway} and reports through a {@link Reporter}. Neither k6 nor http is used here, so the engine is
 * tested offline with a fake gateway (see _feeder-engine.test.ts).
 */
import type { AAFGeneratedModel, AAFGeneratedStudent, AAFGeneratorBuilder } from "../directory/_aaf-generator.ts";
import type { AAFFeeder, ImportSummary } from "../directory/_aaf-utils.ts";
import type { FeederGateway, PersonRow } from "./_feeder-gateway.ts";
import {
  abortMessage,
  countErrors,
  describeImport,
  describeImportFailure,
  ExpectationFailed,
  failureMessage,
  FeederScenarioError,
  plural,
  preview,
  stepPrefix,
  unknownReference,
} from "./_feeder-errors.ts";
import { relativesCsv } from "./_feeder-csv.ts";
import { diffModel, findByExternalId, personLabel } from "./_feeder-verify.ts";
import type { GraphStructure, GraphUser } from "./_feeder-verify.ts";

/** Bound on the students whose parents are read to compare a whole import with its files. */
export const MAX_STUDENTS_WITH_PARENTS_READ = 100;

/** What the engine tells about the run. */
export interface Reporter {
  pass(name: string): void;
  fail(name: string): void;
  info(message: string): void;
  /** Stops the scenario. */
  abort(message: string): never;
}

// ---------------------------------------------------------------------------------------------
// References to the people and structures of the scenario
// ---------------------------------------------------------------------------------------------

/** Structure of the generated files, by its (0-based) index. */
export type StructureHandle = { kind: "structure"; index: number };
/** Student of the generated files, by its (0-based, global) index. */
export type StudentHandle = { kind: "student"; index: number };
/** Relative of the generated files, by its (0-based, global) index. */
export type RelativeHandle = { kind: "relative"; index: number };
/** Relative created by hand during the scenario. */
export type ManualRelativeHandle = { kind: "manualRelative"; ref: string };
/** Relative imported with a CSV file during the scenario. */
export type CsvRelativeHandle = { kind: "csvRelative"; ref: string };
/** A relative, either generated, created by hand or imported by CSV. A plain string is the ref of a relative created by hand. */
export type RelativeRef = RelativeHandle | ManualRelativeHandle | CsvRelativeHandle | string;

/** How a relative which is not in the generated files got on the platform. */
export type RelativeOrigin = "manual" | "csv";

export type ManualRelative = { id: string; firstName: string; lastName: string; origin: RelativeOrigin };

const ORIGIN_LABELS: Record<RelativeOrigin, string> = { manual: "créé à la main", csv: "importé par CSV" };

function refName(ref: RelativeRef): string {
  return typeof ref === "string" ? ref : ref.kind === "relative" ? `relative(${ref.index})` : ref.ref;
}

// ---------------------------------------------------------------------------------------------
// State of a run
// ---------------------------------------------------------------------------------------------

export class World {
  /** Prefix the feeder puts in front of the external ids of the directory the files are uploaded in. */
  readonly externalIdPrefix: string;
  builder: AAFGeneratorBuilder | undefined;
  model: AAFGeneratedModel | undefined;
  structures: (GraphStructure | undefined)[] = [];
  lastImport: ImportSummary | undefined;
  lastImportLabel: string | undefined;
  readonly manualRelatives = new Map<string, ManualRelative>();
  private usersCache = new Map<number, GraphUser[]>();

  constructor(readonly ticket: string, readonly seed: number, readonly subPath: string, private gateway: FeederGateway) {
    this.externalIdPrefix = `${subPath}-`;
  }

  /** The platform changed : what has been read is out of date. */
  invalidate(): void {
    this.usersCache.clear();
  }

  generatedModel(what: string): AAFGeneratedModel {
    if (!this.model) {
      throw new FeederScenarioError(`${what} : aucun import n'a encore eu lieu, impossible de désigner un élément des fichiers générés (déclarer given(...) puis whenImport())`);
    }
    return this.model;
  }

  users(structureIndex: number): GraphUser[] {
    const structure = this.structures[structureIndex];
    if (!structure) return [];
    let users = this.usersCache.get(structureIndex);
    if (!users) {
      users = this.gateway.listUsers(structure.id);
      this.usersCache.set(structureIndex, users);
    }
    return users;
  }

  structureOf(handle: StructureHandle, what: string): GraphStructure {
    const model = this.generatedModel(what);
    if (handle.index < 0 || handle.index >= model.structures.length) {
      throw new FeederScenarioError(`${what} : structure(${handle.index}) n'existe pas, les fichiers en déclarent ${model.structures.length} (indices 0..${model.structures.length - 1})`);
    }
    const structure = this.structures[handle.index];
    if (!structure) {
      throw new FeederScenarioError(`${what} : la structure « ${model.structures[handle.index].name} » est introuvable sur la plateforme après l'import`);
    }
    return structure;
  }

  student(handle: StudentHandle, what: string): { model: AAFGeneratedStudent; user: GraphUser | undefined } {
    const model = this.generatedModel(what);
    if (handle.index < 0 || handle.index >= model.students.length) {
      throw new FeederScenarioError(`${what} : student(${handle.index}) n'existe pas, les fichiers en déclarent ${model.students.length} (indices 0..${model.students.length - 1})`);
    }
    const student = model.students[handle.index];
    return { model: student, user: findByExternalId(this.users(student.structureIndex), student.externalId, this.externalIdPrefix) };
  }

  /** @return The graph user of a student, or an explanation of why it is not there. */
  existingStudent(handle: StudentHandle, what: string): { model: AAFGeneratedStudent; user: GraphUser } {
    const found = this.student(handle, what);
    if (!found.user) {
      throw new FeederScenarioError(`${what} : l'élève ${personLabel(found.model.firstName, found.model.lastName, found.model.externalId)} n'existe pas sur la plateforme (${describeImport(this.lastImportLabel, this.lastImport)})`);
    }
    return { model: found.model, user: found.user };
  }

  /** @return The person designated by a relative reference, once it exists on the platform. */
  relative(ref: RelativeRef, what: string): PersonRow & { label: string } {
    if (typeof ref !== "string" && ref.kind === "relative") {
      const model = this.generatedModel(what);
      if (ref.index < 0 || ref.index >= model.relatives.length) {
        throw new FeederScenarioError(`${what} : relative(${ref.index}) n'existe pas, les fichiers en déclarent ${model.relatives.length} (indices 0..${model.relatives.length - 1})`);
      }
      const relative = model.relatives[ref.index];
      const structureIndex = model.students.find((s) => s.relativeLinks.some((l) => l.relativeIndex === ref.index))!.structureIndex;
      const user = findByExternalId(this.users(structureIndex), relative.externalId, this.externalIdPrefix);
      if (!user) {
        throw new FeederScenarioError(`${what} : le responsable ${personLabel(relative.firstName, relative.lastName, relative.externalId)} n'existe pas sur la plateforme`);
      }
      return { id: user.id, firstName: user.firstName, lastName: user.lastName, label: personLabel(user.firstName, user.lastName, user.externalId) };
    }
    const name = refName(ref);
    const manual = this.manualRelatives.get(name);
    if (!manual) {
      throw new FeederScenarioError(`${what} : ${unknownReference(typeof ref !== "string" && ref.kind === "csvRelative" ? "responsable importé par CSV" : "responsable créé à la main", name, Array.from(this.manualRelatives.keys()))}`);
    }
    return { id: manual.id, firstName: manual.firstName, lastName: manual.lastName, label: `« ${manual.firstName} ${manual.lastName} » (${ORIGIN_LABELS[manual.origin]}, ${name})` };
  }
}

// ---------------------------------------------------------------------------------------------
// Steps
// ---------------------------------------------------------------------------------------------

export type StepContext = {
  world: World;
  gateway: FeederGateway;
  /** Reports a broken expectation without stopping the step nor the scenario. */
  fail(failure: ExpectationFailed): void;
};

export type Step = {
  /** How the step is called in the messages, ex : `thenRelative(parent).hasChildren(student(0))` */
  label: string;
  run(context: StepContext): void;
};

export type ScenarioDefinition = {
  ticket: string;
  title: string;
  steps: Step[];
  /** Ticket of the bug the scenario reproduces : the scenario is only played on demand until it is fixed. */
  pendingFor?: string;
};

export type ScenarioRunData = { seed: number };

/** Unique directory name for the files of a run. */
export function subPathOf(ticket: string, seed: number): string {
  return `it-${ticket.replace(/[^A-Za-z0-9]+/g, "-")}-${seed}`.substring(0, 100);
}

// ---------------------------------------------------------------------------------------------
// Steps provided by the DSL
// ---------------------------------------------------------------------------------------------

/** Replaces the data of the following imports. */
export function givenStep(label: string, builder: AAFGeneratorBuilder): Step {
  return { label, run: ({ world }) => { world.builder = builder; } };
}

export type ImportOptions = {
  /** Compares the files with the platform after the import. On by default. */
  verify?: boolean;
};

export function importStep(label: string, importLabel: string, builder: AAFGeneratorBuilder | undefined, options: ImportOptions = {}): Step {
  return {
    label,
    run: (context) => {
      const { world, gateway } = context;
      if (builder) world.builder = builder;
      if (!world.builder) {
        throw new FeederScenarioError("aucun jeu de données à importer : déclarer given(<données>) avant whenImport()");
      }
      const feeder: AAFFeeder = world.builder.getFormat();
      // what depends on the run (not on the scenario) is applied here, so that the same data give the same files for every import
      const { files, model } = world.builder
        .withIndexStart(world.seed)
        .withUAIFormat(`${String(world.seed % 1000000).padStart(6, "0")}{idx}A`)
        .withStructureNameFormat(`IT ${world.ticket} ${world.seed} #{idx}`)
        .build()
        .generate();
      gateway.uploadAAF(feeder, world.subPath, files);
      const summary = gateway.importAndWait(feeder);
      world.lastImport = summary;
      world.lastImportLabel = importLabel;
      world.model = model;
      world.invalidate();
      if (summary.status !== "ok" || countErrors(summary.result && summary.result.errors) > 0) {
        throw new FeederScenarioError(describeImportFailure(importLabel, summary));
      }
      world.structures = model.structures.map((s) => gateway.findStructureByName(s.name));
      if (options.verify === false) return;

      const usersByStructure = model.structures.map((s) => world.users(s.index));
      const parentsOf: Record<string, string[]> = {};
      let read = 0;
      model.students.forEach((student) => {
        const user = findByExternalId(usersByStructure[student.structureIndex] || [], student.externalId, world.externalIdPrefix);
        if (user && student.relativeLinks.length > 0 && read < MAX_STUDENTS_WITH_PARENTS_READ) {
          parentsOf[user.id] = gateway.getParentsOf(user.id).map((p) => p.id);
          read++;
        }
      });
      diffModel(model, { structures: world.structures, usersByStructure, parentsOf }, world.externalIdPrefix)
        .forEach((problem) => context.fail(new ExpectationFailed(`${problem.category} : ${problem.expected}`, problem.actual)));
    },
  };
}

export type CreateRelativeOptions = {
  /** Structure the relative is created in. */
  in: StructureHandle;
  /** Students the relative is attached to at creation. */
  children?: StudentHandle[];
  firstName?: string;
  lastName?: string;
};

export function createRelativeStep(label: string, ref: string, options: CreateRelativeOptions): Step {
  return {
    label,
    run: ({ world, gateway }) => {
      const structure = world.structureOf(options.in, label);
      const children = (options.children || []).map((c) => world.existingStudent(c, label).user.id);
      const firstName = options.firstName || "Manuel";
      const lastName = options.lastName || ref.toUpperCase();
      const created = gateway.createRelative(structure.id, firstName, lastName, children);
      world.manualRelatives.set(ref, { id: created.id, firstName, lastName, origin: "manual" });
      world.invalidate();
    },
  };
}

export type ImportCsvRelativeOptions = {
  /** Structure the CSV file is imported in. */
  in: StructureHandle;
  /** Students the relative is attached to by the file. */
  children: StudentHandle[];
  firstName?: string;
  lastName?: string;
};

/** Imports a CSV file holding a relative attached to students of the generated files, with the import wizard. */
export function importCsvRelativeStep(label: string, ref: string, options: ImportCsvRelativeOptions): Step {
  return {
    label,
    run: ({ world, gateway }) => {
      const structure = world.structureOf(options.in, label);
      if (!structure.externalId) {
        throw new FeederScenarioError(`${label} : la plateforme ne renvoie pas l'externalId de la structure « ${structure.name} », nécessaire à l'import CSV`);
      }
      const children = options.children.map((c) => world.existingStudent(c, label).user);
      const firstName = options.firstName || "Csv";
      const lastName = options.lastName || ref.toUpperCase();
      // unique per run (the platform keeps what the previous runs created) and made of characters the feeder keeps as they are
      const externalId = `${world.subPath}-csv-${ref}`.replace(/[^A-Za-z0-9_-]/g, "-");
      gateway.importCsv(structure, { Relative: relativesCsv([{ externalId, firstName, lastName, childExternalIds: children.map((c) => c.externalId) }]) });
      world.invalidate();
      const created = world.users(options.in.index).find((u) => u.externalId === externalId);
      if (!created) {
        throw new FeederScenarioError(`${label} : le responsable « ${firstName} ${lastName} » (externalId ${externalId}) est introuvable dans la structure « ${structure.name} » après l'import CSV`);
      }
      world.manualRelatives.set(ref, { id: created.id, firstName, lastName, origin: "csv" });
    },
  };
}

export function linkRelativeStep(label: string, ref: RelativeRef, student: StudentHandle, link: boolean): Step {
  return {
    label,
    run: ({ world, gateway }) => {
      const relative = world.relative(ref, label);
      const studentUser = world.existingStudent(student, label).user;
      if (link) gateway.linkRelative(studentUser.id, relative.id);
      else gateway.unlinkRelative(studentUser.id, relative.id);
      world.invalidate();
    },
  };
}

function studentLabel(found: { model: AAFGeneratedStudent; user?: GraphUser }): string {
  return personLabel(found.model.firstName, found.model.lastName, found.user ? found.user.externalId : found.model.externalId);
}

/** "aucun enfant" / "2 enfants (Lucas Martin, Emma Petit)" + what is missing when several were expected. */
function describeFound(found: PersonRow[], noun: string, missing: string[], expectedCount: number): string {
  const list = found.length === 0 ? `aucun ${noun}` : `${plural(found.length, noun)} (${preview(found.map((p) => p.displayName || `${p.firstName || "?"} ${p.lastName || "?"}`))})`;
  return expectedCount > 1 ? `${list} — ${plural(missing.length, "manquant")} : ${preview(missing)}` : list;
}

export function relativeHasChildrenStep(label: string, ref: RelativeRef, children: StudentHandle[]): Step {
  return {
    label,
    run: ({ world, gateway, fail }) => {
      const relative = world.relative(ref, label);
      const expected = children.map((c) => world.existingStudent(c, label));
      const actual = gateway.getChildrenOf(relative.id);
      const actualIds = actual.map((c) => c.id);
      const missing = expected.filter((e) => actualIds.indexOf(e.user.id) < 0);
      if (missing.length > 0) {
        fail(new ExpectationFailed(
          `${relative.label} est le parent de ${preview(expected.map(studentLabel))}`,
          describeFound(actual, "enfant", missing.map(studentLabel), expected.length)
        ));
      }
    },
  };
}

export function relativeHasNoChildStep(label: string, ref: RelativeRef): Step {
  return {
    label,
    run: ({ world, gateway, fail }) => {
      const relative = world.relative(ref, label);
      const actual = gateway.getChildrenOf(relative.id);
      if (actual.length > 0) {
        fail(new ExpectationFailed(`${relative.label} n'a aucun enfant`, describeFound(actual, "enfant", [], 1)));
      }
    },
  };
}

export function studentHasParentsStep(label: string, student: StudentHandle, parents: RelativeRef[]): Step {
  return {
    label,
    run: ({ world, gateway, fail }) => {
      const found = world.existingStudent(student, label);
      const expected = parents.map((p) => world.relative(p, label));
      const actual = gateway.getParentsOf(found.user.id);
      const actualIds = actual.map((p) => p.id);
      const missing = expected.filter((e) => actualIds.indexOf(e.id) < 0);
      if (missing.length > 0) {
        fail(new ExpectationFailed(
          `${studentLabel(found)} a pour parent ${preview(expected.map((e) => e.label))}`,
          describeFound(actual, "parent", missing.map((m) => m.label), expected.length)
        ));
      }
    },
  };
}

export function lastImportStep(label: string, check: (summary: ImportSummary) => ExpectationFailed | undefined): Step {
  return {
    label,
    run: ({ world, fail }) => {
      if (!world.lastImport) {
        throw new FeederScenarioError(`${label} : aucun import n'a encore eu lieu`);
      }
      const failure = check(world.lastImport);
      if (failure) fail(failure);
    },
  };
}

// ---------------------------------------------------------------------------------------------
// Runner
// ---------------------------------------------------------------------------------------------

/** Plays a scenario. Every broken expectation is reported ; a scenario which cannot go on is aborted. */
export function runScenario(definition: ScenarioDefinition, data: ScenarioRunData, gateway: FeederGateway, reporter: Reporter): void {
  const { ticket, steps } = definition;
  const world = new World(ticket, data.seed, subPathOf(ticket, data.seed), gateway);
  reporter.info(`[${ticket}] ${definition.title} — seed ${data.seed}, dossier d'import ${world.subPath}`);
  gateway.login();
  let broken = 0;
  steps.forEach((step, i) => {
    const prefix = stepPrefix(ticket, i + 1, steps.length, step.label);
    const failures: ExpectationFailed[] = [];
    const context: StepContext = { world, gateway, fail: (failure) => failures.push(failure) };
    const importContext = () => `${describeImport(world.lastImportLabel, world.lastImport)}`;
    reporter.info(`${prefix} ...`);
    try {
      step.run(context);
    } catch (e) {
      failures.forEach((failure) => reporter.fail(failureMessage(prefix, failure, importContext())));
      const reason = e instanceof FeederScenarioError || e instanceof ExpectationFailed ? (<Error>e).message : `erreur inattendue : ${e && (<Error>e).stack ? (<Error>e).stack : e}`;
      reporter.fail(abortMessage(prefix, reason));
      reporter.abort(abortMessage(prefix, reason));
    }
    if (failures.length === 0) {
      reporter.pass(`${prefix} ok`);
    } else {
      broken += failures.length;
      failures.forEach((failure) => reporter.fail(failureMessage(prefix, failure, importContext())));
    }
  });
  reporter.info(`[${ticket}] ${broken === 0 ? "toutes les attentes sont satisfaites" : plural(broken, "attente non satisfaite", "attentes non satisfaites")}`);
}

/** Removes what a run uploaded. Never fails : a clean up problem must not hide the result of the scenario. */
export function cleanUp(definition: ScenarioDefinition, data: ScenarioRunData, gateway: FeederGateway, feeders: AAFFeeder[], reporter: Reporter): void {
  const subPath = subPathOf(definition.ticket, data.seed);
  try {
    gateway.login();
    feeders.forEach((feeder) => gateway.cleanAAF(feeder, subPath));
  } catch (e) {
    reporter.info(`[${definition.ticket}] nettoyage du dossier ${subPath} impossible : ${(<Error>e).message}`);
  }
}
