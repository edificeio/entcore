/**
 * In memory platform understanding the AAF1D files of the generator, to play scenarios without any platform :
 * used by the offline tests (_feeder-engine.test.ts) and by the k6 self test (_feeder-selftest.ts).
 */
import type { Reporter } from "./_feeder-engine.ts";
import type { CsvFiles, FeederGateway, PersonRow } from "./_feeder-gateway.ts";
import type { AAFFeeder, ImportSummary } from "../directory/_aaf-utils.ts";
import type { GraphStructure, GraphUser } from "./_feeder-verify.ts";

/**
 * {@code linkSources} gives, for each parent of a student, the source of the link : "AAF1D" (written by the import),
 * "MANUAL" (created by hand) or "" (written by a CSV import, which sets none).
 */
export type FakeUser = GraphUser & { parents: string[]; structureId: string; linkSources: Record<string, string> };

/** In memory platform understanding the AAF1D files of the generator. */
export class FakePlatform implements FeederGateway {
  readonly calls: string[] = [];
  structures: GraphStructure[] = [];
  users: FakeUser[] = [];
  private imports = 0;
  private sequence = 0;
  /** ENABLING-1377 : at the second import, the links of the relatives created by hand are lost. */
  dropManualLinksOnReimport = false;
  /**
   * ENABLING-1377 (cause found on a real platform) : an AAF1D import deletes the links of a student which have no
   * source and whose parent is not in its file, when the file gives the student at least one relative.
   */
  dropSourcelessLinks = false;
  /** Files given to the CSV wizard. */
  csvImports: CsvFiles[] = [];
  /** What the import answers instead of an empty report. */
  importSummary: ImportSummary = { status: "ok", result: { errors: {}, ignored: {}, files: {}, profiles: [] } };
  uploaded: Record<string, string>[] = [];

  login(): void { this.calls.push("login"); }

  uploadAAF(feeder: AAFFeeder, subPath: string, files: Record<string, string>): void {
    this.calls.push(`upload ${feeder} ${subPath}`);
    this.uploaded.push(files);
    this.subPath = subPath;
    this.files = files;
  }
  private subPath = "";
  private files: Record<string, string> = {};

  cleanAAF(feeder: AAFFeeder, subPath: string): void { this.calls.push(`clean ${feeder} ${subPath}`); }

