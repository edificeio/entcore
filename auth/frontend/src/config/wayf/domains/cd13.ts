import type { WayfDomainConfig } from '~/models/wayf';

const EDUCONNECT_ACS =
  'https://educonnect.education.gouv.fr/idp/profile/SAML2/Unsolicited/SSO?providerId=urn%3Afi%3Aent%3Aprod-cd13-edu%3A1.0';
const ARENA_ACS =
  'https://appli.ac-aix-marseille.fr/sso/SSO?SPEntityID=urn:fi:ent:prod-cd13-aaa:1.0&TARGET=https://www.eduprovence.fr';

export const cd13Config: WayfDomainConfig = {
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
      acs: '/auth/saml/authn/teacher',
    },
    {
      i18n: 'wayf.other',
      color: 'other',
      icon: 'other',
      acs: '/auth/login',
    },
  ],
  partners: [
    {
      logo: '/img/partners/logo-ac-aix-marseille.png',
      url: 'https://www.ac-aix-marseille.fr/la-region-academique-provence-alpes-cote-d-azur-121441',
    },
    { logo: '/img/partners/logo-departement13.png', url: 'https://departement13.fr/' },
    {
      logo: '/img/partners/logo-france-2030.png',
      url: 'https://www.info.gouv.fr/grand-dossier/france-2030',
    },
  ],
};
