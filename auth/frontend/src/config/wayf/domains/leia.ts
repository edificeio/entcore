import type { WayfDomainConfig } from '~/models/wayf';

const EDUCONNECT_ACS =
  'https://educonnect.education.gouv.fr/idp/profile/SAML2/Unsolicited/SSO?providerId=urn%3Afi%3Aent%3Aprod-leia-edu%3A1.0';
const AGRI_ACS =
  'https://auth.educagri.fr/idp/profile/SAML2/Unsolicited/SSO?providerId=urn%3Afi%3Aent%3Aprod-leia-agri%3A1.0';
const ARENA_ACS =
  'https://id.ac-corse.fr/sso/SSO?SPEntityID=urn:fi:ent:prod-leia-aaa:1.0&TARGET=https://ent.leia.corsica';

export const leiaConfig: WayfDomainConfig = {
  providers: [
    {
      i18n: 'wayf.student',
      color: 'student',
      icon: 'student',
      children: [
        {
          i18n: 'wayf.student.educonnect',
          color: 'student',
          acs: EDUCONNECT_ACS,
        },
        {
          i18n: 'wayf.student.agri',
          color: 'student',
          acs: AGRI_ACS,
        },
      ],
    },
    {
      i18n: 'wayf.relative',
      color: 'relative',
      icon: 'relative',
      children: [
        {
          i18n: 'wayf.relative.educonnect',
          color: 'relative',
          acs: EDUCONNECT_ACS,
        },
        {
          i18n: 'wayf.relative.agri',
          color: 'relative',
          acs: AGRI_ACS,
        },
      ],
    },
    {
      i18n: 'wayf.teacher',
      color: 'teacher',
      icon: 'teacher',
      children: [
        {
          i18n: 'wayf.perseducnat.arena',
          color: 'teacher',
          acs: ARENA_ACS,
        },
        {
          i18n: 'wayf.teacher.agri',
          color: 'teacher',
          acs: AGRI_ACS,
        },
      ],
    },
    {
      i18n: 'wayf.perseducnat',
      color: 'perseducnat',
      icon: 'perseducnat',
      children: [
        {
          i18n: 'wayf.perseducnat.arena',
          color: 'perseducnat',
          acs: ARENA_ACS,
        },
        {
          i18n: 'wayf.perseducnat.collectivite',
          color: 'perseducnat',
          acs: '/auth/login',
        },
        {
          i18n: 'wayf.perseducnat.agri',
          color: 'perseducnat',
          acs: AGRI_ACS,
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
    { logo: '/img/partners/logo-isula.png', url: 'https://www.isula.corsica/' },
    { logo: '/img/partners/logo-ac-corse.png', url: 'https://www.ac-corse.fr/' },
    { logo: '/img/partners/UE_80.png' },
  ],
};
