/**
 * Non-regression integration tests for the feeder manual-* actions after DTO migration.
 *
 * Each scenario exercises one group of operations end-to-end through the HTTP API,
 * verifying that the JSON → DTO → ManualFeeder pipeline still produces correct results.
 *
 * Focus: operations whose mappers have non-trivial logic (nested data blocks, key renames,
 * legacy-typo handling, defaults). Basic CRUD already covered by other IT test suites.
 *
 * Run: k6 run --env ADMC_LOGIN=admc --env ADMC_PASSWORD=... --env BASE_URL=http://localhost:8090 manual-actions.ts
 */
import http from "k6/http";
import { check, group } from "k6";
import {
  authenticateWeb,
  assertOk,
  checkReturnCode,
  initStructure,
  createClassAndGetIdOrFail,
  createUserAndGetData,
  getAdmlsOrMakThem,
  getUsersOfSchool,
  getRandomUserWithProfile,
  getHeaders,
  getClassesOfStructureOrFail,
  activateUsers,
  BASE_URL,
  Structure,
  UserInfo,
} from "../../../node_modules/edifice-k6-commons/dist/index.js";

export const options = {
  setupTimeout: "1h",
  maxRedirects: 0,
  thresholds: { checks: ["rate == 1.00"] },
  scenarios: {
    testUpdateStructure: {
      exec: "testUpdateStructure",
      executor: "per-vu-iterations",
      vus: 1,
      maxDuration: "15s",
      gracefulStop: "1s",
    },
    testClassLifecycle: {
      exec: "testClassLifecycle",
      executor: "per-vu-iterations",
      vus: 1,
      maxDuration: "15s",
      gracefulStop: "1s",
    },
    testUpdateUser: {
      exec: "testUpdateUser",
      executor: "per-vu-iterations",
      vus: 1,
      maxDuration: "15s",
      gracefulStop: "1s",
    },
    testDeleteAndRestoreUser: {
      exec: "testDeleteAndRestoreUser",
      executor: "per-vu-iterations",
      vus: 1,
      maxDuration: "15s",
      gracefulStop: "1s",
    },
    testRelativeStudent: {
      exec: "testRelativeStudent",
      executor: "per-vu-iterations",
      vus: 1,
      maxDuration: "15s",
      gracefulStop: "1s",
    },
    testSubjectCrud: {
      exec: "testSubjectCrud",
      executor: "per-vu-iterations",
      vus: 1,
      maxDuration: "15s",
      gracefulStop: "1s",
    },
    testStructureAttachmentDetachment: {
      exec: "testStructureAttachmentDetachment",
      executor: "per-vu-iterations",
      vus: 1,
      maxDuration: "15s",
      gracefulStop: "1s",
    },
  },
};

export function setup() {
  authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
  const suffix = Date.now();
  const structure = initStructure(`IT-Feeder`, "tiny");
  const structure2 = initStructure(`IT-Feeder-Child}`, "tiny");

  const users = getUsersOfSchool(structure);
  const student = getRandomUserWithProfile(users, "Student", []);
  const teacher = getAdmlsOrMakThem(structure, "Teacher", 1, [])[0];

  const relative = createUserAndGetData({
    firstName: "Parent",
    lastName: `ITFeeder-${suffix}`,
    type: "Relative",
    structureId: structure.id,
    birthDate: "1985-06-15",
    positionIds: [],
  });
  activateUsers(structure);
  // UAI must be unique across runs — derive one from the millisecond timestamp
  const uai = "0" + (suffix % 1000000).toString().padStart(6, "0") + "Z";

  return { structure, structure2, student, teacher, relative, uai };
}

// ─── manual-update-structure ──────────────────────────────────────────────────
// Verifies: UAI key rename (data.UAI → dto.uai), nested data block extraction.

export function testUpdateStructure({ structure, uai }: { structure: Structure; uai: string }) {
  group("[Feeder] manual-update-structure", () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);

    const updatedName = `${structure.name} - Updated`;
    const res = jsonPut(
      `/directory/structure/${structure.id}`,
      { name: updatedName, UAI: uai }
    );
    assertOk(res, "ADMC should be able to update a structure");

    const resGet = http.get(
        `${BASE_URL}/directory/school/${structure.id}`,
        { headers : getHeaders() }
    );

    const body = JSON.parse(resGet.body as string);
    check(body, {
      "updated structure contains new name": (b) => b.name === updatedName,
      "updated structure contains UAI (key must be UAI not uai)": (b) => b.UAI === uai,
    });
  });
}

