import type { CSSProperties } from 'react';
import type { WayfPartner } from '~/models/wayf';
import './PartnerLogos.css';

interface PartnerLogosProps {
  partners?: WayfPartner[];
  childTheme?: string;
  /** Logo height in px; the CSS default (40px) applies when omitted. */
  logoHeight?: number;
}

export const PartnerLogos = ({
  partners,
  childTheme,
  logoHeight,
}: PartnerLogosProps) => {
  if (!partners?.length || !childTheme) return null;

  const style = logoHeight
    ? ({ '--wayf-partner-logo-height': `${logoHeight}px` } as CSSProperties)
    : undefined;

  return (
    <div className="wayf-partners" style={style}>
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
