import type { JSX, RefAttributes } from 'react';
import { Avatar, Dropdown, IconButton } from '@edifice.io/react';
import type { IconButtonProps } from '@edifice.io/react';
import { IconAddUser } from '@edifice.io/react/icons';
import { useTranslation } from 'react-i18next';
import type { Child } from '~/models/child';

const VISIBLE_MAX = 3;

interface ChildTabsProps {
  children: Child[];
  currentIndex: number;
  onSelect: (index: number) => void;
}

export function ChildTabs({
  children,
  currentIndex,
  onSelect,
}: ChildTabsProps) {
  const { t } = useTranslation('timeline');

  if (children.length <= 1) return null;

  const visibleIndexes =
    children.length <= VISIBLE_MAX
      ? children.map((_, i) => i)
      : (() => {
          const base = Array.from({ length: VISIBLE_MAX - 1 }, (_, i) => i);
          return base.includes(currentIndex)
            ? [...base, VISIBLE_MAX - 1]
            : [...base, currentIndex];
        })();

  const overflowIndexes = children
    .map((_, i) => i)
    .filter((i) => !visibleIndexes.includes(i));

  return (
    <div className="timetable-child-tabs">
      {visibleIndexes.map((i) => {
        const child = children[i];
        const isActive = i === currentIndex;
        return (
          <button
            key={child.id}
            type="button"
            className={`timetable-child-tab${isActive ? ' timetable-child-tab--active' : ''}`}
            onClick={() => onSelect(i)}
          >
            <Avatar
              alt={child.name}
              src={child.avatar}
              size="xs"
              variant="circle"
              className="timetable-child-tab-avatar"
            />
            <span className="timetable-child-tab-label">{child.name}</span>
          </button>
        );
      })}
      {overflowIndexes.length > 0 && (
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
                aria-label={t(
                  'homepage.crna.widget.timetable.select-child',
                  'Choisir un enfant',
                )}
                color="tertiary"
                variant="ghost"
                icon={<IconAddUser />}
              />
              <Dropdown.Menu>
                {overflowIndexes.map((i) => (
                  <Dropdown.RadioItem
                    key={children[i].id}
                    value={String(i)}
                    model={String(currentIndex)}
                    onChange={() => onSelect(i)}
                  >
                    {children[i].name}
                  </Dropdown.RadioItem>
                ))}
              </Dropdown.Menu>
            </>
          )}
        </Dropdown>
      )}
    </div>
  );
}
