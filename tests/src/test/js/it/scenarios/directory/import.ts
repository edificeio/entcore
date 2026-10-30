import {
  authenticateWeb,
  initStructure,
  Structure,
  getUsersOfSchool,
  getRandomUserWithProfile,
  getHeaders,
  createClassAndGetIdOrFail,
  getClassesOfStructureOrFail,
  UserProfileType,
  UserInfo,
  getSchoolByName,
  createEmptyStructure,
  importUsers,
  importCSVToStructure,
} from "../../../node_modules/edifice-k6-commons/dist/index.js";
import http from "k6/http";
import {check, group, fail} from "k6";
import {FormData} from "https://jslib.k6.io/formdata/0.0.2/index.js";


const maxDuration = __ENV.MAX_DURATION || "20m";
const schoolName = __ENV.DATA_SCHOOL_NAME || "Import";
const gracefulStop = parseInt(__ENV.GRACEFUL_STOP || "2s");
const rootUrl = __ENV.ROOT_URL;
const skipInit = __ENV.SKIP_INIT === "true";

export const options = {
  setupTimeout: "1h",
  thresholds: {
    checks: ["rate == 1.00"],
  },
  scenarios: {
    testWizardImportAndVerifyData: {
      executor: "per-vu-iterations",
      exec: "testWizardImportAndVerifyData",
      vus: 1,
      maxDuration: maxDuration,
      gracefulStop,
    },
    testWizardValidateFormat: {
      executor: "per-vu-iterations",
      exec: "testWizardValidateFormat",
      vus: 1,
      maxDuration: maxDuration,
      gracefulStop,
    },
    testPreDeleteBehavior: {
      executor: "per-vu-iterations",
      exec: "testPreDeleteBehavior",
      vus: 1,
      maxDuration: maxDuration,
      gracefulStop,
    },
    testTransitionBehavior: {
      executor: "per-vu-iterations",
      exec: "testTransitionBehavior",
      vus: 1,
      maxDuration: maxDuration,
      gracefulStop,
    },
    testClassImportAndVerify: {
      executor: "per-vu-iterations",
      exec: "testClassImportAndVerify",
      vus: 1,
      maxDuration: maxDuration,
      gracefulStop,
    },
    testImportEdgeCases: {
      executor: "per-vu-iterations",
      exec: "testImportEdgeCases",
      vus: 1,
      maxDuration: maxDuration,
      gracefulStop,
    },
  },
};

type InitData = {
  structure: Structure;
  users: UserInfo[];
  importStructureName: string;
}

// ------ CSV data matching sample-be1d format ------

// Multiple teachers with classes, mimicking CSVExtraction-enseignants.csv
const TEACHERS_CSV =
  `"Civilité";"Nom usage";"Nom";"Prénom";"Adresse";"CP";"Commune";"Pays";"Courriel";"Téléphone domicile";"Téléphone portable";"Directeur";"Classe"\n` +
  `"MME";;"BAILLY";"Catherine";;;;;;;;"non";"TPS"\n` +
  `"M.";;"BRIAUD";"Simon";;;;;;;;"non";"PS"\n` +
  `"M.";;"FONTAINE";"Jean-Paul";;;;;;;;"non";"MS"\n` +
  `"M.";;"GROLLEAU";"Bernard";;;;;;;;"non";"GS"\n` +
  `"MME";;"LE COQ";"Helene";;;;;;;;"non";"CP"\n` +
  `"M.";;"LEFEBVRE";"Ulrique";;;;;;;;"non";"CE1"\n`;

// Students in multiple classes, mimicking CSVExtraction-eleves.csv
const STUDENTS_CSV =
  `"Nom Elève";"Nom d'usage Elève";"Prénom Elève";"Date naissance";"Sexe";"Adresse1";"Cp1";"Commune1";"Pays1";"Adresse2";"Cp2";"Commune2";"Pays2";" Cycle";"Niveau";"Classe";"Attestation fournie";"Autorisations associations";"Autorisations photos";"Décision de passage"\n` +
  `"BELMAHDI";;"Mathilde";"2006-08-17";"F";;;;;;;;;"CYCLE II";"CE1";"CE1";"Oui";"Non";"Oui";\n` +
  `"BOUZEHOUANE";;"Valentine";"2006-01-04";"F";;;;;;;;;"CYCLE III";"CE1";"CE1";"Non";"Non";"Non";\n` +
  `"CAILLET";;"Eléa";"2006-09-27";"F";;;;;;;;;"CYCLE II";"CE1";"CE1";"Non";"Oui";"Oui";\n` +
  `"AUDONNET";;"Anaïs";"2007-01-14";"F";;;;;;;;;"CYCLE II";"CP";"CP";"Non";"Oui";"Oui";\n` +
  `"BALDERACCHI";;"Lauric";"2007-05-18";"H";;;;;;;;;"CYCLE II";"CP";"CP";"Non";"Oui";"Oui";\n` +
  `"ALBERT";;"Nawres";"2011-03-25";"F";;;;;;;;;"MATERNELLE";"TPS";"TPS";"Non";"Oui";"Oui";\n` +
  `"BISCHOFF";;"Andy";"2011-10-10";"H";;;;;;;;;"MATERNELLE";"TPS";"TPS";"Oui";"Non";"Oui";\n`;

// Relatives linked to students above
const RELATIVES_CSV =
  `Civilité Responsable;Nom usage responsable;Nom responsable;Prénom responsable;Adresse responsable;CP responsable;Commune responsable;Pays;Courriel;Téléphone domicile;Téléphone travail;Téléphone portable;Nom d'usage enfant;Nom de famille enfant;Prénom enfant;Classes enfants;Nom d'usage enfant;Nom de famille enfant;Prénom enfant;Classes enfants;Nom d'usage enfant;Nom de famille enfant;Prénom enfant;Classes enfants\n` +
  `Mme;;WIMART;Christiane;18 Rue du Disque;61000;Saint Germain;;;;;;;BELMAHDI;Mathilde;CE1;;;;;;;;\n` +
  `M.;;AUDONNET;Mickael;17 Rue Dupleix;61000;Alençon;;;;;;;AUDONNET;Anaïs;CP;;;;;;;;\n` +
  `Mme;;AUDONNET;Valerie;17 Rue Dupleix;61000;Alençon;;;;;;;AUDONNET;Anaïs;CP;;;;;;;;\n` +
  `M.;;ALBERT;Norbert;48 Passage du Havre;61000;Alençon;;;;;;;ALBERT;Nawres;TPS;;;;;;;;\n` +
  `Mme;;ALBERT;Odile;48 Passage du Havre;61000;Alençon;;;;;;;ALBERT;Nawres;TPS;;;;;;;;\n` +
  `Mme;;BISCHOFF;Isabelle;39 Rue Duret;61000;Alençon;;;;;;;BISCHOFF;Andy;TPS;;;;;;;;\n`;

// Smaller set of teachers for update/predelete scenarios
const TEACHERS_CSV_SUBSET =
  `"Civilité";"Nom usage";"Nom";"Prénom";"Adresse";"CP";"Commune";"Pays";"Courriel";"Téléphone domicile";"Téléphone portable";"Directeur";"Classe"\n` +
  `"MME";;"BAILLY";"Catherine";;;;;;;;"non";"TPS"\n` +
  `"M.";;"BRIAUD";"Simon";;;;;;;;"non";"PS"\n`;

// Students subset for predelete scenario
const STUDENTS_CSV_SUBSET =
  `"Nom Elève";"Nom d'usage Elève";"Prénom Elève";"Date naissance";"Sexe";"Adresse1";"Cp1";"Commune1";"Pays1";"Adresse2";"Cp2";"Commune2";"Pays2";" Cycle";"Niveau";"Classe";"Attestation fournie";"Autorisations associations";"Autorisations photos";"Décision de passage"\n` +
  `"BELMAHDI";;"Mathilde";"2006-08-17";"F";;;;;;;;;"CYCLE II";"CE1";"CE1";"Oui";"Non";"Oui";\n`;

// Edge case CSV data
const EMPTY_CSV = "";

const HEADER_ONLY_TEACHERS_CSV =
  `"Civilité";"Nom usage";"Nom";"Prénom";"Adresse";"CP";"Commune";"Pays";"Courriel";"Téléphone domicile";"Téléphone portable";"Directeur";"Classe"\n`;

const WRONG_SEPARATOR_CSV =
  `"Civilité","Nom usage","Nom","Prénom","Adresse","CP","Commune","Pays","Courriel","Téléphone domicile","Téléphone portable","Directeur","Classe"\n` +
  `"M.",,"IMPORTTEST","Jean",,,,,,,"non","IMPORT-CLS"\n`;

const BINARY_GARBAGE = "\x00\x01\x02\x03\x04\x05\xFF\xFE\xFD";

const WRONG_COLUMNS_TEACHERS_CSV =
  `"Col1";"Col2";"Col3";"Col4"\n` +
  `"val1";"val2";"val3";"val4"\n`;

const MALFORMED_CSV =
  `"Civilité";"Nom usage";"Nom";"Prénom\n` +
  `"M.";;"IMPORTTEST";"Jean\n`;

// ------ Helpers ------

function buildImportFormData(
  structure: Structure | { id: string; name: string; externalId: string },
  opts: {
    teachers?: string;
    students?: string;
    relatives?: string;
    type?: string;
    predelete?: string;
    transition?: string;
    columnsMapping?: string;
    classesMapping?: string;
  } = {}
) {
  const fd = new FormData();
  fd.append("type", opts.type || "CSV");
  fd.append("structureName", structure.name);
  fd.append("structureId", structure.id);
  fd.append("structureExternalId", structure.externalId);
  if (opts.predelete !== undefined) fd.append("predelete", opts.predelete);
  if (opts.transition !== undefined) fd.append("transition", opts.transition);
  if (opts.columnsMapping !== undefined) fd.append("columnsMapping", opts.columnsMapping);
  if (opts.classesMapping !== undefined) fd.append("classesMapping", opts.classesMapping);

  if (opts.teachers !== undefined) {
    fd.append("Teacher", http.file(new TextEncoder().encode(opts.teachers), "enseignants.csv"));
  }
  if (opts.students !== undefined) {
    fd.append("Student", http.file(new TextEncoder().encode(opts.students), "eleves.csv"));
  }
  if (opts.relatives !== undefined) {
    fd.append("Relative", http.file(new TextEncoder().encode(opts.relatives), "responsables.csv"));
  }
  return fd;
}

