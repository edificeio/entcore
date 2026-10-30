/**
 * Messages of the feeder scenarios. Everything here is pure (no k6, no http) so that it can be unit tested with
 * `node --test` (see _feeder-errors.test.ts).
 *
 * Why the messages are built so carefully : the integration tests report of Jenkins only shows the *name* of the
 * k6 checks which failed, on a single line. A failure has therefore to be fully explained by its name :
 * which scenario, which step, what was expected, what has been found, and what the last import said.
 */
import type { ImportSummary } from "../directory/_aaf-utils.ts";

/** Longest message given to k6 as the name of a check. */
export const MAX_MESSAGE_LENGTH = 1500;

/** The scenario cannot go on (upload refused, import in error...) : the next steps would only fail in cascade. */
export class FeederScenarioError extends Error {
  constructor(message: string) {
    super(message);
    this.name = "FeederScenarioError";
  }
}

/** An expectation is not met. The scenario goes on to report every broken expectation. */
export class ExpectationFailed extends Error {
  readonly expected: string;
  readonly actual: string;

  constructor(expected: string, actual: string) {
    super(`${expected} ; constaté : ${actual}`);
    this.name = "ExpectationFailed";
    this.expected = expected;
    this.actual = actual;
  }
}

/** Keeps a message on one line (the report of Jenkins is parsed line by line) and bounded. */
export function oneLine(message: string): string {
  const flat = message.replace(/\s*[\r\n]+\s*/g, " ⏎ ");
  return flat.length > MAX_MESSAGE_LENGTH ? `${flat.substring(0, MAX_MESSAGE_LENGTH - 1)}…` : flat;
}

/** "a, b, c (+12 autres)" */
export function preview(items: string[], max: number = 5): string {
  if (items.length === 0) return "aucun";
  const shown = items.slice(0, max).join(", ");
  return items.length > max ? `${shown} (+${items.length - max} autres)` : shown;
}

export function plural(count: number, singular: string, pluralForm: string = `${singular}s`): string {
  return `${count} ${count > 1 ? pluralForm : singular}`;
}

export function levenshtein(a: string, b: string): number {
  const previous = Array.from({ length: b.length + 1 }, (_, i) => i);
  for (let i = 1; i <= a.length; i++) {
    let diagonal = previous[0];
    previous[0] = i;
    for (let j = 1; j <= b.length; j++) {
      const above = previous[j];
      previous[j] = Math.min(previous[j] + 1, previous[j - 1] + 1, diagonal + (a[i - 1] === b[j - 1] ? 0 : 1));
      diagonal = above;
    }
  }
  return previous[b.length];
}

/** Explains an unknown reference, with a suggestion when a known one looks alike. */
export function unknownReference(kind: string, unknown: string, known: string[]): string {
  const closest = known
    .map((name) => ({ name, distance: levenshtein(unknown.toLowerCase(), name.toLowerCase()) }))
    .sort((a, b) => a.distance - b.distance)[0];
  const suggestion = closest && closest.distance <= Math.max(2, Math.floor(unknown.length / 3)) ? ` Vouliez-vous dire « ${closest.name} » ?` : "";
  const knownList = known.length === 0 ? "aucune n'est déclarée à ce stade" : `connues : ${known.join(", ")}`;
  return `${kind} inconnu(e) « ${unknown} » (${knownList}).${suggestion}`;
}

/** What the last import said, in one sentence. */
export function describeImport(label: string | undefined, summary: ImportSummary | undefined): string {
  if (!summary) {
    return "aucun import n'a encore eu lieu";
  }
  const name = label ? `Dernier import (« ${label} »)` : "Dernier import";
  const result = summary.result;
  if (!result) {
    return `${name} : statut ${summary.status}${summary.message ? ` (${summary.message})` : ""}`;
  }
  const errors = countErrors(result.errors);
  const ignoredByReason = ignoredReasons(result.ignored);
  const nbIgnored = Object.values(result.ignored || {}).reduce((total, records) => total + records.length, 0)
    + Object.values(result.ignoredNotReturned || {}).reduce((total, n) => total + n, 0);
  const ignored = nbIgnored === 0 ? "0 enregistrement ignoré" : `${plural(nbIgnored, "enregistrement ignoré", "enregistrements ignorés")} [${ignoredByReason}]`;
  const imported = describeImported(result);
  return `${name} : ${plural(errors, "erreur")}, ${ignored}${imported ? `, ${imported}` : ""}`;
}

