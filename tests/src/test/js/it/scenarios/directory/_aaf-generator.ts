/**
 * Programmatic generator for AAF test fixtures.
 *
 * This generator supports:
 *  - several structures, with UAI/name/type/academy, and inter-structure functional attachment
 *    (ENTEtablissementStructRattachFctl -> HAS_ATTACHMENT)
 *  - classes and functional groups, which the feeder never declares as their own XML entities: they
 *    only ever exist as a side effect of a student's/teacher's `<structureExternalId>$<name>` values
 *    (ENTEleveClasses/ENTEleveGroupes, ENTAuxEnsClassesMatieres/ENTAuxEnsGroupesMatieres). This
 *    generator models them as first-class objects for configuration purposes, then flattens them back
 *    into those attributes at serialization time.
 *  - students with 0..n relatives, sibling sharing (several students pointing at the very same
 *    relative external id, as real families do), and the legal-authority/"person in charge" quality
 *    digit carried by ENTElevePersRelEleve.
 *  - teachers (and non-teaching personnel, since AAF has no dedicated "Guest" category -- see note
 *    below) with functions/disciplines (ENTPersonFonctions), several taught classes/groups optionally
 *    tied to a field of study, head-teacher and "direction" assignment, and cross-structure functions
 *    (the mechanism the feeder uses to attach a person to a structure they don't administratively
 *    belong to).
 *  - optional MEF (modules) and matiere (fields of study) nomenclature files, referenced by students
 *    and teachers.
 *
 * Builder usage sketch:
 *
 *   const files: AAFFiles = new AAFGeneratorBuilder()
 *     .withNbStructures(3)
 *     .withUAIFormat("000000{idx}A")
 *     .withUAIForStructure(1, "0751234B")
 *     .withNbClasses(2)
 *     .withNbClassesForStructure(1, 5)
 *     .withNbStudentsPerClass(24)
 *     .withNbStudentsForClass(2, 30)
 *     .build()
 *     .generate();
 *
 *   importAAFStructureOrFail(files.files);
 *
 * Indexing convention used by every `...For...` override method (documented per-method too):
 *  - structure index: 0-based position among the structures declared via withNbStructures.
 *  - class/group/student/teacher index: 0-based, but GLOBAL and FLAT across every structure, counted
 *    in generation order (structure 0's classes first, then structure 1's, etc). This mirrors the
 *    single-index-parameter style requested for `...For...` setters while still letting you target
 *    an element that lives inside a nested structure.
 */

// ---------------------------------------------------------------------------------------------
// Public types
// ---------------------------------------------------------------------------------------------

/** A single ENTPersonFonctions entry template: `<structureExternalId>$code$name$positionCode$positionName`. */
export type AAFFunctionSpec = {
  code: string;
  name: string;
  positionCode: string;
  positionName: string;
};

export type AAFModuleSpec = {
  externalId: string;
  name: string;
  attachment: string;
  stat: string;
  level?: string;
  filiere?: string;
};

export type AAFFieldOfStudySpec = {
  externalId: string;
  name: string;
  nationalSubject?: string;
};

export type AAFGeneratedStructure = {
  index: number;
  externalId: string;
  uai: string;
  name: string;
  type: string;
  academy: string;
};

export type AAFGeneratedClass = {
  index: number;
  localIndex: number;
  structureIndex: number;
  name: string;
  externalId: string;
};

export type AAFGeneratedGroup = {
  index: number;
  localIndex: number;
  structureIndex: number;
  name: string;
  externalId: string;
  studentIndices: number[];
};

export type AAFGeneratedRelative = {
  index: number;
  externalId: string;
  firstName: string;
  lastName: string;
};

export type AAFGeneratedRelativeLink = {
  relativeIndex: number;
  quality: "1" | "2";
};

export type AAFGeneratedStudent = {
  index: number;
  externalId: string;
  firstName: string;
  lastName: string;
  structureIndex: number;
  classIndex: number;
  relativeLinks: AAFGeneratedRelativeLink[];
  groupIndices: number[];
};

export type AAFGeneratedTeacher = {
  index: number;
  externalId: string;
  firstName: string;
  lastName: string;
  structureIndex: number;
  isTeacher: boolean;
  classIndices: number[];
  groupIndices: number[];
  headTeacherOfClassIndices: number[];
  directionOfStructureIndices: number[];
};

export type AAFGeneratedModel = {
  structures: AAFGeneratedStructure[];
  classes: AAFGeneratedClass[];
  groups: AAFGeneratedGroup[];
  students: AAFGeneratedStudent[];
  relatives: AAFGeneratedRelative[];
  teachers: AAFGeneratedTeacher[];
};

/** Result of AAFGenerator.generate(): the raw files ready for upload, plus the model that produced them. */
export type AAFFiles = {
  /** filename -> file content, directly usable as the `files` field of an upload-aaf request. */
  files: Record<string, string>;
  model: AAFGeneratedModel;
};

// ---------------------------------------------------------------------------------------------
// Small generic helpers
// ---------------------------------------------------------------------------------------------

/** Holds a default value plus per-index overrides, resolved by `.get(idx)`. */
class IndexedConfig<T> {
  private overrides = new Map<number, T>();
  constructor(private defaultValue: T) {}
  setDefault(value: T): void {
    this.defaultValue = value;
  }
  setOverride(idx: number, value: T): void {
    this.overrides.set(idx, value);
  }
  get(idx: number): T {
    const override = this.overrides.get(idx);
    return override !== undefined ? override : this.defaultValue;
  }
}

