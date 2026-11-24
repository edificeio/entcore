import {
  Edumalin,
  EdumalinItem,
  EdumalinModal,
} from '@edifice.io/react/homepage';
import { useMemo, useState } from 'react';

const EDUMALIN_URL = 'https://edumalin.fr/';

/** Local date-time `dayOffset` days away from today, at the given time. */
const dateTimeFromToday = (dayOffset: number, time: string): string => {
  const date = new Date();
  date.setDate(date.getDate() + dayOffset);
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${date.getFullYear()}-${month}-${day}T${time}:00`;
};

const buildItems = (): EdumalinItem[] => [
  {
    title: 'Préparer son contrôle sans stress',
    description:
      'Une demi-heure en direct pour apprendre à planifier ses révisions, se tester avec des questions flash et arriver serein le jour J. Les questions des élèves sont les bienvenues !',
    type: { name: 'Webinaire' },
    dateTime: dateTimeFromToday(6, '17:30'),
    duration: 30,
    buttonTitle: 'S’inscrire',
    buttonLink: EDUMALIN_URL,
  },
  {
    title: 'Apprendre à apprendre : 5 astuces pour mieux mémoriser ses leçons',
    description:
      'Fiches, cartes mentales, récupération active : découvre en vidéo les méthodes qui marchent vraiment pour retenir durablement.',
    type: { name: 'Tutoriel' },
    buttonTitle: 'Voir le tutoriel',
    buttonLink: EDUMALIN_URL,
  },
  {
    title: 'Organiser son temps de travail à la maison',
    description:
      'Dans ce podcast, deux élèves de 5e racontent comment ils ont construit leur planning de la semaine avec leur professeur principal.',
    type: { name: 'Podcast' },
  },
];

/** Demo "Édumalin": three contents for students, each opening its modal. */
export function EdumalinDemo() {
  const items = useMemo(buildItems, []);
  const [selectedItem, setSelectedItem] = useState<EdumalinItem>();

  return (
    <>
      <Edumalin
        status="default"
        items={items}
        onItemClick={setSelectedItem}
        onEdumalinClick={() => {}}
      />
      {selectedItem && (
        <EdumalinModal
          item={selectedItem}
          onClose={() => setSelectedItem(undefined)}
          onEdumalinClick={() => {}}
          onActionClick={() => {}}
        />
      )}
    </>
  );
}
