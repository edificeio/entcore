export type TimetableColor =
  | 'grey'
  | 'blue'
  | 'yellow'
  | 'green'
  | 'orange'
  | 'pink';

export interface TimetableEntry {
  id: string;
  subject: string;
  room?: string;
  teacher?: string;
  startDate: string;
  endDate: string;
  color: TimetableColor;
}

export interface TimetableDay {
  /** ISO date (yyyy-MM-dd), used as key. */
  date: string;
  entries: TimetableEntry[];
}

export interface UseTimetableResult {
  days: TimetableDay[];
  isLoading: boolean;
  isError: boolean;
}

const WEEKDAYS_LONG = [
  'dimanche',
  'lundi',
  'mardi',
  'mercredi',
  'jeudi',
  'vendredi',
  'samedi',
];

const capitalize = (value: string): string =>
  value.charAt(0).toUpperCase() + value.slice(1);

/** Weekday tab label, e.g. "Lundi", "Mercredi". */
export const formatTimetableTabWeekday = (isoDate: string): string => {
  const date = new Date(`${isoDate}T00:00:00`);
  return capitalize(WEEKDAYS_LONG[date.getDay()]);
};

/** Date shown under the weekday tab label, e.g. "26/05". */
export const formatTimetableTabDate = (isoDate: string): string => {
  const date = new Date(`${isoDate}T00:00:00`);
  return `${String(date.getDate()).padStart(2, '0')}/${String(date.getMonth() + 1).padStart(2, '0')}`;
};

/** Formats a time as "8h00" (no zero-padding on the hour, matching the design). */
export const formatTimetableTime = (isoDate: string): string => {
  const date = new Date(isoDate);
  return `${date.getHours()}h${String(date.getMinutes()).padStart(2, '0')}`;
};

export const isTimetableEntryCurrent = (
  entry: TimetableEntry,
  now: Date,
): boolean => {
  const start = new Date(entry.startDate);
  const end = new Date(entry.endDate);
  return now >= start && now < end;
};

export const toIsoDate = (date: Date): string =>
  `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;

/** Returns the ISO dates of Monday through Friday of the week containing `now`. */
export const getCurrentWeekSchoolDays = (now: Date): string[] => {
  const monday = new Date(now);
  monday.setHours(0, 0, 0, 0);
  const dayOfWeek = monday.getDay();
  const diffToMonday = dayOfWeek === 0 ? -6 : 1 - dayOfWeek;
  monday.setDate(monday.getDate() + diffToMonday);

  return Array.from({ length: 5 }, (_, i) => {
    const date = new Date(monday);
    date.setDate(date.getDate() + i);
    return toIsoDate(date);
  });
};

/** Buckets entries under each of `dates`, in chronological order, defaulting to an empty list for free days. */
export const buildTimetableDays = (
  entries: TimetableEntry[],
  dates: string[],
): TimetableDay[] => {
  const byDate = new Map<string, TimetableEntry[]>();

  [...entries]
    .sort(
      (a, b) =>
        new Date(a.startDate).getTime() - new Date(b.startDate).getTime(),
    )
    .forEach((entry) => {
      const key = toIsoDate(new Date(entry.startDate));
      const bucket = byDate.get(key);
      if (bucket) bucket.push(entry);
      else byDate.set(key, [entry]);
    });

  return dates.map((date) => ({ date, entries: byDate.get(date) ?? [] }));
};
