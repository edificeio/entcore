/**
 * Comparison between what the generator put in the files (the model) and what the platform holds afterwards.
 * Pure (no k6, no http) : the graph is read by the gateway and given here as a snapshot.
 *
 * The comparison is a *subset* one : everything the files declare must exist, but what the platform holds
 * beyond the files (accounts created by hand, accounts of other sources...) is ignored, since scenarios
 * precisely mix several sources.
 */
import type { AAFGeneratedModel, AAFGeneratedStudent } from "../directory/_aaf-generator.ts";
import { preview } from "./_feeder-errors.ts";

export type GraphUser = {
  id: string;
  externalId: string;
  type: string;
  firstName: string;
  lastName: string;
  classes?: { name: string }[];
};

export type GraphStructure = { id: string; name: string; externalId?: string };

export type GraphSnapshot = {
  /** Indexed like model.structures. {@code undefined} when the structure has not been found. */
  structures: (GraphStructure | undefined)[];
  /** Indexed like model.structures. */
  usersByStructure: GraphUser[][];
  /** student user id -> ids of its parents. Absent when the parents of the student have not been read. */
  parentsOf: Record<string, string[]>;
};

/** One broken aspect of the comparison. There is at most one per category, to give one check per category. */
export type ModelProblem = { category: string; expected: string; actual: string };

/**
 * The feeder prefixes the external ids of the AAF files of a registered import directory with the name of this
 * directory. Both forms are accepted, so that the comparison does not depend on how the directory was registered.
 */
export function externalIdMatches(actual: string, modelExternalId: string, prefix: string): boolean {
  return actual === modelExternalId || (prefix !== "" && actual === `${prefix}${modelExternalId}`);
}

export function findByExternalId(users: GraphUser[], modelExternalId: string, prefix: string): GraphUser | undefined {
  return users.find((u) => externalIdMatches(u.externalId, modelExternalId, prefix));
}

export function personLabel(firstName: string, lastName: string, externalId: string): string {
  return `« ${firstName} ${lastName} » (externalId ${externalId})`;
}

function studentLabel(student: AAFGeneratedStudent): string {
  return personLabel(student.firstName, student.lastName, student.externalId);
}

/**
 * @param prefix The prefix the feeder puts in front of the external ids ("" when unknown)
 * @param maxParentChecks Bound on the students whose parents are compared
 */
export function diffModel(model: AAFGeneratedModel, snapshot: GraphSnapshot, prefix: string): ModelProblem[] {
  const missingStructures: string[] = [];
  const missingStudents: string[] = [];
  const missingTeachers: string[] = [];
  const missingRelatives: string[] = [];
  const wrongClass: string[] = [];
  const missingParents: string[] = [];

  model.structures.forEach((structure) => {
    if (!snapshot.structures[structure.index]) {
      missingStructures.push(`« ${structure.name} » (externalId ${structure.externalId})`);
    }
  });

  model.structures.forEach((structure) => {
    const users = snapshot.usersByStructure[structure.index] || [];
    if (!snapshot.structures[structure.index]) return;

    const students = model.students.filter((s) => s.structureIndex === structure.index);
    const relativeIndices = new Set<number>();
    students.forEach((student) => student.relativeLinks.forEach((l) => relativeIndices.add(l.relativeIndex)));

    students.forEach((student) => {
      const actual = findByExternalId(users, student.externalId, prefix);
      if (!actual) {
        missingStudents.push(studentLabel(student));
        return;
      }
      const expectedClass = model.classes[student.classIndex].name;
      if (!(actual.classes || []).some((c) => c.name === expectedClass)) {
        const actualClasses = (actual.classes || []).map((c) => c.name);
        wrongClass.push(`${studentLabel(student)} devrait être dans la classe « ${expectedClass} » (classes : ${preview(actualClasses)})`);
      }
    });

    model.teachers
      .filter((t) => t.structureIndex === structure.index)
      .forEach((teacher) => {
        if (!findByExternalId(users, teacher.externalId, prefix)) {
          missingTeachers.push(personLabel(teacher.firstName, teacher.lastName, teacher.externalId));
        }
      });

    relativeIndices.forEach((index) => {
      const relative = model.relatives[index];
      if (!findByExternalId(users, relative.externalId, prefix)) {
        missingRelatives.push(personLabel(relative.firstName, relative.lastName, relative.externalId));
      }
    });

    students.forEach((student) => {
      const actualStudent = findByExternalId(users, student.externalId, prefix);
      const actualParents = actualStudent ? snapshot.parentsOf[actualStudent.id] : undefined;
      if (!actualStudent || actualParents === undefined) return;
      student.relativeLinks.forEach((link) => {
        const relative = model.relatives[link.relativeIndex];
        const actualRelative = findByExternalId(users, relative.externalId, prefix);
        if (actualRelative && actualParents.indexOf(actualRelative.id) < 0) {
          missingParents.push(`${studentLabel(student)} n'a pas pour parent ${personLabel(relative.firstName, relative.lastName, relative.externalId)}`);
        }
      });
    });
  });

  const problems: ModelProblem[] = [];
  const add = (category: string, expected: string, items: string[], noun: string = "manquant(s)") => {
    if (items.length > 0) {
      problems.push({ category, expected, actual: `${items.length} ${noun} : ${preview(items, 4)}` });
    }
  };
  add("structures", "les structures des fichiers existent", missingStructures);
  add("élèves", "les élèves des fichiers existent", missingStudents);
  add("enseignants", "les enseignants des fichiers existent", missingTeachers);
  add("responsables", "les responsables référencés par un élève existent", missingRelatives);
  add("classes", "chaque élève est dans sa classe", wrongClass, "écart(s)");
  add("liens parents", "chaque élève est lié à ses responsables", missingParents, "lien(s) absent(s)");
  return problems;
}