/** "12 utilisateurs lus" or "importés : Eleve 2, PersEducNat 1" : what the import read, when the platform tells it. */
function describeImported(result: { files?: Record<string, number>; usersImported?: number }): string {
  if (result.usersImported !== undefined) {
    return plural(result.usersImported, "utilisateur lu", "utilisateurs lus");
  }
  const parts = Object.keys(result.files || {}).sort().map((file) => `${file.replace(/^.*[\\/]/, "")} ${result.files![file]}`);
  return parts.length === 0 ? "" : `importés : ${preview(parts, 6)}`;
}

export function countErrors(errors: Record<string, any[]> | undefined): number {
  return Object.values(errors || {}).reduce((total, list) => total + (Array.isArray(list) ? list.length : 0), 0);
}

/** "Eleve: invalid.birthDate ×2, PersEducNat: required.firstName" */
function ignoredReasons(ignored: Record<string, { reason: string }[]> | undefined): string {
  const parts: string[] = [];
  for (const file of Object.keys(ignored || {})) {
    const counts = new Map<string, number>();
    for (const record of ignored![file]) {
      counts.set(record.reason, (counts.get(record.reason) || 0) + 1);
    }
    counts.forEach((count, reason) => parts.push(`${file}: ${reason}${count > 1 ? ` ×${count}` : ""}`));
  }
  return preview(parts, 4);
}

/** The explanation of an import in error, with the first errors and ignored records. */
export function describeImportFailure(label: string, summary: ImportSummary): string {
  const result = summary.result;
  const details: string[] = [];
  if (result) {
    for (const file of Object.keys(result.errors || {})) {
      for (const error of result.errors[file].slice(0, 2)) {
        details.push(`${file}: ${error.translation || error.key || JSON.stringify(error)}`);
      }
    }
  }
  const status = summary.status === "ok" ? "terminé avec des erreurs" : `en échec (statut ${summary.status}${summary.message ? `, ${summary.message}` : ""})`;
  return `l'import « ${label} » est ${status}. ${describeImport(undefined, summary)}.${details.length > 0 ? ` Premières erreurs : ${preview(details, 4)}` : ""}`;
}

/** Prefix of every message about a step : `[ENABLING-1377] étape 4/6 « label »`. */
export function stepPrefix(ticket: string, stepNumber: number, stepCount: number, label: string): string {
  return `[${ticket}] étape ${stepNumber}/${stepCount} « ${label} »`;
}

/** The name of the check reporting a broken expectation. */
export function failureMessage(prefix: string, failure: ExpectationFailed, importContext: string): string {
  return oneLine(`${prefix} ▸ attendu : ${failure.expected} ; constaté : ${failure.actual}. ${importContext}.`);
}

/** The name of the check reporting a scenario which could not go on. */
export function abortMessage(prefix: string, reason: string): string {
  return oneLine(`${prefix} ▸ ARRÊT : ${reason}`);
}

/** What to do about the errors returned by the routes which feed the feeder, by status. */
export function describeHttpFailure(method: string, route: string, status: number, body: string): string {
  const hints: Record<number, string> = {
    401: "la session est invalide : vérifier ADMC_LOGIN/ADMC_PASSWORD",
    403: "route désactivée : positionner ALLOW_UPLOAD_AAF_FILES=true sur la plateforme testée (ou dossier non créé par un upload)",
    409: "un import est déjà en cours sur la plateforme : l'import est unique, relancer après sa fin",
    504: "le serveur a cessé d'attendre la fin de l'import : augmenter le délai ou chercher un import bloqué",
    0: "le serveur n'a pas répondu (ROOT_URL injoignable ou délai dépassé)",
  };
  const hint = hints[status] ? ` — ${hints[status]}` : "";
  return `${method} ${route} a répondu ${status}${body ? ` (${oneLine(body).substring(0, 300)})` : ""}${hint}`;
}
