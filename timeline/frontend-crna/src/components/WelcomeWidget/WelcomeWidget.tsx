import { Avatar, Grid, Heading, useUser } from '@edifice.io/react';
import { IconSettings } from '@edifice.io/react/icons';
import { FavoritesContainer, HomeCard } from '@edifice.io/react/homepage';
import { useTranslation } from 'react-i18next';
import { CreateDocumentWidget } from '~/components/CreateDocumentWidget';
import './WelcomeWidget.css';

const PROFILE_LABELS: Record<string, string> = {
  ENSEIGNANT: 'Enseignant.e',
  ELEVE: 'Élève',
  PERSRELELEVE: 'Parent',
  PERSEDUCNAT: 'Personnel',
  SUPERADMIN: 'Administrateur',
};

interface WelcomeWidgetProps {
  onCreateDocumentSuccess?: (message: string) => void;
  onOpenSettings?: () => void;
}

export function WelcomeWidget({ onCreateDocumentSuccess, onOpenSettings }: WelcomeWidgetProps) {
  const { t } = useTranslation();
  const { user, avatar } = useUser();

  const firstName = user?.firstName ?? '';
  const profile = user?.type ?? '';

  return (
    <HomeCard variant="user" className="welcome-widget">
      <HomeCard.Header
        title={
          <div className="welcome-widget-user">
            <Avatar alt={firstName} src={avatar} size="md" variant="circle" />
            <div className="d-flex flex-column">
              <Heading level="h2" headingStyle="h4" className="mb-0 fw-bold">
                {t('homepage.crna.widget.welcome.greeting', 'Bonjour')} {firstName}
              </Heading>
              <span className="text-muted small">
                {t(
                  `homepage.crna.profile.${profile.toLowerCase()}`,
                  PROFILE_LABELS[profile] ?? profile,
                )}
              </span>
            </div>
          </div>
        }
        actionLabel={t('homepage.crna.widget.welcome.settings', 'Paramètres')}
        actionLeftIcon={<IconSettings width={20} height={20} />}
        onActionClick={onOpenSettings}
      />

      <Grid>
        <Grid.Col sm="12" lg="6" className="d-flex flex-column gap-16">
          <FavoritesContainer />
        </Grid.Col>
        <Grid.Col sm="12" lg="6" className="d-flex flex-column gap-16">
          <CreateDocumentWidget onSuccess={onCreateDocumentSuccess} />
        </Grid.Col>
      </Grid>
    </HomeCard>
  );
}
