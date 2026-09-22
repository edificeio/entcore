import type { WayfDomainConfig } from '~/models/wayf';

const ARENA_ACS =
  'https://portail.ac-lyon.fr/sso/SSO?SPEntityID=urn:fi:ent:prod-sem-aaa:1.0&TARGET=https://sem.edifice.io';
const EDUCONNECT_PARENT_ACS =
  'https://educonnect.education.gouv.fr/idp/profile/SAML2/Unsolicited/SSO?providerId=urn%3Afi%3Aent%3Aprod-sem-edu%3A1.0';

// NOTE: "École publique"/"École privée" n'ont pas de clé i18n dédiée — réutilise
// les clés wayf.relative.* les plus proches en attendant la passe de nettoyage
// i18n avec le CPO. Écart consigné dans project-wayf-i18n-key-cleanup.
export const semConfig: WayfDomainConfig = {
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
      children: [
        {
          i18n: 'wayf.relative.ecole-college-lycee',
          color: 'relative',
          acs: EDUCONNECT_PARENT_ACS,
        },
        {
          i18n: 'wayf.relative.special',
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
      children: [
        {
          i18n: 'wayf.perseducnat.arena',
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
    { logo: '/img/partners/logo-numerique-a-lecole.png' },
    {
      logo: '/img/partners/logo-saint-etienne-metropole.png',
      url: 'https://www.saint-etienne-metropole.fr/',
    },
  ],
};
