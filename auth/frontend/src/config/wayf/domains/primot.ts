import type { WayfDomainConfig } from '~/models/wayf';

const ARENA_TOURS_ACS = '/auth/saml/authn/perseducnat';

export const primotConfig: WayfDomainConfig = {
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
      acs: '/auth/saml/authn/relative',
    },
    {
      i18n: 'wayf.teacher',
      color: 'teacher',
      icon: 'teacher',
      acs: ARENA_TOURS_ACS,
    },
    {
      i18n: 'wayf.perseducnat',
      color: 'perseducnat',
      icon: 'perseducnat',
      children: [
        {
          i18n: 'wayf.perseducnat.academy',
          color: 'perseducnat',
          acs: ARENA_TOURS_ACS,
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
    { logo: '/img/partners/logo-recia.png', url: 'https://www.recia.fr/' },
    {
      logo: '/img/partners/logo-ac-orleans-tours.png',
      url: 'https://www.ac-orleans-tours.fr/',
    },
  ],
};
