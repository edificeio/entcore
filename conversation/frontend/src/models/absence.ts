import { Content } from '@edifice.io/react/editor';

export type AbsenceSettings = {
  enabled: boolean;
  /** UTC instant (ISO 8601), start of the local day picked by the user. */
  startAt: string;
  /** UTC instant (ISO 8601), end of the local day picked by the user. */
  endAt: string;
  bodyJson: Content;
};

/**
 * Shape returned by `GET`/`PUT /conversation/absence`: `AbsenceSettings` plus
 * server-derived fields. `bodyHtml` is derived from `bodyJson` and must never
 * be sent back as-is.
 */
export type AbsenceSettingsResponse = AbsenceSettings & {
  bodyHtml: string;
  updatedAt: string;
};
