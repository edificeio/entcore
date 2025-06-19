import { Application } from '~/models/application';
import { ApplicationIcon } from './ApplicationIcon';
import { IconOptions } from '@edifice.io/react/icons';
import clsx from 'clsx';
import { Dropdown, IconButton, IconButtonProps } from '@edifice.io/react';
import { RefAttributes, useState } from 'react';
import { ApplicationMenu } from './ApplicationMenu';
import { useUserPreferencesStore } from '~/store/userPreferencesStore';
import { useUpdateUserPreferences } from '~/services/queries/preferences';

export function ApplicationWrapper({ data }: { data: Application }) {
  const [dropdownActive, setDropdownActive] = useState(false);
  const [hover, setHover] = useState(false);
  const classApplicationCard = clsx(
    'rounded application-card position-relative py-8 px-4',
    (dropdownActive || hover) && 'active border border-secondary bg-gray-200',
  );
  const { bookmarks, toggleBookmark, applications, isHydrated } =
    useUserPreferencesStore();
  const isFavorite = isHydrated
    ? bookmarks.includes(data.name)
    : data.isFavorite;
  const updatePreferences = useUpdateUserPreferences();

  const handleToggleFavorite = () => {
    toggleBookmark(data.name);

    updatePreferences.mutate({
      bookmarks: useUserPreferencesStore.getState().bookmarks,
      applications,
    });
  };

  return (
    <a
      tabIndex={0}
      data-id={data.name}
      href={data.address}
      rel={data.isExternal ? 'noopener noreferrer' : undefined}
      target={data.isExternal ? '_blank' : undefined}
      className={classApplicationCard}
      style={{ width: 107, height: 127, textAlign: 'center' }}
      onMouseEnter={() => setHover(true)}
      onMouseLeave={() => setHover(false)}
    >
      <ApplicationIcon data={{ ...data, isFavorite }} isFavorite={isFavorite} />

      <h1 className="small text-gray-900 ellipsis-3 application-title">
        {data.appName}
      </h1>
      <div
        className="dropdown-wrapper"
        style={{ position: 'absolute', top: 0, right: 0 }}
      >
        <Dropdown
          onToggle={(active) => setDropdownActive(active)}
          placement="right-start"
          data-id="menu-dropdown"
        >
          {(
            triggerProps: JSX.IntrinsicAttributes &
              Omit<IconButtonProps, 'ref'> &
              RefAttributes<HTMLButtonElement>,
          ) => (
            <>
              <IconButton
                {...triggerProps}
                data-id="btn-application-menu"
                tabIndex={0}
                type="button"
                aria-label="label"
                color="secondary"
                variant="ghost"
                className="bg-secondary-200 border border-white text-secondary"
                icon={<IconOptions />}
              />
              <ApplicationMenu
                data={{ ...data, isFavorite }}
                onToggleFavorite={handleToggleFavorite}
              />
            </>
          )}
        </Dropdown>
      </div>
    </a>
  );
}
