import type { WayfIconKey, WayfProvider } from '~/models/wayf';
import { ProviderButton } from '../ProviderButton';
import './ProviderList.css';

interface ProviderListProps {
  providers: WayfProvider[];
  onProviderClick: (provider: WayfProvider) => void;
  /** Forwarded to each ProviderButton — used at level 2 for color inheritance */
  parentColorKey?: string;
  /** Forwarded to each ProviderButton — used at level 2 for icon inheritance */
  parentIconKey?: WayfIconKey;
}

export const ProviderList = ({
  providers,
  onProviderClick,
  parentColorKey,
  parentIconKey,
}: ProviderListProps) => {
  return (
    <ul className="wayf-provider-list">
      {providers.map((provider) => (
        <li key={provider.i18n}>
          <ProviderButton
            provider={provider}
            onClick={onProviderClick}
            parentColorKey={parentColorKey}
            parentIconKey={parentIconKey}
          />
        </li>
      ))}
    </ul>
  );
};
