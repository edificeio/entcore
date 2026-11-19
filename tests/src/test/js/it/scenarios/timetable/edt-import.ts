import { check } from "k6";
import { chai, describe } from "https://jslib.k6.io/k6chaijs/4.3.4.0/index.js";
import {
  authenticateWeb,
  Structure,
} from "../../../node_modules/edifice-k6-commons/dist/index.js";
import {
  buildEdtXml,
  buildPopulation,
  EdtExpectations,
  getCourses,
  getReportText,
  importEdt,
  randomUai,
  setupEdtStructure,
} from "./_edt-utils.ts";

/*
 * EDT timetable import, end to end: directory HTTP upload -> feeder `manual-edt` -> EDTImporter.
 *
 * Springboard prerequisites (see _edt-utils.ts): feeder `"mode": "dev"` (feederMode=dev) and an
 * `edt.pronote-private-key` (edtKey) so that the plain XML export generated here is accepted.
 */

chai.config.logFailures = true;

const LARGE_IMPORT_MAX_MS = Number(__ENV.EDT_LARGE_IMPORT_MAX_MS || 300000);

export const options = {
  setupTimeout: "1h",
  maxRedirects: 0,
  thresholds: {
    checks: ["rate == 1.00"],
  },
  scenarios: {
    // A single VU runs both sizes sequentially: concurrent imports would make durations meaningless.
    edtImport: {
      exec: "testEdtImports",
      executor: "per-vu-iterations",
      vus: 1,
      iterations: 1,
      maxDuration: "40m",
      gracefulStop: "5s",
    },
  },
};

type EdtCase = {
  label: string;
  teachers: number;
  students: number;
  classSize: number;
  coursesPerClass: number;
  maxImportMs?: number;
};

const SMALL: EdtCase = { label: "small", teachers: 4, students: 16, classSize: 8, coursesPerClass: 10 };
const LARGE: EdtCase = {
  label: "large",
  teachers: 50,
  students: 950,
  classSize: 25,
  coursesPerClass: 20,
  maxImportMs: LARGE_IMPORT_MAX_MS,
};

export function testEdtImports() {
  authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
  runEdtImport(SMALL);
  runEdtImport(LARGE);
}

function runEdtImport(c: EdtCase) {
  const people = c.teachers + c.students;
  describe(`[Timetable-EDT] ${c.label} import (${people} people)`, () => {
    const population = buildPopulation(c.teachers, c.students, c.classSize, c.coursesPerClass);
    const { xml, expected } = buildEdtXml(population);
    const structureName = `IT EDT ${c.label} ${Date.now()}`;
    const structure = setupEdtStructure(structureName, randomUai(), population);
    console.log(`[${c.label}] structure ${structure.id} created, EDT export of ${xml.length} chars`);

    // First import: students are matched by name + birth date, teachers by name (their IDPN is set then).
    const first = importAndCheck(c, structure, xml, expected, "first import");
    if (first) {
      const report = getReportText(structure.id, first);
      check(report, {
        [`[${c.label}] report: all ${expected.students} students found`]: (r) =>
          r.indexOf(`${expected.students} élèves ont été trouvés`) >= 0 && r.indexOf("Les élèves suivants") < 0,
        [`[${c.label}] report: all ${expected.classes} classes found`]: (r) =>
          r.indexOf(`${expected.classes} classes ont été trouvées`) >= 0 && r.indexOf("Les classes suivantes") < 0,
      });
    }

    // Second import of the same export: idempotent, and teachers are now found through their IDPN.
    // (On the first import, EDTImporter.addProfesseur reports every teacher without IDPN as unknown,
    // even though courses get their teacherIds once the name matching has run.)
    const second = importAndCheck(c, structure, xml, expected, "re-import");
    if (second) {
      const report = getReportText(structure.id, second);
      check(report, {
        [`[${c.label}] re-import report: all ${expected.teachers} teachers found`]: (r) =>
          r.indexOf(`${expected.teachers} professeurs ont été trouvés`) >= 0 &&
          r.indexOf("Les enseignants suivants") < 0,
      });
    }
  });
}

/** Imports the export, checks the response and the resulting courses. Returns the report id, if any. */
function importAndCheck(
  c: EdtCase,
  structure: Structure,
  xml: string,
  expected: EdtExpectations,
  step: string,
): string | null {
  const { res, durationMs } = importEdt(structure, xml);
  console.log(`[${c.label}] ${step}: HTTP ${res.status} in ${durationMs} ms`);
  let body: any = null;
  try {
    body = res.json();
  } catch (e) {
    console.error(`[${c.label}] ${step}: non JSON response ${res.body}`);
  }
  check(res, {
    [`[${c.label}] ${step}: HTTP 200`]: (r) => r.status === 200,
    [`[${c.label}] ${step}: no error in result`]: () =>
      body !== null &&
      (!body.errors || Object.keys(body.errors).length === 0) &&
      !body["error.global"],
    [`[${c.label}] ${step}: timetable report id returned`]: () => body !== null && !!body.timetableReport,
  });
  if (res.status !== 200) {
    console.error(`[${c.label}] ${step}: ${res.body}`);
  }
  if (c.maxImportMs) {
    check(durationMs, {
      [`[${c.label}] ${step}: duration < ${c.maxImportMs} ms`]: (d) => d < c.maxImportMs!,
    });
  }

  const courses = getCourses(structure.id);
  console.log(`[${c.label}] ${step}: ${courses.length} courses (expected ${expected.courses})`);
  check(courses, {
    [`[${c.label}] ${step}: ${expected.courses} courses`]: (cs) => cs.length === expected.courses,
    [`[${c.label}] ${step}: every course has a teacher`]: (cs) =>
      cs.every((co: any) => Array.isArray(co.teacherIds) && co.teacherIds.length > 0),
    [`[${c.label}] ${step}: every course has a class`]: (cs) =>
      cs.every((co: any) => Array.isArray(co.classes) && co.classes.length > 0),
  });
  return body && body.timetableReport ? String(body.timetableReport) : null;
}

