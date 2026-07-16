import type { WayfPartner } from '~/models/wayf';
import './PartnerLogos.css';

interface PartnerLogosProps {
  partners?: WayfPartner[];
  childTheme?: string;
}

export const PartnerLogos = ({ partners, childTheme }: PartnerLogosProps) => {
  if (!partners?.length || !childTheme) return null;

  return (
    <div className="wayf-partners">
      {partners.map((partner) => {
        const img = (
          <img
            src={`/assets/themes/${childTheme}${partner.logo}`}
            alt=""
            className="wayf-partners__logo"
          />
        );

        return partner.url ? (
          <a
            key={partner.logo}
            href={partner.url}
            target="_blank"
            rel="noreferrer"
            className="wayf-partners__link"
          >
            {img}
          </a>
        ) : (
          <span key={partner.logo} className="wayf-partners__link">
            {img}
          </span>
        );
      })}
    </div>
  );
};
