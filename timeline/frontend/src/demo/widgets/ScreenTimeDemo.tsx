import {
  ScreenTime,
  ScreenTimeDayEntry,
  ScreenTimeHourEntry,
  ScreenTimeKid,
  ScreenTimeModal,
  ScreenTimeView,
} from '@edifice.io/react/homepage';
import { useMemo, useState } from 'react';

/** A student is their own only kid: no tab bar, no kid select. */
const KIDS: ScreenTimeKid[] = [
  {
    id: 'demo-student',
    firstName: 'Léa',
    lastName: 'Martin',
    displayName: 'Léa Martin',
  },
];

/** Share of the daily screen time spent at each hour, from 0h to 23h. */
const SCHOOL_DAY_WEIGHTS = [
  0, 0, 0, 0, 0, 0, 0, 2, 3, 4, 2, 3, 2, 4, 3, 2, 3, 6, 8, 5, 7, 5, 1, 0,
];
const DAY_OFF_WEIGHTS = [
  0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 4, 5, 2, 3, 5, 6, 6, 5, 6, 5, 7, 6, 2, 0,
];

const pad = (value: number) => String(value).padStart(2, '0');

/** Local "YYYY-MM-DD" date. */
const toDay = (date: Date): string =>
  `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;

const parseDay = (day: string): Date => new Date(`${day}T00:00:00`);

const addDays = (day: string, days: number): string => {
  const date = parseDay(day);
  date.setDate(date.getDate() + days);
  return toDay(date);
};

/** Monday of the week containing `day`. */
const weekStart = (day: string): string =>
  addDays(day, -((parseDay(day).getDay() + 6) % 7));

/**
 * Screen time of a day, made up but stable for a given date: more on
 * Wednesday afternoon and at the weekend. Hours to come are empty.
 */
const hoursOf = (day: string): ScreenTimeHourEntry[] => {
  const now = new Date();
  const today = toDay(now);
  if (day > today) return [];

  const date = parseDay(day);
  const weekDay = date.getDay();
  const isDayOff = weekDay === 0 || weekDay === 6;
  const weights = isDayOff ? DAY_OFF_WEIGHTS : SCHOOL_DAY_WEIGHTS;
  const total =
    (isDayOff ? 165 : weekDay === 3 ? 130 : 95) +
    ((date.getDate() * 13) % 40) -
    20;
  const weightSum = weights.reduce((sum, weight) => sum + weight, 0);
  const lastHour = day === today ? now.getHours() : 23;

  return weights.flatMap((weight, hour) =>
    weight > 0 && hour <= lastHour
      ? [
          {
            hour: pad(hour),
            durationMinutes: Math.round((total * weight) / weightSum),
            schoolUsePercentage: !isDayOff && hour >= 8 && hour < 17 ? 60 : 5,
          },
        ]
      : [],
  );
};

const minutesOf = (day: string): number =>
  hoursOf(day).reduce((sum, entry) => sum + entry.durationMinutes, 0);

const daysOfWeek = (day: string): ScreenTimeDayEntry[] => {
  const start = weekStart(day);
  return Array.from({ length: 7 }, (_, index) => addDays(start, index)).map(
    (date) => ({
      date,
      durationMinutes: minutesOf(date),
      schoolUsePercentage: 30,
    }),
  );
};

/**
 * Demo "Temps d'écran": the student's own screen time, generated around
 * today. The modal switches between weekly and daily views and browses past
 * periods, up to today.
 */
export function ScreenTimeDemo() {
  const today = toDay(new Date());
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [view, setView] = useState<ScreenTimeView>('weekly');
  const [date, setDate] = useState(today);

  const days = useMemo(() => daysOfWeek(date), [date]);
  const hours = useMemo(() => hoursOf(date), [date]);

  const step = view === 'weekly' ? 7 : 1;
  const nextDate = addDays(view === 'weekly' ? weekStart(date) : date, step);

  const openModal = () => {
    setView('weekly');
    setDate(today);
    setIsModalOpen(true);
  };

  return (
    <>
      <ScreenTime
        status="default"
        kids={KIDS}
        selectedKidId={KIDS[0].id}
        onKidChange={() => {}}
        todayMinutes={minutesOf(today)}
        yesterdayMinutes={minutesOf(addDays(today, -1))}
        onFullScreenClick={openModal}
      />
      <ScreenTimeModal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        kids={KIDS}
        selectedKid={KIDS[0]}
        onKidChange={() => {}}
        view={view}
        onViewChange={setView}
        date={date}
        canGoPrevious
        canGoNext={nextDate <= today}
        onPrevious={() => setDate(addDays(date, -step))}
        onNext={() => setDate(nextDate)}
        days={days}
        hours={hours}
        status="default"
      />
    </>
  );
}
