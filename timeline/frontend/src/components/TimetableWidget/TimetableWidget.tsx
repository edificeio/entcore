import { EmptyScreen, TextSkeleton } from '@edifice.io/react';
import { HomeCard } from '@edifice.io/react/homepage';
import { IconArrowRight } from '@edifice.io/react/icons';
import clsx from 'clsx';
import { useMemo, useState } from 'react';
import { useI18n } from '~/hooks/useI18n';
import type { Child } from '~/models/child';
import {
  formatTimetableTabDate,
  formatTimetableTabWeekday,
  formatTimetableTime,
  isTimetableEntryCurrent,
  toIsoDate,
  TimetableDay,
} from '~/models/timetable';
import illuNoEvent from './assets/illu-no-event.svg';
import { ChildTabs } from './ChildTabs';
import './TimetableWidget.css';

export interface TimetableWidgetProps {
  /** School days of the week to display, each with its entries. */
  days: TimetableDay[];
  /** Children of a relative, shown as tabs above the days (2 or more). */
  childList?: Child[];
  currentChildIndex?: number;
  onSelectChild?: (index: number) => void;
  isLoading?: boolean;
  isError?: boolean;
  onSeeMore?: () => void;
}

// Matches .timetable-entry's min-height and .timetable-entries' gap in
// TimetableWidget.css — used to pre-size the entries area to the busiest
// day, so switching tabs never resizes the widget.
const ENTRY_MIN_HEIGHT = 70;
const ENTRIES_GAP = 4;

/**
 * "Emploi du temps" widget, migrated from the CRNA homepage
 * (`timeline/frontend-crna`) as a presentational component: the data
 * (days, children) comes from props.
 */
export function TimetableWidget({
  days,
  childList = [],
  currentChildIndex = 0,
  onSelectChild = () => {},
  isLoading = false,
  isError = false,
  onSeeMore = () => window.open('/edt', '_self'),
}: TimetableWidgetProps) {
  const { t } = useI18n();
  const now = useMemo(() => new Date(), []);
  // Opens on today when it is one of the displayed days
  const [selectedIndex, setSelectedIndex] = useState(() =>
    Math.max(
      0,
      days.findIndex((day) => day.date === toIsoDate(now)),
    ),
  );

  const selectedEntries = days[selectedIndex]?.entries ?? [];
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
          <div className="d-flex flex-column gap-8">
            {[0, 1, 2].map((index) => (
              <TextSkeleton key={index} size="md" />
            ))}
          </div>
        ) : isError ? (
          <EmptyScreen
            imageSrc={illuNoEvent}
            size={64}
            text={t(
              'homepage.crna.widget.timetable.error',
              'Impossible de récupérer votre emploi du temps. Veuillez réessayer plus tard.',
            )}
          />
        ) : (
          <>
            <ChildTabs
              childList={childList}
              currentIndex={currentChildIndex}
              onSelect={onSelectChild}
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
              {selectedEntries.length === 0 ? (
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
                  {selectedEntries.map((entry) => {
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
