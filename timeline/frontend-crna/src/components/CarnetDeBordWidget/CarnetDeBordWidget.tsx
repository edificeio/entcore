import { useEffect, useMemo, useState } from 'react';
import { Avatar, Badge, ButtonBeta, Modal } from '@edifice.io/react';
import { HomeCard } from '@edifice.io/react/homepage';
import {
  IconClockAlert,
  IconExternalLink,
  IconTeacher,
  IconNotes,
  IconUser,
} from '@edifice.io/react/icons';
import { IconCahierDeTexte } from '@edifice.io/react/icons/apps';
import { useTranslation } from 'react-i18next';
import { computeContentTypes, useCarnetDeBord } from '~/hooks/useCarnetDeBord';
import type { ContentItem, ContentTitle, ContentType } from '~/models/carnetDeBord';
import { WidgetEmptyState } from '../ui/WidgetEmptyState';
import { WidgetSkeleton } from '../ui/WidgetSkeleton';
import './CarnetDeBordWidget.css';

const CONTENT_ICONS: Record<ContentTitle, JSX.Element> = {
  lateness: <IconClockAlert width={20} height={20} />,
  absences: <IconUser width={20} height={20} />,
  grades:   <IconNotes width={20} height={20} />,
  diary:    <IconCahierDeTexte width={20} height={20} />,
  skills:   <IconTeacher width={20} height={20} />,
};

const CONTENT_LABELS: Record<ContentTitle, string> = {
  lateness: 'Retards',
  absences: 'Absences',
  grades:   'Notes',
  diary:    'Cahier de textes',
  skills:   'Compétences',
};

const LIGHTBOX_TITLES: Record<ContentTitle, string> = {
  lateness: 'Tous les retards non justifiés',
  absences: 'Toutes les absences non justifiées',
  grades:   'Toutes les notes',
  diary:    'Tous les devoirs',
  skills:   'Toutes les compétences acquises',
};

const CONTENT_EMPTY_LABELS: Record<ContentTitle, string> = {
  lateness: 'Pas de retard disponible',
  absences: "Pas d'absence disponible",
  grades:   'Pas de note disponible',
  diary:    'Pas de nouveau devoir disponible',
  skills:   "Pas d'évaluation disponible",
};

interface CarnetDeBordWidgetProps {
  onError?: (message: string) => void;
}

// ─── Lightbox item renderers ──────────────────────────────────────────────────

interface ItemRendererProps {
  item: ContentItem;
  address: string;
  pronoteLabel: string;
}

function PronoteButton({ href, label }: { href: string; label: string }) {
  return (
    <ButtonBeta
      variant="ghost"
      color="default"
      rightIcon={<IconExternalLink width={14} height={14} />}
      className="carnet-de-bord-pronote-btn"
      onClick={() => window.open(href, '_blank')}
    >
      {label}
    </ButtonBeta>
  );
}

function DefaultItem({ item, address, pronoteLabel }: ItemRendererProps) {
  return (
    <li className="carnet-de-bord-lightbox-item">
      <div className="d-flex align-items-center justify-content-between gap-8">
        <span className="carnet-de-bord-entry-label">{item.value}</span>
        {address && item.pageUrl && (
          <PronoteButton href={`${address}?page=${item.pageUrl}`} label={pronoteLabel} />
        )}
      </div>
    </li>
  );
}

function GradeItem({ item, address, pronoteLabel }: ItemRendererProps) {
  const match = item.value.match(/^(.+) en (.+) le (.+)$/);
  const [grade, subject, date] = match ? [match[1], match[2], match[3]] : [item.value, '', ''];

  return (
    <li className="carnet-de-bord-lightbox-item">
      <div className="d-flex align-items-center gap-8 flex-wrap">
        <Badge variant={{ type: 'chip' }} className="carnet-de-bord-grade-badge">
          {grade}
        </Badge>
        <span className="carnet-de-bord-entry-label flex-fill">{subject}</span>
        {date && <span className="carnet-de-bord-lightbox-value">{date}</span>}
        {address && item.pageUrl && (
          <PronoteButton href={`${address}?page=${item.pageUrl}`} label={pronoteLabel} />
        )}
      </div>
    </li>
  );
}

