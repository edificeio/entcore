import type { EmploiDuTempsEntry } from '~/components/EmploiDuTempsWidget';
import type { ListWidgetItem } from '~/components/ListWidget';
import type { VieScolaireChild } from '~/components/VieScolaireWidget';
// ListWidgetItem is reused for MesEmprunts

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

const SOPHIE_ENTRIES = [
  { id: 's1', type: 'retard' as const, label: 'Retards non justifiés', sublabel: 'le 08/06/2024 - 08:30' },
  { id: 's2', type: 'absence' as const, label: 'Absences non justifiées', sublabel: 'du 02/02/2024 09:00 au 02/02/2024 16:00' },
  { id: 's3', type: 'note' as const, label: 'Notes', sublabel: '16,00/20 en TECHNOLOGIE le 18/04/2024' },
  { id: 's4', type: 'cahier' as const, label: 'Cahier de textes', sublabel: 'Travail en MATHEMATIQUES pour le 12/06/2024' },
  { id: 's5', type: 'competence' as const, label: 'Compétences acquises', sublabel: 'Evaluation le 13/06/2024 en MATHEMATIQUES' },
];

const JULIEN_ENTRIES = [
  { id: 'j1', type: 'absence' as const, label: 'Absences non justifiées', sublabel: 'le 10/06/2024 - journée entière' },
  { id: 'j2', type: 'note' as const, label: 'Notes', sublabel: '12,50/20 en FRANÇAIS le 05/06/2024' },
  { id: 'j3', type: 'cahier' as const, label: 'Cahier de textes', sublabel: 'Exposé en HISTOIRE-GEO pour le 15/06/2024' },
  { id: 'j4', type: 'competence' as const, label: 'Compétences acquises', sublabel: 'Evaluation le 20/06/2024 en SCIENCES' },
];

export const MOCK_VIE_SCOLAIRE: VieScolaireChild[] = [
  { id: 'sophie', name: 'Sophie', entries: SOPHIE_ENTRIES },
  { id: 'julien', name: 'Julien', entries: JULIEN_ENTRIES },
];

export const MOCK_EMPLOI_DU_TEMPS: EmploiDuTempsEntry[] = [
  { id: 'e1', subject: 'Spé-SVT',           room: 'Bat A - Salle 202', teacher: 'M.DUCHEMIN',  startTime: '8h30',  color: 'green'  },
  { id: 'e2', subject: 'Art-plastique',      room: 'Bat B - Salle 102', teacher: 'Mme.LIAM',    startTime: '9h40',  color: 'pink'   },
  { id: 'e3', subject: 'Éducation sportive', room: 'Gymnase',           teacher: 'Mme.Oraiche', startTime: '10h40', color: 'orange' },
  { id: 'e4', subject: 'Francais',           room: 'Bat A - Salle 202', teacher: 'M.DUBOIS',    startTime: '11h40', color: 'blue'   },
  { id: 'e5', subject: 'Pause',                                                                  startTime: '12h35', color: 'grey'   },
  { id: 'e6', subject: 'Éducation sportive', room: 'Gymnase',           teacher: 'Mme.Oraiche', startTime: '14h',   color: 'orange' },
];

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
