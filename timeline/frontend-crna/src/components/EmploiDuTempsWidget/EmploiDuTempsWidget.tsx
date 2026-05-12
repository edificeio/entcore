import { Button, Grid, Heading, TextSkeleton } from '@edifice.io/react';
import { IconExternalLink } from '@edifice.io/react/icons';
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
  isLoading?: boolean;
  onSeeMore?: () => void;
}

export function EmploiDuTempsWidget({
  date,
  entries = [],
  currentTimeIndex = 0,
  isLoading = false,
  onSeeMore = () => window.open('/edt', '_self'),
}: EmploiDuTempsWidgetProps) {
  const { t } = useTranslation();

  const renderBody = () => {
    if (isLoading) {
      return (
        <div className="d-flex flex-column gap-12 p-8">
          <TextSkeleton size="md" />
          <TextSkeleton size="lg" />
          <TextSkeleton size="md" />
        </div>
      );
    }
    if (entries.length === 0) {
      return (
        <p className="edt-empty">
          {t('homepage.widget.edt.empty', "Aucun cours aujourd'hui")}
        </p>
      );
    }
    return (
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
            <div key={entry.id} className={`edt-block ${entry.color ?? 'grey'}`}>
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
    );
  };

  return (
    <div className="edt-widget">
      <Grid className="align-items-center edt-header">
        <Grid.Col sm="2" lg="6">
          <Heading level="h3" headingStyle="h5" className="edt-date">
            {date ?? t('homepage.widget.edt.date', 'Lundi 19 janvier')}
          </Heading>
        </Grid.Col>
        <Grid.Col sm="2" lg="6" className="d-flex justify-content-end">
          <Button
            variant="ghost"
            size="sm"
            className="edt-see-more"
            rightIcon={<IconExternalLink />}
            onClick={onSeeMore}
          >
            {t('homepage.widget.see.more', 'Voir plus')}
          </Button>
        </Grid.Col>
      </Grid>

      {renderBody()}
    </div>
  );
}
