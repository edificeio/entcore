export type WidgetId =
  | 'communities'
  | 'agenda'
  | 'carnet-de-bord'
  | 'mediacentre'
  | 'avantages'
  | 'timetable';

export interface WidgetPreferences {
  /** Ids of widgets the user has chosen to hide from the homepage. */
  hidden: WidgetId[];
}
