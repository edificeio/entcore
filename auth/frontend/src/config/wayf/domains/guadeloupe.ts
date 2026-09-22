import type { WayfDomainConfig } from '~/models/wayf';

export const guadeloupeConfig: WayfDomainConfig = {
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
      acs: 'https://bv.ac-guadeloupe.fr/sso/SSO?SPEntityID=https://karukera.ac-guadeloupe.fr/auth/saml/metadata/idp.xml&TARGET=https://karukera.ac-guadeloupe.fr',
    },
    {
      i18n: 'wayf.perseducnat',
      color: 'perseducnat',
      icon: 'perseducnat',
      children: [
        {
          i18n: 'wayf.perseducnat.arena',
          color: 'perseducnat',
          acs: 'https://bv.ac-guadeloupe.fr/sso/SSO?SPEntityID=https://karukera.ac-guadeloupe.fr/auth/saml/metadata/idp.xml&TARGET=https://karukera.ac-guadeloupe.fr',
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
