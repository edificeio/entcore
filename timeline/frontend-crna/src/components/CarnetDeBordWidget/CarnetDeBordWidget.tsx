import { useEffect, useMemo, useState } from 'react';
import { Avatar, ButtonBeta, Modal } from '@edifice.io/react';
import {
  IconClockAlert,
  IconExternalLink,
  IconFiles,
  IconLsuCompetenceNumerique,
  IconNotes,
  IconUser,
} from '@edifice.io/react/icons';
import { useTranslation } from 'react-i18next';
import { computeContentTypes, useCarnetDeBord } from '~/hooks/useCarnetDeBord';
import type { ContentTitle, ContentType } from '~/models/carnetDeBord';
import { WidgetEmptyState } from '../ui/WidgetEmptyState';
import { WidgetHeader } from '../ui/WidgetHeader';
import { WidgetSkeleton } from '../ui/WidgetSkeleton';
import './CarnetDeBordWidget.css';

const CONTENT_ICONS: Record<ContentTitle, JSX.Element> = {
  lateness: <IconClockAlert width={20} height={20} />,
  absences: <IconUser width={20} height={20} />,
  grades:   <IconNotes width={20} height={20} />,
  diary:    <IconFiles width={20} height={20} />,
  skills:   <IconLsuCompetenceNumerique width={20} height={20} />,
};

const CONTENT_LABELS: Record<ContentTitle, string> = {
  lateness: 'Retards',
  absences: 'Absences',
  grades:   'Notes',
  diary:    'Cahier de textes',
  skills:   'Compétences',
};

const CONTENT_EMPTY_LABELS: Record<ContentTitle, string> = {
  lateness: 'Pas de retard disponible',
  absences: "Pas d'absence disponible",
  grades:   'Pas de note disponible',
  diary:    'Pas de nouveau devoir disponible',
  skills:   'Pas d\'évaluation disponible',
};

interface CarnetDeBordWidgetProps {
  onError?: (message: string) => void;
}

