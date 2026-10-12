import type { WayfDomainConfig } from '~/models/wayf';

const ARENA_ACS =
  'https://extranet.ac-versailles.fr/sso/SSO?SPEntityID=urn:fs:edifice:moncollege-ent:1.0&TARGET=https://www.moncollege-ent.essonne.fr';

export const moncollegeConfig: WayfDomainConfig = {
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
    { logo: '/img/partners/logo-internet-sans-crainte.png', url: 'https://www.internetsanscrainte.fr/' },
    { logo: '/img/partners/logo-ode.png', url: 'https://ode.essonne.fr/ode/index.xhtml?jfwid=c0898' },
    { logo: '/img/partners/logo-essonne.png', url: 'https://www.essonne.fr/' },
    { logo: '/img/partners/logo-monecole.png', url: 'https://monecole.essonne.fr/' },
  ],
};