function pad(n: number, width: number): string {
  return String(n).padStart(width, "0");
}

/** Replaces every `{token}` occurrence in `fmt` with the corresponding value from `tokens`. */
function formatTemplate(fmt: string, tokens: Record<string, string | number>): string {
  return Object.keys(tokens).reduce(
    (acc, key) => acc.split(`{${key}}`).join(String(tokens[key])),
    fmt
  );
}

function xmlEscape(value: string): string {
  return value.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;");
}

function ddmmyyyy(year: number, month: number, day: number): string {
  return `${pad(day, 2)}/${pad(month, 2)}/${year}`;
}

const FIRST_NAMES_F = ["Emma", "Lea", "Chloe", "Manon", "Camille", "Ines", "Sarah", "Julie", "Laura", "Anais"];
const FIRST_NAMES_M = ["Lucas", "Hugo", "Nathan", "Louis", "Adam", "Gabriel", "Raphael", "Arthur", "Jules", "Tom"];
const LAST_NAMES = [
  "Martin", "Bernard", "Dubois", "Thomas", "Robert", "Richard", "Petit", "Durand", "Leroy", "Moreau",
  "Simon", "Laurent", "Lefebvre", "Michel", "Garcia", "David", "Bertrand", "Roux", "Vincent", "Fontaine",
];

function firstNameFor(idx: number): string {
  const pool = idx % 2 === 0 ? FIRST_NAMES_M : FIRST_NAMES_F;
  const cycle = Math.floor(idx / 2);
  const base = pool[cycle % pool.length];
  const suffix = Math.floor(cycle / pool.length);
  return suffix === 0 ? base : `${base}${suffix}`;
}

function lastNameFor(idx: number): string {
  const base = LAST_NAMES[idx % LAST_NAMES.length];
  const suffix = Math.floor(idx / LAST_NAMES.length);
  return suffix === 0 ? base : `${base}${suffix}`;
}

function titleFor(idx: number): "M" | "F" {
  return idx % 2 === 0 ? "M" : "F";
}

const DEFAULT_FUNCTION_POOL: AAFFunctionSpec[] = [
  { code: "ENS", name: "ENSEIGNEMENT", positionCode: "P0210", positionName: "LETTRES HISTOIRE GEOGRAPHIE" },
  { code: "ENS", name: "ENSEIGNEMENT", positionCode: "P1300", positionName: "MATHEMATIQUES" },
  { code: "ENS", name: "ENSEIGNEMENT", positionCode: "P1700", positionName: "SCIENCES DE LA VIE ET DE LA TERRE" },
  { code: "ENS", name: "ENSEIGNEMENT", positionCode: "P2000", positionName: "EDUCATION PHYSIQUE ET SPORTIVE" },
];

const DEFAULT_NON_TEACHING_FUNCTION: AAFFunctionSpec = {
  code: "ADM",
  name: "ADMINISTRATION",
  positionCode: "P0000",
  positionName: "SECRETARIAT",
};

// ---------------------------------------------------------------------------------------------
// XML serialization helpers
// ---------------------------------------------------------------------------------------------

/** Builds `<attr name="...">` blocks, skipping any attribute whose value is empty/undefined. */
function attrs(entries: Array<[string, string | string[] | undefined | null]>): string {
  const parts: string[] = [];
  for (const [name, raw] of entries) {
    if (raw === undefined || raw === null) continue;
    const values = Array.isArray(raw) ? raw : [raw];
    if (values.length === 0) continue;
    const valuesXml = values.map((v) => `<value>${xmlEscape(v)}</value>`).join("");
    parts.push(`<attr name="${name}">${valuesXml}</attr>`);
  }
  return parts.join("\n");
}

function addRequest(categoryAttr: string, categoryValue: string, id: string, attributesXml: string): string {
  return `<addRequest>
<operationalAttributes><attr name="${categoryAttr}"><value>${categoryValue}</value></attr></operationalAttributes>
<identifier><id>${xmlEscape(id)}</id></identifier>
<attributes>
${attributesXml}
</attributes>
</addRequest>`;
}

function wrapFile(body: string): string {
  return `<?xml version="1.0" encoding="ISO-8859-15"?>
<!DOCTYPE ficAlimMENESR SYSTEM "ficAlimMENESR.dtd">
<ficAlimMENESR>
${body}
</ficAlimMENESR>`;
}

const FIC_ALIM_MENESR_DTD = `<!ELEMENT ficAlimMENESR (addRequest|modifyRequest|deleteRequest)*>
<!ELEMENT addRequest (operationalAttributes, identifier, attributes)>
<!ELEMENT modifyRequest (operationalAttributes, identifier, modifications)>
<!ELEMENT deleteRequest (operationalAttributes, identifier)>
<!ELEMENT operationalAttributes (attr)>
<!ELEMENT identifier (id)>
<!ELEMENT attributes (attr+)>
<!ELEMENT attr (value+)>
<!ELEMENT modifications (modification+)>
<!ELEMENT modification (value+)>
<!ELEMENT value (#PCDATA)>
<!ELEMENT id (#PCDATA)>
<!ATTLIST attr name CDATA #REQUIRED>
<!ATTLIST modification
	name CDATA #REQUIRED
	operation (replace) #REQUIRED>`;

// ---------------------------------------------------------------------------------------------
// Internal configuration bag built by the builder, consumed by AAFGenerator
// ---------------------------------------------------------------------------------------------

