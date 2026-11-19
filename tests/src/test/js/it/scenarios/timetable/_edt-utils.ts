import http from "k6/http";
import { fail } from "k6";
import { FormData } from "https://jslib.k6.io/formdata/0.0.2/index.js";
import {
  getHeaders,
  getSchoolByName,
  Structure,
} from "../../../node_modules/edifice-k6-commons/dist/index.js";

/*
 * Helpers for EDT (Index Éducation) timetable import integration tests.
 *
 * The EDT export is generated as PLAIN XML: the springboard feeder must run with `"mode": "dev"`
 * (`feederMode=dev`) and have an `edt.pronote-private-key` configured (`edtKey`, any path), so that
 * EDTUtils.getContent falls back to the raw file when decryption fails.
 */

const rootUrl = __ENV.ROOT_URL;
/** Imports are synchronous on the HTTP side: directory waits up to 600 s for the feeder. */
const LONG_TIMEOUT = "10m";
/** Weeks of the school year (relative to week 1 = current week) in which generated courses happen. */
const COURSE_WEEKS = [2, 3, 4, 5];
const SUBJECTS = [
  { code: "MATHS", label: "Mathematiques" },
  { code: "FRANC", label: "Francais" },
  { code: "HIGEO", label: "Histoire geographie" },
  { code: "ANGLA", label: "Anglais" },
  { code: "EPS", label: "Education physique" },
];
const DAYS_PER_WEEK = 5;
const SLOTS_PER_DAY = 8;

export type EdtPerson = {
  ident: string;
  idpn: string;
  lastName: string;
  firstName: string;
  /** yyyy-MM-dd */
  birthDate: string;
  className: string;
};

export type EdtPopulation = {
  teachers: EdtPerson[];
  students: EdtPerson[];
  classes: string[];
  coursesPerClass: number;
};

export type EdtExpectations = {
  courses: number;
  teachers: number;
  students: number;
  classes: number;
  groups: number;
};

/** Converts an index into a letters-only suffix (names must not contain digits). */
function toLetters(index: number, length: number = 3): string {
  let s = "";
  let n = index;
  for (let i = 0; i < length; i++) {
    s = String.fromCharCode(65 + (n % 26)) + s;
    n = Math.floor(n / 26);
  }
  return s;
}

/** Random letters-only string (names must not contain digits). */
function randomLetters(length: number): string {
  let s = "";
  for (let i = 0; i < length; i++) {
    s += String.fromCharCode(65 + Math.floor(Math.random() * 26));
  }
  return s;
}

function capitalize(s: string): string {
  return s.charAt(0).toUpperCase() + s.slice(1).toLowerCase();
}

/**
 * Builds a population: `nbTeachers` teachers and `nbStudents` students spread in classes
 * of `classSize` students. Names are unique inside the structure (required by the EDT matching queries)
 * and, thanks to a random letters-only `runTag`, across runs: user logins are derived from the names and
 * must be globally unique, while users of previous runs are never deleted.
 */
export function buildPopulation(
  nbTeachers: number,
  nbStudents: number,
  classSize: number,
  coursesPerClass: number,
  runTag: string = randomLetters(5),
): EdtPopulation {
  const nbClasses = Math.ceil(nbStudents / classSize);
  const classes: string[] = [];
  for (let i = 0; i < nbClasses; i++) {
    classes.push(`EDT-${toLetters(i, 2)}`);
  }
  const teachers: EdtPerson[] = [];
  for (let i = 0; i < nbTeachers; i++) {
    const l = toLetters(i);
    teachers.push({
      ident: `P${i}`,
      idpn: `IDPNPROF${i}`,
      lastName: `EDTPROF${l}${runTag}`,
      firstName: capitalize(`Prof${l}${runTag}`),
      birthDate: "1980-05-12",
      className: classes[i % nbClasses],
    });
  }
  const students: EdtPerson[] = [];
  for (let i = 0; i < nbStudents; i++) {
    const l = toLetters(i);
    students.push({
      ident: `E${i}`,
      idpn: `IDPNELEVE${i}`,
      lastName: `EDTELEVE${l}${runTag}`,
      firstName: capitalize(`Eleve${l}${runTag}`),
      birthDate: "2012-03-15",
      className: classes[Math.floor(i / classSize)],
    });
  }
  return { teachers, students, classes, coursesPerClass };
}

function csvLine(values: string[]): string {
  return values.map((v) => `"${v}"`).join(";");
}

