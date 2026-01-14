import { Dropdown, IconButton, IconButtonProps } from '@edifice.io/react';
import { IconOptions } from '@edifice.io/react/icons';
import clsx from 'clsx';
import { RefAttributes, useEffect, useRef, useState } from 'react';
import { Application } from '~/models/application';
import { useUserPreferencesStore } from '~/store/userPreferencesStore';
import { ApplicationIcon } from './ApplicationIcon/ApplicationIcon';
import { ApplicationMenu } from './ApplicationMenu';

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

export function ApplicationWrapper({
  data,
  onToggleFavorite,
}: {
  data: Application;
  onToggleFavorite: (appName: string) => void;
}) {
  const buttonRef = useRef<HTMLButtonElement>(null);
  const wrapperRef = useRef<HTMLAnchorElement>(null);

  const [dropdownActive, setDropdownActive] = useState(false);
  const [hover, setHover] = useState(false);
  const [isVisible, setIsVisible] = useState(false);

  const classApplicationCard = clsx(
    'rounded application-card position-relative py-12 px-4',
    (dropdownActive || hover) && 'active border border-secondary bg-gray-200',
  );

  const { bookmarks, isHydrated } = useUserPreferencesStore();
  const isFavorite = isHydrated
    ? bookmarks.includes(data.name)
    : data.isFavorite;

  const handleActionDone = () => {
    setHover(false);
    setDropdownActive(false);
    setTimeout(() => {
      buttonRef.current?.blur();
    }, 0);
  };

  // ✅ Intersection Observer for lazy load
  useEffect(() => {
    const observer = new IntersectionObserver(
      (entries) => {
        const [entry] = entries;
        if (entry.isIntersecting) {
          setIsVisible(true);
          observer.disconnect();
        }
      },
      { rootMargin: '200px' }, // preload before visible
    );

    if (wrapperRef.current) {
      observer.observe(wrapperRef.current);
    }

    return () => observer.disconnect();
  }, []);

  return (
    <a
      ref={wrapperRef}
      tabIndex={0}
      data-id={data.name}
      href={data.address}
      rel={data.isExternal ? 'noopener noreferrer' : undefined}
      target={
        data.isExternal || data.category === 'connector' ? '_blank' : undefined
      }
      className={classApplicationCard}
      onMouseEnter={() => setHover(true)}
      onMouseLeave={() => setHover(false)}
    >
      {/* ✅ Load ApplicationIcon only when visible */}
      {isVisible ? (
        <ApplicationIcon
          data={{ ...data, isFavorite }}
          isFavorite={isFavorite}
        />
      ) : (
        <div className="w-full h-24 bg-gray-200 animate-pulse rounded" />
      )}

      <h1
        className="small text-gray-900 ellipsis-3 application-title"
        title={data.appName}
      >
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
            <div data-testid="dropdown">
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
                className="bg-white border border-white"
                icon={<IconOptions />}
              />
              <ApplicationMenu
                data={{ ...data, isFavorite }}
                onToggleFavorite={() => onToggleFavorite(data.name)}
                onActionDone={handleActionDone}
              />
            </div>
          )}
        </Dropdown>
      </div>
    </a>
  );
}
