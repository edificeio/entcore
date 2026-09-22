import type { WayfDomainConfig } from '~/models/wayf';

const EDUCONNECT_ACS =
  'https://educonnect.education.gouv.fr/idp/profile/SAML2/Unsolicited/SSO?providerId=https%3A%2F%2Fwilapa-guyane.com%2Fauth%2Fsaml%2Fmetadata%2Fidp.xml';
const ARENA_ACS =
  'https://extranet.ac-guyane.fr/sso/SSO?SPEntityID=https://wilapa-guyane.com/auth/saml/metadata/idp.xml&TARGET=https://wilapa-guyane.com';

// NOTE: wayf.relative n'a pas de clés École / Collège-Lycée séparées (seulement
// la combinée wayf.relative.ecole-college-lycee) — réutilise les clés
// wayf.student.ecole/.college-lycee (texte identique, namespace emprunté),
// écart consigné dans project-wayf-i18n-key-cleanup.
export const guyaneConfig: WayfDomainConfig = {
  providers: [
    {
      i18n: 'wayf.student',
      color: 'student',
      icon: 'student',
      children: [
        {
          i18n: 'wayf.student.ecole',
          color: 'student',
          acs: '/auth/login',
        },
        {
          i18n: 'wayf.student.college-lycee',
          color: 'student',
          acs: EDUCONNECT_ACS,
        },
        {
          i18n: 'wayf.student.agri',
          color: 'student',
          acs: '/auth/login',
        },
      ],
    },
    {
      i18n: 'wayf.relative',
      color: 'relative',
      icon: 'relative',
      children: [
        {
          i18n: 'wayf.student.ecole',
          color: 'relative',
          acs: '/auth/login',
        },
        {
          i18n: 'wayf.student.college-lycee',
          color: 'relative',
          acs: EDUCONNECT_ACS,
        },
        {
          i18n: 'wayf.relative.agri',
          color: 'relative',
          acs: '/auth/login',
        },
      ],
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
      acs: '/auth/login',
    },
    {
      i18n: 'wayf.other',
      color: 'other',
      icon: 'other',
      acs: '/auth/login',
    },
  ],
};
