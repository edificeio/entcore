import type { WayfDomainConfig } from '~/models/wayf';

const EDUCONNECT_ACS =
  'https://educonnect.education.gouv.fr/idp/profile/SAML2/Unsolicited/SSO?providerId=https%3A%2F%2Fent.l-educdenormandie.fr%2Fauth%2Fsaml%2Fmetadata%2Fidp.xml';
const AGRI_ACS =
  'https://auth.educagri.fr/idp/profile/SAML2/Unsolicited/SSO?providerId=https%3A%2F%2Fent.l-educdenormandie.fr%2Fauth%2Fsaml%2Fmetadata%2Fidp.xml';
const ARENA_ACS =
  'https://extranet.ac-normandie.fr/sso/SSO?SPEntityID=urn%3Afi%3Asp%3Aent-EDUC-Normandie%3A1%3A0';

// NOTE: wayf.agri, wayf.perseducnat ("Personnel Education Nationale"), la
// surcharge de wayf.other ("Personnel collectivité et invité") et le texte
// "Compte EduConnect" (wayf.student/relative.educonnect) sont tous définis
// uniquement dans les overrides de thème Normandie (theme-open-ent + panda),
// pas dans l'i18n générique — spécifiques à ce domaine. Plus aucun écart
// i18n connu pour cette conf (voir project-wayf-i18n-key-cleanup).
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
              i18n: 'wayf.student.school.local',
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
          i18n: 'wayf.student.st-pierre-et-miquelon',
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
          i18n: 'wayf.relative.ecole',
          color: 'relative',
          children: [
            {
              i18n: 'wayf.relative.school.local',
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
          i18n: 'wayf.relative.college-lycee',
          color: 'relative',
          acs: EDUCONNECT_ACS,
        },
        {
          i18n: 'wayf.relative.st-pierre-et-miquelon',
          color: 'relative',
          acs: '/auth/login',
        },
      ],
    },
    {
      i18n: 'wayf.perseducnat',
      color: 'perseducnat',
      icon: 'perseducnat',
      acs: ARENA_ACS,
    },
    {
      i18n: 'wayf.agri',
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