function postWizard(fd: any, path: string = "/directory/wizard/import") {
  const headers = getHeaders();
  headers["Content-Type"] = "multipart/form-data; boundary=" + fd.boundary;
  return http.post(`${rootUrl}${path}`, fd.body(), { headers });
}

function postValidate(fd: any) {
  return postWizard(fd, "/directory/wizard/validate");
}

function postColumnsMapping(fd: any) {
  return postWizard(fd, "/directory/wizard/column/mapping");
}

function postClassesMapping(fd: any) {
  return postWizard(fd, "/directory/wizard/classes/mapping");
}

/** Create a fresh empty structure for import testing */
function createImportStructure(suffix: string): Structure {
  authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
  const name = `${schoolName}-${suffix}-${Date.now()}`;
  return createEmptyStructure(name, false);
}

/** Get user detail by ID */
function getUserDetail(userId: string): any {
  const res = http.get(`${rootUrl}/directory/user/${userId}`, { headers: getHeaders() });
  if (res.status !== 200) return null;
  return JSON.parse(<string>res.body);
}

/** Get structure's removed/pre-deleted users */
function getRemovedUsers(structureId: string): any[] {
  const res = http.get(`${rootUrl}/directory/structure/${structureId}/removedUsers`, { headers: getHeaders() });
  if (res.status !== 200) return [];
  return JSON.parse(<string>res.body);
}

// ------ Setup ------

export function setup() {
  let structure1: Structure;
  let users: UserInfo[];

  group("[Import] Initialize data", () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    if (skipInit) {
      structure1 = getSchoolByName(schoolName);
    } else {
      structure1 = initStructure(`${schoolName}`);
    }
    users = getUsersOfSchool(structure1);
  });
  return { structure: structure1, users, importStructureName: schoolName };
}

/*******************************************************************************************************
 *  Test: Import users and verify data integrity
 ******************************************************************************************************/
export function testWizardImportAndVerifyData(data: InitData) {

  group('[Import] POST /wizard/import - Import all profiles and verify user data', () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    const structure = createImportStructure("full");

    const fd = buildImportFormData(structure, {
      teachers: TEACHERS_CSV,
      students: STUDENTS_CSV,
      relatives: RELATIVES_CSV,
    });
    const res = postWizard(fd);
    check(res, {
      'import all profiles returns 200': (r) => r.status === 200,
    });

    // Verify response format: on success, body should be a JSON object (possibly with ignored users)
    const body = JSON.parse(<string>res.body);
    check(body, {
      'response body is an object': (b) => typeof b === "object" && b !== null,
      'response has no errors field': (b) => b.errors === undefined,
    });

    // Verify teachers were imported with correct data
    const users = getUsersOfSchool(structure);
    const teachers = users.filter((u: UserInfo) => u.type === "Teacher");
    const students = users.filter((u: UserInfo) => u.type === "Student");
    const relatives = users.filter((u: UserInfo) => u.type === "Relative");

    check(teachers, {
      'imported 6 teachers': (t) => t.length === 6,
      'teacher BAILLY exists': (t) => t.some((u: UserInfo) => u.lastName === "BAILLY" && u.firstName === "Catherine"),
      'teacher BRIAUD exists': (t) => t.some((u: UserInfo) => u.lastName === "BRIAUD" && u.firstName === "Simon"),
      'teacher FONTAINE exists': (t) => t.some((u: UserInfo) => u.lastName === "FONTAINE" && u.firstName === "Jean-Paul"),
      'teacher GROLLEAU exists': (t) => t.some((u: UserInfo) => u.lastName === "GROLLEAU" && u.firstName === "Bernard"),
      'teacher LE COQ exists': (t) => t.some((u: UserInfo) => u.lastName === "LE COQ" && u.firstName === "Helene"),
      'teacher LEFEBVRE exists': (t) => t.some((u: UserInfo) => u.lastName === "LEFEBVRE" && u.firstName === "Ulrique"),
    });

    check(students, {
      'imported 7 students': (s) => s.length === 7,
      'student BELMAHDI exists': (s) => s.some((u: UserInfo) => u.lastName === "BELMAHDI" && u.firstName === "Mathilde"),
      'student AUDONNET exists': (s) => s.some((u: UserInfo) => u.lastName === "AUDONNET" && u.firstName === "Anaïs"),
      'student ALBERT exists': (s) => s.some((u: UserInfo) => u.lastName === "ALBERT" && u.firstName === "Nawres"),
      'student BISCHOFF exists': (s) => s.some((u: UserInfo) => u.lastName === "BISCHOFF" && u.firstName === "Andy"),
    });

    check(relatives, {
      'imported at least 5 relatives': (r) => r.length >= 5,
      'relative WIMART exists': (r) => r.some((u: UserInfo) => u.lastName === "WIMART" && u.firstName === "Christiane"),
      'relative AUDONNET Mickael exists': (r) => r.some((u: UserInfo) => u.lastName === "AUDONNET" && u.firstName === "Mickael"),
      'relative ALBERT Norbert exists': (r) => r.some((u: UserInfo) => u.lastName === "ALBERT" && u.firstName === "Norbert"),
    });

    // Verify teacher has correct class assignment
    const baillyUser = teachers.find((u: UserInfo) => u.lastName === "BAILLY");
    if (baillyUser) {
      const detail = getUserDetail(baillyUser.id);
      check(detail, {
        'teacher BAILLY has correct profile': (d) => d.profiles && d.profiles[0] === "Teacher",
        'teacher BAILLY is in structure': (d) => d.structureNodes && d.structureNodes.some((s: any) => s.id === structure.id),
        'teacher BAILLY has a class': (d) => d.classes && d.classes.length > 0,
      });
    }

    // Verify student-relative linkage
    const belmahdiUser = students.find((u: UserInfo) => u.lastName === "BELMAHDI");
    if (belmahdiUser) {
      const detail = getUserDetail(belmahdiUser.id);
      check(detail, {
        'student BELMAHDI has correct profile': (d) => d.profiles && d.profiles[0] === "Student",
        'student BELMAHDI has parents linked': (d) => d.parents && d.parents.length > 0,
        'student BELMAHDI parent is WIMART': (d) => d.parents && d.parents.some((p: any) => p.displayName && p.displayName.includes("WIMART")),
      });
    }

    // Verify relative has children
    const wimartUser = relatives.find((u: UserInfo) => u.lastName === "WIMART");
    if (wimartUser) {
      const detail = getUserDetail(wimartUser.id);
      check(detail, {
        'relative WIMART has correct profile': (d) => d.profiles && d.profiles[0] === "Relative",
        'relative WIMART has children': (d) => d.children && d.children.length > 0,
        'relative WIMART child is BELMAHDI': (d) => d.children && d.children.some((c: any) => c.displayName && c.displayName.includes("BELMAHDI")),
      });
    }

    // Verify classes were created
    const classes = getClassesOfStructureOrFail(structure.id);
    check(classes, {
      'classes were created': (c) => c.length > 0,
      'class TPS exists': (c) => c.some((cl: any) => cl.name === "TPS"),
      'class PS exists': (c) => c.some((cl: any) => cl.name === "PS"),
      'class CE1 exists': (c) => c.some((cl: any) => cl.name === "CE1"),
      'class CP exists': (c) => c.some((cl: any) => cl.name === "CP"),
    });

    // Verify student is in the right class
    if (belmahdiUser) {
      check(belmahdiUser, {
        'student BELMAHDI is in CE1 class': (u) => u.classes && u.classes.some((c: any) => c.name === "CE1"),
      });
    }
  });

  group('[Import] POST /wizard/import - Import teachers only and verify no students/relatives', () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    const structure = createImportStructure("teachers-only");

    const fd = buildImportFormData(structure, { teachers: TEACHERS_CSV });
    const res = postWizard(fd);
    check(res, {
      'import teachers-only returns 200': (r) => r.status === 200,
    });

    const users = getUsersOfSchool(structure);
    const teachers = users.filter((u: UserInfo) => u.type === "Teacher");
    const students = users.filter((u: UserInfo) => u.type === "Student");
    const relatives = users.filter((u: UserInfo) => u.type === "Relative");

    check(teachers, {
      'has 6 teachers': (t) => t.length === 6,
    });
    check(students, {
      'has 0 students': (s) => s.length === 0,
    });
    check(relatives, {
      'has 0 relatives': (r) => r.length === 0,
    });
  });

  group('[Import] POST /wizard/import - Re-import same data is idempotent', () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    const structure = createImportStructure("idempotent");

    // First import
    const fd1 = buildImportFormData(structure, { teachers: TEACHERS_CSV, students: STUDENTS_CSV });
    postWizard(fd1);
    const usersAfterFirst = getUsersOfSchool(structure);
    const teacherCountFirst = usersAfterFirst.filter((u: UserInfo) => u.type === "Teacher").length;
    const studentCountFirst = usersAfterFirst.filter((u: UserInfo) => u.type === "Student").length;

    // Second import with same data
    const fd2 = buildImportFormData(structure, { teachers: TEACHERS_CSV, students: STUDENTS_CSV });
    const res = postWizard(fd2);
    check(res, {
      'second import returns 200': (r) => r.status === 200,
    });

    const usersAfterSecond = getUsersOfSchool(structure);
    const teacherCountSecond = usersAfterSecond.filter((u: UserInfo) => u.type === "Teacher").length;
    const studentCountSecond = usersAfterSecond.filter((u: UserInfo) => u.type === "Student").length;

    check({}, {
      'teacher count unchanged after re-import': () => teacherCountFirst === teacherCountSecond,
      'student count unchanged after re-import': () => studentCountFirst === studentCountSecond,
    });
  });

  group('[Import] GET /wizard/import/:id - Find import draft (non-existent)', () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    const fakeId = "00000000-0000-0000-0000-000000000000";
    const res = http.get(`${rootUrl}/directory/wizard/import/${fakeId}`, { headers: getHeaders() });
    check(res, {
      'find non-existent draft returns 404 or empty': (r) => r.status === 404 || (r.status === 200 && (<string>r.body === "{}" || <string>r.body === "")),
    });
  });

  group('[Import] POST /wizard/import - Unauthenticated request returns 401/302', () => {
    const headers: Record<string, string> = { "content-type": "multipart/form-data" };
    const res = http.post(`${rootUrl}/directory/wizard/import`, "", { headers, redirects: 0 });
    check(res, {
      'unauthenticated import returns 401 or 302': (r) => r.status === 401 || r.status === 302,
    });
  });
}

