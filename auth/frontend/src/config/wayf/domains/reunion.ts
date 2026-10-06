import type { WayfDomainConfig } from '~/models/wayf';

const ARENA_ACS =
  'https://portail.ac-reunion.fr/sso/SSO?SPEntityID=sso.ac-reunion.fr&TARGET=https%3A%2F%2Fsso.ac-reunion.fr%2Fsaml%3Fsp_ident%3Durn%3Afi%3Aone%3Aprod-reunion%3A1.0%26RelayState%3Dhttps%3A%2F%2Fent1d.ac-reunion.fr';

export const reunionConfig: WayfDomainConfig = {
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
      acs: '/auth/login',
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
