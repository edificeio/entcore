import type { WayfDomainConfig } from '~/models/wayf';

const EDUCONNECT_ACS =
  'https://educonnect.education.gouv.fr/idp/profile/SAML2/Unsolicited/SSO?providerId=https%3A%2F%2Fent.l-educdenormandie.fr%2Fauth%2Fsaml%2Fmetadata%2Fidp.xml';
const AGRI_ACS =
  'https://auth.educagri.fr/idp/profile/SAML2/Unsolicited/SSO?providerId=https%3A%2F%2Fent.l-educdenormandie.fr%2Fauth%2Fsaml%2Fmetadata%2Fidp.xml';
const ARENA_ACS =
  'https://extranet.ac-normandie.fr/sso/SSO?SPEntityID=urn%3Afi%3Asp%3Aent-EDUC-Normandie%3A1%3A0';

// NOTE: plusieurs i18n ci-dessous réutilisent la clé la plus proche disponible
// en attendant une passe de nettoyage du nommage wayf.* avec le CPO (aucune
// nouvelle clé créée pour l'instant). Écarts consignés dans la mémoire
// project-wayf-i18n-key-cleanup.
export const normandieConfig: WayfDomainConfig = {
  providers: [
    {
      i18n: 'wayf.student',
      color: 'student',
      icon: 'student',
      children: [
        {
          i18n: 'wayf.student.ecole',
          color: 'student',
          children: [
            {
              i18n: 'wayf.perseducnat.local',
              color: 'student',
              acs: '/auth/login',
            },
            {
              i18n: 'wayf.student.educonnect',
              color: 'student',
              acs: EDUCONNECT_ACS,
            },
          ],
        },
        {
          i18n: 'wayf.student.college-lycee',
          color: 'student',
          acs: EDUCONNECT_ACS,
        },
        {
          i18n: 'wayf.student.special',
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
          children: [
            {
              i18n: 'wayf.perseducnat.local',
              color: 'relative',
              acs: '/auth/login',
            },
            {
              i18n: 'wayf.relative.educonnect',
              color: 'relative',
              acs: EDUCONNECT_ACS,
            },
          ],
        },
        {
          i18n: 'wayf.student.college-lycee',
          color: 'relative',
          acs: EDUCONNECT_ACS,
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
      i18n: 'wayf.perseducnat.agri',
      color: 'perseducnat',
      icon: 'perseducnat',
      acs: AGRI_ACS,
    },
    {
      i18n: 'wayf.other',
      color: 'other',
      icon: 'other',
      acs: '/auth/login',
    },
  ],
  partners: [
    { logo: '/img/partners/logo-normandie.png', url: 'https://www.normandie.fr/' },
    { logo: '/img/partners/logo-calvados.png', url: 'https://www.calvados.fr/accueil.html' },
    { logo: '/img/partners/logo-manche.png', url: 'https://www.manche.fr/' },
    { logo: '/img/partners/logo-orne.png', url: 'https://www.orne.fr/' },
    {
      logo: '/img/partners/logo-manche-numerique.png',
      url: 'https://manchenumerique.fr/',
    },
    {
      logo: '/img/partners/logo-ac-normandie.png',
      url: 'https://www.ac-normandie.fr/',
    },
    { logo: '/img/partners/logo-mer.png', url: 'https://www.mer.gouv.fr/' },
    {
      logo: '/img/partners/logo-draaf.png',
      url: 'https://draaf.normandie.agriculture.gouv.fr/l-enseignement-agricole-en-normandie-r520.html',
    },
  ],
};