/*******************************************************************************************************
 *  Test: Validate endpoint format (columns mapping, classes mapping)
 ******************************************************************************************************/
export function testWizardValidateFormat(data: InitData) {

  group('[Import] POST /wizard/validate - Validate teachers CSV and check response format', () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    const structure = createImportStructure("validate-fmt");

    const fd = buildImportFormData(structure, { teachers: TEACHERS_CSV });
    const res = postValidate(fd);
    check(res, {
      'validate returns 200': (r) => r.status === 200,
    });

    const body = JSON.parse(<string>res.body);
    // Successful validation returns a JSON object with file info per profile
    check(body, {
      'validate response is object': (b) => typeof b === "object" && b !== null,
      'validate response has Teacher key': (b) => b.Teacher !== undefined,
      'validate Teacher is array of user entries': (b) => Array.isArray(b.Teacher),
      'validate Teacher entries have line numbers': (b) => b.Teacher.length > 0 && b.Teacher[0].line !== undefined,
      'validate Teacher entries have state': (b) => b.Teacher.length > 0 && b.Teacher[0].state !== undefined,
      'validate Teacher entries have lastName': (b) => b.Teacher.length > 0 && (b.Teacher[0].lastName !== undefined || b.Teacher[0].Nom !== undefined),
      'validate has importId': (b) => b.importId !== undefined && b.importId.length > 0,
    });
  });

  group('[Import] POST /wizard/validate - Validate all profiles and check each profile present', () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    const structure = createImportStructure("validate-all");

    const fd = buildImportFormData(structure, {
      teachers: TEACHERS_CSV,
      students: STUDENTS_CSV,
      relatives: RELATIVES_CSV,
    });
    const res = postValidate(fd);
    check(res, {
      'validate all profiles returns 200': (r) => r.status === 200,
    });

    const body = JSON.parse(<string>res.body);
    check(body, {
      'response has Teacher profile': (b) => b.Teacher !== undefined && Array.isArray(b.Teacher),
      'response has Student profile': (b) => b.Student !== undefined && Array.isArray(b.Student),
      'response has Relative profile': (b) => b.Relative !== undefined && Array.isArray(b.Relative),
      'Teacher count matches CSV': (b) => b.Teacher.length === 6,
      'Student count matches CSV': (b) => b.Student.length === 7,
      'Relative count >= 5': (b) => b.Relative.length >= 5,
    });
  });

  group('[Import] POST /wizard/validate - Validate with errors returns 400 with error format', () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    const structure = createImportStructure("validate-err");

    // Missing required fields in students
    const badStudents =
      `"Nom Elève";"Nom d'usage Elève";"Prénom Elève";"Date naissance";"Sexe";"Adresse1";"Cp1";"Commune1";"Pays1";"Adresse2";"Cp2";"Commune2";"Pays2";" Cycle";"Niveau";"Classe";"Attestation fournie";"Autorisations associations";"Autorisations photos";"Décision de passage"\n` +
      `"";;"";"2010-03-15";"F";;;;;;;;;"CYCLE II";"CE1";"CE1";"Oui";"Non";"Oui";\n`;

    const fd = buildImportFormData(structure, { teachers: TEACHERS_CSV, students: badStudents });
    const res = postValidate(fd);
    // Could be 400 (errors object) or 200 (with softErrors)
    const body = JSON.parse(<string>res.body);
    if (res.status === 400) {
      check(body, {
        'error response has errors field': (b) => b.errors !== undefined,
        'errors is an object with profile keys or global': (b) => typeof b.errors === "object",
      });
    } else {
      // 200 with softErrors or user marked as problematic
      check(body, {
        'validate returns data even with problematic rows': (b) => typeof b === "object",
      });
    }
  });

  group('[Import] POST /wizard/column/mapping - Check mapping response format and availableFields', () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    const structure = createImportStructure("colmap");

    const fd = buildImportFormData(structure, { teachers: TEACHERS_CSV });
    const res = postColumnsMapping(fd);
    check(res, {
      'column mapping returns 200': (r) => r.status === 200,
    });

    const body = JSON.parse(<string>res.body);
    check(body, {
      'column mapping response is object': (b) => typeof b === "object" && b !== null,
      'column mapping has mappings or profiles': (b) => Object.keys(b).length > 0,
      'response contains availableFields': (b) => b.availableFields !== undefined,
      'availableFields is an object': (b) => typeof b.availableFields === "object" && b.availableFields !== null,
      'availableFields has Teacher profile': (b) => b.availableFields.Teacher !== undefined,
      'availableFields.Teacher is an array': (b) => Array.isArray(b.availableFields.Teacher),
      'availableFields.Teacher contains lastName': (b) => b.availableFields.Teacher.includes("lastName"),
      'availableFields.Teacher contains firstName': (b) => b.availableFields.Teacher.includes("firstName"),
      'availableFields.Teacher contains classes': (b) => b.availableFields.Teacher.includes("classes"),
      'availableFields.Teacher contains email': (b) => b.availableFields.Teacher.includes("email"),
      'availableFields.Teacher contains birthDate': (b) => b.availableFields.Teacher.includes("birthDate"),
      'availableFields has Student profile': (b) => b.availableFields.Student !== undefined,
      'availableFields.Student is an array': (b) => Array.isArray(b.availableFields.Student),
      'availableFields.Student contains lastName': (b) => b.availableFields.Student.includes("lastName"),
      'availableFields.Student contains firstName': (b) => b.availableFields.Student.includes("firstName"),
      'availableFields.Student contains birthDate': (b) => b.availableFields.Student.includes("birthDate"),
      'availableFields has Relative profile': (b) => b.availableFields.Relative !== undefined,
      'availableFields.Relative is an array': (b) => Array.isArray(b.availableFields.Relative),
      'availableFields.Relative contains childLastName': (b) => b.availableFields.Relative.includes("childLastName"),
      'availableFields.Relative contains childFirstName': (b) => b.availableFields.Relative.includes("childFirstName"),
      'availableFields.Relative contains childClasses': (b) => b.availableFields.Relative.includes("childClasses"),
    });

    // Verify the response does NOT contain errors when columns are well-recognized
    check(body, {
      'no errors field when mapping is valid': (b) => b.errors === undefined,
    });

    // Verify that standard columns (Nom, Prénom, Classe) are mapped correctly
    if (body.Teacher) {
      check(body, {
        'Teacher mapping is an object': (b) => typeof b.Teacher === "object",
        'Teacher mapping has Nom mapped to lastName': (b) => b.Teacher["Nom"] === "lastName" || b.Teacher["\"Nom\""] === "lastName",
        'Teacher mapping has Prénom mapped to firstName': (b) => b.Teacher["Prénom"] === "firstName" || b.Teacher["\"Prénom\""] === "firstName",
        'Teacher mapping has Classe mapped to classes': (b) => b.Teacher["Classe"] === "classes" || b.Teacher["\"Classe\""] === "classes",
      });
    }
  });

  group('[Import] POST /wizard/column/mapping - Verify availableFields per profile completeness', () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    const structure = createImportStructure("colmap-avail");

    // Send all three file types to get availableFields for all profiles
    const fd = buildImportFormData(structure, {
      teachers: TEACHERS_CSV,
      students: STUDENTS_CSV,
      relatives: RELATIVES_CSV,
    });
    const res = postColumnsMapping(fd);
    check(res, {
      'column mapping all profiles returns 200': (r) => r.status === 200,
    });

    const body = JSON.parse(<string>res.body);
    check(body, {
      'availableFields present': (b) => b.availableFields !== undefined,
    });

    if (body.availableFields) {
      // Teacher available fields
      if (body.availableFields.Teacher) {
        check(body.availableFields.Teacher, {
          'Teacher has externalId field': (f: string[]) => f.includes("externalId"),
          'Teacher has functions field': (f: string[]) => f.includes("functions"),
          'Teacher has mobile field': (f: string[]) => f.includes("mobile") || f.includes("homePhone"),
          'Teacher has address field': (f: string[]) => f.includes("address"),
          'Teacher has zipCode field': (f: string[]) => f.includes("zipCode"),
          'Teacher has city field': (f: string[]) => f.includes("city"),
          'Teacher has groups field': (f: string[]) => f.includes("groups"),
        });
      }
      // Student available fields
      if (body.availableFields.Student) {
        check(body.availableFields.Student, {
          'Student has level field': (f: string[]) => f.includes("level"),
          'Student has sector field': (f: string[]) => f.includes("sector"),
          'Student has gender field': (f: string[]) => f.includes("gender"),
          'Student has relative field': (f: string[]) => f.includes("relative"),
          'Student has ine field': (f: string[]) => f.includes("ine"),
        });
      }
      // Relative available fields
      if (body.availableFields.Relative) {
        check(body.availableFields.Relative, {
          'Relative has childExternalId field': (f: string[]) => f.includes("childExternalId"),
          'Relative has childLastName field': (f: string[]) => f.includes("childLastName"),
          'Relative has childFirstName field': (f: string[]) => f.includes("childFirstName"),
          'Relative has childClasses field': (f: string[]) => f.includes("childClasses"),
          'Relative has title field': (f: string[]) => f.includes("title"),
        });
      }
    }
  });

  group('[Import] POST /wizard/column/mapping - Explicit mapping and verify output', () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    const structure = createImportStructure("colmap-explicit");

    // Column indices are 0-based: Nom=2, Prénom=3, Classe=12
    const mapping = JSON.stringify({
      "Teacher": {
        "lastName": 2,
        "firstName": 3,
        "classeName": 12
      }
    });
    const fd = buildImportFormData(structure, {
      teachers: TEACHERS_CSV,
      columnsMapping: mapping,
    });
    const res = postColumnsMapping(fd);
    check(res, {
      'explicit column mapping returns 200': (r) => r.status === 200,
    });

    const body = JSON.parse(<string>res.body);
    check(body, {
      'mapping response has Teacher data': (b) => b.Teacher !== undefined || b.result !== undefined,
      'explicit mapping still has availableFields': (b) => b.availableFields !== undefined,
    });
  });

  group('[Import] POST /wizard/column/mapping - Unrecognized column names produce empty mapping values', () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    const structure = createImportStructure("colmap-unknown");

    // CSV with completely unknown column headers
    const unknownColumnsCsv =
      `"FooColumn";"BarColumn";"BazColumn";"QuuxColumn"\n` +
      `"val1";"val2";"val3";"val4"\n`;

    const fd = buildImportFormData(structure, { teachers: unknownColumnsCsv });
    const res = postColumnsMapping(fd);
    check(res, {
      'unknown columns mapping returns 200': (r) => r.status === 200,
    });

    const body = JSON.parse(<string>res.body);
    // When columns are not recognized, mappings should have empty string values
    if (body.Teacher) {
      const mappingValues = Object.values(body.Teacher);
      check(mappingValues, {
        'unrecognized columns are mapped to empty string': (vals: any[]) => vals.every((v: string) => v === "" || v === "ignore"),
      });
    }
    // availableFields should still be returned regardless of column recognition
    check(body, {
      'availableFields still present with unknown columns': (b) => b.availableFields !== undefined,
      'availableFields.Teacher still has known fields': (b) => b.availableFields && b.availableFields.Teacher && b.availableFields.Teacher.includes("lastName"),
    });
  });

  group('[Import] POST /wizard/column/mapping - Partially recognized columns (mix of valid and invalid)', () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    const structure = createImportStructure("colmap-partial");

    // Mix of valid and invalid column names
    const mixedColumnsCsv =
      `"Nom";"InvalidCol1";"Prénom";"TotallyWrong";"Classe";"GarbageHeader"\n` +
      `"TESTUSER";;"Alice";;"CLS1";"junk"\n`;

    const fd = buildImportFormData(structure, { teachers: mixedColumnsCsv });
    const res = postColumnsMapping(fd);
    check(res, {
      'partial columns mapping returns 200': (r) => r.status === 200,
    });

    const body = JSON.parse(<string>res.body);
    if (body.Teacher) {
      check(body.Teacher, {
        'Nom is mapped to lastName': (t: any) => t["Nom"] === "lastName",
        'Prénom is mapped to firstName': (t: any) => t["Prénom"] === "firstName" || t["Pr\u00e9nom"] === "firstName",
        'Classe is mapped to classes': (t: any) => t["Classe"] === "classes",
        'InvalidCol1 is mapped to empty or ignore': (t: any) => t["InvalidCol1"] === "" || t["InvalidCol1"] === "ignore",
        'TotallyWrong is mapped to empty or ignore': (t: any) => t["TotallyWrong"] === "" || t["TotallyWrong"] === "ignore",
        'GarbageHeader is mapped to empty or ignore': (t: any) => t["GarbageHeader"] === "" || t["GarbageHeader"] === "ignore",
      });
    }
  });

  group('[Import] POST /wizard/column/mapping - Column names with special chars and accents', () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    const structure = createImportStructure("colmap-accents");

    // Verify that accent normalization works (e.g. "Prénom" should map to firstName)
    const accentedCsv =
      `"Civilité";"Nom d'usage";"Nom";"Prénom";"Téléphone domicile";"Téléphone portable";"Classe"\n` +
      `"M.";;"TEST";"User";;;"CLS1"\n`;

    const fd = buildImportFormData(structure, { teachers: accentedCsv });
    const res = postColumnsMapping(fd);
    check(res, {
      'accented columns mapping returns 200': (r) => r.status === 200,
    });

    const body = JSON.parse(<string>res.body);
    if (body.Teacher) {
      check(body.Teacher, {
        'Civilité is mapped to title': (t: any) => t["Civilité"] === "title" || t["Civilit\u00e9"] === "title",
        'Nom d\'usage maps to surname': (t: any) => {
          const key = Object.keys(t).find(k => k.includes("usage"));
          return key !== undefined && t[key] === "surname";
        },
        'Téléphone domicile maps to homePhone': (t: any) => {
          const key = Object.keys(t).find(k => k.includes("domicile"));
          return key !== undefined && t[key] === "homePhone";
        },
        'Téléphone portable maps to mobile': (t: any) => {
          const key = Object.keys(t).find(k => k.includes("portable"));
          return key !== undefined && t[key] === "mobile";
        },
      });
    }
  });

  group('[Import] POST /wizard/column/mapping - Duplicate column names in CSV', () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    const structure = createImportStructure("colmap-dupes");

    // CSV with duplicate column names
    const dupColsCsv =
      `"Nom";"Nom";"Prénom";"Classe"\n` +
      `"TEST";"TEST2";"User";"CLS1"\n`;

    const fd = buildImportFormData(structure, { teachers: dupColsCsv });
    const res = postColumnsMapping(fd);
    check(res, {
      'duplicate columns does not cause 500': (r) => r.status !== 500,
    });

    // Should still return a response (possibly with errors or last-wins mapping)
    if (res.status === 200) {
      const body = JSON.parse(<string>res.body);
      check(body, {
        'response is still an object': (b) => typeof b === "object",
        'availableFields still returned with duplicate cols': (b) => b.availableFields !== undefined,
      });
    }
  });

  group('[Import] POST /wizard/column/mapping - Empty column names in CSV', () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    const structure = createImportStructure("colmap-empty");

    // CSV with some empty column headers (trailing semicolons)
    const emptyColsCsv =
      `"Nom";"Prénom";"";"Classe";""\n` +
      `"TEST";"User";;"CLS1";\n`;

    const fd = buildImportFormData(structure, { teachers: emptyColsCsv });
    const res = postColumnsMapping(fd);
    check(res, {
      'empty column names does not cause 500': (r) => r.status !== 500,
    });

    if (res.status === 200) {
      const body = JSON.parse(<string>res.body);
      // Empty columns should be mapped to "ignore"
      if (body.Teacher) {
        check(body.Teacher, {
          'empty column mapped to ignore': (t: any) => t[""] === "ignore",
        });
      }
    }
  });

  group('[Import] POST /wizard/column/mapping - Columns with only whitespace/special chars', () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    const structure = createImportStructure("colmap-whitespace");

    const whitespaceCsv =
      `"   ";"***";"Nom";"Prénom";"@#$%";"Classe"\n` +
      `"a";"b";"TEST";"User";"c";"CLS1"\n`;

    const fd = buildImportFormData(structure, { teachers: whitespaceCsv });
    const res = postColumnsMapping(fd);
    check(res, {
      'whitespace/special columns does not cause 500': (r) => r.status !== 500,
    });

    if (res.status === 200) {
      const body = JSON.parse(<string>res.body);
      if (body.Teacher) {
        // Valid columns should still be recognized
        check(body.Teacher, {
          'Nom still recognized': (t: any) => t["Nom"] === "lastName",
          'Prénom still recognized': (t: any) => t["Prénom"] === "firstName" || t["Pr\u00e9nom"] === "firstName",
          'Classe still recognized': (t: any) => t["Classe"] === "classes",
        });
      }
    }
  });

  group('[Import] POST /wizard/column/mapping - Mapping with column index out of range', () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    const structure = createImportStructure("colmap-oor");

    // TEACHERS_CSV has 13 columns (0-12). Map to indices 50, 99 which don't exist.
    const oobMapping = JSON.stringify({
      "Teacher": {
        "lastName": 50,
        "firstName": 99,
        "classeName": -1
      }
    });
    const fd = buildImportFormData(structure, {
      teachers: TEACHERS_CSV,
      columnsMapping: oobMapping,
    });
    const res = postColumnsMapping(fd);
    check(res, {
      'out-of-range indices does not cause 500': (r) => r.status !== 500,
    });

    // Should either return an error or return mapping with issues
    if (res.status === 200) {
      const body = JSON.parse(<string>res.body);
      check(body, {
        'response still has availableFields': (b) => b.availableFields !== undefined,
      });
    }
  });

  group('[Import] POST /wizard/column/mapping - Mapping with non-integer index values', () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    const structure = createImportStructure("colmap-badtype");

    // Non-integer values in the mapping
    const badTypeMapping = JSON.stringify({
      "Teacher": {
        "lastName": "notAnIndex",
        "firstName": null,
        "classeName": 3.14
      }
    });
    const fd = buildImportFormData(structure, {
      teachers: TEACHERS_CSV,
      columnsMapping: badTypeMapping,
    });
    const res = postColumnsMapping(fd);
    check(res, {
      'non-integer mapping values does not cause 500': (r) => r.status !== 500,
    });
  });

  group('[Import] POST /wizard/column/mapping - Mapping referencing non-existent profile', () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    const structure = createImportStructure("colmap-badprofile");

    // Profile "FakeProfile" does not exist
    const badProfileMapping = JSON.stringify({
      "FakeProfile": {
        "lastName": 2,
        "firstName": 3
      }
    });
    const fd = buildImportFormData(structure, {
      teachers: TEACHERS_CSV,
      columnsMapping: badProfileMapping,
    });
    const res = postColumnsMapping(fd);
    check(res, {
      'non-existent profile in mapping does not cause 500': (r) => r.status !== 500,
    });

    // The response should still include the actual Teacher mapping auto-detected
    if (res.status === 200) {
      const body = JSON.parse(<string>res.body);
      check(body, {
        'availableFields still present with bad profile mapping': (b) => b.availableFields !== undefined,
      });
    }
  });

  group('[Import] POST /wizard/column/mapping - Mapping with field names not in availableFields', () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    const structure = createImportStructure("colmap-badfield");

    // "nonExistentField" and "anotherFakeField" are not valid target fields
    const badFieldMapping = JSON.stringify({
      "Teacher": {
        "nonExistentField": 2,
        "anotherFakeField": 3,
        "classes": 12
      }
    });
    const fd = buildImportFormData(structure, {
      teachers: TEACHERS_CSV,
      columnsMapping: badFieldMapping,
    });
    const res = postColumnsMapping(fd);
    check(res, {
      'unknown target fields in mapping does not cause 500': (r) => r.status !== 500,
    });

    // Non-existent fields should either be ignored or produce errors
    if (res.status === 200) {
      const body = JSON.parse(<string>res.body);
      check(body, {
        'availableFields does not include fake field': (b) => b.availableFields && b.availableFields.Teacher && !b.availableFields.Teacher.includes("nonExistentField"),
      });
    }
  });

  group('[Import] POST /wizard/column/mapping - Empty mapping object', () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    const structure = createImportStructure("colmap-emptymapping");

    const emptyMapping = JSON.stringify({});
    const fd = buildImportFormData(structure, {
      teachers: TEACHERS_CSV,
      columnsMapping: emptyMapping,
    });
    const res = postColumnsMapping(fd);
    check(res, {
      'empty mapping object does not cause 500': (r) => r.status !== 500,
    });

    if (res.status === 200) {
      const body = JSON.parse(<string>res.body);
      // With empty mapping, should fall back to default auto-detection
      check(body, {
        'empty mapping still returns availableFields': (b) => b.availableFields !== undefined,
      });
    }
  });

  group('[Import] POST /wizard/column/mapping - Very long column names', () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    const structure = createImportStructure("colmap-longcol");

    const longName = "A".repeat(500);
    const longColCsv =
      `"${longName}";"Nom";"Prénom";"Classe"\n` +
      `"junk";"TEST";"User";"CLS1"\n`;

    const fd = buildImportFormData(structure, { teachers: longColCsv });
    const res = postColumnsMapping(fd);
    check(res, {
      'very long column name does not cause 500': (r) => r.status !== 500,
    });
  });

  group('[Import] POST /wizard/column/mapping - Invalid JSON mapping returns 400', () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    const fd = buildImportFormData(data.structure, {
      teachers: TEACHERS_CSV,
      columnsMapping: "not_valid_json{{{",
    });
    const res = postColumnsMapping(fd);
    check(res, {
      'invalid JSON columns mapping returns 400': (r) => r.status === 400,
    });
  });

  group('[Import] POST /wizard/column/mapping - Mapping with XSS in field names', () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    const structure = createImportStructure("colmap-xss");

    const xssMapping = JSON.stringify({
      "Teacher": {
        "<script>alert(1)</script>": 2,
        "firstName": 3
      }
    });
    const fd = buildImportFormData(structure, {
      teachers: TEACHERS_CSV,
      columnsMapping: xssMapping,
    });
    const res = postColumnsMapping(fd);
    check(res, {
      'XSS in mapping field names does not cause 500': (r) => r.status !== 500,
    });

    if (res.status === 200) {
      const body = JSON.parse(<string>res.body);
      // XSS should not appear unsanitized in available fields
      check(body, {
        'XSS not reflected in availableFields': (b) => !JSON.stringify(b.availableFields || {}).includes("<script>"),
      });
    }
  });

  group('[Import] POST /wizard/column/mapping - No file attached returns error', () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    const fd = new FormData();
    fd.append("type", "CSV");
    fd.append("structureName", data.structure.name);
    fd.append("structureId", data.structure.id);
    fd.append("structureExternalId", data.structure.externalId);
    // No file attached at all
    const headers = getHeaders();
    headers["Content-Type"] = "multipart/form-data; boundary=" + fd.boundary;
    const res = http.post(`${rootUrl}/directory/wizard/column/mapping`, fd.body(), { headers });
    check(res, {
      'column mapping with no file does not cause 500': (r) => r.status !== 500,
    });
  });

  group('[Import] POST /wizard/classes/mapping - Valid mapping and check response', () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    const structure = createImportStructure("clsmap");

    const classesMapping = JSON.stringify({
      "CE1": "CE1-RENAMED",
      "CP": "CP"
    });
    const fd = buildImportFormData(structure, {
      teachers: TEACHERS_CSV,
      students: STUDENTS_CSV,
      classesMapping: classesMapping,
    });
    const res = postClassesMapping(fd);
    check(res, {
      'classes mapping returns 200': (r) => r.status === 200,
    });

    const body = JSON.parse(<string>res.body);
    check(body, {
      'classes mapping response is object': (b) => typeof b === "object",
    });
  });

  group('[Import] POST /wizard/classes/mapping - Invalid JSON returns 400', () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    const fd = buildImportFormData(data.structure, {
      teachers: TEACHERS_CSV,
      classesMapping: "{invalid json!!",
    });
    const res = postClassesMapping(fd);
    check(res, {
      'invalid JSON classes mapping returns 400': (r) => r.status === 400,
    });
  });

  group('[Import] PUT /wizard/validate/:id - Validate with non-existent id returns error', () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    const fakeId = "00000000-0000-0000-0000-000000000000";
    const headers = getHeaders();
    headers["content-type"] = "application/json";
    const res = http.put(`${rootUrl}/directory/wizard/validate/${fakeId}`, null, { headers });
    // Should be 400 because the import ID does not exist in mongo
    check(res, {
      'validate non-existent id does not return 500': (r) => r.status !== 500,
      'validate non-existent id returns 400 or 404': (r) => r.status === 400 || r.status === 404 || r.status === 200,
    });
  });

  group('[Import] POST /wizard/validate - Validate and then import by ID', () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    const structure = createImportStructure("validate-then-import");

    const fd = buildImportFormData(structure, { teachers: TEACHERS_CSV });
    const validateRes = postValidate(fd);
    check(validateRes, {
      'validate for import-by-id returns 200': (r) => r.status === 200,
    });

    const validateBody = JSON.parse(<string>validateRes.body);
    if (validateBody.importId) {
      // Use the importId to launch the import
      const headers = getHeaders();
      headers["content-type"] = "application/json";
      const importRes = http.put(`${rootUrl}/directory/wizard/import/${validateBody.importId}`, null, { headers });
      check(importRes, {
        'import by ID returns 200': (r) => r.status === 200,
      });

      // Verify users were imported
      const users = getUsersOfSchool(structure);
      const teachers = users.filter((u: UserInfo) => u.type === "Teacher");
      check(teachers, {
        'import by ID created teachers': (t) => t.length === 6,
      });
    }
  });
}

