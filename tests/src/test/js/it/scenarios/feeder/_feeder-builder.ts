/**
 * Builder of the steps of a feeder scenario (the vocabulary of the DSL). Pure : neither k6 nor http is used here,
 * so scenarios can be built and played offline (see _feeder-engine.test.ts). The k6 glue is in _feeder-dsl.ts.
 */
import { AAFGeneratorBuilder } from "../directory/_aaf-generator.ts";
import { ExpectationFailed, countErrors, plural } from "./_feeder-errors.ts";
import {
  createRelativeStep,
  importCsvRelativeStep,
  givenStep,
  importStep,
  lastImportStep,
  linkRelativeStep,
  relativeHasChildrenStep,
  relativeHasNoChildStep,
  studentHasParentsStep,
} from "./_feeder-engine.ts";
import type {
  CreateRelativeOptions,
  CsvRelativeHandle,
  ImportCsvRelativeOptions,
  ImportOptions,
  ManualRelativeHandle,
  RelativeHandle,
  RelativeRef,
  ScenarioDefinition,
  Step,
  StructureHandle,
  StudentHandle,
} from "./_feeder-engine.ts";

function describeRef(ref: RelativeRef): string {
  return typeof ref === "string" ? ref : ref.kind === "relative" ? `relative(${ref.index})` : ref.ref;
}

function describeStudents(students: StudentHandle[]): string {
  return students.map((s) => `student(${s.index})`).join(", ");
}

export class ScenarioBuilder {
  readonly steps: Step[] = [];
  private pendingFor: string | undefined;

  constructor(private ticket: string) {}

  // --- Data and handles ------------------------------------------------------------------------

  /** Files of the second degree. The ids, names and UAI of the structures are given by the scenario. */
  aaf(): AAFGeneratorBuilder {
    return new AAFGeneratorBuilder().withFormat("AAF");
  }

  /** Files of the first degree. The ids, names and UAI of the structures are given by the scenario. */
  aaf1d(): AAFGeneratorBuilder {
    return new AAFGeneratorBuilder().withFormat("AAF1D");
  }

  structure(index: number): StructureHandle {
    return { kind: "structure", index };
  }

  student(index: number): StudentHandle {
    return { kind: "student", index };
  }

  relative(index: number): RelativeHandle {
    return { kind: "relative", index };
  }

  /** Declares a relative which will be created by hand. Using a handle rather than a string turns typos into compile errors. */
  manualRelative(ref: string): ManualRelativeHandle {
    return { kind: "manualRelative", ref };
  }

  /** Declares a relative which will be imported with a CSV file. */
  csvRelative(ref: string): CsvRelativeHandle {
    return { kind: "csvRelative", ref };
  }

  // --- Given / when ----------------------------------------------------------------------------

  /** The data the next imports read. Can be called again to change them, ex : the files of the next day. */
  given(data: AAFGeneratorBuilder): this {
    this.steps.push(givenStep(`given(${data.getFormat()})`, data));
    return this;
  }

  /**
   * Imports the data of the scenario, then checks that what the files declare is on the platform.
   * @param label How the import is called in the messages, ex : "le lendemain"
   * @param data Replaces the data of the scenario for this import and the next ones
   */
  whenImport(label: string = "import", data?: AAFGeneratorBuilder, options?: ImportOptions): this {
    this.steps.push(importStep(`whenImport("${label}")`, label, data, options));
    return this;
  }

  /** What a person does by hand on the platform. */
  get whenManual(): ManualActions {
    return new ManualActions(this);
  }

  /** What a person does with the import wizard : a CSV file imported in a structure. */
  get whenCsv(): CsvActions {
    return new CsvActions(this);
  }

  // --- Then ------------------------------------------------------------------------------------

  thenRelative(ref: RelativeRef): RelativeExpectations {
    return new RelativeExpectations(this, ref);
  }

  thenStudent(student: StudentHandle): StudentExpectations {
    return new StudentExpectations(this, student);
  }

  /** What the last import reported. */
  get thenImport(): ImportExpectations {
    return new ImportExpectations(this);
  }

