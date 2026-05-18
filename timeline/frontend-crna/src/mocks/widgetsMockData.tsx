import type { LastInfosProps } from '@edifice.io/react/homepage';
import type { EmploiDuTempsEntry, ListWidgetItem } from '~/models';

function IconPlaceholder({ label, bg }: { label: string; bg: string }) {
  return (
    <div
      style={{
        width: '100%',
        height: '100%',
        background: bg,
        borderRadius: '12px',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        fontSize: '13px',
        fontWeight: 700,
        color: '#fff',
      }}
    >
      {label}
    </div>
  );
}

export const MOCK_MEDIACENTRE: ListWidgetItem[] = [
  {
    id: '1',
    icon: <IconPlaceholder label="M" bg="#4a90d9" />,
    label: 'Hyperbole | Mathématiques 3e',
    sublabel: 'Éditions Nathan',
    href: '#',
  },
  {
    id: '2',
    icon: <IconPlaceholder label="H" bg="#e07a5f" />,
    label: 'Magellan Histoire-Géographie',
    sublabel: 'Éditions Hatier',
    href: '#',
  },
  {
    id: '3',
    icon: <IconPlaceholder label="P" bg="#3d9a6d" />,
    label: 'Dictionnaire de Physique et de Chimie',
    sublabel: 'Éditions Nathan',
    href: '#',
  },
];

export const MOCK_LIENS_UTILES: ListWidgetItem[] = [
  {
    id: '1',
    icon: <IconPlaceholder label="C" bg="#7b61ff" />,
    label: 'Cap Métiers Nouvelle-Aquitaine',
    sublabel: 'Découvrez les métiers et formations de votre région',
    href: 'https://www.cap-metiers.pro',
  },
  {
    id: '2',
    icon: <IconPlaceholder label="P" bg="#2b6cbf" />,
    label: 'ParcourSup',
    sublabel: "Je m'inscris pour formuler mes vœux et je finalise mon dossier",
    href: 'https://www.parcoursup.gouv.fr',
  },
  {
    id: '3',
    icon: <IconPlaceholder label="O" bg="#e05e5e" />,
    label: 'Onisep',
    sublabel: 'Découvrez les métiers et formations de votre région',
    href: 'https://www.onisep.fr',
  },
];

export const MOCK_AVANTAGES: ListWidgetItem[] = [
  {
    id: '1',
    icon: <IconPlaceholder label="J" bg="#e07a5f" />,
    label: 'Jeunes en Nouvelle-Aquitaine',
    sublabel: 'Accédez à tous vos avantages régionaux',
    href: '#',
  },
  {
    id: '2',
    icon: <IconPlaceholder label="P" bg="#7b61ff" />,
    label: 'Pass Culture',
    sublabel: '300€ de crédit pour vos activités culturelles',
    href: '#',
  },
  {
    id: '3',
    icon: <IconPlaceholder label="T" bg="#3d9a6d" />,
    label: 'Transport scolaire',
    sublabel: 'Réduction sur votre abonnement',
    href: '#',
  },
];


export const MOCK_EMPLOI_DU_TEMPS: EmploiDuTempsEntry[] = [
  { id: 'e1', subject: 'Spé-SVT',           room: 'Bat A - Salle 202', teacher: 'M.DUCHEMIN',  startTime: '8h30',  color: 'green'  },
  { id: 'e2', subject: 'Art-plastique',      room: 'Bat B - Salle 102', teacher: 'Mme.LIAM',    startTime: '9h40',  color: 'pink'   },
  { id: 'e3', subject: 'Éducation sportive', room: 'Gymnase',           teacher: 'Mme.Oraiche', startTime: '10h40', color: 'orange' },
  { id: 'e4', subject: 'Francais',           room: 'Bat A - Salle 202', teacher: 'M.DUBOIS',    startTime: '11h40', color: 'blue'   },
  { id: 'e5', subject: 'Pause',                                                                  startTime: '12h35', color: 'grey'   },
  { id: 'e6', subject: 'Éducation sportive', room: 'Gymnase',           teacher: 'Mme.Oraiche', startTime: '14h',   color: 'orange' },
];

const RAW_LAST_INFOS = [
  { id: 16, title: 'latest news with a very very VERY long title with no use other than testing !!', content: 'blah blah 1', username: 'Jean Aymar', modifiedDate: '2021-03-24T16:36:05.398+02', thread: { id: 1, icon: '', title: 'News collège A' } },
  { id: 15, title: 'another info', content: 'blah blah plus ancien', username: 'Jean Aymar', modifiedDate: '2021-03-23T01:01:00.000+02', thread: { id: 2, icon: '', title: 'News Ecole B' } },
  { id: 14, title: 'older info', content: 'blah blah encore plus ancien', username: 'Jean Aymar', modifiedDate: '2021-03-22T01:01:00.000+02', thread: { id: 1, icon: '', title: 'News collège A' } },
  { id: 13, title: 'oldest info', content: 'blah blah périmé', username: 'Jean Aymar', modifiedDate: '2021-03-21T01:01:00.000+02', thread: { id: 1, icon: '', title: 'News collège A' } },
  { id: 12, title: 'fresh news', content: 'not so fresh', username: 'Jean Aymar', modifiedDate: '2021-03-20T16:36:05.398+02', thread: { id: 3, icon: '', title: 'News école C' } },
];

export const MOCK_LAST_INFOS: LastInfosProps[] = RAW_LAST_INFOS.map(
  ({ content, title, username, thread, id, modifiedDate }, index) => ({
    id,
    content:
      index === 0
        ? `${content} <img src="https://picsum.photos/id/1015/300/180" alt="img 1" />`
        : index === 1
          ? `${content} <img src="https://picsum.photos/id/1015/300/180" alt="img 1" /> <img src="https://picsum.photos/id/1016/300/180" alt="img 2" />`
          : index === 2
            ? `${content} <img src="https://picsum.photos/id/1015/300/180" alt="img 1" /> <img src="https://picsum.photos/id/1016/300/180" alt="img 2" /> <img src="https://picsum.photos/id/1024/300/180" alt="img 3" />`
            : index === 3
              ? `${content} <img src="https://picsum.photos/id/1015/300/180" alt="img 1" /> <img src="https://picsum.photos/id/1016/300/180" alt="img 2" /> <img src="https://picsum.photos/id/1024/300/180" alt="img 3" /> <img src="https://picsum.photos/id/1036/300/180" alt="img 4" />`
              : content,
    title,
    username,
    icon: thread.icon,
    threadId: thread.id,
    threadName: thread.title,
    publicationDate: modifiedDate,
    isHeadline: index === 0,
  }),
);

export const MOCK_MES_EMPRUNTS: ListWidgetItem[] = [
  {
    id: 'b1',
    icon: <IconPlaceholder label="J" bg="#e07a5f" />,
    label: "Le journal d'un dégonflé",
    sublabel: 'À rendre avant le 25/04',
    href: '#',
  },
  {
    id: 'b2',
    icon: <IconPlaceholder label="M" bg="#3d9a6d" />,
    label: 'Manuel de Mathématique',
    sublabel: 'À rendre avant le 30/04',
    href: '#',
  },
];
