import type { WayfDomainConfig } from '~/models/wayf';

const ARENA_ACS =
  'https://appli.ac-aix-marseille.fr/sso/SSO?SPEntityID=urn:fs:edifice:aucollege84:1.0&TARGET=https://www.aucollege84.vaucluse.fr';

export const cd84Config: WayfDomainConfig = {
  providers: [
    {
      i18n: 'wayf.student',
      color: 'student',
      icon: 'student',
      acs: '/auth/saml/authn/student',
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
    { logo: '/img/partners/logo-vaucluse.png', url: 'https://www.vaucluse.fr/' },
    { logo: '/img/partners/logo-ac-aix-marseille.png', url: 'https://www.ac-aix-marseille.fr/' },
  ],
};
