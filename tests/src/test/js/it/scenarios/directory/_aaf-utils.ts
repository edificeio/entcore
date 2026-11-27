import http, { RefinedResponse } from "k6/http";
import { sleep } from "k6";
import { getHeaders } from "../../../node_modules/edifice-k6-commons/dist/index.js";

const rootUrl = __ENV.ROOT_URL;

/** The feeders reading AAF files. */
export type AAFFeeder = "AAF" | "AAF1D";

/** Every feeder reading AAF files. They may share the same import directory (as on recette). */
export const AAF_FEEDERS: AAFFeeder[] = ["AAF", "AAF1D"];

/** What the feeder reports at the end of an import (see ImportReportSummary on the directory side). */
export type ImportSummary = {
  status: string;
  message?: string;
  result?: {
    /** file -> errors. Any entry means the import is in error. */
    errors: Record<string, any[]>;
    /** file -> records which have been silently dropped, with the reason. Truncated by the server. */
    ignored: Record<string, { reason: string; object: any }[]>;
    ignoredNotReturned?: Record<string, number>;
    /** file -> number of records (filled by the CSV imports) */
    files: Record<string, number>;
    /** number of users the import read (absent from the platforms which do not send it) */
    usersImported?: number;
    profiles: string[];
  };
};

/**
 * Writes AAF files in the import directory of a feeder and registers the directory for the next import.
 * Requires ALLOW_UPLOAD_AAF_FILES=true on the platform.
 */
export function uploadAAF(feeder: AAFFeeder, subPath: string, files: Record<string, string>): RefinedResponse<any> {
  return http.post(`${rootUrl}/directory/api/internal/upload-aaf`, JSON.stringify({ feeder, subPath, files }), {
    headers: getHeaders("application/json"),
  });
}

/**
 * A number unique to the run, to start the ids of the generated data with ({@link AAFGeneratorBuilder.withIndexStart}).
 * The platform keeps what previous runs imported, and the feeder attaches the users of a file to the structures
 * they had : with the same ids at every run, the structures of a test pile up the users of all its runs (and of the
 * older versions of the generator). Different ids give new structures, users and UAIs at every run.
 */
export function newRunSeed(): number {
  return (Date.now() % 900000000) + 100000;
}

/**
 * The externalId under which the feeder stores an entity (structure, class, group, user...) of the files uploaded by
 * {@link uploadAAF} : the feeder prefixes the externalIds of a sub-directory of the import directory with its name
 * (the "academy" prefix of the import), e.g. "STRUCT-9999" uploaded in "test" becomes "test-STRUCT-9999".
 * @param subPath The directory the files were uploaded in
 * @param externalId The externalId written in the files (the one of the generated model)
 */
export function importedExternalId(subPath: string, externalId: string): string {
  return `${subPath}-${externalId}`;
}

/**
 * Removes the directories created by {@link uploadAAF}.
 * @param subPath The directory to remove. All the uploaded directories of the feeder are removed when missing.
 */
export function cleanAAF(feeder: AAFFeeder, subPath?: string): RefinedResponse<any> {
  return http.post(`${rootUrl}/directory/api/internal/clean-aaf`, JSON.stringify({ feeder, subPath }), {
    headers: getHeaders("application/json"),
  });
}

/**
 * Removes an uploaded directory once the import which reads it is over. The feeder refuses to remove anything (409)
 * while an import is running, which is still the case for a while after the imported data became visible, so the
 * removal is retried.
 * A directory which stays listed is read by every later import of the platform, whatever its feeder, and breaks the
 * ones which cannot read its format : a test which uploads files must always call this at the end.
 * @param maxWaitSeconds How long to wait for the end of the running import
 * @return null when the directory has been removed, otherwise why it could not be
 */
export function cleanAAFWhenIdle(feeder: AAFFeeder, subPath?: string, maxWaitSeconds: number = 120): string | null {
  const end = Date.now() + maxWaitSeconds * 1000;
  for (;;) {
    const res = cleanAAF(feeder, subPath);
    if (res.status === 200) return null;
    if (res.status !== 409 || Date.now() >= end) return `${res.status} ${res.body}`;
    sleep(2);
  }
}

