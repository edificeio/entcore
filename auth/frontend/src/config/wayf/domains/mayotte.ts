import type { WayfDomainConfig } from '~/models/wayf';

const EDUCONNECT_ACS =
  'https://educonnect.education.gouv.fr/idp/profile/SAML2/Unsolicited/SSO?providerId=urn%3Afi%3Aent%3Aprod-edifice-mayotte-edu%3A1.0';
const ARENA_ACS =
  'https://extranet.ac-mayotte.fr/sso/SSO?SPEntityID=urn:fi:ent:prod-mayotte-aaa:1.0&TARGET=https://mayotte.edifice.io';

export const mayotteConfig: WayfDomainConfig = {
  providers: [
    {
      i18n: 'wayf.student',
      color: 'student',
      icon: 'student',
      children: [
        {
          i18n: 'wayf.student.local',
          color: 'student',
          acs: '/auth/login',
        },
        {
          i18n: 'wayf.student.educonnect',
          color: 'student',
          acs: EDUCONNECT_ACS,
        },
      ],
    },
    {
      i18n: 'wayf.relative',
      color: 'relative',
      icon: 'relative',
      children: [
        {
          i18n: 'wayf.relative.local',
          color: 'relative',
          acs: '/auth/login',
        },
        {
          i18n: 'wayf.relative.educonnect',
          color: 'relative',
          acs: EDUCONNECT_ACS,
        },
      ],
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
};
