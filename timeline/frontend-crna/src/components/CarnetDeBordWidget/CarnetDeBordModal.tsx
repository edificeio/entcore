import { useEffect, useState } from 'react';
import type { JSX, RefAttributes } from 'react';
import { ButtonBeta, Dropdown, Modal, useBreakpoint } from '@edifice.io/react';
import type { IconButtonProps } from '@edifice.io/react';
import { IconArrowDown, IconExternalLink } from '@edifice.io/react/icons';
import { useTranslation } from 'react-i18next';
import type { ContentItem, ContentTitle, ContentType } from '~/models/carnetDeBord';
import { CategoryRow } from './CategoryRow';
import {
  CONTENT_COLORS,
  CONTENT_EMPTY_LABELS,
  CONTENT_ICONS,
  CONTENT_LABELS,
  RAIL_LABELS,
} from './categoryConfig';
import emptyScreenDesert from './assets/empty-screen-desert.png';

interface CarnetDeBordModalProps {
  isOpen: boolean;
  onClose: () => void;
  studentName: string;
  contentTypes: ContentType[];
  initialTab: ContentTitle;
  address: string;
}

function RetardsAbsencesRow({ item }: { item: ContentItem }) {
  const typeLabel = item.kind === 'absence' ? 'Absence' : 'Retard';
  return (
    <li className="carnet-de-bord-row">
      <span className="carnet-de-bord-row-date">{item.value}</span>
      <strong className="carnet-de-bord-row-type">{typeLabel}</strong>
      {item.motif && <span className="carnet-de-bord-row-motif">{item.motif}</span>}
    </li>
  );
}

function NoteRow({ item }: { item: ContentItem }) {
  const match = item.value.match(/^(.+) en (.+) le (.+)$/);
  const [grade, subject, date] = match
    ? [match[1], match[2], match[3]]
    : [item.value, '', ''];
  return (
    <li className="carnet-de-bord-row carnet-de-bord-row--grade">
      <div className="d-flex flex-column">
        {date && <span className="carnet-de-bord-row-date">{date}</span>}
        <strong className="carnet-de-bord-row-type">{subject}</strong>
      </div>
      <span className="carnet-de-bord-row-value">{grade}</span>
    </li>
  );
}

interface DiaryDayRowProps {
  subject: string;
  date?: number;
  content: string | null;
}

function DiaryDayRow({ subject, date, content }: DiaryDayRowProps) {
  const fullDate = date
    ? new Intl.DateTimeFormat('fr-FR', {
        weekday: 'long',
        day: 'numeric',
        month: 'long',
        year: 'numeric',
      }).format(date)
    : '';
  const day = date
    ? new Intl.DateTimeFormat('fr-FR', { day: '2-digit' }).format(date)
    : '';
  const month = date
    ? new Intl.DateTimeFormat('fr-FR', { month: 'long' }).format(date)
    : '';

  return (
    <li className="carnet-de-bord-row carnet-de-bord-row--cdt">
      <div className="d-flex flex-column flex-fill">
        {fullDate && <span className="carnet-de-bord-row-date">Pour {fullDate}</span>}
        <strong className="carnet-de-bord-row-type">{subject}</strong>
        <span className="carnet-de-bord-row-motif carnet-de-bord-row-motif--italic">
          {content || 'Pas de description'}
        </span>
      </div>
      {day && (
        <div className="carnet-de-bord-row-daybadge">
          <span className="carnet-de-bord-row-daybadge-num">{day}</span>
          <span className="carnet-de-bord-row-daybadge-month">{month}</span>
        </div>
      )}
    </li>
  );
}

function SkillRow({ item }: { item: ContentItem }) {
  const withSubject = item.value.match(/^(.+) le (.+) en (.+)$/);
  const withoutSubject = item.value.match(/^(.+) le (.+)$/);
  const [status, date, subject] = withSubject
    ? [withSubject[1], withSubject[2], withSubject[3]]
    : withoutSubject
      ? [withoutSubject[1], withoutSubject[2], '']
      : [item.value, '', ''];

  return (
    <li className="carnet-de-bord-row carnet-de-bord-row--cdt">
      <div className="d-flex flex-column flex-fill">
        {date && <span className="carnet-de-bord-row-date">{date}</span>}
        {subject && <strong className="carnet-de-bord-row-type">{subject}</strong>}
        {item.subsections?.map(
          (sub, j) =>
            sub.content && (
              <span key={j} className="carnet-de-bord-row-motif">
                {sub.header} : {sub.content}
              </span>
            ),
        )}
      </div>
      <span className="carnet-de-bord-row-value">{status}</span>
    </li>
  );
}

function renderRow(type: ContentTitle, item: ContentItem, index: number) {
  switch (type) {
    case 'grades':
      return <NoteRow key={index} item={item} />;
    case 'skills':
      return <SkillRow key={index} item={item} />;
    default:
      return <RetardsAbsencesRow key={index} item={item} />;
  }
}