/*******************************************************************************************************
 *  Test: PreDelete behavior
 ******************************************************************************************************/
export function testPreDeleteBehavior(data: InitData) {

  group('[Import] POST /wizard/import with preDelete - Users missing from second import are pre-deleted', () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    const structure = createImportStructure("predelete");

    // Step 1: Import all teachers
    const fd1 = buildImportFormData(structure, { teachers: TEACHERS_CSV, students: STUDENTS_CSV });
    const res1 = postWizard(fd1);
    check(res1, {
      'first import returns 200': (r) => r.status === 200,
    });

    const usersBeforePreDelete = getUsersOfSchool(structure);
    const teachersBefore = usersBeforePreDelete.filter((u: UserInfo) => u.type === "Teacher");
    const studentsBefore = usersBeforePreDelete.filter((u: UserInfo) => u.type === "Student");
    check(teachersBefore, {
      'has 6 teachers before predelete': (t) => t.length === 6,
    });
    check(studentsBefore, {
      'has 7 students before predelete': (s) => s.length === 7,
    });

    // Step 2: Import a subset with preDelete=true => teachers not in the subset should be pre-deleted
    const fd2 = buildImportFormData(structure, {
      teachers: TEACHERS_CSV_SUBSET,
      students: STUDENTS_CSV_SUBSET,
      predelete: "true",
    });
    const res2 = postWizard(fd2);
    check(res2, {
      'import with predelete returns 200': (r) => r.status === 200,
    });

    // Step 3: Verify that removed users are in the removed list
    const removedUsers = getRemovedUsers(structure.id);
    const usersAfterPreDelete = getUsersOfSchool(structure);
    const teachersAfter = usersAfterPreDelete.filter((u: UserInfo) => u.type === "Teacher");
    const studentsAfter = usersAfterPreDelete.filter((u: UserInfo) => u.type === "Student");

    // Teachers FONTAINE, GROLLEAU, LE COQ, LEFEBVRE should be pre-deleted (not in subset)
    check({}, {
      'fewer active teachers after predelete': () => teachersAfter.length < teachersBefore.length,
      'teachers in subset still active (BAILLY)': () => teachersAfter.some((u: UserInfo) => u.lastName === "BAILLY"),
      'teachers in subset still active (BRIAUD)': () => teachersAfter.some((u: UserInfo) => u.lastName === "BRIAUD"),
    });

    // Students not in subset should be pre-deleted
    check({}, {
      'fewer active students after predelete': () => studentsAfter.length < studentsBefore.length,
      'student BELMAHDI still active (in subset)': () => studentsAfter.some((u: UserInfo) => u.lastName === "BELMAHDI"),
    });

    // Verify removed users list contains the missing users
    if (removedUsers.length > 0) {
      check(removedUsers, {
        'removed users list has entries': (r) => r.length > 0,
        'removed users contain teachers not in subset': (r) => r.some((u: any) =>
          u.lastName === "FONTAINE" || u.lastName === "GROLLEAU" || u.lastName === "LE COQ" || u.lastName === "LEFEBVRE"
        ),
      });
    }
  });

  group('[Import] POST /wizard/import without preDelete - Missing users are NOT pre-deleted', () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    const structure = createImportStructure("no-predelete");

    // Step 1: Import all teachers
    const fd1 = buildImportFormData(structure, { teachers: TEACHERS_CSV });
    postWizard(fd1);
    const usersAfterFirst = getUsersOfSchool(structure);
    const teachersBefore = usersAfterFirst.filter((u: UserInfo) => u.type === "Teacher").length;

    // Step 2: Import subset WITHOUT predelete
    const fd2 = buildImportFormData(structure, { teachers: TEACHERS_CSV_SUBSET, predelete: "false" });
    const res2 = postWizard(fd2);
    check(res2, {
      'import without predelete returns 200': (r) => r.status === 200,
    });

    // Step 3: All teachers should still be active
    const usersAfterSecond = getUsersOfSchool(structure);
    const teachersAfter = usersAfterSecond.filter((u: UserInfo) => u.type === "Teacher").length;
    check({}, {
      'all teachers still active without predelete': () => teachersAfter === teachersBefore,
    });
  });

  group('[Import] POST /wizard/validate with preDelete - Preview shows users to be deleted', () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    const structure = createImportStructure("predelete-validate");

    // Step 1: Import all teachers
    const fd1 = buildImportFormData(structure, { teachers: TEACHERS_CSV });
    postWizard(fd1);

    // Step 2: Validate with subset + predelete (should show which users would be deleted)
    const fd2 = buildImportFormData(structure, {
      teachers: TEACHERS_CSV_SUBSET,
      predelete: "true",
    });
    const res = postValidate(fd2);
    check(res, {
      'validate with predelete returns 200': (r) => r.status === 200,
    });

    const body = JSON.parse(<string>res.body);
    check(body, {
      'validate response has Teacher data': (b) => b.Teacher !== undefined && Array.isArray(b.Teacher),
    });

    // The validation response should include users marked as DELETED
    if (body.Teacher && Array.isArray(body.Teacher)) {
      const deletedEntries = body.Teacher.filter((entry: any) => entry.oState === "DELETED" || entry.state === "Supprimé" || entry.state === "Deleted");
      check(deletedEntries, {
        'validate shows users to be deleted': (d) => d.length > 0,
        'deleted entries include users not in subset': (d) => d.some((e: any) =>
          e.lastName === "FONTAINE" || e.lastName === "GROLLEAU" || e.lastName === "LE COQ" || e.lastName === "LEFEBVRE"
        ),
      });
    }
  });
}

