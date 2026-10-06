import type { WayfDomainConfig } from '~/models/wayf';

// NOTE: l'ancienne WAYF envoie <student> "Élève" vers /auth/login (compte
// local ENT) — seul <parent> utilise réellement l'EduConnect
// prod-reims-1d-edu aujourd'hui. Gardé sur /auth/login pour <student>
// (décision Pascal, 2026-10-06) plutôt que de réutiliser l'URL EduConnect du
// parent.
const EDUCONNECT_ACS =
  'https://educonnect.education.gouv.fr/idp/profile/SAML2/Unsolicited/SSO?providerId=urn%3Afi%3Aent%3Aprod-reims-1d-edu%3A1.0';
const ARENA_ACS =
  'https://extranet.ac-reims.fr/sso/SSO?SPEntityID=urn:fi:ent:prod-reims-1d-aaa:1.0&TARGET=https://ent-ecoles.ac-reims.fr';

export const reimsConfig: WayfDomainConfig = {
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
      acs: EDUCONNECT_ACS,
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
          i18n: 'wayf.perseducnat.local',
          color: 'perseducnat',
          acs: '/auth/login',
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
