import { useMemo } from 'react';
import { TimetableWidget } from '~/components/TimetableWidget';
import {
  buildTimetableDays,
  getCurrentWeekSchoolDays,
  TimetableColor,
  TimetableEntry,
} from '~/models/timetable';

/** [start, end, subject, room, color] — times as "HH:mm". */
type Slot = [string, string, string, string, TimetableColor];

/** Typical week of a 5e class, from Monday to Friday. */
const WEEK: Slot[][] = [
  [
    ['08:00', '09:00', 'Français', 'B12', 'blue'],
    ['09:00', '10:00', 'Mathématiques', 'A04', 'orange'],
    ['10:15', '11:15', 'Histoire-Géographie', 'B08', 'yellow'],
    ['11:15', '12:15', 'Anglais', 'C02', 'green'],
    ['14:00', '15:00', 'SVT', 'Labo 1', 'pink'],
    ['15:00', '16:00', 'Éducation musicale', 'Salle de musique', 'grey'],
  ],
  [
    ['08:00', '10:00', 'EPS', 'Gymnase', 'green'],
    ['10:15', '11:15', 'Mathématiques', 'A04', 'orange'],
    ['11:15', '12:15', 'Français', 'B12', 'blue'],
    ['14:00', '15:00', 'Espagnol', 'C05', 'pink'],
    ['15:00', '16:00', 'Technologie', 'Atelier 2', 'grey'],
  ],
  [
    ['08:00', '09:00', 'Anglais', 'C02', 'green'],
    ['09:00', '10:00', 'Physique-Chimie', 'Labo 2', 'blue'],
    ['10:15', '12:15', 'Arts plastiques', "Salle d'arts", 'pink'],
  ],
  [
    ['08:00', '09:00', 'Histoire-Géographie', 'B08', 'yellow'],
    ['09:00', '10:00', 'Français', 'B12', 'blue'],
    ['10:15', '11:15', 'Mathématiques', 'A04', 'orange'],
    ['11:15', '12:15', 'SVT', 'Labo 1', 'pink'],
    ['14:00', '15:00', 'Anglais', 'C02', 'green'],
    ['15:00', '16:00', 'Espagnol', 'C05', 'pink'],
  ],
  [
    ['08:00', '09:00', 'Mathématiques', 'A04', 'orange'],
    ['09:00', '10:00', 'Physique-Chimie', 'Labo 2', 'blue'],
    ['10:15', '11:15', 'Français', 'B12', 'blue'],
    ['11:15', '12:15', 'EMC', 'B08', 'yellow'],
    ['14:00', '16:00', 'EPS', 'Gymnase', 'green'],
  ],
];

/** Demo "Emploi du temps": a typical week, always on the current week. */
export function TimetableDemo() {
  const days = useMemo(() => {
    const dates = getCurrentWeekSchoolDays(new Date());
    const entries: TimetableEntry[] = dates.flatMap((date, dayIndex) =>
      WEEK[dayIndex].map(([start, end, subject, room, color], slotIndex) => ({
        id: `${date}-${slotIndex}`,
        subject,
        room,
        // Local times, read back with `new Date()` by the widget
        startDate: `${date}T${start}:00`,
        endDate: `${date}T${end}:00`,
        color,
      })),
    );
    return buildTimetableDays(entries, dates);
  }, []);

  return <TimetableWidget days={days} onSeeMore={() => {}} />;
}