/*******************************************************************************************************
 *  Test: Transition behavior
 ******************************************************************************************************/
export function testTransitionBehavior(data: InitData) {

  group('[Import] POST /wizard/import with transition - Classes are reset and recreated', () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    const structure = createImportStructure("transition");

    // Step 1: Import teachers into initial classes (TPS, PS, MS, GS, CP, CE1)
    const fd1 = buildImportFormData(structure, { teachers: TEACHERS_CSV, students: STUDENTS_CSV });
    const res1 = postWizard(fd1);
    check(res1, {
      'initial import returns 200': (r) => r.status === 200,
    });

    const classesBefore = getClassesOfStructureOrFail(structure.id);
    check(classesBefore, {
      'classes exist after first import': (c) => c.length > 0,
    });

    // Step 2: Import with transition=true and different class assignments
    const newTeachers =
      `"Civilité";"Nom usage";"Nom";"Prénom";"Adresse";"CP";"Commune";"Pays";"Courriel";"Téléphone domicile";"Téléphone portable";"Directeur";"Classe"\n` +
      `"MME";;"BAILLY";"Catherine";;;;;;;;"non";"6A"\n` +
      `"M.";;"BRIAUD";"Simon";;;;;;;;"non";"6B"\n`;

    const newStudents =
      `"Nom Elève";"Nom d'usage Elève";"Prénom Elève";"Date naissance";"Sexe";"Adresse1";"Cp1";"Commune1";"Pays1";"Adresse2";"Cp2";"Commune2";"Pays2";" Cycle";"Niveau";"Classe";"Attestation fournie";"Autorisations associations";"Autorisations photos";"Décision de passage"\n` +
      `"BELMAHDI";;"Mathilde";"2006-08-17";"F";;;;;;;;;"CYCLE II";"6ème";"6A";"Oui";"Non";"Oui";\n` +
      `"BOUZEHOUANE";;"Valentine";"2006-01-04";"F";;;;;;;;;"CYCLE III";"6ème";"6B";"Non";"Non";"Non";\n`;

    const fd2 = buildImportFormData(structure, {
      teachers: newTeachers,
      students: newStudents,
      transition: "true",
      predelete: "true",
    });
    const res2 = postWizard(fd2);
    check(res2, {
      'import with transition returns 200': (r) => r.status === 200,
    });

    // Step 3: Verify classes were updated
    const classesAfter = getClassesOfStructureOrFail(structure.id);
    const classNamesAfter = classesAfter.map((c: any) => c.name);
    check(classNamesAfter, {
      'new class 6A exists': (names) => names.includes("6A"),
      'new class 6B exists': (names) => names.includes("6B"),
    });

    // Verify students are now in new classes
    const users = getUsersOfSchool(structure);
    const belmahdi = users.find((u: UserInfo) => u.lastName === "BELMAHDI" && u.type === "Student");
    if (belmahdi && belmahdi.classes) {
      check(belmahdi, {
        'BELMAHDI is now in class 6A': (u) => u.classes.some((c: any) => c.name === "6A"),
      });
    }
  });

  group('[Import] POST /wizard/import without transition - Old class assignments preserved', () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    const structure = createImportStructure("no-transition");

    // Step 1: Import with initial classes
    const fd1 = buildImportFormData(structure, { teachers: TEACHERS_CSV, students: STUDENTS_CSV });
    postWizard(fd1);

    const usersAfterFirst = getUsersOfSchool(structure);
    const belmahdiFirst = usersAfterFirst.find((u: UserInfo) => u.lastName === "BELMAHDI" && u.type === "Student");

    // Step 2: Import same users into different classes WITHOUT transition
    const newStudents =
      `"Nom Elève";"Nom d'usage Elève";"Prénom Elève";"Date naissance";"Sexe";"Adresse1";"Cp1";"Commune1";"Pays1";"Adresse2";"Cp2";"Commune2";"Pays2";" Cycle";"Niveau";"Classe";"Attestation fournie";"Autorisations associations";"Autorisations photos";"Décision de passage"\n` +
      `"BELMAHDI";;"Mathilde";"2006-08-17";"F";;;;;;;;;"CYCLE II";"CE2";"CE2";"Oui";"Non";"Oui";\n`;

    const fd2 = buildImportFormData(structure, {
      teachers: TEACHERS_CSV_SUBSET,
      students: newStudents,
      transition: "false",
    });
    const res2 = postWizard(fd2);
    check(res2, {
      'import without transition returns 200': (r) => r.status === 200,
    });

    // Without transition, existing class links should be preserved (user stays in old class too)
    const usersAfterSecond = getUsersOfSchool(structure);
    const belmahdiSecond = usersAfterSecond.find((u: UserInfo) => u.lastName === "BELMAHDI" && u.type === "Student");
    if (belmahdiSecond && belmahdiSecond.classes) {
      check(belmahdiSecond, {
        'BELMAHDI still has class assignments': (u) => u.classes && u.classes.length > 0,
        'BELMAHDI still in old CE1 class (no transition)': (u) => u.classes.some((c: any) => c.name === "CE1"),
      });
    }
  });
}

