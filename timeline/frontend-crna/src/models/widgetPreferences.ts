export type WidgetId =
  | 'communities'
  | 'agenda'
  | 'carnet-de-bord'
  | 'mediacentre'
  | 'avantages'
  | 'timetable'
  | 'useful-links'
  | 'cantine'
  | 'screen-time'
  | 'edumalin'
  | 'briefme'
  | 'rss'
  | 'universalis'
  | 'qwant'
  | 'generation-hdf'
  | 'allo-region';

export interface WidgetPreferences {
  /** Ids of widgets the user has chosen to hide from the homepage. */
  hidden: WidgetId[];
}