export function CarnetDeBordModal({
  isOpen,
  onClose,
  studentName,
  contentTypes,
  initialTab,
  address,
}: CarnetDeBordModalProps) {
  const { t } = useTranslation('timeline');
  const { md } = useBreakpoint();
  const [activeTab, setActiveTab] = useState<ContentTitle>(initialTab);

  useEffect(() => {
    if (isOpen) setActiveTab(initialTab);
  }, [isOpen, initialTab]);

  if (!isOpen) return null;

  const activeContentType = contentTypes.find((ct) => ct.title === activeTab);
  const items = Array.isArray(activeContentType?.full) ? activeContentType.full : [];

  const diaryRows =
    activeTab === 'diary'
      ? items.flatMap((item) => {
          const subject = item.value.replace(/^Nouveau devoir\s+/i, '');
          return (item.subsections ?? []).map((sub) => ({
            subject,
            date: sub.date,
            content: sub.content,
          }));
        })
      : [];

  const hasContent = activeTab === 'diary' ? diaryRows.length > 0 : items.length > 0;

  const pronoteLabel = t(
    'homepage.crna.widget.carnet-de-bord.open-pronote',
    'Voir sur Pronote',
  );

  const railLabel = (title: ContentTitle) =>
    t(`homepage.crna.widget.carnet-de-bord.${title}.rail`, RAIL_LABELS[title]);

  const categorySelector = md ? (
    <div className="carnet-de-bord-rail">
      {contentTypes.map((ct) => (
        <CategoryRow
          key={ct.title}
          variant="rail"
          title={ct.title}
          label={railLabel(ct.title)}
          active={ct.title === activeTab}
          onClick={() => setActiveTab(ct.title)}
        />
      ))}
    </div>
  ) : (
    <Dropdown block>
      {(
        triggerProps: JSX.IntrinsicAttributes &
          Omit<IconButtonProps, 'ref'> &
          RefAttributes<HTMLButtonElement>,
      ) => (
        <>
          <button
            type="button"
            {...triggerProps}
            className="carnet-de-bord-rail-select"
          >
            <div
              className="carnet-de-bord-entry-icon"
              data-color={CONTENT_COLORS[activeTab]}
            >
              {CONTENT_ICONS[activeTab]}
            </div>
            <span className="carnet-de-bord-rail-select-label">
              {railLabel(activeTab)}
            </span>
            <IconArrowDown width={16} height={16} />
          </button>
          <Dropdown.Menu block>
            {contentTypes.map((ct) => (
              <Dropdown.RadioItem
                key={ct.title}
                value={ct.title}
                model={activeTab}
                onChange={() => setActiveTab(ct.title)}
              >
                {railLabel(ct.title)}
              </Dropdown.RadioItem>
            ))}
          </Dropdown.Menu>
        </>
      )}
    </Dropdown>
  );

  const pronoteButton = address ? (
    <ButtonBeta
      variant="ghost"
      color="default"
      rightIcon={<IconExternalLink width={14} height={14} />}
      className="carnet-de-bord-pronote-btn"
      onClick={() => window.open(address, '_blank')}
    >
      {pronoteLabel}
    </ButtonBeta>
  ) : null;

  return (
    <Modal id="carnet-de-bord-modal" isOpen={isOpen} onModalClose={onClose} size="lg" scrollable>
      <Modal.Header onModalClose={onClose}>
        <span>
          {t(
            'homepage.crna.widget.carnet-de-bord.modal-title-prefix',
            'Carnet de bord de',
          )}{' '}
          {studentName}
        </span>
      </Modal.Header>
      <Modal.Body>
        <div
          className={`carnet-de-bord-modal-body${md ? '' : ' carnet-de-bord-modal-body--mobile'}`}
        >
          {categorySelector}
          <div className="carnet-de-bord-modal-content">
            <h3 className="carnet-de-bord-modal-content-title">
              {t(
                `homepage.crna.widget.carnet-de-bord.${activeTab}`,
                CONTENT_LABELS[activeTab],
              )}
            </h3>
            {!hasContent ? (
              <div className="carnet-de-bord-modal-empty">
                <img src={emptyScreenDesert} alt="" width={163} height={125} />
                <p>
                  {t(
                    `homepage.crna.widget.carnet-de-bord.${activeTab}.empty`,
                    CONTENT_EMPTY_LABELS[activeTab],
                  )}
                </p>
                {pronoteButton}
              </div>
            ) : (
              <>
                <ul className="carnet-de-bord-row-list">
                  {activeTab === 'diary'
                    ? diaryRows.map((row, i) => <DiaryDayRow key={i} {...row} />)
                    : items.map((item, i) => renderRow(activeTab, item, i))}
                </ul>
                {pronoteButton}
              </>
            )}
          </div>
        </div>
      </Modal.Body>
    </Modal>
  );
}