// ─── manual-update-class / manual-remove-class ───────────────────────────────
// Verifies: nested data block (data.name, data.level) for update.

export function testClassLifecycle({ structure }: { structure: Structure }) {
  group("[Feeder] manual-update-class + manual-remove-class", () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);

    // Create class first (exercising manual-create-class indirectly via commons)
    const classId = createClassAndGetIdOrFail(structure.id, `IT Class ${Date.now()}`);

    // Update: name and level are in the request body, the service wraps them in `data`
    const updatedName = `IT Class Updated ${Date.now()}`;
    const updateRes = jsonPut(`/directory/class/${classId}`, { name: updatedName, level: "6" });
    assertOk(updateRes, "should update class name and level");

    const classList = getClassesOfStructureOrFail(structure.id);
    const updated = classList.find((x) => x.id === classId);
    check(updated, {
      "class name was updated": (b) => b.name === updatedName
    });

    // Delete the class
    const deleteRes = jsonDel(`/directory/class/${classId}`);
    checkReturnCode(deleteRes, "Removing class is actually forbidden", 403);
  });
}

// ─── manual-update-user / manual-update-user-login ───────────────────────────
// Verifies: nested data block (data.firstName, data.lastName, ...) for user update.

export function testUpdateUser({ student }: { student: UserInfo }) {
  group("[Feeder] manual-update-user + manual-update-user-login", () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);

    // Update user fields (firstName/lastName are in data block in bus message)
    const updatedFirstName = `UpdatedFirst-${Date.now()}`;
    const updateRes = jsonPut(`/directory/user/${student.id}`, {
      firstName: updatedFirstName,
      lastName: student.lastName,
    });
    assertOk(updateRes, "should update user fields");

    const getRes = http.get(
        `${BASE_URL}/directory/user/${student.id}`,
        { headers: getHeaders(), redirects: 0 })
    const updated = JSON.parse(getRes.body as string);

    check(updated, {
      "user firstName was updated": (b) => b.firstName === updatedFirstName,
    });

    // Update login (separate mapper: userId + login fields at root level of bus message)
    const newLogin = `it.feeder.${Date.now()}`;
    const loginRes = jsonPut(`/directory/user/login/${student.id}`, { login: newLogin });
    assertOk(loginRes, "should update user login");

    const getResLogin = http.get(
        `${BASE_URL}/directory/user/${student.id}`,
        { headers: getHeaders(), redirects: 0 })
    const updatedLogin = JSON.parse(getResLogin.body as string);
    check(updatedLogin, {
      "user login was updated": (b) => b.login === newLogin,
    });
  });
}

// ─── manual-delete-user / manual-restore-user ────────────────────────────────

export function testDeleteAndRestoreUser({ relative }: { relative: UserInfo }) {
  group("[Feeder] manual-delete-user + manual-restore-user", () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    // Pre-delete (soft-delete) via DELETE /directory/user?userId=...
    const preDeleteRes = jsonDel(`/directory/user?userId=${relative.id}`);
    assertOk(preDeleteRes, "should pre-delete (soft-delete) user");

    const getResLogin = http.get(
        `${BASE_URL}/directory/user/${relative.id}`,
        { headers: getHeaders(), redirects: 0 })

    // Restore the pre-deleted user
    const restoreRes = jsonPut(`/directory/restore/user?userId=${relative.id}`, { });
    assertOk(restoreRes, "should restore the pre-deleted user");
  });
}

// ─── manual-relative-student / manual-unlink-relative-student ────────────────
// Verifies: relativeId + studentId from route params, no data block.

