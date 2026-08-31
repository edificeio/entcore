import { useState } from 'react';
import {
  ButtonBeta,
  Dropdown,
  IconButton,
  useEdificeClient,
} from '@edifice.io/react';
import type { IconButtonProps } from '@edifice.io/react';
import { IconFilter } from '@edifice.io/react/icons';
import type { JSX, RefAttributes } from 'react';
import { useTranslation } from 'react-i18next';
import {
  useMediacentre,
  useMediacentreHasUniversalis,
  useMediacentrePins,
} from '~/hooks/useMediacentre';
import { ListWidget } from '../ui/ListWidget';
import { MediacentreStateMessage } from './MediacentreStateMessage';
import { UniversalisSearch } from './UniversalisSearch';
import './MediacentreWidget.css';

type MediacentreView = 'favorites' | 'pins';

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

  const hasUniversalis = useMediacentreHasUniversalis();

  const items = view === 'favorites' ? favorites : pins;
  const isLoading = view === 'favorites' ? isFavLoading : isPinsLoading;
  const isError = view === 'favorites' ? isFavError : isPinsError;

  const filter = (
    <div className="d-flex flex-column gap-4">
      {/* <div className="d-flex align-items-center gap-8 flex-wrap mb-8">
        <ButtonBeta
          color={view === 'favorites' ? 'destructive' : 'default'}
          variant={view === 'favorites' ? 'filled' : 'ghost'}
          onClick={() => setView('favorites')}
        >
          {t('homepage.crna.widget.mediacentre.favorites', 'Mes favoris')}
        </ButtonBeta>
        <ButtonBeta
          color={view === 'pins' ? 'destructive' : 'default'}
          variant={view === 'pins' ? 'filled' : 'ghost'}
          onClick={() => setView('pins')}
        >
          {t('homepage.crna.widget.mediacentre.pins', 'Ressources épinglées')}
        </ButtonBeta>
      </div> */}
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
                  aria-label={selectedSchool?.name ?? t('homepage.crna.widget.mediacentre.select-school', 'Choisir un établissement')}
                  color="tertiary"
                  variant="ghost"
                  icon={<IconFilter />}
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

  return (
    <ListWidget
      title={t('homepage.crna.widget.mediacentre.title', 'Médiacentre')}
      items={items}
      isLoading={isLoading}
      isError={isError}
      onSeeMore={onSeeMore}
      filter={filter}
      itemClassName="list-widget-item--highlight"
      emptyState={
        <MediacentreStateMessage
          variant="empty"
          text={
            view === 'favorites'
              ? t(
                  'homepage.crna.widget.mediacentre.empty',
                  'Aucune ressource en favoris',
                )
              : t(
                  'homepage.crna.widget.mediacentre.empty-pins',
                  'Aucune ressource épinglée',
                )
          }
        />
      }
      errorState={
        <MediacentreStateMessage
          variant="error"
          text={t(
            'homepage.crna.widget.mediacentre.error',
            "Impossible d'établir une connexion avec Médiacentre. Si le problème persiste, ouvrez une demande d'aide sur le module Assistance ENT.",
          )}
        />
      }
      footer={
        !isError && hasUniversalis ? (
          <UniversalisSearch uai={selectedSchool?.UAI} />
        ) : undefined
      }
    />
  );
}