type StructureAttachment = { childStructureIdx: number; parentStructureIdx: number };
type GroupComposition = { groupCode: string; classNames: string[] };
type CrossStructureFunction = { teacherIdx: number; structureIdx: number };

class AAFGeneratorConfig {
  indexStart = 9999;

  nbStructures = 1;
  uaiFormat = "{idxPad7}A";
  uaiForStructure = new IndexedConfig<string | undefined>(undefined);
  structureNameFormat = "Structure {idx}";
  structureNameForStructure = new IndexedConfig<string | undefined>(undefined);
  structureTypeFormat = "LYCEE";
  structureTypeForStructure = new IndexedConfig<string | undefined>(undefined);
  academy = "TEST";
  academyForStructure = new IndexedConfig<string | undefined>(undefined);
  structureAttachments: StructureAttachment[] = [];
  groupCompositions: GroupComposition[][] = []; // indexed by structureIdx

  nbClasses = 1;
  nbClassesForStructure = new IndexedConfig<number | undefined>(undefined);
  classNameFormat = "{idx}A";
  classNameForClass = new IndexedConfig<string | undefined>(undefined);

  nbStudentsPerClass = 1;
  nbStudentsForClass = new IndexedConfig<number | undefined>(undefined);
  nbRelativesPerStudent = 2;
  nbRelativesForStudent = new IndexedConfig<number | undefined>(undefined);
  siblingGroupOfStudent = new Map<number, number>();
  nextSiblingGroupId = 0;
  personInChargeLinks = new Set<string>(); // `${studentIdx}:${relativeLocalIdx}`
  moduleForStudent = new IndexedConfig<string | undefined>(undefined);
  fieldsOfStudyForStudent = new IndexedConfig<string[] | undefined>(undefined);

  nbGroupsPerStructure = 0;
  nbGroupsForStructure = new IndexedConfig<number | undefined>(undefined);
  groupNameFormat = "GRP{idx}";
  groupNameForGroup = new IndexedConfig<string | undefined>(undefined);
  nbStudentsPerGroup = 0;
  nbStudentsForGroup = new IndexedConfig<number | undefined>(undefined);

  nbTeachersPerStructure = 1;
  nbTeachersForStructure = new IndexedConfig<number | undefined>(undefined);
  nbNonTeachingPersonnelPerStructure = 0;
  nbNonTeachingPersonnelForStructure = new IndexedConfig<number | undefined>(undefined);
  nbClassesPerTeacher = 1;
  nbClassesForTeacher = new IndexedConfig<number | undefined>(undefined);
  nbGroupsPerTeacher = 0;
  nbGroupsForTeacher = new IndexedConfig<number | undefined>(undefined);
  functionPool: AAFFunctionSpec[] = DEFAULT_FUNCTION_POOL;
  nbFunctionsPerTeacher = 1;
  nbFunctionsForTeacher = new IndexedConfig<number | undefined>(undefined);
  firstNameForTeacher = new IndexedConfig<string | undefined>(undefined);
  lastNameForTeacher = new IndexedConfig<string | undefined>(undefined);
  headTeacherOfClass = new Map<number, number>(); // classIdx -> teacherIdx
  directionAssignments: Array<{ structureIdx: number; teacherIdx: number }> = [];
  crossStructureFunctions: CrossStructureFunction[] = [];

  modules: AAFModuleSpec[] = [];
  fieldsOfStudy: AAFFieldOfStudySpec[] = [];
}

// ---------------------------------------------------------------------------------------------
// Generator: turns the resolved config into the AAF XML files
// ---------------------------------------------------------------------------------------------

export class AAFGenerator {
  constructor(private config: AAFGeneratorConfig) {}

