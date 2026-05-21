import { useTranslation } from 'react-i18next';
import type { WayfPartner } from '~/models/wayf';
import './PartnerLogos.css';

interface PartnerLogosProps {
  partners?: WayfPartner[];
}

export const PartnerLogos = ({ partners }: PartnerLogosProps) => {
  const { t } = useTranslation('auth');

  if (!partners?.length) return null;

  return (
    <div className="wayf-partners">
      {partners.map((partner) => {
        const img = (
          <img
            src={t(partner.logoI18n)}
            alt=""
            className="wayf-partners__logo"
          />
        );

        return partner.url ? (
          <a
            key={partner.logoI18n}
            href={partner.url}
            target="_blank"
            rel="noreferrer"
            className="wayf-partners__link"
          >
            {img}
          </a>
        ) : (
          <span key={partner.logoI18n} className="wayf-partners__link">
            {img}
          </span>
        );
      })}
    </div>
  );
};
