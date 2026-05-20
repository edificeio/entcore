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
import { useMediacentre, useMediacentrePins } from '~/hooks/useMediacentre';
import { ListWidget } from '../ui/ListWidget';

type MediacentreView = 'favorites' | 'pins';

export function MediacentreWidget({
  onSeeMore = () => window.open('/mediacentre', '_self'),
}: {
  onSeeMore?: () => void;
}) {
  const { t } = useTranslation();
  const { sessionQuery } = useEdificeClient();
  const schools = sessionQuery.data?.userDescription?.schools ?? [];

  const [view, setView] = useState<MediacentreView>('favorites');
  const [selectedSchoolIndex, setSelectedSchoolIndex] = useState(0);
  const selectedSchool = schools[selectedSchoolIndex];

  const { data: favorites = [], isLoading: isFavLoading } = useMediacentre();
  const { data: pins = [], isLoading: isPinsLoading } = useMediacentrePins(
    selectedSchool?.id,
  );

  const items = view === 'favorites' ? favorites : pins;
  const isLoading = view === 'favorites' ? isFavLoading : isPinsLoading;

  const filter = (
    <div className="d-flex flex-column gap-4">
      <div className="d-flex align-items-center gap-8 flex-wrap">
        <ButtonBeta
          color={view === 'favorites' ? 'destructive' : 'default'}
          variant={view === 'favorites' ? 'filled' : 'ghost'}
          onClick={() => setView('favorites')}
        >
          {t('homepage.widget.mediacentre.favorites', 'Mes favoris')}
        </ButtonBeta>
        <ButtonBeta
          color={view === 'pins' ? 'destructive' : 'default'}
          variant={view === 'pins' ? 'filled' : 'ghost'}
          onClick={() => setView('pins')}
        >
          {t('homepage.widget.mediacentre.pins', 'Ressources épinglées')}
        </ButtonBeta>
      </div>
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
                  aria-label={selectedSchool?.name ?? t('homepage.widget.mediacentre.select-school', 'Choisir un établissement')}
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
      title={t('homepage.widget.mediacentre.title', 'Médiacentre')}
      items={items}
      isLoading={isLoading}
      onSeeMore={onSeeMore}
      filter={filter}
    />
  );
}