  generate(): AAFFiles {
    const cfg = this.config;
    let idCounter = cfg.indexStart;
    const nextId = () => String(idCounter++);

    const structures: AAFGeneratedStructure[] = [];
    const classes: AAFGeneratedClass[] = [];
    const groups: AAFGeneratedGroup[] = [];
    const students: AAFGeneratedStudent[] = [];
    const relatives: AAFGeneratedRelative[] = [];
    const teachers: AAFGeneratedTeacher[] = [];

    const classesByStructure: number[][] = [];
    const groupsByStructure: number[][] = [];
    const studentsByStructure: number[][] = [];
    // relative external ids already created for a given sibling group, so siblings can reuse them.
    const siblingGroupRelatives = new Map<number, AAFGeneratedRelativeLink[]>();

    // --- Structures -------------------------------------------------------------------------
    for (let s = 0; s < cfg.nbStructures; s++) {
      const externalId = `STRUCT-${nextId()}`;
      const uai = cfg.uaiForStructure.get(s) ?? formatTemplate(cfg.uaiFormat, { idx: s, idxPad7: pad(s, 7) });
      const name = cfg.structureNameForStructure.get(s) ?? formatTemplate(cfg.structureNameFormat, { idx: s });
      const type = cfg.structureTypeForStructure.get(s) ?? cfg.structureTypeFormat;
      const academy = cfg.academyForStructure.get(s) ?? cfg.academy;
      structures.push({ index: s, externalId, uai, name, type, academy });
      classesByStructure.push([]);
      groupsByStructure.push([]);
      studentsByStructure.push([]);
    }

    // --- Classes ------------------------------------------------------------------------------
    for (let s = 0; s < cfg.nbStructures; s++) {
      const nb = cfg.nbClassesForStructure.get(s) ?? cfg.nbClasses;
      for (let local = 0; local < nb; local++) {
        const globalIdx = classes.length;
        const name = cfg.classNameForClass.get(globalIdx) ?? formatTemplate(cfg.classNameFormat, { idx: local, structureIdx: s, globalIdx });
        const externalId = `${structures[s].externalId}$${name}`;
        classes.push({ index: globalIdx, localIndex: local, structureIndex: s, name, externalId });
        classesByStructure[s].push(globalIdx);
      }
    }

    // --- Students (+ relatives) -----------------------------------------------------------------
    for (const klass of classes) {
      const nb = cfg.nbStudentsForClass.get(klass.index) ?? cfg.nbStudentsPerClass;
      for (let local = 0; local < nb; local++) {
        const studentIdx = students.length;
        const externalId = nextId();
        const firstName = firstNameFor(studentIdx);
        const lastName = lastNameFor(studentIdx);

        const siblingGroupId = cfg.siblingGroupOfStudent.get(studentIdx);
        let relativeLinks: AAFGeneratedRelativeLink[];
        if (siblingGroupId !== undefined && siblingGroupRelatives.has(siblingGroupId)) {
          relativeLinks = siblingGroupRelatives.get(siblingGroupId)!;
        } else {
          const nbRelatives = cfg.nbRelativesForStudent.get(studentIdx) ?? cfg.nbRelativesPerStudent;
          relativeLinks = [];
          for (let r = 0; r < nbRelatives; r++) {
            const relativeIdx = relatives.length;
            const relativeExternalId = nextId();
            relatives.push({
              index: relativeIdx,
              externalId: relativeExternalId,
              firstName: firstNameFor(relativeIdx + 1000),
              lastName, // same family name by default, as a real relative usually shares it
            });
            const quality: "1" | "2" = cfg.personInChargeLinks.has(`${studentIdx}:${r}`) ? "2" : "1";
            relativeLinks.push({ relativeIndex: relativeIdx, quality });
          }
          if (siblingGroupId !== undefined) {
            siblingGroupRelatives.set(siblingGroupId, relativeLinks);
          }
        }

        students.push({
          index: studentIdx,
          externalId,
          firstName,
          lastName,
          structureIndex: klass.structureIndex,
          classIndex: klass.index,
          relativeLinks,
          groupIndices: [],
        });
        studentsByStructure[klass.structureIndex].push(studentIdx);
      }
    }

    // --- Functional groups (+ student membership) ----------------------------------------------
    for (let s = 0; s < cfg.nbStructures; s++) {
      const nb = cfg.nbGroupsForStructure.get(s) ?? cfg.nbGroupsPerStructure;
      for (let local = 0; local < nb; local++) {
        const globalIdx = groups.length;
        const name = cfg.groupNameForGroup.get(globalIdx) ?? formatTemplate(cfg.groupNameFormat, { idx: local, structureIdx: s, globalIdx });
        const externalId = `${structures[s].externalId}$${name}`;
        const structureStudents = studentsByStructure[s];
        const nbStudents = Math.min(cfg.nbStudentsForGroup.get(globalIdx) ?? cfg.nbStudentsPerGroup, structureStudents.length);
        const memberStudentIndices: number[] = [];
        for (let k = 0; k < nbStudents; k++) {
          const studentIdx = structureStudents[(local + k) % structureStudents.length];
          memberStudentIndices.push(studentIdx);
          students[studentIdx].groupIndices.push(globalIdx);
        }
        groups.push({ index: globalIdx, localIndex: local, structureIndex: s, name, externalId, studentIndices: memberStudentIndices });
        groupsByStructure[s].push(globalIdx);
      }
    }

    // --- Teachers & non-teaching personnel -------------------------------------------------------
    for (let s = 0; s < cfg.nbStructures; s++) {
      const structureClasses = classesByStructure[s];
      const structureGroups = groupsByStructure[s];

      const nbTeaching = cfg.nbTeachersForStructure.get(s) ?? cfg.nbTeachersPerStructure;
      for (let local = 0; local < nbTeaching; local++) {
        const teacherIdx = teachers.length;
        const nbClassesAssigned = Math.min(cfg.nbClassesForTeacher.get(teacherIdx) ?? cfg.nbClassesPerTeacher, structureClasses.length);
        const classIndices = structureClasses.length === 0 ? [] : Array.from({ length: nbClassesAssigned }, (_, k) => structureClasses[(local + k) % structureClasses.length]);
        const nbGroupsAssigned = Math.min(cfg.nbGroupsForTeacher.get(teacherIdx) ?? cfg.nbGroupsPerTeacher, structureGroups.length);
        const groupIndices = structureGroups.length === 0 ? [] : Array.from({ length: nbGroupsAssigned }, (_, k) => structureGroups[(local + k) % structureGroups.length]);

        teachers.push(this.buildTeacher(teacherIdx, s, nextId(), classIndices, groupIndices, true));
      }

      const nbNonTeaching = cfg.nbNonTeachingPersonnelForStructure.get(s) ?? cfg.nbNonTeachingPersonnelPerStructure;
      for (let local = 0; local < nbNonTeaching; local++) {
        const teacherIdx = teachers.length;
        teachers.push(this.buildTeacher(teacherIdx, s, nextId(), [], [], false));
      }
    }

    // head-teacher / direction assignments are recorded after all teachers exist
    for (const [classIdx, teacherIdx] of cfg.headTeacherOfClass.entries()) {
      teachers[teacherIdx].headTeacherOfClassIndices.push(classIdx);
    }
    for (const { structureIdx, teacherIdx } of cfg.directionAssignments) {
      teachers[teacherIdx].directionOfStructureIndices.push(structureIdx);
    }

    const model: AAFGeneratedModel = { structures, classes, groups, students, relatives, teachers };
    const filesRecord = this.serialize(model);
    return { files: filesRecord, model };
  }