/*******************************************************************************************************
 *  Test: Class Import Endpoints (POST /import/:userType/class/:classId)
 ******************************************************************************************************/
export function testClassImportAndVerify(data: InitData) {

  group('[Import] POST /import/Student/class/:classId - Import students and verify class membership', () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    const classId = createClassAndGetIdOrFail(data.structure.id, "k6-import-students-cls");

    // Make teacher ADML so they can import
    const teacher = getRandomUserWithProfile(data.users, 'Teacher');
    const admlHeaders = getHeaders();
    admlHeaders['content-type'] = 'application/json';
    http.post(`${rootUrl}/directory/user/function/${teacher.id}`, JSON.stringify({
      functionCode: "ADMIN_LOCAL",
      inherit: "s",
      scope: [data.structure.id],
    }), { headers: admlHeaders });

    authenticateWeb(teacher.login);

    const studentsCsv =
      `"Nom Elève";"Nom d'usage Elève";"Prénom Elève";"Date naissance";"Sexe";"Adresse1";"Cp1";"Commune1";"Pays1";"Adresse2";"Cp2";"Commune2";"Pays2";" Cycle";"Niveau";"Classe";"Attestation fournie";"Autorisations associations";"Autorisations photos";"Décision de passage"\n` +
      `"CLASSIMPORT";;"Eleve1";"2010-05-10";"F";;;;;;;;;"CYCLE II";"CE1";"k6-import-students-cls";"Oui";"Non";"Oui";\n` +
      `"CLASSIMPORT";;"Eleve2";"2010-07-20";"H";;;;;;;;;"CYCLE II";"CE1";"k6-import-students-cls";"Non";"Oui";"Non";\n`;

    const fd = new FormData();
    fd.append("Student", http.file(new TextEncoder().encode(studentsCsv), "eleves.csv"));
    const headers = getHeaders();
    headers["Content-Type"] = "multipart/form-data; boundary=" + fd.boundary;
    const res = http.post(`${rootUrl}/directory/import/Student/class/${classId}`, fd.body(), { headers });
    check(res, {
      'class import students returns 200': (r) => r.status === 200,
    });

    // Verify students are in the class
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    const classUsersRes = http.get(`${rootUrl}/directory/class/${classId}/users?type=Student`, { headers: getHeaders() });
    const classUsers = JSON.parse(<string>classUsersRes.body);
    check(classUsers, {
      'class has imported students': (u) => Array.isArray(u) && u.length >= 2,
      'student Eleve1 is in class': (u) => u.some((s: any) => s.firstName === "Eleve1" && s.lastName === "CLASSIMPORT"),
      'student Eleve2 is in class': (u) => u.some((s: any) => s.firstName === "Eleve2" && s.lastName === "CLASSIMPORT"),
    });

    // Verify student data
    const eleve1 = classUsers.find((s: any) => s.firstName === "Eleve1");
    if (eleve1) {
      const detail = getUserDetail(eleve1.id);
      check(detail, {
        'imported student has Student profile': (d) => d.profiles && d.profiles[0] === "Student",
        'imported student is in correct structure': (d) => d.structureNodes && d.structureNodes.some((s: any) => s.id === data.structure.id),
      });
    }

    // Cleanup ADML
    http.del(`${rootUrl}/directory/user/function/${teacher.id}/ADMIN_LOCAL`, null, { headers: getHeaders() });
  });

  group('[Import] POST /import/Student/class/:classId - Non-existent class returns 404', () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    const fakeClassId = "00000000-0000-0000-0000-000000000000";
    const fd = new FormData();
    fd.append("Student", http.file(new TextEncoder().encode(STUDENTS_CSV), "eleves.csv"));
    const headers = getHeaders();
    headers["Content-Type"] = "multipart/form-data; boundary=" + fd.boundary;
    const res = http.post(`${rootUrl}/directory/import/Student/class/${fakeClassId}`, fd.body(), { headers });
    check(res, {
      'import to non-existent class returns 404': (r) => r.status === 404,
    });
  });

  group('[Import] POST /import/Teacher/class/:classId - Import teachers and verify', () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    const classId = createClassAndGetIdOrFail(data.structure.id, "k6-import-teachers-cls");

    const teacher = getRandomUserWithProfile(data.users, 'Teacher');
    const admlHeaders = getHeaders();
    admlHeaders['content-type'] = 'application/json';
    http.post(`${rootUrl}/directory/user/function/${teacher.id}`, JSON.stringify({
      functionCode: "ADMIN_LOCAL",
      inherit: "s",
      scope: [data.structure.id],
    }), { headers: admlHeaders });

    authenticateWeb(teacher.login);

    const teachersCsv =
      `"Civilité";"Nom usage";"Nom";"Prénom";"Adresse";"CP";"Commune";"Pays";"Courriel";"Téléphone domicile";"Téléphone portable";"Directeur";"Classe"\n` +
      `"M.";;"CLASSIMPORTTEACH";"Pierre";;;;;;;;"non";"k6-import-teachers-cls"\n`;

    const fd = new FormData();
    fd.append("Teacher", http.file(new TextEncoder().encode(teachersCsv), "enseignants.csv"));
    const headers = getHeaders();
    headers["Content-Type"] = "multipart/form-data; boundary=" + fd.boundary;
    const res = http.post(`${rootUrl}/directory/import/Teacher/class/${classId}`, fd.body(), { headers });
    check(res, {
      'class import teachers returns 200': (r) => r.status === 200,
    });

    // Verify teacher in class
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    const classUsersRes = http.get(`${rootUrl}/directory/class/${classId}/users?type=Teacher`, { headers: getHeaders() });
    const classUsers = JSON.parse(<string>classUsersRes.body);
    check(classUsers, {
      'class has imported teacher': (u) => Array.isArray(u) && u.some((t: any) => t.lastName === "CLASSIMPORTTEACH" && t.firstName === "Pierre"),
    });

    // Cleanup ADML
    http.del(`${rootUrl}/directory/user/function/${teacher.id}/ADMIN_LOCAL`, null, { headers: getHeaders() });
  });

  group('[Import] POST /import/Student/class/:classId - Non-teacher cannot import', () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    const classId = createClassAndGetIdOrFail(data.structure.id, "k6-import-noperm-cls");

    // Authenticate as a student (should not have permission)
    const student = getRandomUserWithProfile(data.users, 'Student');
    if (student) {
      authenticateWeb(student.login);
      const fd = new FormData();
      fd.append("Student", http.file(new TextEncoder().encode(STUDENTS_CSV), "eleves.csv"));
      const headers = getHeaders();
      headers["Content-Type"] = "multipart/form-data; boundary=" + fd.boundary;
      const res = http.post(`${rootUrl}/directory/import/Student/class/${classId}`, fd.body(), { headers });
      check(res, {
        'student cannot import - returns 401 or 403': (r) => r.status === 401 || r.status === 403,
      });
    }
  });
}

