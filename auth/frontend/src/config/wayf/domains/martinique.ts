import type { WayfDomainConfig } from '~/models/wayf';

const EDUCONNECT_ACS =
  'https://educonnect.education.gouv.fr/idp/profile/SAML2/Unsolicited/SSO?providerId=https%3A%2F%2Fcolibri.ac-martinique.fr%2Fauth%2Fsaml%2Fmetadata%2Fidp.xml';
const AGRI_ACS =
  'https://auth.educagri.fr/idp/profile/SAML2/Unsolicited/SSO?providerId=https%3A%2F%2Fcolibri.ac-martinique.fr%2Fauth%2Fsaml%2Fmetadata%2Fidp.xml';
const ARENA_ACS =
  'https://authentification.ac-martinique.fr/saml?sp_ident=colibri&RelayState=https://colibri.ac-martinique.fr/timeline/timeline';

export const martiniqueConfig: WayfDomainConfig = {
  providers: [
    {
      i18n: 'wayf.student',
      color: 'student',
      icon: 'student',
      children: [
        {
          i18n: 'wayf.student.maternelle-cm1',
          color: 'student',
          acs: '/auth/login',
        },
        {
          i18n: 'wayf.student.cm2-lycee',
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
          i18n: 'wayf.teacher.educnat',
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
          i18n: 'wayf.perseducnat.academy',
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
    { logo: '/img/partners/logo-collectivite-martinique.png' },
    { logo: '/img/partners/logo-ac-martinique.png' },
  ],
};