  private buildTeacher(
    teacherIdx: number,
    structureIdx: number,
    externalId: string,
    classIndices: number[],
    groupIndices: number[],
    isTeachingPool: boolean
  ): AAFGeneratedTeacher {
    const cfg = this.config;
    return {
      index: teacherIdx,
      externalId,
      firstName: cfg.firstNameForTeacher.get(teacherIdx) ?? firstNameFor(teacherIdx + 5000),
      lastName: cfg.lastNameForTeacher.get(teacherIdx) ?? lastNameFor(teacherIdx + 5000),
      structureIndex: structureIdx,
      isTeacher: isTeachingPool,
      classIndices,
      groupIndices,
      headTeacherOfClassIndices: [],
      directionOfStructureIndices: [],
    };
  }

  private serialize(model: AAFGeneratedModel): Record<string, string> {
    const cfg = this.config;
    const { structures, classes, groups, students, relatives, teachers } = model;

    // --- Structures --------------------------------------------------------------------------
    const structureBlocks = structures.map((structure) => {
      const attachmentExternalIds = cfg.structureAttachments
        .filter((a) => a.childStructureIdx === structure.index)
        .map((a) => structures[a.parentStructureIdx].externalId);
      const compositions = cfg.groupCompositions[structure.index] ?? [];
      const groupCompositionValues = compositions.map((c) => [c.groupCode, "0", ...c.classNames].join("$"));
      return addRequest(
        "categorieStructure",
        "EtabEducNat",
        structure.externalId,
        attrs([
          ["ENTStructureJointure", structure.externalId],
          ["ENTStructureUAI", structure.uai],
          ["ENTEtablissementUAI", structure.uai],
          ["ENTStructureNomCourant", structure.name],
          ["ENTStructureTypeStruct", structure.type],
          ["ENTEtablissementMinistereTutelle", "MINISTERE DE L'EDUCATION NATIONALE"],
          ["ENTEtablissementContrat", "PU"],
          ["ENTEtablissementStructRattachFctl", attachmentExternalIds],
          ["ENTServAcAcademie", structure.academy],
          ["ENTStructureGroupes", groupCompositionValues],
        ])
      );
    });

    // --- Students ------------------------------------------------------------------------------
    const studentBlocks = students.map((student) => {
      const structure = structures[student.structureIndex];
      const klass = classes[student.classIndex];
      const relativeLinkValues = student.relativeLinks.map((link) => `${relatives[link.relativeIndex].externalId}$1$0$${link.quality}$0$0`);
      const groupValues = student.groupIndices.map((gi) => groups[gi].externalId);
      const module = cfg.moduleForStudent.get(student.index);
      const fieldsOfStudy = cfg.fieldsOfStudyForStudent.get(student.index);
      const birthYear = 2003 + (student.index % 8);
      const birthDate = ddmmyyyy(birthYear, (student.index % 12) + 1, (student.index % 28) + 1);
      return addRequest(
        "categoriePersonne",
        "Eleve",
        student.externalId,
        attrs([
          ["ENTPersonJointure", student.externalId],
          ["ENTPersonDateNaissance", birthDate],
          ["ENTPersonNomPatro", student.lastName],
          ["sn", student.lastName],
          ["givenName", student.firstName],
          ["personalTitle", titleFor(student.index)],
          ["ENTElevePersRelEleve", relativeLinkValues],
          ["ENTPersonStructRattach", structure.externalId],
          ["ENTEleveBoursier", "N"],
          ["ENTEleveRegime", "EXTERNE LIBRE"],
          ["ENTEleveTransport", "N"],
          ["ENTEleveStatutEleve", "SCOLAIRE"],
          ["ENTEleveMEF", module],
          ["ENTEleveCodeEnseignements", fieldsOfStudy],
          ["ENTEleveClasses", klass.externalId],
          ["ENTEleveGroupes", groupValues],
        ])
      );
    });

    // --- Relatives (only relatives actually linked from a student are emitted: the feeder itself
    // silently discards any PersRelEleve record that isn't referenced by ENTElevePersRelEleve) -----
    const relativeBlocks = relatives.map((relative) => {
      const birthYear = 1970 + (relative.index % 30);
      const birthDate = ddmmyyyy(birthYear, (relative.index % 12) + 1, (relative.index % 28) + 1);
      return addRequest(
        "categoriePersonne",
        "PersRelEleve",
        relative.externalId,
        attrs([
          ["ENTPersonJointure", relative.externalId],
          ["ENTPersonDateNaissance", birthDate],
          ["ENTPersonNomPatro", relative.lastName],
          ["sn", relative.lastName],
          ["givenName", relative.firstName],
          ["personalTitle", titleFor(relative.index)],
          ["ENTPersonPays", "FRANCE"],
          ["ENTPersonAdresseDiffusion", "N"],
        ])
      );
    });

    // --- Teachers / non-teaching personnel -------------------------------------------------------
    const teacherBlocks = teachers.map((teacher) => {
      const homeStructure = structures[teacher.structureIndex];
      const nbFunctions = cfg.nbFunctionsForTeacher.get(teacher.index) ?? cfg.nbFunctionsPerTeacher;
      const pool = teacher.isTeacher ? cfg.functionPool : [DEFAULT_NON_TEACHING_FUNCTION];
      const functionValues: string[] = [];
      for (let f = 0; f < nbFunctions; f++) {
        const spec = pool[(teacher.index + f) % pool.length];
        functionValues.push(`${homeStructure.externalId}$${spec.code}$${spec.name}$${spec.positionCode}$${spec.positionName}`);
      }
      for (const cross of cfg.crossStructureFunctions) {
        if (cross.teacherIdx !== teacher.index) continue;
        const spec = pool[teacher.index % pool.length];
        const otherStructure = structures[cross.structureIdx];
        functionValues.push(`${otherStructure.externalId}$${spec.code}$${spec.name}$${spec.positionCode}$${spec.positionName}`);
      }

      const fosPool = cfg.fieldsOfStudy;
      const classValues = teacher.classIndices.map((ci, k) => {
        const klass = classes[ci];
        return fosPool.length > 0 ? `${klass.externalId}$${fosPool[k % fosPool.length].externalId}` : klass.externalId;
      });
      const groupValues = teacher.groupIndices.map((gi, k) => {
        const group = groups[gi];
        return fosPool.length > 0 ? `${group.externalId}$${fosPool[k % fosPool.length].externalId}` : group.externalId;
      });
      const headTeacherValues = teacher.headTeacherOfClassIndices.map((ci) => classes[ci].externalId);
      const directionValues = teacher.directionOfStructureIndices.map((si) => structures[si].externalId);
      const birthYear = 1965 + (teacher.index % 30);
      const birthDate = ddmmyyyy(birthYear, (teacher.index % 12) + 1, (teacher.index % 28) + 1);

      return addRequest(
        "categoriePersonne",
        "PersEducNat",
        teacher.externalId,
        attrs([
          ["ENTPersonJointure", teacher.externalId],
          ["ENTPersonDateNaissance", birthDate],
          ["ENTPersonNomPatro", teacher.lastName],
          ["sn", teacher.lastName],
          ["givenName", teacher.firstName],
          ["personalTitle", titleFor(teacher.index)],
          ["ENTPersonStructRattach", homeStructure.externalId],
          ["ENTAuxEnsClassesMatieres", classValues],
          ["ENTAuxEnsGroupesMatieres", groupValues],
          ["ENTAuxEnsClassesPrincipal", headTeacherValues],
          ["ENTEnsFonctionDir", directionValues],
          ["ENTPersonFonctions", functionValues],
          ["PersEducNatPresenceDevantEleves", teacher.isTeacher ? "O" : "N"],
        ])
      );
    });

    // --- Optional nomenclature files --------------------------------------------------------------
    const moduleBlocks = cfg.modules.map((m) =>
      addRequest(
        "categorieMef",
        "Mef",
        m.externalId,
        attrs([
          ["ENTMefJointure", m.externalId],
          ["ENTLibelleMef", m.name],
          ["ENTMEFRattach", m.attachment],
          ["ENTMEFSTAT11", m.stat],
          ["ENTNivFormation", m.level],
          ["ENTFiliere", m.filiere],
        ])
      )
    );
    const fieldOfStudyBlocks = cfg.fieldsOfStudy.map((f) =>
      addRequest(
        "categorieMatiere",
        "Matiere",
        f.externalId,
        attrs([
          ["ENTMatJointure", f.externalId],
          ["ENTLibelleMatiere", f.name],
          ["ENTMatiereRattach", f.nationalSubject],
        ])
      )
    );

    const files: Record<string, string> = {
      "ENT_IT_Complet_EtabEducNat_0001.xml": wrapFile(structureBlocks.join("\n")),
      "ENT_IT_Complet_Eleve_0001.xml": wrapFile(studentBlocks.join("\n")),
      "ENT_IT_Complet_PersEducNat_0001.xml": wrapFile(teacherBlocks.join("\n")),
      "ENT_IT_Complet_PersRelEleve_0001.xml": wrapFile(relativeBlocks.join("\n")),
      "ficAlimMENESR.dtd": FIC_ALIM_MENESR_DTD,
    };
    if (cfg.modules.length > 0) {
      files["ENT_IT_Complet_MefEducNat_0001.xml"] = wrapFile(moduleBlocks.join("\n"));
    }
    if (cfg.fieldsOfStudy.length > 0) {
      files["ENT_IT_Complet_MatiereEducNat_0001.xml"] = wrapFile(fieldOfStudyBlocks.join("\n"));
    }
    return files;
  }
}

