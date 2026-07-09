import { IconClockAlert, IconNotes, IconTeacher } from '@edifice.io/react/icons';
import { IconCahierDeTexte } from '@edifice.io/react/icons/apps';
import type { ContentTitle } from '~/models/carnetDeBord';

export type CategoryColor = 'orange' | 'blue' | 'green' | 'pink';

export const CONTENT_ICONS: Record<ContentTitle, JSX.Element> = {
  'retards-absences': <IconClockAlert width={20} height={20} />,
  grades: <IconNotes width={20} height={20} />,
  diary: <IconCahierDeTexte width={20} height={20} />,
  skills: <IconTeacher width={20} height={20} />,
};

export const CONTENT_COLORS: Record<ContentTitle, CategoryColor> = {
  'retards-absences': 'orange',
  grades: 'blue',
  diary: 'green',
  skills: 'pink',
};

/** Full label, used in the widget row and the modal's active category header. */
export const CONTENT_LABELS: Record<ContentTitle, string> = {
  'retards-absences': 'Retards et absences non justifiés',
  grades: 'Notes',
  diary: 'Cahier de textes',
  skills: 'Compétences acquises',
};

/** Short label, used in the modal's tab rail. */
export const RAIL_LABELS: Record<ContentTitle, string> = {
  'retards-absences': 'Retards et absences',
  grades: 'Notes',
  diary: 'Cahier de textes',
  skills: 'Compétences',
};

export const CONTENT_EMPTY_LABELS: Record<ContentTitle, string> = {
  'retards-absences': 'Pas de retards ou absences',
  grades: 'Pas de notes',
  diary: 'Pas de devoirs',
  skills: 'Pas de compétences',
};
