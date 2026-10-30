import http, { RefinedResponse } from "k6/http";
import { getHeaders } from "../../../node_modules/edifice-k6-commons/dist/index.js";

const rootUrl = __ENV.ROOT_URL;

/** The feeders reading AAF files. */
export type AAFFeeder = "AAF" | "AAF1D";

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
 * Removes the directories created by {@link uploadAAF}.
 * @param subPath The directory to remove. All the uploaded directories of the feeder are removed when missing.
 */
export function cleanAAF(feeder: AAFFeeder, subPath?: string): RefinedResponse<any> {
  return http.post(`${rootUrl}/directory/api/internal/clean-aaf`, JSON.stringify({ feeder, subPath }), {
    headers: getHeaders("application/json"),
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
 * GET /directory/structure/:id/users does not return the externalId of the users (whatever the UserInfo type says).
 * Completes the users it returned with the one of the admin list, in a single call for the whole structure. The
 * users are returned as they are when they all have one already or when the admin list cannot be read.
 */
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