// ---------------------------------------------------------------------------------------------
// Builder
// ---------------------------------------------------------------------------------------------

export class AAFGeneratorBuilder {
  private config = new AAFGeneratorConfig();

  /** Base numeric offset for every generated external id, to avoid collisions across test runs. */
  withIndexStart(n: number): this {
    this.config.indexStart = n;
    return this;
  }

  // --- Structures ----------------------------------------------------------------------------

  withNbStructures(n: number): this {
    this.config.nbStructures = n;
    return this;
  }

  /** Default UAI format applied to every structure. Supports `{idx}` and `{idxPad7}` (idx padded to 7 digits). */
  withUAIFormat(fmt: string): this {
    this.config.uaiFormat = fmt;
    return this;
  }

  /** Overrides the UAI of the structure at `structureIdx`. */
  withUAIForStructure(structureIdx: number, uai: string): this {
    this.config.uaiForStructure.setOverride(structureIdx, uai);
    return this;
  }

  withStructureNameFormat(fmt: string): this {
    this.config.structureNameFormat = fmt;
    return this;
  }

  withStructureNameForStructure(structureIdx: number, name: string): this {
    this.config.structureNameForStructure.setOverride(structureIdx, name);
    return this;
  }

  withStructureType(type: string): this {
    this.config.structureTypeFormat = type;
    return this;
  }