export function testRelativeStudent({ student, relative }: { student: UserInfo; relative: UserInfo }) {
  group("[Feeder] manual-relative-student + manual-unlink-relative-student", () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);

    // Link relative to student
    const linkRes = jsonPut(`/directory/user/${student.id}/related/${relative.id}`, {});
    assertOk(linkRes, "should link relative to student");

    // Verify the link was created by reading student's children
    const checkRes = http.get(
      `${BASE_URL}/directory/user/${relative.id}/children`,
      { headers: getHeaders(), redirects: 0 }
    );
    assertOk(checkRes, "should fetch relative's children");
    const children = JSON.parse(checkRes.body as string);

    check(children, {
      "student is listed as relative's child": (c) =>
        Array.isArray(c) && c.some((child) => child.children.find( c => c.id === student.id)),
    });

    // Unlink
    const unlinkRes = jsonDel(`/directory/user/${student.id}/related/${relative.id}`);
    assertOk(unlinkRes, "should unlink relative from student");
  });
}

// ─── manual-add-subject / manual-update-subject / manual-delete-subject ──────
// Verifies: subject nested block extraction (body.subject.*).

export function testSubjectCrud({ structure }: { structure: Structure }) {
  group("[Feeder] manual-add-subject + manual-update-subject + manual-delete-subject", () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);

    const subjectLabel = `IT Maths ${Date.now()}`;
    const subjectCode = `MATH-${Date.now()}`;

    // Create — HTTP body becomes bus message `subject` field via SubjectMapper.toCreateSubjectDTO
    const createRes = jsonPost(`/directory/subject`, {
      structureId: structure.id,
      label: subjectLabel,
      code: subjectCode,
    });
    checkReturnCode(createRes, "should create subject", 201);
    const created = JSON.parse(createRes.body as string);
    check(created, {
      "created subject has correct label": (b) => b.label === subjectLabel,
      "created subject has correct code": (b) => b.code === subjectCode,
    });
    const subjectId = created.id;

    // Update — HTTP body becomes bus message `subject` field + id injected by controller
    const updatedLabel = `${subjectLabel} - Updated`;
    const updateRes = jsonPut(`/directory/subject/${subjectId}`, {
      label: updatedLabel,
      code: `${subjectCode}-U`,
    });
    assertOk(updateRes, "should update subject", 201);
    const updatedSubject = JSON.parse(updateRes.body as string);
    check(updatedSubject, {
      "updated subject has new label": (b) => b.label === updatedLabel,
    });

    // Delete
    const deleteRes = jsonDel(`/directory/subject/${subjectId}`);
    checkReturnCode(deleteRes, "should delete subject", 204);
  });
}

// ─── manual-structure-attachment / manual-structure-detachment ────────────────
// Verifies: structureId + parentStructureId from route params.

export function testStructureAttachmentDetachment({ structure, structure2 }: { structure: Structure; structure2: Structure }) {
  group("[Feeder] manual-structure-attachment + manual-structure-detachment", () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);

    // Attach structure2 as child of structure
    const attachRes = jsonPut(`/directory/structure/${structure2.id}/parent/${structure.id}`, {});
    assertOk(attachRes, "should attach child structure to parent");

    // Verify attachment via GET children
    const childrenRes = http.get(
      `${BASE_URL}/directory/structure/${structure.id}/children`,
      { headers: getHeaders(), redirects: 0 }
    );
    assertOk(childrenRes, "should fetch children of parent structure");
    const children = JSON.parse(childrenRes.body as string);
    check(children, {
      "child structure appears in parent's children": (c) =>
        Array.isArray(c) && c.some((child) => child === structure2.id),
    });

    // Detach
    const detachRes = jsonDel(`/directory/structure/${structure2.id}/parent/${structure.id}`);
    assertOk(detachRes, "should detach child structure from parent");
  });
}

// ─── HTTP helpers ─────────────────────────────────────────────────────────────

function jsonPost(path: string, body: object) {
  const headers = getHeaders();
  headers["content-type"] = "application/json";
  return http.post(`${BASE_URL}${path}`, JSON.stringify(body), { headers, redirects: 0 });
}

function jsonPut(path: string, body: object) {
  const headers = getHeaders();
  headers["content-type"] = "application/json";
  return http.put(`${BASE_URL}${path}`, JSON.stringify(body), { headers, redirects: 0 });
}

function jsonDel(path: string) {
  return http.del(`${BASE_URL}${path}`, null, { headers: getHeaders(), redirects: 0 });
}