  /** The scenario reproduces a bug which is not fixed yet : it is only played when FEEDER_RUN_PENDING=true. */
  pending(bugTicket: string): this {
    this.pendingFor = bugTicket;
    return this;
  }

  add(step: Step): this {
    this.steps.push(step);
    return this;
  }

  definition(title: string): ScenarioDefinition {
    return { ticket: this.ticket, title, steps: this.steps, pendingFor: this.pendingFor };
  }
}

export class ManualActions {
  constructor(private scenario: ScenarioBuilder) {}

  /** Creates a relative with the "create an account" form, attached to the given students. */
  createRelative(ref: ManualRelativeHandle | string, options: CreateRelativeOptions): ScenarioBuilder {
    const name = describeRef(ref);
    return this.scenario.add(createRelativeStep(`whenManual.createRelative(${name}, children: ${describeStudents(options.children || [])})`, name, options));
  }

  linkRelative(ref: RelativeRef, student: StudentHandle): ScenarioBuilder {
    return this.scenario.add(linkRelativeStep(`whenManual.linkRelative(${describeRef(ref)}, student(${student.index}))`, ref, student, true));
  }

  unlinkRelative(ref: RelativeRef, student: StudentHandle): ScenarioBuilder {
    return this.scenario.add(linkRelativeStep(`whenManual.unlinkRelative(${describeRef(ref)}, student(${student.index}))`, ref, student, false));
  }
}

export class CsvActions {
  constructor(private scenario: ScenarioBuilder) {}

  /** Imports a file creating a relative (source CSV) attached to the given students, which come from the generated files. */
  importRelative(ref: CsvRelativeHandle | string, options: ImportCsvRelativeOptions): ScenarioBuilder {
    const name = describeRef(ref);
    return this.scenario.add(importCsvRelativeStep(`whenCsv.importRelative(${name}, children: ${describeStudents(options.children)})`, name, options));
  }
}

export class RelativeExpectations {
  constructor(private scenario: ScenarioBuilder, private ref: RelativeRef) {}

  hasChildren(...children: StudentHandle[]): ScenarioBuilder {
    return this.scenario.add(relativeHasChildrenStep(`thenRelative(${describeRef(this.ref)}).hasChildren(${describeStudents(children)})`, this.ref, children));
  }

  hasNoChild(): ScenarioBuilder {
    return this.scenario.add(relativeHasNoChildStep(`thenRelative(${describeRef(this.ref)}).hasNoChild()`, this.ref));
  }
}

export class StudentExpectations {
  constructor(private scenario: ScenarioBuilder, private student: StudentHandle) {}

  hasParents(...parents: RelativeRef[]): ScenarioBuilder {
    return this.scenario.add(studentHasParentsStep(`thenStudent(student(${this.student.index})).hasParents(${parents.map(describeRef).join(", ")})`, this.student, parents));
  }
}

export class ImportExpectations {
  constructor(private scenario: ScenarioBuilder) {}

  hasNoIgnoredRecord(): ScenarioBuilder {
    return this.scenario.add(lastImportStep("thenImport.hasNoIgnoredRecord()", (summary) => {
      const ignored = summary.result ? summary.result.ignored : {};
      const count = Object.values(ignored || {}).reduce((total, records) => total + records.length, 0)
        + Object.values((summary.result && summary.result.ignoredNotReturned) || {}).reduce((total, n) => total + n, 0);
      if (count === 0) return undefined;
      const reasons = Object.keys(ignored).map((file) => `${file}: ${ignored[file].slice(0, 3).map((r) => r.reason).join(", ")}`);
      return new ExpectationFailed("aucun enregistrement ignoré par le dernier import", `${plural(count, "enregistrement ignoré", "enregistrements ignorés")} (${reasons.join(" ; ")})`);
    }));
  }

  hasNoError(): ScenarioBuilder {
    return this.scenario.add(lastImportStep("thenImport.hasNoError()", (summary) => {
      const errors = countErrors(summary.result && summary.result.errors);
      return errors === 0 ? undefined : new ExpectationFailed("aucune erreur au dernier import", plural(errors, "erreur"));
    }));
  }
}