export function CarnetDeBordWidget({ onError }: CarnetDeBordWidgetProps) {
  const { t } = useTranslation();
  const { eleves, isLoading, isError } = useCarnetDeBord();

  useEffect(() => {
    if (isError) {
      onError?.(
        t(
          'homepage.widget.carnet-de-bord.error',
          'Impossible de récupérer les données Pronote. Veuillez réessayer plus tard.',
        ),
      );
    }
  }, [isError]);

  const [currentEleveIndex, setCurrentEleveIndex] = useState(0);
  const [showLightbox, setShowLightbox] = useState(false);
  const [currentContentType, setCurrentContentType] = useState<ContentType | null>(null);

  const contentTypes = useMemo(
    () => computeContentTypes(eleves[currentEleveIndex] ?? null),
    [eleves, currentEleveIndex]
  );

  const openLightbox = (ct: ContentType) => {
    setCurrentContentType(ct);
    setShowLightbox(true);
  };

  const closeLightbox = () => setShowLightbox(false);

  const hasContent = contentTypes.length > 0;

  const list = !hasContent ? (
    <WidgetEmptyState
      text={t('homepage.widget.carnet-de-bord.empty', 'Aucune donnée disponible')}
    />
  ) : (
    <ul className="carnet-de-bord-list">
      {contentTypes.map((ct) => {
        const itemCount = Array.isArray(ct.full) ? ct.full.length : 0;
        const isEmpty = ct.compact === false && itemCount === 0;

        const sublabel = isEmpty
          ? t(`homepage.widget.carnet-de-bord.${ct.title}.empty`, CONTENT_EMPTY_LABELS[ct.title])
          : ct.compact !== false
            ? ct.compact
            : null;

        const inner = (
          <>
            <div className="carnet-de-bord-entry-icon" data-type={ct.title}>
              {CONTENT_ICONS[ct.title]}
            </div>
            <div className="d-flex flex-column carnet-de-bord-entry-text">
              <strong className="carnet-de-bord-entry-label">
                {t(
                  `homepage.widget.carnet-de-bord.${ct.title}`,
                  CONTENT_LABELS[ct.title] ?? ct.title,
                )}
              </strong>
              {sublabel && (
                <span className="carnet-de-bord-entry-sublabel">{sublabel}</span>
              )}
            </div>
          </>
        );

        return (
          <li key={ct.title}>
            {itemCount > 0 ? (
              <button
                type="button"
                className="carnet-de-bord-entry"
                onClick={() => openLightbox(ct)}
              >
                {inner}
              </button>
            ) : (
              <div className={`carnet-de-bord-entry${isEmpty ? ' carnet-de-bord-entry--disabled' : ''}`}>
                {inner}
              </div>
            )}
          </li>
        );
      })}
    </ul>
  );

  const contentPanel = eleves.length > 1
    ? <div className="carnet-de-bord-list-card">{list}</div>
    : list;

  return (
    <div className="carnet-de-bord-widget">
      <WidgetHeader
        className="carnet-de-bord-header"
        title={t('homepage.widget.carnet-de-bord.title', 'Carnet de bord')}
        action={
          eleves[currentEleveIndex]?.address ? (
            <ButtonBeta
              color="default"
              variant="ghost"
              rightIcon={<IconExternalLink />}
              onClick={() => window.open(eleves[currentEleveIndex].address, '_blank')}
            >
              {t('homepage.widget.see.more', 'Voir plus')}
            </ButtonBeta>
          ) : undefined
        }
      />

      {isLoading ? (
        <WidgetSkeleton />
      ) : isError || eleves.length === 0 ? (
        <WidgetEmptyState
          text={t('homepage.widget.carnet-de-bord.empty', 'Aucune donnée disponible')}
        />
      ) : (
        <>
          {eleves.length > 1 && (
            <div className="gap-8 flex-wrap">
              {eleves.map((eleve, i) => (
                <ButtonBeta
                  key={i}
                  color={i === currentEleveIndex ? 'destructive' : 'default'}
                  variant={i === currentEleveIndex ? 'filled' : 'ghost'}
                  leftIcon={<Avatar alt={eleve.name} src={eleve.avatar} size="xs" variant="circle" />}
                  onClick={() => setCurrentEleveIndex(i)}
                >
                  {eleve.name || `Élève ${i + 1}`}
                </ButtonBeta>
              ))}
            </div>
          )}
          {contentPanel}
        </>
      )}

      {showLightbox && currentContentType && (
        <Modal
          id="carnet-de-bord-lightbox"
          isOpen={showLightbox}
          onModalClose={closeLightbox}
          size="md"
        >
          <Modal.Header onModalClose={closeLightbox}>
            {t(
              `homepage.widget.${currentContentType.lightboxTitle}`,
              CONTENT_LABELS[currentContentType.title] ?? currentContentType.title
            )}
          </Modal.Header>
          <Modal.Body>
            <ul className="carnet-de-bord-lightbox-list">
              {Array.isArray(currentContentType.full) &&
                currentContentType.full.map((item, i) => (
                  <li key={i} className="carnet-de-bord-lightbox-item">
                    <div className="d-flex align-items-center gap-8">
                      <span className="carnet-de-bord-entry-sublabel flex-fill">{item.value}</span>
                      {eleves[currentEleveIndex].address && item.pageUrl && (
                        <a
                          href={`${eleves[currentEleveIndex].address}?page=${item.pageUrl}`}
                          target="_blank"
                          rel="noreferrer"
                          className="carnet-de-bord-lightbox-link"
                          aria-label={t(
                            'homepage.widget.carnet-de-bord.open-pronote',
                            'Voir sur Pronote'
                          )}
                        >
                          <IconExternalLink width={16} height={16} />
                        </a>
                      )}
                    </div>
                    {item.subsections?.map((sub, j) => (
                      <div key={j} className="carnet-de-bord-lightbox-subsection">
                        <strong className="carnet-de-bord-entry-label">{sub.header}</strong>
                        {sub.content && (
                          <p className="carnet-de-bord-entry-sublabel">{sub.content}</p>
                        )}
                      </div>
                    ))}
                  </li>
                ))}
            </ul>
          </Modal.Body>
        </Modal>
      )}
    </div>
  );
}
