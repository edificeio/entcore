import type { WayfDomainConfig } from '~/models/wayf';

export const portovecchioConfig: WayfDomainConfig = {
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
      acs: '/auth/openid/login',
    },
    {
      i18n: 'wayf.teacher',
      color: 'teacher',
      icon: 'teacher',
      acs: '/auth/login',
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
    {
      logo: '/img/partners/logo-collectivite-corse.png',
      url: 'https://www.isula.corsica/',
    },
    {
      logo: '/img/partners/logo-portivechju.png',
      url: 'https://www.portivechju.corsica/',
    },
    {
      logo: '/img/partners/logo-ac-corse.png',
      url: 'https://www.ac-corse.fr/',
    },
  ],
};
