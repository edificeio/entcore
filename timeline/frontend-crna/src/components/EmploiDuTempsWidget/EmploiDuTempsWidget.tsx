import { useTranslation } from 'react-i18next';
import './EmploiDuTempsWidget.css';

export type EmploiDuTempsColor = 'green' | 'pink' | 'orange' | 'blue' | 'grey';

export interface EmploiDuTempsEntry {
  id: string;
  subject: string;
  room?: string;
  teacher?: string;
  startTime: string;
  color?: EmploiDuTempsColor;
}

export interface EmploiDuTempsWidgetProps {
  date?: string;
  entries?: EmploiDuTempsEntry[];
  currentTimeIndex?: number;
  onSeeMore?: () => void;
}

export function EmploiDuTempsWidget({
  date,
  entries = [],
  currentTimeIndex = 0,
  onSeeMore = () => window.open('/edt', '_self'),
}: EmploiDuTempsWidgetProps) {
  const { t } = useTranslation();

  return (
    <div className="edt-widget">
      <div className="edt-header">
        <p className="edt-date">
          {date ?? t('homepage.widget.edt.date', 'Lundi 19 janvier')}
        </p>
        <button type="button" className="edt-see-more" onClick={onSeeMore}>
          {t('homepage.widget.see.more', 'Voir plus')}
          <svg width="20" height="20" viewBox="0 0 20 20" fill="none" aria-hidden="true">
            <path
              d="M11 3h6v6M16.5 3.5L9 11M8 5H4a1 1 0 0 0-1 1v10a1 1 0 0 0 1 1h10a1 1 0 0 0 1-1v-4"
              stroke="currentColor"
              strokeWidth="1.5"
              strokeLinecap="round"
              strokeLinejoin="round"
            />
          </svg>
        </button>
      </div>

      <div className="edt-body">
        {/* Timeline */}
        <div className="edt-timeline">
          <div className="edt-timeline-line" />
          {entries.map((entry, idx) => (
            <div
              key={entry.id}
              className={`edt-time-badge ${idx < currentTimeIndex ? 'past' : idx === currentTimeIndex ? 'current' : 'past'}`}
            >
              {entry.startTime}
            </div>
          ))}
        </div>

        {/* Course blocks */}
        <div className="edt-blocks">
          {entries.map((entry) => (
            <div
              key={entry.id}
              className={`edt-block ${entry.color ?? 'grey'}`}
            >
              <div className="edt-block-bar" />
              <div className="edt-block-content">
                <p className="edt-block-subject">{entry.subject}</p>
                {(entry.room || entry.teacher) && (
                  <div className="edt-block-details">
                    {entry.room && <span>{entry.room}</span>}
                    {entry.teacher && <span>{entry.teacher}</span>}
                  </div>
                )}
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
