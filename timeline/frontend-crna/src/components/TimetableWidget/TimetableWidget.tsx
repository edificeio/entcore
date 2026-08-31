import { EmptyScreen } from '@edifice.io/react';
import { HomeCard } from '@edifice.io/react/homepage';
import { IconArrowRight } from '@edifice.io/react/icons';
import clsx from 'clsx';
import { useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useTimetable } from '~/hooks/useTimetable';
import { useTimetableChildren } from '~/hooks/useTimetableChildren';
import {
  formatTimetableTabDate,
  formatTimetableTabWeekday,
  formatTimetableTime,
  isTimetableEntryCurrent,
} from '~/models/timetable';
import { WidgetEmptyState } from '../ui/WidgetEmptyState';
import { WidgetSkeleton } from '../ui/WidgetSkeleton';
import illuNoEvent from './assets/illu-no-event.svg';
import { ChildTabs } from './ChildTabs';
import './TimetableWidget.css';

export interface TimetableWidgetProps {
  onSeeMore?: () => void;
}

// Matches .timetable-entry's min-height and .timetable-entries' gap in
// TimetableWidget.css — used to pre-size the entries area to the busiest
// day, so switching tabs never resizes the widget.
const ENTRY_MIN_HEIGHT = 70;
const ENTRIES_GAP = 4;

export function TimetableWidget({
  onSeeMore = () => window.open('/edt', '_self'),
}: TimetableWidgetProps) {
  const { t } = useTranslation('timeline');
  // Not gated on `user.childrenIds`: that session field has proven
  // unreliable (mirrors the earlier `firstName` mismatch) — the dedicated
  // EDT endpoint just returns an empty list for non-relative users, which
  // ChildTabs already handles by rendering nothing.
  const { children } = useTimetableChildren(true);

  const [currentChildIndex, setCurrentChildIndex] = useState(0);
  const selectedChild = children[currentChildIndex];

  const { days, isLoading, isError } = useTimetable(selectedChild);
  const [selectedIndex, setSelectedIndex] = useState(0);
  const now = useMemo(() => new Date(), []);

  const selectedDay = days[selectedIndex];

  const maxEntriesCount = Math.max(0, ...days.map((day) => day.entries.length));
  const entriesAreaMinHeight =
    maxEntriesCount > 0
      ? maxEntriesCount * ENTRY_MIN_HEIGHT + (maxEntriesCount - 1) * ENTRIES_GAP
      : undefined;

  return (
    <HomeCard variant="user">
      <HomeCard.Header
        title={t('homepage.crna.widget.timetable.title', 'Emploi du temps')}
        actionLabel={t('homepage.crna.widget.see.more', 'Voir plus')}
        onActionClick={onSeeMore}
        actionRightIcon={<IconArrowRight />}
      />
      <HomeCard.Content>
        {isLoading ? (
          <WidgetSkeleton />
        ) : isError ? (
          <WidgetEmptyState
            text={t(
              'homepage.crna.widget.timetable.error',
              'Impossible de récupérer votre emploi du temps. Veuillez réessayer plus tard.',
            )}
          />
        ) : (
          <>
            <ChildTabs
              children={children}
              currentIndex={currentChildIndex}
              onSelect={setCurrentChildIndex}
            />
            <div className="timetable-tabs">
              {days.map((day, index) => (
                <button
                  key={day.date}
                  type="button"
                  className={clsx(
                    'timetable-tab',
                    index === selectedIndex && 'timetable-tab--active',
                  )}
                  onClick={() => setSelectedIndex(index)}
                >
                  <span className="timetable-tab-weekday">
                    {formatTimetableTabWeekday(day.date)}
                  </span>
                  <span className="timetable-tab-date">
                    {formatTimetableTabDate(day.date)}
                  </span>
                </button>
              ))}
            </div>
            <div style={{ minHeight: entriesAreaMinHeight }}>
              {selectedDay.entries.length === 0 ? (
                <EmptyScreen
                  imageSrc={illuNoEvent}
                  size={64}
                  text={t(
                    'homepage.crna.widget.timetable.empty',
                    'Pas de cours ce jour-là.',
                  )}
                />
              ) : (
                <div className="timetable-entries">
                  {selectedDay.entries.map((entry) => {
                    const isCurrent = isTimetableEntryCurrent(entry, now);
                    return (
                      <div
                        key={entry.id}
                        className={clsx(
                          'timetable-entry',
                          isCurrent && 'timetable-entry--current',
                        )}
                        data-color={isCurrent ? undefined : entry.color}
                      >
                        {isCurrent && (
                          <div className="timetable-entry-accent" />
                        )}
                        <div className="timetable-entry-content">
                          <div className="timetable-entry-time">
                            <span>{formatTimetableTime(entry.startDate)}</span>
                            <span>{formatTimetableTime(entry.endDate)}</span>
                          </div>
                          <div className="timetable-entry-info">
                            <p className="timetable-entry-subject">
                              {entry.subject}
                            </p>
                            {entry.room && (
                              <p className="timetable-entry-room">
                                {t(
                                  'homepage.crna.widget.timetable.room-prefix',
                                  'Salle : ',
                                )}
                                {entry.room}
                              </p>
                            )}
                          </div>
                        </div>
                      </div>
                    );
                  })}
                </div>
              )}
            </div>
          </>
        )}
      </HomeCard.Content>
    </HomeCard>
  );
}
