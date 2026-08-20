import { Avatar, ButtonBeta, Grid, Heading, useBreakpoint, useUser } from '@edifice.io/react';
import { IconClock, IconSettings } from '@edifice.io/react/icons';
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
  onOpenHistory?: () => void;
}

export function WelcomeWidget({
  onCreateDocumentSuccess,
  onOpenSettings,
  onOpenHistory,
}: WelcomeWidgetProps) {
  const { t } = useTranslation('timeline');
  const { user, avatar } = useUser();
  const { md } = useBreakpoint();

  const firstName = user?.firstName ?? '';
  const profile = user?.type ?? '';

  return (
    <HomeCard variant="user" className="welcome-widget">
      <div className="home-card-header d-flex align-items-center justify-content-between gap-8 flex-wrap">
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
        <div className={`d-flex gap-8${md ? '' : ' flex-column align-items-start'}`}>
          <ButtonBeta
            color="default"
            variant="ghost"
            leftIcon={<IconClock width={20} height={20} />}
            onClick={onOpenHistory}
          >
            {t('homepage.crna.widget.welcome.history', 'Historique des message flash')}
          </ButtonBeta>
          <ButtonBeta
            color="default"
            variant="ghost"
            leftIcon={<IconSettings width={20} height={20} />}
            onClick={onOpenSettings}
          >
            {t('homepage.crna.widget.welcome.settings', 'Paramètres')}
          </ButtonBeta>
        </div>
      </div>

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