function DiaryItem({ item, address, pronoteLabel }: ItemRendererProps) {
  const subject = item.value.replace(/^Nouveau devoir\s+/i, '');

  return (
    <li className="carnet-de-bord-lightbox-item">
      <span className="carnet-de-bord-entry-label">{subject}</span>
      {item.subsections?.map((sub, j) => (
        <div key={j} className="carnet-de-bord-lightbox-subsection">
          <div className="d-flex align-items-center gap-8 flex-wrap">
            <Badge variant={{ type: 'chip' }} className="carnet-de-bord-date-badge">
              {sub.header}
            </Badge>
            {address && sub.pageUrl && (
              <PronoteButton href={`${address}?page=${sub.pageUrl}`} label={pronoteLabel} />
            )}
          </div>
          {sub.content && <p className="carnet-de-bord-lightbox-value">{sub.content}</p>}
        </div>
      ))}
    </li>
  );
}

function SkillItem({ item, address, pronoteLabel }: ItemRendererProps) {
  return (
    <li className="carnet-de-bord-lightbox-item">
      <div className="d-flex align-items-center justify-content-between gap-8">
        <span className="carnet-de-bord-entry-label flex-fill">{item.value}</span>
        {address && item.pageUrl && (
          <PronoteButton href={`${address}?page=${item.pageUrl}`} label={pronoteLabel} />
        )}
      </div>
      {item.subsections?.map((sub, j) => (
        <div key={j} className="carnet-de-bord-lightbox-subsection">
          <Badge variant={{ type: 'chip' }} className="carnet-de-bord-skill-label">
            {sub.header}
          </Badge>
          {sub.content && <p className="carnet-de-bord-lightbox-value">{sub.content}</p>}
        </div>
      ))}
    </li>
  );
}

function renderLightboxItem(
  type: ContentTitle,
  item: ContentItem,
  index: number,
  address: string,
  pronoteLabel: string,
) {
  const props: ItemRendererProps = { item, address, pronoteLabel };
  switch (type) {
    case 'grades': return <GradeItem key={index} {...props} />;
    case 'diary':  return <DiaryItem key={index} {...props} />;
    case 'skills': return <SkillItem key={index} {...props} />;
    default:       return <DefaultItem key={index} {...props} />;
  }
}

// ─── Widget ───────────────────────────────────────────────────────────────────

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
    [eleves, currentEleveIndex],
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

  const itemCount = Array.isArray(currentContentType?.full)
    ? currentContentType.full.length
    : 0;

  const pronoteLabel = t('homepage.widget.carnet-de-bord.open-pronote', 'Voir sur Pronote');
  const address = eleves[currentEleveIndex]?.address ?? '';

  return (
    <>
      <HomeCard variant="user">
        <HomeCard.Header
          title={t('homepage.widget.carnet-de-bord.title', 'Carnet de bord')}
          actionLabel={address ? t('homepage.widget.see.more', 'Voir plus') : undefined}
          onActionClick={address ? () => window.open(address, '_blank') : undefined}
          actionRightIcon={<IconExternalLink />}
        />
        <HomeCard.Content>
          {isLoading ? (
            <WidgetSkeleton />
          ) : isError || eleves.length === 0 ? (
            <WidgetEmptyState
              text={t('homepage.widget.carnet-de-bord.empty', 'Aucune donnée disponible')}
            />
          ) : (
            <>
              {eleves.length > 1 && (
                <div className="gap-8 flex-wrap mb-8">
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
        </HomeCard.Content>
      </HomeCard>

      {showLightbox && currentContentType && (
        <Modal
          id="carnet-de-bord-lightbox"
          isOpen={showLightbox}
          onModalClose={closeLightbox}
          size="md"
          scrollable
        >
          <Modal.Header onModalClose={closeLightbox}>
            <div className="d-flex align-items-center gap-8 flex-wrap">
              <div className="carnet-de-bord-entry-icon" data-type={currentContentType.title}>
                {CONTENT_ICONS[currentContentType.title]}
              </div>
              <span className="carnet-de-bord-lightbox-title">
                {t(
                  `homepage.widget.${currentContentType.lightboxTitle}`,
                  LIGHTBOX_TITLES[currentContentType.title] ?? currentContentType.title,
                )}
                {eleves.length > 1 && eleves[currentEleveIndex]?.name && (
                  <> {`de ${eleves[currentEleveIndex].name}`}</>
                )}
              </span>
              <Badge variant={{ type: 'notification', level: 'danger' }} className="carnet-de-bord-lightbox-count">
                {itemCount}
              </Badge>
            </div>
          </Modal.Header>
          <Modal.Body>
            <ul className="carnet-de-bord-lightbox-list">
              {Array.isArray(currentContentType.full) &&
                currentContentType.full.map((item, i) =>
                  renderLightboxItem(currentContentType.title, item, i, address, pronoteLabel),
                )}
            </ul>
          </Modal.Body>
        </Modal>
      )}
    </>
  );
}
