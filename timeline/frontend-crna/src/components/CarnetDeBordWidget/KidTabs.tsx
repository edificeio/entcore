import type { JSX, RefAttributes } from 'react';
import { Avatar, Dropdown, IconButton } from '@edifice.io/react';
import type { IconButtonProps } from '@edifice.io/react';
import { IconAddUser } from '@edifice.io/react/icons';
import { useTranslation } from 'react-i18next';
import type { ParsedEleve } from '~/models/carnetDeBord';

const VISIBLE_MAX = 3;

interface KidTabsProps {
  eleves: ParsedEleve[];
  currentEleveIndex: number;
  onSelect: (index: number) => void;
}

export function KidTabs({ eleves, currentEleveIndex, onSelect }: KidTabsProps) {
  const { t } = useTranslation();

  if (eleves.length <= 1) return null;

  const visibleIndexes =
    eleves.length <= VISIBLE_MAX
      ? eleves.map((_, i) => i)
      : (() => {
          const base = Array.from({ length: VISIBLE_MAX - 1 }, (_, i) => i);
          return base.includes(currentEleveIndex)
            ? [...base, VISIBLE_MAX - 1]
            : [...base, currentEleveIndex];
        })();

  const overflowIndexes = eleves
    .map((_, i) => i)
    .filter((i) => !visibleIndexes.includes(i));

  return (
    <div className="carnet-de-bord-tabs">
      {visibleIndexes.map((i) => {
        const eleve = eleves[i];
        const isActive = i === currentEleveIndex;
        return (
          <button
            key={i}
            type="button"
            className={`carnet-de-bord-tab${isActive ? ' carnet-de-bord-tab--active' : ''}`}
            onClick={() => onSelect(i)}
          >
            <Avatar
              alt={eleve.name}
              src={eleve.avatar}
              size="xs"
              variant="circle"
              className="carnet-de-bord-tab-avatar"
            />
            <span className="carnet-de-bord-tab-label">
              {eleve.name || `Élève ${i + 1}`}
            </span>
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
                  'homepage.crna.widget.carnet-de-bord.select-child',
                  'Choisir un enfant',
                )}
                color="tertiary"
                variant="ghost"
                icon={<IconAddUser />}
              />
              <Dropdown.Menu>
                {overflowIndexes.map((i) => (
                  <Dropdown.RadioItem
                    key={i}
                    value={String(i)}
                    model={String(currentEleveIndex)}
                    onChange={() => onSelect(i)}
                  >
                    {eleves[i].name || `Élève ${i + 1}`}
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