  withStructureTypeForStructure(structureIdx: number, type: string): this {
    this.config.structureTypeForStructure.setOverride(structureIdx, type);
    return this;
  }

  withAcademy(name: string): this {
    this.config.academy = name;
    return this;
  }

  withAcademyForStructure(structureIdx: number, name: string): this {
    this.config.academyForStructure.setOverride(structureIdx, name);
    return this;
  }

  /**
   * Declares that `childStructureIdx` is functionally attached to `parentStructureIdx`
   * (ENTEtablissementStructRattachFctl -> HAS_ATTACHMENT). The feeder resolves the parent from the
   * structures already parsed earlier in the same file, so prefer parentStructureIdx < childStructureIdx.
   */
  withStructureAttachment(childStructureIdx: number, parentStructureIdx: number): this {
    this.config.structureAttachments.push({ childStructureIdx, parentStructureIdx });
    return this;
  }

  /**
   * Pre-declares (on the structure itself, via ENTStructureGroupes) that functional group `groupCode`
   * is composed of the given class names of that same structure. This is only consulted by the feeder
   * when a teacher is later put into that same group (their taught classes get expanded accordingly).
   */
  withGroupComposition(structureIdx: number, groupCode: string, classNames: string[]): this {
    if (!this.config.groupCompositions[structureIdx]) this.config.groupCompositions[structureIdx] = [];
    this.config.groupCompositions[structureIdx].push({ groupCode, classNames });
    return this;
  }

  // --- Classes ---------------------------------------------------------------------------------

  /** Default number of classes per structure. */
  withNbClasses(n: number): this {
    this.config.nbClasses = n;
    return this;
  }

  /** Overrides the number of classes for the structure at `structureIdx`. */
  withNbClassesForStructure(structureIdx: number, n: number): this {
    this.config.nbClassesForStructure.setOverride(structureIdx, n);
    return this;
  }

  /** Default class-name format. Supports `{idx}` (local index within the structure), `{structureIdx}`, `{globalIdx}`. */
  withClassNameFormat(fmt: string): this {
    this.config.classNameFormat = fmt;
    return this;
  }

  /** Overrides the name of the class at the global (flat, cross-structure) index `classIdx`. */
  withClassNameForClass(classIdx: number, name: string): this {
    this.config.classNameForClass.setOverride(classIdx, name);
    return this;
  }

  // --- Students & relatives ----------------------------------------------------------------------

  /** Default number of students per class. */
  withNbStudentsPerClass(n: number): this {
    this.config.nbStudentsPerClass = n;
    return this;
  }

  /** Overrides the number of students for the class at the global index `classIdx`. */
  withNbStudentsForClass(classIdx: number, n: number): this {
    this.config.nbStudentsForClass.setOverride(classIdx, n);
    return this;
  }

  /** Default number of relatives generated per student (each relative gets legal-authority quality "1"). */
  withNbRelativesPerStudent(n: number): this {
    this.config.nbRelativesPerStudent = n;
    return this;
  }

  /** Overrides the number of relatives for the student at the global index `studentIdx`. */
  withNbRelativesForStudent(studentIdx: number, n: number): this {
    this.config.nbRelativesForStudent.setOverride(studentIdx, n);
    return this;
  }

  /**
   * Declares that the given (global) student indices are siblings: they will share the exact same
   * relative external id(s) instead of each getting their own, like real families in Siecle exports.
   * The relatives actually generated are the ones configured for the first student of the group.
   */
  withSiblings(studentIndices: number[]): this {
    const groupId = this.config.nextSiblingGroupId++;
    for (const idx of studentIndices) this.config.siblingGroupOfStudent.set(idx, groupId);
    return this;
  }

  /**
   * Marks the relative at local position `relativeLocalIndex` (0-based, in the order relatives are
   * generated for that student) of student `studentIdx` as quality "2" ("personne en charge") instead
   * of the default "1" (legal authority). Note the feeder only imports quality-"2" relatives when the
   * server config flag `import-person-in-charge` is enabled.
   */
  withPersonInChargeForStudent(studentIdx: number, relativeLocalIndex: number): this {
    this.config.personInChargeLinks.add(`${studentIdx}:${relativeLocalIndex}`);
    return this;
  }

  /** Links the student at `studentIdx` to a module (MEF) previously declared via withModules(). */
  withModuleForStudent(studentIdx: number, moduleExternalId: string): this {
    this.config.moduleForStudent.setOverride(studentIdx, moduleExternalId);
    return this;
  }

  /** Links the student at `studentIdx` to fields of study previously declared via withFieldsOfStudy(). */
  withFieldsOfStudyForStudent(studentIdx: number, fieldOfStudyExternalIds: string[]): this {
    this.config.fieldsOfStudyForStudent.setOverride(studentIdx, fieldOfStudyExternalIds);
    return this;
  }

  // --- Functional groups -------------------------------------------------------------------------

  /** Default number of functional groups per structure (0 by default: groups are opt-in). */
  withNbGroupsPerStructure(n: number): this {
    this.config.nbGroupsPerStructure = n;
    return this;
  }

