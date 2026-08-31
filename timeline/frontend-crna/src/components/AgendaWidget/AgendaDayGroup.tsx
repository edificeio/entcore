import { IconClock } from '@edifice.io/react/icons';
import { useTranslation } from 'react-i18next';
import { formatAgendaDayBadge, formatAgendaEventTime } from '~/models/agenda';
import type { AgendaDayGroup as AgendaDayGroupModel } from '~/models/agenda';

export interface AgendaDayGroupProps {
  group: AgendaDayGroupModel;
}

export function AgendaDayGroup({ group }: AgendaDayGroupProps) {
  const { t } = useTranslation('timeline');
  const { weekday, day, month } = formatAgendaDayBadge(group.date);
  const allDayLabel = t(
    'homepage.crna.widget.agenda.all-day',
    'Journée entière',
  );

  return (
    <div className="agenda-day-group">
      <div className="agenda-day-badge">
        <span className="agenda-day-badge-weekday">{weekday}</span>
        <span className="agenda-day-badge-num">{day}</span>
        <span className="agenda-day-badge-month">{month}</span>
      </div>
      <div className="agenda-day-events">
        {group.events.map((event) => (
          <div key={event.id} className="agenda-event-item">
            <div className="agenda-event-item-time">
              <IconClock width={20} height={20} />
              <span>{formatAgendaEventTime(event, allDayLabel)}</span>
            </div>
            <p className="agenda-event-item-title">{event.title}</p>
          </div>
        ))}
      </div>
    </div>
  );
}
