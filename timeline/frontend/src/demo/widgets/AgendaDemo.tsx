import { CalendarEvent } from '@edifice.io/client';
import { Agenda } from '@edifice.io/react/homepage';
import { useMemo } from 'react';

/** `count`-th school day (Monday to Friday) after today, at the given time. */
const schoolDayAt = (count: number, hours: number, minutes = 0): Date => {
  const date = new Date();
  let remaining = count;
  while (remaining > 0) {
    date.setDate(date.getDate() + 1);
    const weekDay = date.getDay();
    if (weekDay !== 0 && weekDay !== 6) remaining -= 1;
  }
  date.setHours(hours, minutes, 0, 0);
  return date;
};

const buildEvents = (): CalendarEvent[] => [
  {
    id: 'demo-agenda-controle-maths',
    calendarId: 'demo-agenda-5b',
    title: 'Contrôle de mathématiques : les fractions',
    startMoment: schoolDayAt(1, 10, 15).toISOString(),
    endMoment: schoolDayAt(1, 11, 15).toISOString(),
    allDay: false,
  },
  {
    id: 'demo-agenda-club-theatre',
    calendarId: 'demo-agenda-5b',
    title: 'Club théâtre : répétition du spectacle de fin de trimestre',
    startMoment: schoolDayAt(1, 12, 30).toISOString(),
    endMoment: schoolDayAt(1, 13, 30).toISOString(),
    allDay: false,
  },
  {
    id: 'demo-agenda-sortie-louvre-lens',
    calendarId: 'demo-agenda-5b',
    title: 'Sortie pédagogique au Louvre-Lens avec la classe de 5e B',
    startMoment: schoolDayAt(3, 8).toISOString(),
    endMoment: schoolDayAt(3, 17).toISOString(),
    allDay: true,
  },
];

/** Demo "Agenda": the next three events of a 5e student, always upcoming. */
export function AgendaDemo() {
  const events = useMemo(buildEvents, []);

  return (
    <Agenda
      events={events}
      onEventClick={() => {}}
      onOpenAgendaClick={() => {}}
    />
  );
}
