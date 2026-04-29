import type { WayfIconKey, WayfProvider } from '~/models/wayf';
import { ProviderButton } from '../ProviderButton';
import './ProviderList.css';

interface ProviderListProps {
  providers: WayfProvider[];
  onProviderClick: (provider: WayfProvider) => void;
  /** Forwarded to each ProviderButton — used at level 2 for icon inheritance */
  parentIconKey?: WayfIconKey;
}

export const ProviderList = ({
  providers,
  onProviderClick,
  parentIconKey,
}: ProviderListProps) => {
  return (
    <ul className="wayf-provider-list">
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
