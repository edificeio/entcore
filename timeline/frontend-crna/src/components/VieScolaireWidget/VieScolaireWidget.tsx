import { Avatar, Button, Tabs } from '@edifice.io/react';
import type { TabsItemProps } from '@edifice.io/react';
import {
  IconClockAlert,
  IconExternalLink,
  IconFiles,
  IconLsuCompetenceNumerique,
  IconNotes,
  IconUser,
} from '@edifice.io/react/icons';
import { useTranslation } from 'react-i18next';
import type { WidgetBaseProps } from '../types';
import { WidgetHeader } from '../WidgetHeader';
import { WidgetSkeleton } from '../WidgetSkeleton';
import './VieScolaireWidget.css';

export type VieScolaireEntryType =
  | 'retard'
  | 'absence'
  | 'note'
  | 'cahier'
  | 'competence';

export interface VieScolaireEntry {
  id: string;
  type: VieScolaireEntryType;
  label: string;
  sublabel: string;
}

export interface VieScolaireChild {
  id: string;
  name: string;
  avatar?: string;
  entries: VieScolaireEntry[];
}

export interface VieScolaireWidgetProps extends WidgetBaseProps {
  kids?: VieScolaireChild[];
}

const ENTRY_ICONS: Record<VieScolaireEntryType, JSX.Element> = {
  retard:     <IconClockAlert width={20} height={20} />,
  absence:    <IconUser width={20} height={20} />,
  note:       <IconNotes width={20} height={20} />,
  cahier:     <IconFiles width={20} height={20} />,
  competence: <IconLsuCompetenceNumerique width={20} height={20} />,
};

export function VieScolaireWidget({
  kids = [],
  isLoading = false,
  onSeeMore = () => window.open('/viescolaire', '_self'),
}: VieScolaireWidgetProps) {
  const { t } = useTranslation();

  const tabItems: TabsItemProps[] = kids.map((kid) => ({
    id: kid.id,
    label: kid.name,
    icon: (
      <Avatar
        alt={kid.name}
        src={kid.avatar}
        size="xs"
        variant="circle"
      />
    ),
    content: (
      <ul className="vie-scolaire-list">
        {kid.entries.map((entry) => (
          <li key={entry.id}>
            <div className="d-flex align-items-center gap-8 vie-scolaire-entry">
              <div className="vie-scolaire-entry-icon">
                {ENTRY_ICONS[entry.type]}
              </div>
              <div className="d-flex flex-column vie-scolaire-entry-text">
                <span className="vie-scolaire-entry-label">{entry.label}</span>
                <span className="vie-scolaire-entry-sublabel">{entry.sublabel}</span>
              </div>
            </div>
          </li>
        ))}
      </ul>
    ),
  }));

  return (
    <div className="vie-scolaire-widget">
      <WidgetHeader
        className="vie-scolaire-header"
        title={t('homepage.widget.vie-scolaire.title', 'Vie scolaire')}
        action={
          <Button
            variant="ghost"
            size="sm"
            className="widget-action-link"
            rightIcon={<IconExternalLink />}
            onClick={onSeeMore}
          >
            {t('homepage.widget.see.more', 'Voir plus')}
          </Button>
        }
      />

      {isLoading ? (
        <WidgetSkeleton />
      ) : kids.length === 0 ? (
        <p className="widget-empty">
          {t('homepage.widget.vie-scolaire.empty', 'Aucune donnée disponible')}
        </p>
      ) : (
        <div className="vie-scolaire-content">
          <Tabs defaultId={kids[0]?.id} items={tabItems} />
        </div>
      )}
    </div>
  );
}
