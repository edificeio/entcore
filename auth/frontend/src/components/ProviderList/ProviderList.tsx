import { IconRafterLeft } from '@edifice.io/react/icons';
import { useTranslation } from 'react-i18next';
import type { WayfIconKey, WayfProvider } from '~/models/wayf';
import { ProviderButton } from '../ProviderButton';
import './ProviderList.css';

interface ProviderListProps {
  providers: WayfProvider[];
  onProviderClick: (provider: WayfProvider) => void;
  /** Forwarded to each ProviderButton — used at level 2 for icon inheritance */
  parentIconKey?: WayfIconKey;
  /** When provided, renders a back button as the first list item */
  onBack?: () => void;
}

export const ProviderList = ({
  providers,
  onProviderClick,
  parentIconKey,
  onBack,
}: ProviderListProps) => {
  const { t } = useTranslation('auth');

  return (
    <ul className="wayf-provider-list" data-testid="wayf-list-providers">
      {onBack && (
        <li>
          <button type="button" className="wayf-back-btn" onClick={onBack} data-testid="wayf-button-back">
            <IconRafterLeft aria-hidden="true" width={20} height={20} />
            <span>{t('wayf.link.back') || 'Retour'}</span>
          </button>
        </li>
      )}
      {providers.map((provider) => (
        <li key={provider.i18n}>
          <ProviderButton
            provider={provider}
            onClick={onProviderClick}
            parentIconKey={parentIconKey}
          />
        </li>
      ))}
    </ul>
  );
};
