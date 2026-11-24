import { Mediacentre, MediacentreResource } from '@edifice.io/react/homepage';

/** UAI of the demo school, which enables the Universalis search. */
const SCHOOL_UAI = '0590001A';

/** Favorite resources of a 5e student, linked to their publishers' websites. */
const RESOURCES: MediacentreResource[] = [
  {
    id: 'demo-transmath-5e',
    title: 'Transmath | Mathématiques 5e',
    subtitle: 'Éditions Nathan',
    href: 'https://www.nathan.fr',
  },
  {
    id: 'demo-histoire-geo-5e',
    title: 'Histoire-Géographie-EMC 5e',
    subtitle: 'Lelivrescolaire.fr',
    href: 'https://www.lelivrescolaire.fr',
  },
  {
    id: 'demo-robert-college',
    title: 'Le Robert Collège',
    subtitle: 'Dictionnaires Le Robert',
    href: 'https://www.lerobert.com',
  },
  {
    id: 'demo-educarte',
    title: 'Educ’ARTE',
    subtitle: 'ARTE Éducation',
    href: 'https://educarte.arte.tv',
  },
];

/** Demo "Médiacentre": favorite resources and the Universalis search. */
export function MediacentreDemo() {
  return (
    <Mediacentre
      handleActionClick={() => {}}
      resources={RESOURCES}
      uai={SCHOOL_UAI}
    />
  );
}
