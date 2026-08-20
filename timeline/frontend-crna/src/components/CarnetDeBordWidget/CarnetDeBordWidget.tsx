import { useEffect, useMemo, useState } from 'react';
import { IconExternalLink } from '@edifice.io/react/icons';
import { HomeCard } from '@edifice.io/react/homepage';
import { useTranslation } from 'react-i18next';
import { computeContentTypes, useCarnetDeBord } from '~/hooks/useCarnetDeBord';
import type { ContentTitle } from '~/models/carnetDeBord';
import { WidgetEmptyState } from '../ui/WidgetEmptyState';
import { WidgetSkeleton } from '../ui/WidgetSkeleton';
import { CarnetDeBordErrorState } from './CarnetDeBordErrorState';
import { CarnetDeBordModal } from './CarnetDeBordModal';
import { CategoryRow } from './CategoryRow';
import { KidTabs } from './KidTabs';
import { CONTENT_EMPTY_LABELS, CONTENT_LABELS } from './categoryConfig';
import './CarnetDeBordWidget.css';

interface CarnetDeBordWidgetProps {
  onError?: (message: string) => void;
}

export function CarnetDeBordWidget({ onError }: CarnetDeBordWidgetProps) {
  const { t } = useTranslation('timeline');
  const { eleves, isLoading, isError } = useCarnetDeBord();

  useEffect(() => {
    if (isError) {
      onError?.(
        t(
          'homepage.crna.widget.carnet-de-bord.error',
          'Impossible de récupérer les données Pronote. Veuillez réessayer plus tard.',
        ),
      );
    }
  }, [isError]);

  const [currentEleveIndex, setCurrentEleveIndex] = useState(0);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [activeTab, setActiveTab] = useState<ContentTitle>('retards-absences');

  const contentTypes = useMemo(
    () => computeContentTypes(eleves[currentEleveIndex] ?? null),
    [eleves, currentEleveIndex],
  );

  const openModal = (title: ContentTitle) => {
    setActiveTab(title);
    setIsModalOpen(true);
  };

  const address = eleves[currentEleveIndex]?.address ?? '';
  const studentName = eleves[currentEleveIndex]?.name ?? '';

  return (
    <>
      <HomeCard variant="user">
        <HomeCard.Header
          title={t('homepage.crna.widget.carnet-de-bord.title', 'Carnet de bord')}
          actionLabel={address ? t('homepage.crna.widget.see.more', 'Voir plus') : undefined}
          onActionClick={address ? () => window.open(address, '_blank') : undefined}
          actionRightIcon={<IconExternalLink />}
        />
        <HomeCard.Content>
          {isLoading ? (
            <WidgetSkeleton />
          ) : isError ? (
            <CarnetDeBordErrorState />
          ) : eleves.length === 0 ? (
            <WidgetEmptyState
              text={t('homepage.crna.widget.carnet-de-bord.empty', 'Aucune donnée disponible')}
            />
          ) : (
            <div className="carnet-de-bord-content">
              <KidTabs
                eleves={eleves}
                currentEleveIndex={currentEleveIndex}
                onSelect={setCurrentEleveIndex}
              />
              <div
                className="carnet-de-bord-categories"
                data-has-tabs={eleves.length > 1}
              >
                {contentTypes.map((ct) => {
                  const itemCount = Array.isArray(ct.full) ? ct.full.length : 0;
                  const isEmpty = ct.compact === false && itemCount === 0;
                  const subtext = isEmpty
                    ? t(
                        `homepage.crna.widget.carnet-de-bord.${ct.title}.empty`,
                        CONTENT_EMPTY_LABELS[ct.title],
                      )
                    : ct.compact !== false
                      ? ct.compact
                      : null;

                  return (
                    <CategoryRow
                      key={ct.title}
                      title={ct.title}
                      label={t(
                        `homepage.crna.widget.carnet-de-bord.${ct.title}`,
                        CONTENT_LABELS[ct.title],
                      )}
                      subtext={subtext}
                      subtextEmpty={isEmpty}
                      disabled={itemCount === 0}
                      onClick={itemCount > 0 ? () => openModal(ct.title) : undefined}
                    />
                  );
                })}
              </div>
            </div>
          )}
        </HomeCard.Content>
      </HomeCard>

      <CarnetDeBordModal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        studentName={studentName}
        contentTypes={contentTypes}
        initialTab={activeTab}
        address={address}
      />
    </>
  );
}
