import { useState } from 'react';
import { useTranslation } from 'react-i18next';
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

export interface VieScolaireWidgetProps {
  kids?: VieScolaireChild[];
  onSeeMore?: () => void;
}

const ENTRY_ICONS: Record<VieScolaireEntryType, JSX.Element> = {
  retard: (
    <svg width="20" height="20" viewBox="0 0 20 20" fill="none" aria-hidden="true">
      <circle cx="10" cy="10" r="8" stroke="#4a4a4a" strokeWidth="1.5" />
      <path d="M10 6v4l2.5 2.5" stroke="#4a4a4a" strokeWidth="1.5" strokeLinecap="round" />
      <path d="M3 3l14 14" stroke="#4a4a4a" strokeWidth="1.5" strokeLinecap="round" />
    </svg>
  ),
  absence: (
    <svg width="20" height="20" viewBox="0 0 20 20" fill="none" aria-hidden="true">
      <circle cx="10" cy="7" r="3.5" stroke="#4a4a4a" strokeWidth="1.5" />
      <path d="M3 17c0-3.866 3.134-7 7-7s7 3.134 7 7" stroke="#4a4a4a" strokeWidth="1.5" strokeLinecap="round" />
      <path d="M14 3l4 4M18 3l-4 4" stroke="#4a4a4a" strokeWidth="1.5" strokeLinecap="round" />
    </svg>
  ),
  note: (
    <svg width="20" height="20" viewBox="0 0 20 20" fill="none" aria-hidden="true">
      <rect x="4" y="2" width="12" height="16" rx="2" stroke="#4a4a4a" strokeWidth="1.5" />
      <path d="M7 7h6M7 10h6M7 13h4" stroke="#4a4a4a" strokeWidth="1.5" strokeLinecap="round" />
    </svg>
  ),
  cahier: (
    <svg width="20" height="20" viewBox="0 0 20 20" fill="none" aria-hidden="true">
      <path d="M4 4h12a1 1 0 0 1 1 1v10a1 1 0 0 1-1 1H4" stroke="#4a4a4a" strokeWidth="1.5" strokeLinecap="round" />
      <path d="M4 4a2 2 0 0 0-2 2v8a2 2 0 0 0 2 2" stroke="#4a4a4a" strokeWidth="1.5" />
      <path d="M8 8h5M8 11h5M8 14h3" stroke="#4a4a4a" strokeWidth="1.5" strokeLinecap="round" />
    </svg>
  ),
  competence: (
    <svg width="20" height="20" viewBox="0 0 20 20" fill="none" aria-hidden="true">
      <path d="M3 13l4-4 3 3 4-5 3 3" stroke="#4a4a4a" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  ),
};

export function VieScolaireWidget({
  kids = [],
  onSeeMore = () => window.open('/viescolaire', '_self'),
}: VieScolaireWidgetProps) {
  const { t } = useTranslation();
  const [activeKidId, setActiveKidId] = useState<string>(kids[0]?.id ?? '');

  const activeKid = kids.find((k) => k.id === activeKidId) ?? kids[0];

  return (
    <div className="vie-scolaire-widget">
      <div className="vie-scolaire-header">
        <p className="vie-scolaire-title">
          {t('homepage.widget.vie-scolaire.title', 'Vie scolaire')}
        </p>
        <button type="button" className="vie-scolaire-see-more" onClick={onSeeMore}>
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

      <div className="vie-scolaire-content">
        <div className="vie-scolaire-tabs">
          {kids.map((kid) => (
            <button
              type="button"
              key={kid.id}
              className={`vie-scolaire-tab${activeKidId === kid.id ? ' active' : ''}`}
              onClick={() => setActiveKidId(kid.id)}
            >
              {kid.avatar ? (
                <div className="vie-scolaire-tab-avatar">
                  <img src={kid.avatar} alt={kid.name} />
                </div>
              ) : (
                <div className="vie-scolaire-tab-avatar-placeholder">
                  {kid.name[0].toUpperCase()}
                </div>
              )}
              {kid.name}
            </button>
          ))}
          <button type="button" className="vie-scolaire-more-btn" aria-label="Plus d'options">
            <svg width="20" height="20" viewBox="0 0 20 20" fill="none" aria-hidden="true">
              <circle cx="10" cy="5" r="1.5" fill="currentColor" />
              <circle cx="10" cy="10" r="1.5" fill="currentColor" />
              <circle cx="10" cy="15" r="1.5" fill="currentColor" />
            </svg>
          </button>
        </div>

        <ul className="vie-scolaire-list">
          {activeKid?.entries.map((entry) => (
            <li key={entry.id} className="vie-scolaire-entry">
              <div className="vie-scolaire-entry-icon">
                {ENTRY_ICONS[entry.type]}
              </div>
              <div className="vie-scolaire-entry-text">
                <span className="vie-scolaire-entry-label">{entry.label}</span>
                <span className="vie-scolaire-entry-sublabel">{entry.sublabel}</span>
              </div>
            </li>
          ))}
        </ul>
      </div>
    </div>
  );
}
