import type { WayfDomainConfig } from '~/models/wayf';

const EDUCONNECT_ACS =
  'https://educonnect.education.gouv.fr/idp/profile/SAML2/Unsolicited/SSO?providerId=urn%3Afi%3Aent%3Aprod-cd83-edu%3A1.0';
const ARENA_ACS =
  'https://id.ac-nice.fr/sso/SSO?SPEntityID=urn:fi:ent:prod-cd83-aaa:1.0&TARGET=https://moncollege-ent.var.fr';

export const varConfig: WayfDomainConfig = {
  providers: [
    {
      i18n: 'wayf.student',
      color: 'student',
      icon: 'student',
      acs: EDUCONNECT_ACS,
    },
    {
      i18n: 'wayf.relative',
      color: 'relative',
      icon: 'relative',
      acs: EDUCONNECT_ACS,
    },
    {
      i18n: 'wayf.teacher',
      color: 'teacher',
      icon: 'teacher',
      acs: ARENA_ACS,
    },
    {
      i18n: 'wayf.perseducnat',
      color: 'perseducnat',
      icon: 'perseducnat',
      acs: '/auth/login',
    },
    {
      i18n: 'wayf.other',
      color: 'other',
      icon: 'other',
      acs: '/auth/login',
    },
  ],
  partners: [
    { logo: '/img/partners/logo-var.png', url: 'https://www.var.fr/' },
    { logo: '/img/partners/logo-ac-nice.png', url: 'https://www.ac-nice.fr/' },
    { logo: '/img/partners/logo-region-sud.png', url: 'https://www.maregionsud.fr/' },
    { logo: '/img/partners/logo-ue.png' },
  ],
};