  private parse(xml: string): { category: string; id: string; attrs: Record<string, string[]> }[] {
    return xml.split("<addRequest>").slice(1).map((block) => {
      const attrs: Record<string, string[]> = {};
      for (const match of block.matchAll(/<attr name="([^"]+)">((?:<value>[^<]*<\/value>)*)<\/attr>/g)) {
        attrs[match[1]] = Array.from(match[2].matchAll(/<value>([^<]*)<\/value>/g)).map((v) => v[1]);
      }
      return { category: (attrs["categoriePersonne"] || attrs["categorieStructure"])[0], id: /<id>([^<]*)<\/id>/.exec(block)![1], attrs };
    });
  }

  importAndWait(feeder: AAFFeeder): ImportSummary {
    this.imports++;
    this.calls.push(`import ${feeder}`);
    if (this.importSummary.status !== "ok" || (this.importSummary.result && Object.keys(this.importSummary.result.errors).length > 0)) {
      return this.importSummary;
    }
    const prefix = `${this.subPath}-`;
    const loaded = (name: string) => this.parse(this.files[`ENT_IT_Complet_${name}_0001.xml`]);
    loaded("EtabEducNat").forEach((s) => {
      const name = s.attrs["ENTStructureNomCourant"][0];
      if (!this.structures.find((x) => x.name === name)) this.structures.push({ id: `s-${this.structures.length}`, name, externalId: `${this.subPath}-${s.id}` });
    });
    const upsert = (entry: { category: string; id: string; attrs: Record<string, string[]> }, type: string) => {
      const externalId = `${prefix}${entry.id}`;
      let user = this.users.find((u) => u.externalId === externalId);
      // a relative has no structure in its own file : it follows its children (a single structure is enough for the fake)
      const structure = this.structures[0];
      if (!user) {
        user = { id: `u-${this.sequence++}`, externalId, type, firstName: entry.attrs["ENTPersonPrenom"][0], lastName: entry.attrs["ENTPersonNom"][0], classes: [], parents: [], structureId: structure.id, linkSources: {} };
        this.users.push(user);
      }
      return user;
    };
    loaded("PersEducNat").forEach((t) => upsert(t, "Teacher"));
    loaded("PersRelEleve").forEach((r) => upsert(r, "Relative"));
    loaded("Eleve").forEach((e) => {
      const student = upsert(e, "Student");
      const classValue = e.attrs["ENTPersonClasses"][0].split("$");
      student.classes = [{ name: classValue[4] }];
      const fromFiles = (e.attrs["ENTEleveAutoriteParentale"] || []).map((rel) => this.users.find((u) => u.externalId === `${prefix}${rel}`)!.id);
      const others = student.parents.filter((p) => fromFiles.indexOf(p) < 0).filter((p) => {
        const source = student.linkSources[p];
        if (source === "AAF1D") return false; // not in the file any more
        if (this.dropManualLinksOnReimport && this.imports > 1) return false;
        return !(this.dropSourcelessLinks && fromFiles.length > 0 && source === "");
      });
      fromFiles.forEach((p) => { student.linkSources[p] = "AAF1D"; });
      student.parents = [...fromFiles, ...others];
    });
    return this.importSummary;
  }

  findStructureByName(name: string): GraphStructure | undefined { return this.structures.find((s) => s.name === name); }
  listUsers(structureId: string): GraphUser[] { return this.users.filter((u) => u.structureId === structureId); }
  getParentsOf(studentId: string): PersonRow[] { return this.users.find((u) => u.id === studentId)!.parents.map((id) => this.users.find((u) => u.id === id)!); }
  getChildrenOf(relativeId: string): PersonRow[] { return this.users.filter((u) => u.parents.indexOf(relativeId) >= 0); }
  createRelative(structureId: string, firstName: string, lastName: string, childrenIds: string[]): PersonRow {
    const relative: FakeUser = { id: `u-${this.sequence++}`, externalId: "", type: "Relative", firstName, lastName, classes: [], parents: [], structureId, linkSources: {} };
    this.users.push(relative);
    childrenIds.forEach((id) => this.link(id, relative.id, "MANUAL"));
    return relative;
  }
  private link(studentId: string, relativeId: string, source: string): void {
    const student = this.users.find((u) => u.id === studentId)!;
    student.parents.push(relativeId);
    student.linkSources[relativeId] = source;
  }
  linkRelative(studentId: string, relativeId: string): void { this.link(studentId, relativeId, "MANUAL"); }
  importCsv(structure: GraphStructure, files: CsvFiles): void {
    this.calls.push(`csv ${Object.keys(files).join(",")}`);
    this.csvImports.push(files);
    const rows = (files.Relative || "").split("\n").filter((line) => line.trim() !== "").map((line) => line.split(";").map((cell) => cell.replace(/^"|"$/g, "").replace(/""/g, '"')));
    const header = rows[0];
    rows.slice(1).forEach((row) => {
      const externalId = row[header.indexOf("externalId")];
      let relative = this.users.find((u) => u.externalId === externalId);
      if (!relative) {
        relative = { id: `u-${this.sequence++}`, externalId, type: "Relative", firstName: row[header.indexOf("Prénom responsable")], lastName: row[header.indexOf("Nom responsable")], classes: [], parents: [], structureId: structure.id, linkSources: {} };
        this.users.push(relative);
      }
      header.forEach((column, i) => {
        const child = column === "childExternalId" && row[i] ? this.users.find((u) => u.externalId === row[i]) : undefined;
        if (child && child.parents.indexOf(relative!.id) < 0) this.link(child.id, relative!.id, "");
      });
    });
  }
  unlinkRelative(studentId: string, relativeId: string): void {
    const student = this.users.find((u) => u.id === studentId)!;
    student.parents = student.parents.filter((p) => p !== relativeId);
  }
}

export class RecordingReporter implements Reporter {
  passed: string[] = [];
  failed: string[] = [];
  infos: string[] = [];
  pass(name: string) { this.passed.push(name); }
  fail(name: string) { this.failed.push(name); }
  info(message: string) { this.infos.push(message); }
  abort(message: string): never { throw new Error(`ABORT ${message}`); }
}
