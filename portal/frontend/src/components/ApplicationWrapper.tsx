import { Application } from '~/models/application';
import { ApplicationIcon } from './ApplicationIcon';
import { IconOptions } from '@edifice.io/react/icons';
import clsx from 'clsx';
import { Dropdown, IconButton, IconButtonProps } from '@edifice.io/react';
import { RefAttributes, useRef, useState } from 'react';
import { ApplicationMenu } from './ApplicationMenu';
import { useUserPreferencesStore } from '~/store/userPreferencesStore';
import { useUpdateUserPreferences } from '~/services/queries/preferences';
import { useApplications } from '~/services';

function combineRefs<T>(
  ...refs: (React.Ref<T> | undefined)[]
): React.RefCallback<T> {
  return (node: T) => {
    for (const ref of refs) {
      if (!ref) continue;
      if (typeof ref === 'function') {
        ref(node);
      } else if (typeof ref === 'object' && 'current' in ref) {
        (ref as React.MutableRefObject<T | null>).current = node;
      }
    }
  };
}

export function ApplicationWrapper({ data }: { data: Application }) {
  const { applications: displayedApps } = useApplications();
  const buttonRef = useRef<HTMLButtonElement>(null);
  const [dropdownActive, setDropdownActive] = useState(false);
  const [hover, setHover] = useState(false);
  const classApplicationCard = clsx(
    'rounded application-card position-relative py-12 px-4',
    (dropdownActive || hover) && 'active border border-secondary bg-gray-200',
  );
  const { bookmarks, toggleBookmark, isHydrated } = useUserPreferencesStore();
  const isFavorite = isHydrated
    ? bookmarks.includes(data.name)
    : data.isFavorite;
  const updatePreferences = useUpdateUserPreferences();

  const handleActionDone = () => {
    setHover(false);
    setDropdownActive(false);
    setTimeout(() => {
      buttonRef.current?.blur();
    }, 0);
  };

  const handleToggleFavorite = () => {
    toggleBookmark(data.name);

    updatePreferences.mutate({
      bookmarks: useUserPreferencesStore.getState().bookmarks,
      applications: displayedApps?.map((app) => app.name) ?? [],
    });
  };

  return (
    <a
      tabIndex={0}
      data-id={data.name}
      href={data.address}
      rel={data.isExternal ? 'noopener noreferrer' : undefined}
      target={
        data.isExternal || data.category === 'connector' ? '_blank' : undefined
      }
      className={classApplicationCard}
      title={data.appName}
      onMouseEnter={() => setHover(true)}
      onMouseLeave={() => setHover(false)}
    >
      <ApplicationIcon data={{ ...data, isFavorite }} isFavorite={isFavorite} />

      <h1 className="small text-gray-900 ellipsis-3 application-title">
        {data.appName}
      </h1>
      <div
        className="dropdown-wrapper"
        title=""
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
                ref={combineRefs(
                  triggerProps.ref as React.Ref<HTMLButtonElement>,
                  buttonRef,
                )}
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
                onActionDone={handleActionDone}
              />
            </>
          )}
        </Dropdown>
      </div>
    </a>
  );
}
