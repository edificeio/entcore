/**
 * Everything a feeder scenario does on the platform goes through a {@link FeederGateway}. The real one talks
 * http to the platform ; the scenarios engine can be driven by a fake one to be tested without any platform
 * (see _feeder-selftest.ts).
 */
import http from "k6/http";
import {
  authenticateWeb,
  getHeaders,
  getAllStructures,
  getUsersOfSchool,
} from "../../../node_modules/edifice-k6-commons/dist/index.js";
import { AAFFeeder, cleanAAF, getUserDetail, ImportSummary, triggerImportAndWait, uploadAAF, withExternalIds } from "../directory/_aaf-utils.ts";
import { describeHttpFailure, FeederScenarioError } from "./_feeder-errors.ts";
import type { GraphStructure, GraphUser } from "./_feeder-verify.ts";

const rootUrl = __ENV.ROOT_URL;

/** Content of the CSV files to import, by profile. */
export type CsvFiles = { Teacher?: string; Student?: string; Relative?: string };

export type PersonRow = { id: string; externalId?: string; firstName?: string; lastName?: string; displayName?: string };

export interface FeederGateway {
  /** Opens the session used by every other call. */
  login(): void;
  uploadAAF(feeder: AAFFeeder, subPath: string, files: Record<string, string>): void;
  /** Removes the uploaded directory {@code subPath}. */
  cleanAAF(feeder: AAFFeeder, subPath: string): void;
  /** Runs an import and returns once it is over. */
  importAndWait(feeder: AAFFeeder): ImportSummary;
  findStructureByName(name: string): GraphStructure | undefined;
  listUsers(structureId: string): GraphUser[];
  getParentsOf(studentId: string): PersonRow[];
  getChildrenOf(relativeId: string): PersonRow[];
  /** Creates a relative by hand, like the "create an account" form does, attached to the given children. */
  createRelative(structureId: string, firstName: string, lastName: string, childrenIds: string[]): PersonRow;
  /**
   * Imports CSV files in a structure with the import wizard, like a person of a school does. The files are the
   * contents of the files of the profiles. Fails when the wizard refuses them.
   */
  importCsv(structure: GraphStructure, files: CsvFiles): void;
  linkRelative(studentId: string, relativeId: string): void;
  unlinkRelative(studentId: string, relativeId: string): void;
}

function expectStatus(method: string, route: string, res: { status: number; body: any }, ...accepted: number[]) {
  if (accepted.indexOf(res.status) < 0) {
    throw new FeederScenarioError(describeHttpFailure(method, route, res.status, res.body ? String(res.body) : ""));
  }
}

function formEncode(fields: [string, string][]): string {
  return fields.map(([name, value]) => `${encodeURIComponent(name)}=${encodeURIComponent(value)}`).join("&");
}

export class HttpFeederGateway implements FeederGateway {
  login(): void {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
  }

  uploadAAF(feeder: AAFFeeder, subPath: string, files: Record<string, string>): void {
    const res = uploadAAF(feeder, subPath, files);
    expectStatus("POST", "/directory/api/internal/upload-aaf", res, 200);
  }

  cleanAAF(feeder: AAFFeeder, subPath: string): void {
    const res = cleanAAF(feeder, subPath);
    expectStatus("POST", "/directory/api/internal/clean-aaf", res, 200);
  }

  importAndWait(feeder: AAFFeeder): ImportSummary {
    const res = triggerImportAndWait(feeder);
    // a 500 still carries the summary (the import ran but ended in error)
    if (res.status !== 200 && res.status !== 500) {
      expectStatus("POST", "/directory/api/internal/trigger-import", res, 200);
    }
    try {
      return <ImportSummary>JSON.parse(<string>res.body);
    } catch (e) {
      throw new FeederScenarioError(describeHttpFailure("POST", "/directory/api/internal/trigger-import", res.status, <string>res.body));
    }
  }

  findStructureByName(name: string): GraphStructure | undefined {
    return getAllStructures().find((s: GraphStructure) => s.name === name);
  }

  listUsers(structureId: string): GraphUser[] {
    const users: GraphUser[] = withExternalIds(structureId, <GraphUser[]>getUsersOfSchool(<any>{ id: structureId }));
    if (users.length > 0 && users.every((u) => !u.externalId)) {
      throw new FeederScenarioError(
        `la plateforme ne renvoie l'externalId d'aucun des ${users.length} utilisateurs de la structure ${structureId} (ni sur /directory/structure/:id/users, ni sur /directory/user/admin/list) : impossible de les comparer aux fichiers`
      );
    }
    return users;
  }

  getParentsOf(studentId: string): PersonRow[] {
    const detail = getUserDetail(studentId);
    return detail && Array.isArray(detail.parents) ? detail.parents : [];
  }

  getChildrenOf(relativeId: string): PersonRow[] {
    const res = http.get(`${rootUrl}/directory/user/${relativeId}/children`, { headers: getHeaders() });
    expectStatus("GET", `/directory/user/${relativeId}/children`, res, 200);
    const children: PersonRow[] = [];
    // one entry per structure of the children
    (<any[]>JSON.parse(<string>res.body)).forEach((entry) => (entry.children || []).forEach((c: PersonRow) => children.push(c)));
    return children;
  }

  createRelative(structureId: string, firstName: string, lastName: string, childrenIds: string[]): PersonRow {
    const fields: [string, string][] = [
      ["firstname", firstName],
      ["lastname", lastName],
      ["type", "Relative"],
      ["structureId", structureId],
    ];
    childrenIds.forEach((id) => fields.push(["childrenIds", id]));
    const headers = getHeaders();
    headers["content-type"] = "application/x-www-form-urlencoded;charset=UTF-8";
    const res = http.post(`${rootUrl}/directory/api/user`, formEncode(fields), { headers });
    expectStatus("POST", "/directory/api/user", res, 200, 201);
    return { id: JSON.parse(<string>res.body).id, firstName, lastName };
  }

  importCsv(structure: GraphStructure, files: CsvFiles): void {
    const body: Record<string, any> = {
      type: "CSV",
      structureName: structure.name,
      structureId: structure.id,
      structureExternalId: structure.externalId || "",
    };
    if (files.Teacher !== undefined) body["Teacher"] = http.file(files.Teacher, "enseignants.csv", "text/csv");
    if (files.Student !== undefined) body["Student"] = http.file(files.Student, "eleves.csv", "text/csv");
    if (files.Relative !== undefined) body["Relative"] = http.file(files.Relative, "responsables.csv", "text/csv");
    // k6 sends a multipart body, with its boundary, when it is given an object holding files
    const headers = getHeaders();
    delete headers["Content-Type"];
    delete headers["content-type"];
    const res = http.post(`${rootUrl}/directory/wizard/import`, body, { headers });
    expectStatus("POST", "/directory/wizard/import", res, 200);
  }

  linkRelative(studentId: string, relativeId: string): void {
    const route = `/directory/user/${studentId}/related/${relativeId}`;
    expectStatus("PUT", route, http.put(`${rootUrl}${route}`, null, { headers: getHeaders() }), 200);
  }

  unlinkRelative(studentId: string, relativeId: string): void {
    const route = `/directory/user/${studentId}/related/${relativeId}`;
    expectStatus("DELETE", route, http.del(`${rootUrl}${route}`, null, { headers: getHeaders() }), 200);
  }
}
