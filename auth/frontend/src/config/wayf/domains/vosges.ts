import type { WayfDomainConfig } from '~/models/wayf';

const EDUCONNECT_ACS =
  'https://educonnect.education.gouv.fr/idp/profile/SAML2/Unsolicited/SSO?providerId=urn%3Afi%3Aent%3Aprod-vosges-edu%3A1.0';
const ARENA_ACS =
  'https://portail.ac-nancy-metz.fr/sso/SSO?SPEntityID=urn:fs:edifice:ent-panda-1d-vosges:1.0&TARGET=https://panda.vosges.fr';

export const vosgesConfig: WayfDomainConfig = {
  providers: [
    {
      i18n: 'wayf.student',
      color: 'student',
      icon: 'student',
      acs: '/auth/login',
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
      children: [
        {
          i18n: 'wayf.perseducnat.academy',
          color: 'perseducnat',
          acs: ARENA_ACS,
        },
        {
          i18n: 'wayf.perseducnat.collectivite',
          color: 'perseducnat',
          acs: '/auth/login',
        },
      ],
    },
    {
      i18n: 'wayf.other',
      color: 'other',
      icon: 'other',
      acs: '/auth/login',
    },
  ],
  partners: [
    { logo: '/img/partners/logo-ac-nancy-metz.png', url: 'https://www.ac-nancy-metz.fr/' },
    { logo: '/img/partners/logo-vosges.png', url: 'https://www.vosges.fr/' },
    {
      logo: '/img/partners/logo-banque-des-territoires.png',
      url: 'https://www.banquedesterritoires.fr/',
    },
    {
      logo: '/img/partners/logo-france-2030.png',
      url: 'https://www.info.gouv.fr/grand-dossier/france-2030',
    },
    { logo: '/img/partners/logo-partenaire-5.png' },
  ],
};