/** Teachers and students CSV files, same format as edifice-k6-commons/data/structure/*.default.csv. */
export function toCsv(population: EdtPopulation): {
  teachers: string;
  students: string;
} {
  const teachers = [
    csvLine([
      "Civilité", "Nom usage", "Nom", "Prénom", "Adresse", "CP", "Commune", "Pays",
      "Courriel", "Téléphone domicile", "Téléphone portable", "Directeur", "Classe",
    ]),
    ...population.teachers.map((t) =>
      csvLine(["M.", "", t.lastName, t.firstName, "", "", "", "", "", "", "", "non", t.className]),
    ),
  ].join("\n");
  const students = [
    csvLine([
      "Nom Elève", "Nom d'usage Elève", "Prénom Elève", "Date naissance", "Sexe",
      "Adresse1", "Cp1", "Commune1", "Pays1", "Adresse2", "Cp2", "Commune2", "Pays2",
      " Cycle", "Niveau", "Classe", "Attestation fournie", "Autorisations associations",
      "Autorisations photos", "Décision de passage",
    ]),
    ...population.students.map((s) =>
      csvLine([
        s.lastName, "", s.firstName, s.birthDate, "F", "", "", "", "", "", "", "", "",
        "CYCLE IV", "5EME", s.className, "Oui", "Non", "Oui", "",
      ]),
    ),
  ].join("\n");
  return { teachers, students };
}

/** yyyy-MM-dd -> dd/MM/yyyy (format of EDT exports, converted back by StringValidation.convertDate). */
function toFrenchDate(isoDate: string): string {
  const [y, m, d] = isoDate.split("-");
  return `${d}/${m}/${y}`;
}

/** Monday of the current week, yyyy-MM-dd: week 1 of the generated school year. */
function currentMonday(): string {
  const now = new Date();
  const day = (now.getUTCDay() + 6) % 7; // 0 = monday
  const monday = new Date(Date.UTC(now.getUTCFullYear(), now.getUTCMonth(), now.getUTCDate() - day));
  return monday.toISOString().substring(0, 10);
}

function weeksMask(weeks: number[]): string {
  // EDTImporter tests bit (1L << i) for week i; the mask stays far below 2^53 for weeks < 53.
  return String(weeks.reduce((mask, w) => mask + Math.pow(2, w), 0));
}

/**
 * Builds the EDT XML read by EDTHandler (SAX, entities recognized by tag name whatever the depth).
 * Order matters: Classe before Groupe (groups reference already known classes), and the first <Cours>
 * element is a container skipped by EDTHandler.
 */
export function buildEdtXml(population: EdtPopulation): {
  xml: string;
  expected: EdtExpectations;
} {
  const out: string[] = [];
  const semaines = weeksMask(COURSE_WEEKS);
  out.push('<?xml version="1.0" encoding="UTF-8"?>');
  out.push("<EDT>");
  out.push(`<AnneeScolaire DatePremierJourSemaine1="${currentMonday()}"/>`);

  out.push('<GrilleHoraire DureePlace="60">');
  for (let p = 0; p < SLOTS_PER_DAY; p++) {
    const start = 8 + p;
    out.push(
      `<Place Numero="${p}" LibelleHeureDebut="${String(start).padStart(2, "0")}h00" ` +
        `LibelleHeureFin="${String(start + 1).padStart(2, "0")}h00"/>`,
    );
  }
  out.push("</GrilleHoraire>");

  out.push("<Matieres>");
  SUBJECTS.forEach((s, i) =>
    out.push(`<Matiere Ident="M${i}" Code="${s.code}" Libelle="${s.label}"/>`),
  );
  out.push("</Matieres>");

  out.push("<Classes>");
  population.classes.forEach((c, i) => out.push(`<Classe Ident="C${i}" Nom="${c}"/>`));
  out.push("</Classes>");

  out.push("<Groupes>");
  population.classes.forEach((c, i) =>
    out.push(`<Groupe Ident="G${i}" Nom="${c}-GRP"><Classe Ident="C${i}"/></Groupe>`),
  );
  out.push("</Groupes>");

  out.push("<Salles>");
  population.classes.forEach((_, i) => out.push(`<Salle Ident="S${i}" Nom="Salle ${i}"/>`));
  out.push("</Salles>");

  out.push("<Professeurs>");
  for (const t of population.teachers) {
    out.push(
      `<Professeur Ident="${t.ident}" IDPN="${t.idpn}" Nom="${t.lastName}" Prenom="${t.firstName}" ` +
        `DateNaissance="${toFrenchDate(t.birthDate)}"/>`,
    );
  }
  out.push("</Professeurs>");

  const classIndex: Record<string, number> = {};
  population.classes.forEach((c, i) => (classIndex[c] = i));
  out.push("<Eleves>");
  for (const s of population.students) {
    out.push(
      `<Eleve Ident="${s.ident}" IDPN="${s.idpn}" Nom="${s.lastName}" Prenom="${s.firstName}" ` +
        `DateNaissance="${toFrenchDate(s.birthDate)}">` +
        `<Classe Ident="C${classIndex[s.className]}" DateEntree="2020-09-01" DateSortie="2099-07-01"/>` +
        `</Eleve>`,
    );
  }
  out.push("</Eleves>");

  // Each class gets `coursesPerClass` courses on distinct (day, slot) pairs, taught by teachers in rotation.
  if (population.coursesPerClass > DAYS_PER_WEEK * SLOTS_PER_DAY) {
    fail(`At most ${DAYS_PER_WEEK * SLOTS_PER_DAY} courses per class`);
  }
  out.push("<Cours>");
  let nbCours = 0;
  population.classes.forEach((_, ci) => {
    for (let k = 0; k < population.coursesPerClass; k++) {
      const teacher = population.teachers[(ci * population.coursesPerClass + k) % population.teachers.length];
      const day = 1 + (k % DAYS_PER_WEEK);
      const slot = Math.floor(k / DAYS_PER_WEEK);
      out.push(
        `<Cours Ident="CO${nbCours}" Jour="${day}" NumeroPlaceDebut="${slot}" NombrePlaces="1">` +
          `<Matiere Ident="M${k % SUBJECTS.length}"/>` +
          `<Professeur Ident="${teacher.ident}" Semaines="${semaines}"/>` +
          `<Classe Ident="C${ci}" Semaines="${semaines}"/>` +
          `<Salle Ident="S${ci}" Semaines="${semaines}"/>` +
          `</Cours>`,
      );
      nbCours++;
    }
  });
  out.push("</Cours>");
  out.push("</EDT>");

  return {
    xml: out.join("\n"),
    expected: {
      courses: nbCours * COURSE_WEEKS.length,
      teachers: population.teachers.length,
      students: population.students.length,
      classes: population.classes.length,
      groups: population.classes.length,
    },
  };
}

