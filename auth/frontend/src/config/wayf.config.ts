import type { WayfConfig, WayfDomainConfig } from '~/models/wayf';

export const DEFAULT_WAYF_CONFIG: WayfDomainConfig = {
  providers: [
    { i18n: 'wayf.teacher', acs: '/auth/saml/default-teacher' },
    { i18n: 'wayf.student', acs: '/auth/saml/default-student' },
    { i18n: 'wayf.parent', acs: '/auth/saml/default-parent' },
    { i18n: 'wayf.personnel', acs: '/auth/saml/default-personnel' },
    { i18n: 'wayf.guest', acs: '/auth/saml/default-guest' },
  ],
};

export const wayfConfig: WayfConfig = {
  'wayf-v2': {
    localhost: {
      providers: [
        {
          i18n: 'wayf.student',
          children: [{ i18n: 'wayf.student', acs: '/auth/login' }],
        },
        { i18n: 'wayf.relative', acs: '/auth/login' },
        { i18n: 'wayf.teacher', acs: '/auth/login' },
        { i18n: 'wayf.perseducnat', acs: '/auth/login' },
        { i18n: 'wayf.other', acs: '/auth/login' },
      ],
    },
  },
};
