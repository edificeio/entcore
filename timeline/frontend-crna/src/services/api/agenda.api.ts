import { odeServices } from '@edifice.io/client';
import type {
  AgendaEvent,
  CalendarEventApi,
  CalendarSummary,
} from '~/models/agenda';

/** Max number of upcoming events fetched for the widget. */
const EVENTS_LIMIT = 10;

function mapEventApiToAgendaEvent(event: CalendarEventApi): AgendaEvent {
  return {
    id: event._id,
    title: event.title,
    startDate: event.startMoment,
    endDate: event.endMoment,
    allDay: event.allday,
  };
}

export async function fetchAgendaEvents(): Promise<AgendaEvent[]> {
  const calendars = await odeServices
    .http()
    .get<CalendarSummary[]>('/calendar/calendars');

  // odeServices.http() never rejects: a failed request resolves with an
  // error response object instead, so we must throw explicitly to let
  // react-query detect it.
  if (!Array.isArray(calendars)) {
    throw new Error('agenda.widget.calendars.fetch.error');
  }
  if (calendars.length === 0) return [];

  const params = new URLSearchParams();
  calendars.forEach(({ _id }) => params.append('calendarId', _id));
  params.set('nb', String(EVENTS_LIMIT));

  const events = await odeServices
    .http()
    .get<CalendarEventApi[]>(`/calendar/events/widget?${params.toString()}`);

  if (!Array.isArray(events)) {
    throw new Error('agenda.widget.events.fetch.error');
  }
  return events.map(mapEventApiToAgendaEvent);
}
