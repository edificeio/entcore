import {
  IconGuest,
  IconParent,
  IconPersonnel,
  IconStudent,
  IconTeacher,
} from '@edifice.io/react/icons/audience';
import type { ComponentType, SVGProps } from 'react';
import { useTranslation } from 'react-i18next';
import type { WayfIconKey, WayfProvider } from '~/models/wayf';
import './ProviderButton.css';

/**
 * Finite registry mapping icon keys to their component.
 * To add a new icon: add the key to WAYF_ICONS in models/wayf.ts and register it here.
 */
const PROVIDER_ICONS: Record<
  WayfIconKey,
  ComponentType<SVGProps<SVGSVGElement>>
> = {
  student: IconStudent,
  teacher: IconTeacher,
  relative: IconParent,
  perseducnat: IconPersonnel,
  other: IconGuest,
};

interface ProviderButtonProps {
  provider: WayfProvider;
  onClick: (provider: WayfProvider) => void;
  /** Inherited at level 2 — child buttons fall back to the parent's icon */
  parentIconKey?: WayfIconKey;
}

export const ProviderButton = ({
  provider,
  onClick,
  parentIconKey,
}: ProviderButtonProps) => {
  const { t } = useTranslation('auth');

  const colorKey = provider.color;

  // Icon resolution: explicit provider.icon > parentIconKey > none
  const resolvedIconKey = provider.icon ?? parentIconKey;
  const IconComponent = resolvedIconKey
    ? PROVIDER_ICONS[resolvedIconKey]
    : undefined;

  return (
    <button
      className={`wayf-provider-btn wayf-provider-btn--${colorKey}`}
      onClick={() => onClick(provider)}
      type="button"
    >
      <span className="wayf-provider-btn__icon-wrap">
        {provider.iconSrc ? (
          <img
            src={provider.iconSrc}
            alt=""
            className="wayf-provider-btn__icon"
          />
        ) : (
          IconComponent && (
            <IconComponent aria-hidden="true" width={24} height={24} />
          )
        )}
      </span>

      <span className="wayf-provider-btn__label">{t(provider.i18n)}</span>

      <span className="wayf-provider-btn__arrow" aria-hidden="true">
        <svg width="20" height="20" viewBox="0 0 20 20" fill="none">
          <path
            d="M7.5 4.5L13 10L7.5 15.5"
            stroke="currentColor"
            strokeWidth="1.5"
            strokeLinecap="round"
            strokeLinejoin="round"
          />
        </svg>
      </span>
    </button>
  );
};
