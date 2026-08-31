export interface AgendaEvent {
  id: string;
  title: string;
  startDate: string;
  endDate: string;
  allDay?: boolean;
}

/** Raw shape returned by `GET /calendar/calendars`. */
export interface CalendarSummary {
  _id: string;
  title: string;
  color: string;
}

/** Raw shape returned by `GET /calendar/events/widget`. */
export interface CalendarEventApi {
  _id: string;
  title: string;
  allday?: boolean;
  startMoment: string;
  endMoment: string;
}

export interface AgendaDayGroup {
  /** ISO date (yyyy-MM-dd) of the group, used as key and for the date badge. */
  date: string;
  events: AgendaEvent[];
}

export interface UseAgendaResult {
  dayGroups: AgendaDayGroup[];
  isLoading: boolean;
  isError: boolean;
}

const WEEKDAYS_SHORT = ['Dim.', 'Lun.', 'Mar.', 'Mer.', 'Jeu.', 'Ven.', 'Sam.'];
const MONTHS_SHORT = [
  'Jan.',
  'Fév.',
  'Mar.',
  'Avr.',
  'Mai',
  'Juin',
  'Juil.',
  'Aoû.',
  'Sep.',
  'Oct.',
  'Nov.',
  'Déc.',
];

const dayKey = (date: Date): string =>
  `${date.getFullYear()}-${date.getMonth()}-${date.getDate()}`;

const pad2 = (value: number): string => String(value).padStart(2, '0');

const formatTime = (date: Date): string =>
  `${pad2(date.getHours())}h${pad2(date.getMinutes())}`;

/** Groups events by calendar day (based on `startDate`) and sorts both groups and events chronologically. */
export const groupEventsByDay = (events: AgendaEvent[]): AgendaDayGroup[] => {
  const groups = new Map<string, AgendaDayGroup>();

  [...events]
    .sort(
      (a, b) =>
        new Date(a.startDate).getTime() - new Date(b.startDate).getTime(),
    )
    .forEach((event) => {
      const start = new Date(event.startDate);
      const key = dayKey(start);
      const group = groups.get(key);
      if (group) {
        group.events.push(event);
      } else {
        groups.set(key, {
          date: `${start.getFullYear()}-${pad2(start.getMonth() + 1)}-${pad2(start.getDate())}`,
          events: [event],
        });
      }
    });

  return Array.from(groups.values());
};

export const formatAgendaDayBadge = (
  isoDate: string,
): { weekday: string; day: string; month: string } => {
  const date = new Date(`${isoDate}T00:00:00`);
  return {
    weekday: WEEKDAYS_SHORT[date.getDay()],
    day: String(date.getDate()),
    month: MONTHS_SHORT[date.getMonth()],
  };
};

/** Formats an event's time range, e.g. "11h00 - 12h00", "14h00 - Ven. 11 à 15h30", or all-day. */
export const formatAgendaEventTime = (
  event: AgendaEvent,
  allDayLabel: string,
): string => {
  if (event.allDay) return allDayLabel;

  const start = new Date(event.startDate);
  const end = new Date(event.endDate);

  if (dayKey(start) === dayKey(end)) {
    return `${formatTime(start)} - ${formatTime(end)}`;
  }

  return `${formatTime(start)} - ${WEEKDAYS_SHORT[end.getDay()]} ${end.getDate()} à ${formatTime(end)}`;
};
