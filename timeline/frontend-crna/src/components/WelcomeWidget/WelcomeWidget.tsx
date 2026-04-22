import { Avatar, Button, Grid, useUser } from '@edifice.io/react';
import { useTranslation } from 'react-i18next';
import { CreateDocumentWidget } from '~/components/CreateDocumentWidget';
import { FavoritesWidget } from '~/components/FavoritesWidget';
import {
  PinnedResourcesWidget,
  type PinnedResource,
} from '~/components/PinnedResourcesWidget';
import { WidgetCard } from '../WidgetCard';

const MOOD_LABELS: Record<string, string> = {
  default: 'Neutre',
  happy: 'Joyeux.se',
  proud: 'Fier.e',
  dreamy: 'Rêveur.se',
  love: 'Amoureux.se',
  tired: 'Fatigué.e',
  angry: 'En colère',
  worried: 'Inquiet.e',
  sick: 'Malade',
  joker: 'Joueur.se',
  sad: 'Triste',
};

const PROFILE_LABELS: Record<string, string> = {
  ENSEIGNANT: 'Enseignant.e',
  ELEVE: 'Élève',
  PERSRELELEVE: 'Parent',
  PERSEDUCNAT: 'Personnel',
  SUPERADMIN: 'Administrateur',
};

const MOCK_PINNED: PinnedResource[] = [
  {
    id: '1',
    title: 'Terminale 1 Physique - Chimie',
    subtitle: 'Consulté il y a 40mn',
    href: '#',
  },
  {
    id: '2',
    title: "Introduction aux 4 forces de l'Univers",
    subtitle: 'Consulté il y a 40mn',
    href: '#',
  },
  {
    id: '3',
    title: 'Optique - Réfraction de la lumière',
    subtitle: 'Consulté il y a 40mn',
    href: '#',
  },
];

export function WelcomeWidget() {
  const { t } = useTranslation();
  const { user, avatar, userDescription } = useUser();

  const firstName = user?.firstName ?? '';
  const profile = user?.type ?? '';
  const mood = userDescription?.mood ?? 'default';

  return (
    <WidgetCard
      style={{ marginBottom: '16px', marginTop: '16px' }}
      footerAction={<Button variant="ghost">🏫 Voir mes classes</Button>}
      footerBackgroundColor="#F2F2F2"
    >
      {/* Header — avatar + prénom + rôle + humeur */}
      <Grid className="welcome-widget-header mb-16 align-items-start">
        <Grid.Col sm="3" lg="10">
          <Grid className="align-items-center">
            <Grid.Col sm="1" lg="1">
              <Avatar alt={firstName} src={avatar} size="md" variant="circle" />
            </Grid.Col>
            <Grid.Col sm="3" lg="11">
              <span className="fw-bold fs-5 d-block">
                {t('homepage.widget.welcome.greeting', 'Bonjour')} {firstName}
              </span>
              <span className="text-muted small">
                {PROFILE_LABELS[profile] ?? profile}
              </span>
            </Grid.Col>
          </Grid>
        </Grid.Col>
        <Grid.Col
          sm="1"
          lg="2"
          className="d-flex justify-content-end align-items-start"
        >
          {mood !== 'default' && (
            <span className="badge bg-light text-dark border small">
              {MOOD_LABELS[mood]}
            </span>
          )}
        </Grid.Col>
      </Grid>

      <Grid>
        <Grid.Col sm="12" lg="6" style={{padding: '.8rem'}}>
          <FavoritesWidget />
          <CreateDocumentWidget />
        </Grid.Col>
        <Grid.Col sm="12" lg="6">
          <PinnedResourcesWidget resources={MOCK_PINNED} />
        </Grid.Col>
      </Grid>
    </WidgetCard>
  );
}