/** Random UAI-like identifier, unique per run. */
export function randomUai(): string {
  return `0${String(Math.floor(Math.random() * 1e6)).padStart(6, "0")}E`;
}

/**
 * Creates the structure from CSV (long timeout: the wizard import of ~1000 users can exceed k6's default 60 s),
 * sets its UAI (required by the timetable import) and initializes it as an EDT structure.
 */
export function setupEdtStructure(name: string, uai: string, population: EdtPopulation): Structure {
  const csv = toCsv(population);
  const fd = new FormData();
  fd.append("type", "CSV");
  fd.append("structureName", name);
  fd.append("Teacher", http.file(csv.teachers, "enseignants.csv"));
  fd.append("Student", http.file(csv.students, "eleves.csv"));
  const headers = getHeaders();
  //@ts-ignore
  headers["Content-Type"] = "multipart/form-data; boundary=" + fd.boundary;
  //@ts-ignore
  let res = http.post(`${rootUrl}/directory/wizard/import`, fd.body(), { headers, timeout: LONG_TIMEOUT });
  if (res.status !== 200) {
    fail(`Could not create structure ${name} (${res.status}): ${res.body}`);
  }
  const structure: Structure = getSchoolByName(name);
  if (!structure) {
    fail(`Structure ${name} has been imported but is not in the structures list`);
  }

  res = http.put(
    `${rootUrl}/directory/structure/${structure.id}`,
    JSON.stringify({ name, UAI: uai }),
    { headers: getHeaders("application/json") },
  );
  if (res.status !== 200) {
    fail(`Could not set UAI ${uai} on structure ${name} (${res.status}): ${res.body}`);
  }

  res = http.put(
    `${rootUrl}/directory/timetable/init/${structure.id}`,
    JSON.stringify({ type: "EDT" }),
    { headers: getHeaders("application/json") },
  );
  if (res.status !== 200) {
    fail(`Could not init EDT timetable on structure ${name} (${res.status}): ${res.body}`);
  }
  return structure;
}

/** Uploads the EDT export. The HTTP call returns once the feeder has finished the import. */
export function importEdt(structure: Structure, xml: string) {
  const fd = new FormData();
  fd.append("file", http.file(xml, "export.xml", "text/xml"));
  const headers = getHeaders();
  //@ts-ignore
  headers["Content-Type"] = "multipart/form-data; boundary=" + fd.boundary;
  const start = Date.now();
  //@ts-ignore
  const res = http.post(`${rootUrl}/directory/timetable/import/${structure.id}`, fd.body(), {
    headers,
    timeout: LONG_TIMEOUT,
  });
  const durationMs = Date.now() - start;
  if (res.status !== 200 && String(res.body).indexOf("invalid.edt.key") >= 0) {
    fail(
      "EDT import is not configured on this springboard: set `edtKey` (feeder edt.pronote-private-key) " +
        "and `feederMode=dev` so that the plain XML export is accepted.",
    );
  }
  return { res, durationMs };
}

/** Active (not deleted) courses of the structure. */
export function getCourses(structureId: string): any[] {
  const res = http.get(`${rootUrl}/directory/timetable/courses/${structureId}`, {
    headers: getHeaders(),
  });
  if (res.status !== 200) {
    fail(`Could not list courses of ${structureId} (${res.status}): ${res.body}`);
  }
  return <any[]>res.json();
}

/** Text of the timetable import report (rendered from feeder template/timetable-report.txt). */
export function getReportText(structureId: string, reportId: string): string {
  const res = http.get(`${rootUrl}/directory/timetable/import/${structureId}/report/${reportId}`, {
    headers: getHeaders(),
  });
  if (res.status !== 200) {
    fail(`Could not get report ${reportId} (${res.status}): ${res.body}`);
  }
  const body = <any>res.json();
  return String(body.report || "");
}