  withNbGroupsForStructure(structureIdx: number, n: number): this {
    this.config.nbGroupsForStructure.setOverride(structureIdx, n);
    return this;
  }

  withGroupNameFormat(fmt: string): this {
    this.config.groupNameFormat = fmt;
    return this;
  }

  withGroupNameForGroup(groupIdx: number, name: string): this {
    this.config.groupNameForGroup.setOverride(groupIdx, name);
    return this;
  }

  /** Default number of students (picked from the owning structure) assigned to each functional group. */
  withNbStudentsPerGroup(n: number): this {
    this.config.nbStudentsPerGroup = n;
    return this;
  }

  withNbStudentsForGroup(groupIdx: number, n: number): this {
    this.config.nbStudentsForGroup.setOverride(groupIdx, n);
    return this;
  }

  // --- Teachers / non-teaching personnel -------------------------------------------------------

  /** Default number of teaching staff per structure. */
  withNbTeachersPerStructure(n: number): this {
    this.config.nbTeachersPerStructure = n;
    return this;
  }

  withNbTeachersForStructure(structureIdx: number, n: number): this {
    this.config.nbTeachersForStructure.setOverride(structureIdx, n);
    return this;
  }

  /**
   * Default number of non-teaching personnel per structure (0 by default). AAF has no dedicated
   * "Guest" category: non-teaching staff are PersEducNat records with no ENS/DOC function and
   * PersEducNatPresenceDevantEleves=N, which is the closest AAF equivalent.
   */
  withNbNonTeachingPersonnelPerStructure(n: number): this {
    this.config.nbNonTeachingPersonnelPerStructure = n;
    return this;
  }

  withNbNonTeachingPersonnelForStructure(structureIdx: number, n: number): this {
    this.config.nbNonTeachingPersonnelForStructure.setOverride(structureIdx, n);
    return this;
  }

  /** Default number of classes each teacher is assigned to teach (picked from their home structure's classes). */
  withNbClassesPerTeacher(n: number): this {
    this.config.nbClassesPerTeacher = n;
    return this;
  }

  withNbClassesForTeacher(teacherIdx: number, n: number): this {
    this.config.nbClassesForTeacher.setOverride(teacherIdx, n);
    return this;
  }

  /** Default number of functional groups each teacher is assigned to teach. */
  withNbGroupsPerTeacher(n: number): this {
    this.config.nbGroupsPerTeacher = n;
    return this;
  }

  withNbGroupsForTeacher(teacherIdx: number, n: number): this {
    this.config.nbGroupsForTeacher.setOverride(teacherIdx, n);
    return this;
  }

  /** Replaces the default pool of ENTPersonFonctions specs cycled through for teaching staff. */
  withFunctionPool(functions: AAFFunctionSpec[]): this {
    this.config.functionPool = functions;
    return this;
  }

  /** Default number of ENTPersonFonctions entries per teacher. */
  withNbFunctionsPerTeacher(n: number): this {
    this.config.nbFunctionsPerTeacher = n;
    return this;
  }

  withNbFunctionsForTeacher(teacherIdx: number, n: number): this {
    this.config.nbFunctionsForTeacher.setOverride(teacherIdx, n);
    return this;
  }

  /** Overrides the first name generated for the teacher at `teacherIdx` (default: cycles a French first-name pool). */
  withFirstNameForTeacher(teacherIdx: number, firstName: string): this {
    this.config.firstNameForTeacher.setOverride(teacherIdx, firstName);
    return this;
  }

  /** Overrides the last name generated for the teacher at `teacherIdx` (default: cycles a French last-name pool). */
  withLastNameForTeacher(teacherIdx: number, lastName: string): this {
    this.config.lastNameForTeacher.setOverride(teacherIdx, lastName);
    return this;
  }

  /** Marks the teacher at `teacherIdx` as head teacher ("professeur principal") of the class at `classIdx`. */
  withHeadTeacherForClass(classIdx: number, teacherIdx: number): this {
    this.config.headTeacherOfClass.set(classIdx, teacherIdx);
    return this;
  }

  /** Adds the teacher at `teacherIdx` to the direction/management team of the structure at `structureIdx`. */
  withDirectionForStructure(structureIdx: number, teacherIdx: number): this {
    this.config.directionAssignments.push({ structureIdx, teacherIdx });
    return this;
  }

  /**
   * Gives the teacher at `teacherIdx` an extra ENTPersonFonctions entry whose leading segment is
   * `structureIdx` instead of their home structure -- the mechanism the feeder uses to attach a
   * person to a structure they don't administratively belong to (multi-establishment staff).
   */
  withCrossStructureFunctionForTeacher(teacherIdx: number, structureIdx: number): this {
    this.config.crossStructureFunctions.push({ teacherIdx, structureIdx });
    return this;
  }

  // --- Nomenclature ------------------------------------------------------------------------------

  /** Declares MEF (module) nomenclature records; emits ENT_IT_Complet_MefEducNat_0001.xml when non-empty. */
  withModules(modules: AAFModuleSpec[]): this {
    this.config.modules = modules;
    return this;
  }

  /** Declares field-of-study (matiere) nomenclature records; emits ENT_IT_Complet_MatiereEducNat_0001.xml when non-empty. */
  withFieldsOfStudy(fieldsOfStudy: AAFFieldOfStudySpec[]): this {
    this.config.fieldsOfStudy = fieldsOfStudy;
    return this;
  }

  build(): AAFGenerator {
    return new AAFGenerator(this.config);
  }
}
