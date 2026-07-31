import { Content } from '@edifice.io/react/editor';

export type AbsenceSettings = {
  enabled: boolean;
  /** UTC instant (ISO 8601), start of the local day picked by the user. */
  startAt: string;
  /** UTC instant (ISO 8601), end of the local day picked by the user. */
  endAt: string;
  bodyJson: Content;
};