/**
 * Removes every directory uploaded by an integration test, whatever its feeder and its test, before importing.
 * A test which was interrupted (job cancelled, k6 killed) never ran its teardown : its directory stays listed and
 * every later import reads it, which fails as soon as its format is not the one of the feeder (AAF1D files read by
 * the AAF import...). Only the directories created by {@link uploadAAF} are removed : the platform refuses to touch
 * the others.
 * Must not be called while another test uploads files : the integration tests are played one after the other.
 * @param maxWaitSeconds How long to wait for the end of a running import
 * @throws Error when a directory could not be removed : the import of the test would fail anyway
 */
export function cleanAllUploadedAAF(maxWaitSeconds: number = 120): void {
  AAF_FEEDERS.forEach((feeder) => {
    const failure = cleanAAFWhenIdle(feeder, undefined, maxWaitSeconds);
    if (failure) {
      throw new Error(`Cannot remove the AAF directories left by previous tests for the feeder ${feeder} : ${failure}`);
    }
  });
}

/**
 * Triggers an import and waits for its end, which makes re-imports and disappearances testable without polling.
 * @param timeoutMs How long the server waits for the end of the import (the http client waits a bit longer)
 */
export function triggerImportAndWait(feeder: AAFFeeder, timeoutMs: number = 600000): RefinedResponse<any> {
  return http.post(`${rootUrl}/directory/api/internal/trigger-import`, JSON.stringify({ feeder, wait: true, timeout: timeoutMs }), {
    headers: getHeaders("application/json"),
    timeout: `${Math.ceil(timeoutMs / 1000) + 30}s`,
  });
}

/**
 * Triggers an import, waits for its end (post import included) and fails unless it succeeded.
 * The feeder answers "ok" even when the import failed (an unreadable file for instance) : the failure is only in the
 * errors of the report, which are checked here. A failed import usually comes from a directory left by another test
 * (see {@link cleanAAFWhenIdle}) : its name is in the errors.
 * @param timeoutMs How long the server waits for the end of the import
 * @return The summary of the import
 */
export function importAndWaitOrFail(feeder: AAFFeeder, timeoutMs?: number): ImportSummary {
  const res = triggerImportAndWait(feeder, timeoutMs);
  let summary: ImportSummary | null = null;
  try {
    summary = <ImportSummary>JSON.parse(<string>res.body);
  } catch (e) {
    // reported below with the raw body
  }
  const errors = summary?.result?.errors || {};
  if (res.status !== 200 || !summary || summary.status !== "ok" || Object.keys(errors).length > 0) {
    const details = summary && Object.keys(errors).length > 0 ? JSON.stringify(errors) : `${res.status} ${res.body}`;
    throw new Error(`The ${feeder} import failed : ${details}`);
  }
  return summary;
}

/**
 * GET /directory/structure/:id/users does not return the externalId of the users (whatever the UserInfo type says).
 * Completes the users it returned with the one of the admin list, in a single call for the whole structure. The
 * users are returned as they are when they all have one already or when the admin list cannot be read.
 */
/**
 * {@link getUsersOfSchool} (/structure/:id/users) returns a user once per distinct list of structures of their duplicates (DefaultSchoolService.userList
 * groups by that list). The generated names come back at every run, so the users of a run have duplicates in the
 * structures of the older runs : without this, they are counted several times.
 */
export function distinctById<T extends { id: string }>(users: T[]): T[] {
  const byId = new Map<string, T>();
  users.forEach((user) => { if (!byId.has(user.id)) byId.set(user.id, user); });
  return Array.from(byId.values());
}

export function withExternalIds<T extends { id: string; externalId?: string }>(structureId: string, users: T[]): T[] {
  if (users.every((user) => !!user.externalId)) return users;
  const res = http.get(`${rootUrl}/directory/user/admin/list?structureId=${structureId}`, { headers: getHeaders() });
  if (res.status !== 200) return users;
  const externalIds = new Map<string, string>();
  (<any[]>JSON.parse(<string>res.body)).forEach((user) => {
    if (user.externalId) externalIds.set(user.id, user.externalId);
  });
  return users.map((user) => (user.externalId || !externalIds.has(user.id) ? user : { ...user, externalId: externalIds.get(user.id)! }));
}

export function getUserDetail(userId: string): any {
  const res = http.get(`${rootUrl}/directory/user/${userId}`, { headers: getHeaders() });
  if (res.status !== 200) return null;
  return JSON.parse(<string>res.body);
}
