import clsx from 'clsx';
import type { ContentTitle } from '~/models/carnetDeBord';
import { CONTENT_COLORS, CONTENT_ICONS } from './categoryConfig';

interface CategoryRowProps {
  title: ContentTitle;
  label: string;
  /** Omitted for the modal's tab rail, which shows icon + label only. */
  subtext?: string | null;
  subtextEmpty?: boolean;
  active?: boolean;
  disabled?: boolean;
  onClick?: () => void;
  variant?: 'row' | 'rail';
}

export function CategoryRow({
  title,
  label,
  subtext,
  subtextEmpty,
  active,
  disabled,
  onClick,
  variant = 'row',
}: CategoryRowProps) {
  const content = (
    <>
      <div className="carnet-de-bord-entry-icon" data-color={CONTENT_COLORS[title]}>
        {CONTENT_ICONS[title]}
      </div>
      <div className="d-flex flex-column carnet-de-bord-entry-text">
        <strong className="carnet-de-bord-entry-label">{label}</strong>
        {subtext && (
          <span
            className={clsx(
              'carnet-de-bord-entry-sublabel',
              subtextEmpty && 'carnet-de-bord-entry-sublabel--empty',
            )}
          >
            {subtext}
          </span>
        )}
      </div>
    </>
  );

  const className = clsx(
    variant === 'rail' ? 'carnet-de-bord-rail-tab' : 'carnet-de-bord-entry',
    variant === 'rail' && active && 'carnet-de-bord-rail-tab--active',
    variant === 'row' && disabled && 'carnet-de-bord-entry--disabled',
  );

  if (!onClick) {
    return <div className={className}>{content}</div>;
  }

  return (
    <button type="button" className={className} onClick={onClick}>
      {content}
    </button>
  );
}
