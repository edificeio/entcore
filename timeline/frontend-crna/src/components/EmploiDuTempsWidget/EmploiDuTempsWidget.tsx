import { ButtonBeta } from '@edifice.io/react';
import { IconExternalLink } from '@edifice.io/react/icons';
import { useTranslation } from 'react-i18next';
import type { WidgetBaseProps } from '../types';
import { WidgetHeader } from '../WidgetHeader';
import { WidgetSkeleton } from '../WidgetSkeleton';
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

export interface EmploiDuTempsWidgetProps extends WidgetBaseProps {
  date?: string;
  entries?: EmploiDuTempsEntry[];
  currentTimeIndex?: number;
}

export function EmploiDuTempsWidget({
  date,
  entries = [],
  currentTimeIndex = 0,
  isLoading = false,
  onSeeMore = () => window.open('/edt', '_self'),
}: EmploiDuTempsWidgetProps) {
  const { t } = useTranslation();

  return (
    <div className="edt-widget">
      <WidgetHeader
        className="edt-header"
        titleClassName="edt-date"
        title={date ?? t('homepage.widget.edt.date', 'Lundi 19 janvier')}
        action={
          <ButtonBeta
            color="default"
            variant="ghost"
            rightIcon={<IconExternalLink />}
            onClick={onSeeMore}
            data-testid="edt-see-more-btn"
          >
            {t('homepage.widget.see.more', 'Voir plus')}
          </ButtonBeta>
        }
      />

      {isLoading ? (
        <WidgetSkeleton />
      ) : entries.length === 0 ? (
        <p className="align-items-center gap-8 d-flex flex-column widget-empty">
          {t('homepage.widget.edt.empty', "Aucun cours aujourd'hui")}
        </p>
      ) : (
        <div className="edt-body">
          <div className="edt-timeline">
            <div className="edt-timeline-line" />
            {entries.map((entry, idx) => (
              <div
                key={entry.id}
                className={`edt-time-badge ${idx === currentTimeIndex ? 'current' : 'past'}`}
              >
                {entry.startTime}
              </div>
            ))}
          </div>
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
                    <div className="d-flex gap-8 edt-block-details">
                      {entry.room && <span>{entry.room}</span>}
                      {entry.teacher && <span>{entry.teacher}</span>}
                    </div>
                  )}
                </div>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