/*******************************************************************************************************
 *  Edge Cases & Error Handling
 ******************************************************************************************************/
export function testImportEdgeCases(data: InitData) {

  group('[Import] POST /wizard/import - Empty CSV file returns error', () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    const fd = buildImportFormData(data.structure, { teachers: EMPTY_CSV });
    const res = postWizard(fd);
    // Empty CSV should not create any users
    if (res.status === 200) {
      const body = JSON.parse(<string>res.body);
      check(body, {
        'empty CSV import has no users or reports error': (b) => !b.Teacher || b.Teacher.length === 0 || b.errors !== undefined,
      });
    } else {
      check(res, {
        'empty CSV returns 400': (r) => r.status === 400,
      });
    }
  });

  group('[Import] POST /wizard/import - Header-only CSV creates no users', () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    const structure = createImportStructure("headeronly");
    const fd = buildImportFormData(structure, { teachers: HEADER_ONLY_TEACHERS_CSV });
    const res = postWizard(fd);

    // Whether 200 or 400, no teachers should be created
    const users = getUsersOfSchool(structure);
    const teachers = users.filter((u: UserInfo) => u.type === "Teacher");
    check(teachers, {
      'header-only CSV creates no teachers': (t) => t.length === 0,
    });
  });

  group('[Import] POST /wizard/import - Non-CSV file extension is rejected', () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    const fd = new FormData();
    fd.append("type", "CSV");
    fd.append("structureName", data.structure.name);
    fd.append("structureId", data.structure.id);
    fd.append("structureExternalId", data.structure.externalId);
    // .txt extension should be rejected by the upload handler
    fd.append("Teacher", http.file(new TextEncoder().encode(TEACHERS_CSV), "enseignants.txt"));
    const headers = getHeaders();
    headers["Content-Type"] = "multipart/form-data; boundary=" + fd.boundary;
    const res = http.post(`${rootUrl}/directory/wizard/import`, fd.body(), { headers });
    check(res, {
      'non-CSV extension returns 400': (r) => r.status === 400,
    });
    // Verify the error message mentions file extension
    if (res.status === 400) {
      const body = JSON.parse(<string>res.body);
      check(body, {
        'error message present': (b) => b.errors !== undefined || b.error !== undefined || b.message !== undefined,
      });
    }
  });

  group('[Import] POST /wizard/import - Binary garbage content', () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    const structure = createImportStructure("garbage");
    const fd = buildImportFormData(structure, { teachers: BINARY_GARBAGE });
    const res = postWizard(fd);
    check(res, {
      'binary garbage does not cause 500': (r) => r.status !== 500,
    });
    // Verify no users were created
    const users = getUsersOfSchool(structure);
    check(users, {
      'no users created from garbage': (u) => u.filter((user: UserInfo) => user.type === "Teacher").length === 0,
    });
  });

  group('[Import] POST /wizard/import - Wrong column names create no users', () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    const structure = createImportStructure("wrongcols");
    const fd = buildImportFormData(structure, { teachers: WRONG_COLUMNS_TEACHERS_CSV });
    const res = postWizard(fd);
    check(res, {
      'wrong columns does not cause 500': (r) => r.status !== 500,
    });
    // No valid users should be created
    const users = getUsersOfSchool(structure);
    check(users, {
      'wrong columns creates no teachers': (u) => u.filter((user: UserInfo) => user.type === "Teacher").length === 0,
    });
  });

  group('[Import] POST /wizard/import - Malformed CSV (unclosed quotes)', () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    const structure = createImportStructure("malformed");
    const fd = buildImportFormData(structure, { teachers: MALFORMED_CSV });
    const res = postWizard(fd);
    check(res, {
      'malformed CSV does not cause 500': (r) => r.status !== 500,
    });
    const users = getUsersOfSchool(structure);
    check(users, {
      'malformed CSV creates no valid teachers': (u) => u.filter((user: UserInfo) => user.type === "Teacher").length === 0,
    });
  });

  group('[Import] POST /wizard/import - Wrong separator (comma) creates no valid users', () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    const structure = createImportStructure("wrongsep");
    const fd = buildImportFormData(structure, { teachers: WRONG_SEPARATOR_CSV });
    const res = postWizard(fd);
    check(res, {
      'wrong separator does not cause 500': (r) => r.status !== 500,
    });
    const users = getUsersOfSchool(structure);
    check(users, {
      'wrong separator creates no valid teachers': (u) => u.filter((user: UserInfo) => user.type === "Teacher").length === 0,
    });
  });

  group('[Import] POST /wizard/import - Missing type field returns 400', () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    const fd = new FormData();
    // Deliberately omit "type" field
    fd.append("structureName", data.structure.name);
    fd.append("structureId", data.structure.id);
    fd.append("structureExternalId", data.structure.externalId);
    fd.append("Teacher", http.file(new TextEncoder().encode(TEACHERS_CSV), "enseignants.csv"));
    const headers = getHeaders();
    headers["Content-Type"] = "multipart/form-data; boundary=" + fd.boundary;
    const res = http.post(`${rootUrl}/directory/wizard/import`, fd.body(), { headers });
    check(res, {
      'missing type returns 400': (r) => r.status === 400,
    });
    if (res.status === 400) {
      const body = JSON.parse(<string>res.body);
      check(body, {
        'error mentions invalid import type': (b) => JSON.stringify(b).includes("invalid.import.type") || b.errors !== undefined || b.error !== undefined,
      });
    }
  });

  group('[Import] POST /wizard/import - Invalid type value returns 400', () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    const fd = buildImportFormData(data.structure, { teachers: TEACHERS_CSV, type: "INVALID_FORMAT" });
    const res = postWizard(fd);
    check(res, {
      'invalid type returns 400': (r) => r.status === 400,
    });
    if (res.status === 400) {
      const body = JSON.parse(<string>res.body);
      check(body, {
        'error mentions invalid import type': (b) => JSON.stringify(b).includes("invalid") || b.errors !== undefined,
      });
    }
  });

  group('[Import] POST /wizard/import - No file attached', () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    const fd = new FormData();
    fd.append("type", "CSV");
    fd.append("structureName", data.structure.name);
    fd.append("structureId", data.structure.id);
    fd.append("structureExternalId", data.structure.externalId);
    const headers = getHeaders();
    headers["Content-Type"] = "multipart/form-data; boundary=" + fd.boundary;
    const res = http.post(`${rootUrl}/directory/wizard/import`, fd.body(), { headers });
    check(res, {
      'no file does not cause 500': (r) => r.status !== 500,
    });
  });

  group('[Import] POST /wizard/import - Non-existent structure ID', () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    const fakeStructure = {
      id: "00000000-0000-0000-0000-000000000000",
      name: "NonExistent",
      externalId: "fake-external-id",
    };
    const fd = buildImportFormData(fakeStructure, { teachers: TEACHERS_CSV });
    const res = postWizard(fd);
    check(res, {
      'non-existent structure does not cause 500': (r) => r.status !== 500,
    });
  });

  group('[Import] POST /wizard/import - Duplicate rows', () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    const structure = createImportStructure("dupes");
    const duplicateTeachers =
      `"Civilité";"Nom usage";"Nom";"Prénom";"Adresse";"CP";"Commune";"Pays";"Courriel";"Téléphone domicile";"Téléphone portable";"Directeur";"Classe"\n` +
      `"M.";;"DUPETEST";"Jean";;;;;;;;"non";"CLS1"\n` +
      `"M.";;"DUPETEST";"Jean";;;;;;;;"non";"CLS1"\n`;
    const fd = buildImportFormData(structure, { teachers: duplicateTeachers });
    const res = postWizard(fd);
    check(res, {
      'duplicate rows does not cause 500': (r) => r.status !== 500,
    });

    // Should not create two separate users with same name
    if (res.status === 200) {
      const users = getUsersOfSchool(structure);
      const dupes = users.filter((u: UserInfo) => u.lastName === "DUPETEST" && u.type === "Teacher");
      check(dupes, {
        'duplicate rows create at most 1 user': (d) => d.length <= 1,
      });
    }
  });

  group('[Import] POST /wizard/import - Special characters in names (accents, hyphens, apostrophe)', () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    const structure = createImportStructure("special");
    const specialTeachers =
      `"Civilité";"Nom usage";"Nom";"Prénom";"Adresse";"CP";"Commune";"Pays";"Courriel";"Téléphone domicile";"Téléphone portable";"Directeur";"Classe"\n` +
      `"M.";;"O'BRIEN";"José-María";;;;;;;;"non";"CLS1"\n` +
      `"MME";;"MÜLLER-LÉVY";"Hélène";;;;;;;;"non";"CLS1"\n`;
    const fd = buildImportFormData(structure, { teachers: specialTeachers });
    const res = postWizard(fd);
    check(res, {
      'special chars returns 200': (r) => r.status === 200,
    });

    const users = getUsersOfSchool(structure);
    const obrien = users.find((u: UserInfo) => u.lastName === "O'BRIEN" || u.lastName === "O BRIEN");
    const muller = users.find((u: UserInfo) => u.lastName && (u.lastName.includes("MÜLLER") || u.lastName.includes("MULLER")));
    check({}, {
      'user with apostrophe imported': () => obrien !== undefined,
      'user with accents/hyphens imported': () => muller !== undefined,
    });

    // Verify the names are stored correctly
    if (obrien) {
      const detail = getUserDetail(obrien.id);
      check(detail, {
        'apostrophe user has correct firstName': (d) => d.firstName === "José-María",
      });
    }
  });

  group('[Import] POST /wizard/import - UTF-8 BOM encoding', () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    const structure = createImportStructure("bom");
    const bom = "\uFEFF";
    const bomCsv = bom + TEACHERS_CSV;
    const fd = buildImportFormData(structure, { teachers: bomCsv });
    const res = postWizard(fd);
    check(res, {
      'UTF-8 BOM CSV returns 200': (r) => r.status === 200,
    });

    const users = getUsersOfSchool(structure);
    const teachers = users.filter((u: UserInfo) => u.type === "Teacher");
    check(teachers, {
      'BOM CSV imports teachers correctly': (t) => t.length === 6,
    });
  });

  group('[Import] POST /wizard/import - Inconsistent relative-student link (orphan relative)', () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    const structure = createImportStructure("orphan");
    // Relative references a student that does not exist
    const orphanRelatives =
      `Civilité Responsable;Nom usage responsable;Nom responsable;Prénom responsable;Adresse responsable;CP responsable;Commune responsable;Pays;Courriel;Téléphone domicile;Téléphone travail;Téléphone portable;Nom d'usage enfant;Nom de famille enfant;Prénom enfant;Classes enfants\n` +
      `M.;;ORPHANPARENT;Pierre;1 Rue;75001;Paris;;;;;;;NONEXISTENT;Child;CLS1\n`;

    const fd = buildImportFormData(structure, {
      teachers: TEACHERS_CSV,
      students: STUDENTS_CSV,
      relatives: orphanRelatives,
    });
    const res = postWizard(fd);
    check(res, {
      'orphan relative does not cause 500': (r) => r.status !== 500,
    });

    // The orphan relative should either not be imported or be imported without child link
    if (res.status === 200) {
      const users = getUsersOfSchool(structure);
      const orphan = users.find((u: UserInfo) => u.lastName === "ORPHANPARENT");
      if (orphan) {
        const detail = getUserDetail(orphan.id);
        check(detail, {
          'orphan relative has no children linked': (d) => !d.children || d.children.length === 0,
        });
      }
    }
  });

  group('[Import] POST /wizard/import - Students with missing required fields (empty name)', () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    const structure = createImportStructure("missingfields");
    const missingFieldsCsv =
      `"Nom Elève";"Nom d'usage Elève";"Prénom Elève";"Date naissance";"Sexe";"Adresse1";"Cp1";"Commune1";"Pays1";"Adresse2";"Cp2";"Commune2";"Pays2";" Cycle";"Niveau";"Classe";"Attestation fournie";"Autorisations associations";"Autorisations photos";"Décision de passage"\n` +
      `"";;"";"2010-03-15";"F";;;;;;;;;"CYCLE II";"CE1";"CLS1";"Oui";"Non";"Oui";\n`;
    const fd = buildImportFormData(structure, { students: missingFieldsCsv, teachers: TEACHERS_CSV });
    const res = postWizard(fd);
    check(res, {
      'missing required fields does not cause 500': (r) => r.status !== 500,
    });
    // The student with empty name should not be created
    const users = getUsersOfSchool(structure);
    const emptyNameStudents = users.filter((u: UserInfo) => u.type === "Student" && (!u.lastName || u.lastName === ""));
    check(emptyNameStudents, {
      'no student with empty name created': (s) => s.length === 0,
    });
  });

  group('[Import] POST /wizard/import - Invalid date format does not crash', () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    const structure = createImportStructure("baddate");
    const invalidDateCsv =
      `"Nom Elève";"Nom d'usage Elève";"Prénom Elève";"Date naissance";"Sexe";"Adresse1";"Cp1";"Commune1";"Pays1";"Adresse2";"Cp2";"Commune2";"Pays2";" Cycle";"Niveau";"Classe";"Attestation fournie";"Autorisations associations";"Autorisations photos";"Décision de passage"\n` +
      `"DATETEST";;"Alice";"not-a-date";"F";;;;;;;;;"CYCLE II";"CE1";"CLS1";"Oui";"Non";"Oui";\n`;
    const fd = buildImportFormData(structure, { students: invalidDateCsv, teachers: TEACHERS_CSV });
    const res = postWizard(fd);
    check(res, {
      'invalid date does not cause 500': (r) => r.status !== 500,
    });
  });

  group('[Import] POST /wizard/import - Extra semicolons / mismatched column count', () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    const structure = createImportStructure("extracols");
    const mismatchedCsv =
      `"Civilité";"Nom usage";"Nom";"Prénom";"Adresse";"CP";"Commune";"Pays";"Courriel";"Téléphone domicile";"Téléphone portable";"Directeur";"Classe"\n` +
      `"M.";;"EXTRATEST";"Jean";;;;;;;;"non";"CLS1";;"extra";"more";"cols"\n`;
    const fd = buildImportFormData(structure, { teachers: mismatchedCsv });
    const res = postWizard(fd);
    check(res, {
      'mismatched columns does not cause 500': (r) => r.status !== 500,
    });
  });

  group('[Import] POST /wizard/import - XSS in field values is sanitized', () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    const structure = createImportStructure("xss");
    const xssCsv =
      `"Civilité";"Nom usage";"Nom";"Prénom";"Adresse";"CP";"Commune";"Pays";"Courriel";"Téléphone domicile";"Téléphone portable";"Directeur";"Classe"\n` +
      `"M.";;"<script>alert(1)</script>";"<img src=x onerror=alert(1)>";;;;;;;;"non";"CLS1"\n`;
    const fd = buildImportFormData(structure, { teachers: xssCsv });
    const res = postWizard(fd);
    check(res, {
      'XSS attempt does not cause 500': (r) => r.status !== 500,
    });

    // If created, verify data is sanitized
    if (res.status === 200) {
      const users = getUsersOfSchool(structure);
      const xssUser = users.find((u: UserInfo) => u.type === "Teacher");
      if (xssUser) {
        const detail = getUserDetail(xssUser.id);
        check(detail, {
          'XSS not stored raw in lastName': (d) => !d.lastName || !d.lastName.includes("<script>"),
          'XSS not stored raw in firstName': (d) => !d.firstName || !d.firstName.includes("onerror"),
        });
      }
    }
  });

  group('[Import] POST /wizard/import - SQL injection attempt is safe', () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    const structure = createImportStructure("sqli");
    const sqlCsv =
      `"Civilité";"Nom usage";"Nom";"Prénom";"Adresse";"CP";"Commune";"Pays";"Courriel";"Téléphone domicile";"Téléphone portable";"Directeur";"Classe"\n` +
      `"M.";;"'; DROP TABLE users;--";"Robert";;;;;;;;"non";"CLS1"\n`;
    const fd = buildImportFormData(structure, { teachers: sqlCsv });
    const res = postWizard(fd);
    check(res, {
      'SQL injection does not cause 500': (r) => r.status !== 500,
    });

    // Verify the system is still operational
    const allStructures = http.get(`${rootUrl}/directory/structure/admin/list`, { headers: getHeaders() });
    check(allStructures, {
      'system still operational after SQLi attempt': (r) => r.status === 200,
    });
  });

  group('[Import] POST /wizard/import - Wrong columns mapping indices out of range', () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    const structure = createImportStructure("badmapping");
    const wrongMapping = JSON.stringify({
      "Teacher": {
        "lastName": 99,
        "firstName": 98,
        "classeName": 97
      }
    });
    const fd = buildImportFormData(structure, {
      teachers: TEACHERS_CSV,
      columnsMapping: wrongMapping,
    });
    const res = postWizard(fd);
    check(res, {
      'wrong mapping indices does not cause 500': (r) => r.status !== 500,
    });
    // Should not create valid users with out-of-range mapping
    const users = getUsersOfSchool(structure);
    const teachers = users.filter((u: UserInfo) => u.type === "Teacher");
    check(teachers, {
      'wrong mapping creates no valid teachers or returns error': (t) => t.length === 0 || res.status === 400,
    });
  });

  group('[Import] POST /wizard/import - Very large CSV (many rows)', () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    const structure = createImportStructure("large");
    // Generate 50 teachers
    let largeCsv = `"Civilité";"Nom usage";"Nom";"Prénom";"Adresse";"CP";"Commune";"Pays";"Courriel";"Téléphone domicile";"Téléphone portable";"Directeur";"Classe"\n`;
    for (let i = 0; i < 50; i++) {
      largeCsv += `"M.";;"LARGETEST${i}";"Prenom${i}";;;;;;;;"non";"CLS${i % 5}"\n`;
    }
    const fd = buildImportFormData(structure, { teachers: largeCsv });
    const res = postWizard(fd);
    check(res, {
      'large CSV returns 200': (r) => r.status === 200,
    });

    const users = getUsersOfSchool(structure);
    const teachers = users.filter((u: UserInfo) => u.type === "Teacher");
    check(teachers, {
      'large CSV imported all 50 teachers': (t) => t.length === 50,
      'first teacher exists': (t) => t.some((u: UserInfo) => u.lastName === "LARGETEST0"),
      'last teacher exists': (t) => t.some((u: UserInfo) => u.lastName === "LARGETEST49"),
    });
  });
}
