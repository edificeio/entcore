import type { WayfProvider } from '~/models/wayf';
import { ProviderButton } from '../ProviderButton';
import './ProviderList.css';

interface ProviderListProps {
  providers: WayfProvider[];
  onProviderClick: (provider: WayfProvider) => void;
}

export const ProviderList = ({ providers, onProviderClick }: ProviderListProps) => {
  return (
    <ul className="wayf-provider-list">
      {providers.map((provider) => (
        <li key={provider.i18n}>
          <ProviderButton provider={provider} onClick={onProviderClick} />
        </li>
      ))}
    </ul>
  );
};
