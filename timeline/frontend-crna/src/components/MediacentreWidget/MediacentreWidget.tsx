import { useState } from 'react';
import {
  Dropdown,
  Flex,
  IconButton,
  LinkPill,
  SegmentedControl,
  useEdificeClient,
} from '@edifice.io/react';
import type { IconButtonProps } from '@edifice.io/react';
import { HomeCard } from '@edifice.io/react/homepage';
import { IconExternalLink, IconFilter } from '@edifice.io/react/icons';
import type { JSX, RefAttributes } from 'react';
import { useTranslation } from 'react-i18next';
import {
  useMediacentre,
  useMediacentreHasUniversalis,
  useMediacentreOrientation,
  useMediacentrePins,
} from '~/hooks/useMediacentre';
import { WidgetSkeleton } from '../ui/WidgetSkeleton';
import { MediacentreStateMessage } from './MediacentreStateMessage';
import { UniversalisSearch } from './UniversalisSearch';
import './MediacentreWidget.css';

type MediacentreView = 'favorites' | 'pins' | 'orientation';

export function MediacentreWidget({
  onSeeMore = () => window.open('/mediacentre', '_self'),
}: {
  onSeeMore?: () => void;
}) {
  const { t } = useTranslation('timeline');
  const { sessionQuery } = useEdificeClient();
  const schools = sessionQuery.data?.userDescription?.schools ?? [];

  const [view, setView] = useState<MediacentreView>('favorites');
  const [selectedSchoolIndex, setSelectedSchoolIndex] = useState(0);
  const selectedSchool = schools[selectedSchoolIndex];

  const {
    data: favorites = [],
    isLoading: isFavLoading,
    isError: isFavError,
  } = useMediacentre();
  const {
    data: pins = [],
    isLoading: isPinsLoading,
    isError: isPinsError,
  } = useMediacentrePins(selectedSchool?.id);
  const {
    data: orientation = [],
    isLoading: isOrientationLoading,
    isError: isOrientationError,
  } = useMediacentreOrientation(view === 'orientation');

  const hasUniversalis = useMediacentreHasUniversalis();

  const viewStates = {
    favorites: {
      items: favorites,
      isLoading: isFavLoading,
      isError: isFavError,
      emptyText: t(
        'homepage.crna.widget.mediacentre.empty',
        'Vos ressources favorites seront affichées ici dès que vous les aurez sélectionnées. Allez dans le service Médiacentre (ou cliquez sur « voir plus ») et sélectionnez les ressources que vous souhaitez mettre en favoris.',
      ),
    },
    pins: {
      items: pins,
      isLoading: isPinsLoading,
      isError: isPinsError,
      emptyText: t(
        'homepage.crna.widget.mediacentre.empty-pins',
        'Aucune ressource épinglée',
      ),
    },
    orientation: {
      items: orientation,
      isLoading: isOrientationLoading,
      isError: isOrientationError,
      emptyText: t(
        'homepage.crna.widget.mediacentre.empty-orientation',
        "Aucune ressource d'orientation",
      ),
    },
  };
  const { items, isLoading, isError, emptyText } = viewStates[view];

  const filter = (
    <div className="d-flex flex-column gap-4">
      <SegmentedControl
        aria-label={t(
          'homepage.crna.widget.mediacentre.views',
          'Ressources du Médiacentre',
        )}
        options={[
          {
            label: t(
              'homepage.crna.widget.mediacentre.favorites',
              'Mes favoris',
            ),
            value: 'favorites',
          },
          {
            label: t(
              'homepage.crna.widget.mediacentre.pins',
              'Ressources épinglées',
            ),
            value: 'pins',
          },
          {
            label: t(
              'homepage.crna.widget.mediacentre.orientation',
              'Orientation',
            ),
            value: 'orientation',
          },
        ]}
        value={view}
        onChange={(value) => setView(value as MediacentreView)}
        className="mediacentre-widget__views"
      />
      {view === 'pins' && schools.length > 1 && (
        <div className="d-flex justify-content-end">
          <Dropdown>
            {(
              triggerProps: JSX.IntrinsicAttributes &
                Omit<IconButtonProps, 'ref'> &
                RefAttributes<HTMLButtonElement>,
            ) => (
              <>
                <IconButton
                  {...triggerProps}
                  type="button"
                  aria-label={
                    selectedSchool?.name ??
                    t(
                      'homepage.crna.widget.mediacentre.select-school',
                      'Choisir un établissement',
                    )
                  }
                  color="tertiary"
                  variant="ghost"
                  icon={<IconFilter />}
                  data-testid="mediacentre-school-select-button"
                />
                <Dropdown.Menu>
                  {schools.map((school, i) => (
                    <Dropdown.RadioItem
                      key={school.id}
                      value={school.id}
                      model={selectedSchool?.id ?? ''}
                      onChange={() => setSelectedSchoolIndex(i)}
                    >
                      {school.name}
                    </Dropdown.RadioItem>
                  ))}
                </Dropdown.Menu>
              </>
            )}
          </Dropdown>
        </div>
      )}
    </div>
  );

  const renderContent = () => {
    if (isLoading) return <WidgetSkeleton />;
    if (isError) {
      return (
        <MediacentreStateMessage
          variant="error"
          text={t(
            'homepage.crna.widget.mediacentre.error',
            "Impossible d'établir une connexion avec Médiacentre. Si le problème persiste, ouvrez une demande d'aide sur le module Assistance ENT.",
          )}
        />
      );
    }
    if (items.length === 0) {
      return <MediacentreStateMessage variant="empty" text={emptyText} />;
    }
    return (
      <Flex direction="column" gap="8">
        {items.map((item) => (
          <LinkPill
            key={item.id}
            href={item.href}
            label={item.label}
            subtitle={item.sublabel}
            data-testid="mediacentre-resource-link"
            // Some resources have no image: fall back to LinkPill's default icon.
            {...(item.imageUrl
              ? {
                  illustrationType: 'img' as const,
                  illustration: <img src={item.imageUrl} alt="" />,
                }
              : {})}
          />
        ))}
      </Flex>
    );
  };

  return (
    <HomeCard variant="user">
      <HomeCard.Header
        title={t('homepage.crna.widget.mediacentre.title', 'Médiacentre')}
        actionLabel={t('homepage.crna.widget.see.more', 'Voir plus')}
        actionRightIcon={<IconExternalLink />}
        onActionClick={onSeeMore}
        actionProps={{ 'data-testid': 'mediacentre-see-more-button' }}
      />
      <HomeCard.Content>
        {filter}
        {renderContent()}
        {!isError && hasUniversalis && (
          <UniversalisSearch uai={selectedSchool?.UAI} />
        )}
      </HomeCard.Content>
    </HomeCard>
  );
}
