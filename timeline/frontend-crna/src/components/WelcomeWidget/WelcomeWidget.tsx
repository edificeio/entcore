import {
  Avatar,
  ButtonBeta,
  Grid,
  Heading,
  IconButton,
  useBreakpoint,
  useUser,
} from '@edifice.io/react';
import { IconAddWidget, IconClock } from '@edifice.io/react/icons';
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
  onOpenHistory?: () => void;
  onOpenSettings?: () => void;
}

export function WelcomeWidget({
  onOpenHistory,
  onOpenSettings,
}: WelcomeWidgetProps) {
  const { t } = useTranslation('timeline');
  const { user, avatar, userDescription } = useUser();
  const { md } = useBreakpoint();

  const displayName = user?.firstName ?? '';
  const profile = user?.type ?? '';

  return (
    <HomeCard variant="user" className="welcome-widget">
      <div className="home-card-header d-flex align-items-center justify-content-between gap-8 flex-wrap">
        <div className="welcome-widget-user">
          <Avatar alt={displayName} src={avatar} size="md" variant="circle" />
          <div className="d-flex flex-column">
            <Heading level="h2" headingStyle="h4" className="mb-0 fw-bold">
              {t('homepage.crna.widget.welcome.greeting', 'Bonjour')}{' '}
              {displayName}
            </Heading>
            <span className="text-muted small">
              {t(
                `homepage.crna.profile.${profile.toLowerCase()}`,
                PROFILE_LABELS[profile] ?? profile,
              )}
            </span>
          </div>
        </div>
        <div
          className={`d-flex gap-6${md ? '' : ' flex-column align-items-start'}`}
        >
          {/* <ButtonBeta
            color="default"
            variant="ghost"
            leftIcon={<IconClock width={20} height={20} />}
            onClick={onOpenHistory}
            data-testid="welcome-flash-history-button"
          >
            {t(
              'homepage.crna.widget.welcome.history',
              'Historique des message flash',
            )}
          </ButtonBeta>
          <ButtonBeta
            color="default"
            variant="ghost"
            leftIcon={<IconSettings width={20} height={20} />}
            onClick={onOpenSettings}
          >
            {t('homepage.crna.widget.welcome.settings', 'Paramètres')}
          </ButtonBeta> */}
          {onOpenSettings && (
            <IconButton
              icon={<IconAddWidget />}
              variant="ghost"
              color="tertiary"
              aria-label={t(
                'homepage.crna.widget.welcome.customize',
                'Personnaliser mes widgets',
              )}
              title={t(
                'homepage.crna.widget.welcome.customize',
                'Personnaliser mes widgets',
              )}
              onClick={onOpenSettings}
              data-testid="welcome-customize-widgets-button"
            />
          )}
        </div>
      </div>

      <HomeCard.Content>
        <Grid>
          <Grid.Col
            sm="4"
            md="8"
            lg="8"
            xl="6"
            className="d-flex flex-column gap-16"
          >
            <FavoritesContainer />
          </Grid.Col>
          <Grid.Col
            sm="4"
            md="8"
            lg="8"
            xl="6"
            className="d-flex flex-column gap-16"
          >
            <CreateDocumentWidget />
          </Grid.Col>
        </Grid>
      </HomeCard.Content>
    </HomeCard>
  );
}
