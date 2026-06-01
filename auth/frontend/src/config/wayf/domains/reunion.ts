import type { WayfDomainConfig } from '~/models/wayf';

export const reunionConfig: WayfDomainConfig = {
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
      acs: '/auth/saml/teacher-reunion',
    },
    {
      i18n: 'wayf.perseducnat',
      color: 'perseducnat',
      icon: 'perseducnat',
      children: [
        {
          i18n: 'wayf.perseducnat.collectivite',
          color: 'perseducnat',
          acs: '/auth/saml/perseducnat-collectivite',
        },
        {
          i18n: 'wayf.perseducnat.academie',
          color: 'perseducnat',
          acs: '/auth/saml/perseducnat-academie',
        },
      ],
    },
    {
      i18n: 'wayf.other',
      color: 'other',
      icon: 'other',
      acs: '/auth/saml/other',
    },
  ],
  partners: [],
};
