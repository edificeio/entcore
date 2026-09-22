import type { WayfDomainConfig } from '~/models/wayf';

// NOTE: pas de clé i18n pour "Académie de Grenoble"/"Académie de Lyon" —
// réutilise wayf.teacher.lille/.amiens et wayf.perseducnat.lille/.amiens
// (textes affichés : Lille/Amiens, pas Grenoble/Lyon) en attendant la passe de
// nettoyage i18n avec le CPO. Écart consigné dans project-wayf-i18n-key-cleanup.
const ARENA_GRENOBLE_ACS =
  'https://extranet.ac-grenoble.fr/sso/SSO?SPEntityID=urn:fi:ent:prod-vcag-aaa:1.0&TARGET=https://ent.vienne-condrieu-agglomeration.fr';
const ARENA_LYON_ACS =
  'https://portail.ac-lyon.fr/sso/SSO?SPEntityID=urn:fi:ent:prod-vcal-aaa:1.0&TARGET=https://ent.vienne-condrieu-agglomeration.fr';

export const vcaConfig: WayfDomainConfig = {
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
      children: [
        {
          i18n: 'wayf.teacher.lille',
          color: 'teacher',
          acs: ARENA_GRENOBLE_ACS,
        },
        {
          i18n: 'wayf.teacher.amiens',
          color: 'teacher',
          acs: ARENA_LYON_ACS,
        },
      ],
    },
    {
      i18n: 'wayf.perseducnat',
      color: 'perseducnat',
      icon: 'perseducnat',
      children: [
        {
          i18n: 'wayf.perseducnat.lille',
          color: 'perseducnat',
          acs: ARENA_GRENOBLE_ACS,
        },
        {
          i18n: 'wayf.perseducnat.amiens',
          color: 'perseducnat',
          acs: ARENA_LYON_ACS,
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
