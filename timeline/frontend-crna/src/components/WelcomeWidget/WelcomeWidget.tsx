import {
  Avatar,
  Flex,
  Grid,
  Heading,
  IconButton,
  useUser,
} from '@edifice.io/react';
import { IconSettings } from '@edifice.io/react/icons';
import { useTranslation } from 'react-i18next';
import { CreateDocumentWidget } from '~/components/CreateDocumentWidget';
import { FavoritesWidget } from '~/components/FavoritesWidget';
import { WidgetCard } from '../ui/WidgetCard';
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
}

export function WelcomeWidget({ onCreateDocumentSuccess }: WelcomeWidgetProps) {
  const { t } = useTranslation();
  const { user, avatar } = useUser();

  const firstName = user?.firstName ?? '';
  const profile = user?.type ?? '';

  return (
    <WidgetCard className="welcome-widget">
      <div className="widget-header-container">
        <div className="welcome-widget-user">
          <Avatar alt={firstName} src={avatar} size="md" variant="circle" />
          <div className="d-flex flex-column">
            <Heading level="h2" headingStyle="h4" className="mb-0 fw-bold">
              {t('homepage.widget.welcome.greeting', 'Bonjour')} {firstName}
            </Heading>
            <span className="text-muted small">
              {t(
                `homepage.profile.${profile.toLowerCase()}`,
                PROFILE_LABELS[profile] ?? profile,
              )}
            </span>
          </div>
        </div>
        <div className="widget-header-action">
          <Flex align="center" gap="12">
            <IconButton
              icon={<IconSettings width={20} height={20} />}
              variant="outline"
              className="welcome-header-settings"
              aria-label={t('homepage.widget.welcome.settings', 'Paramètres')}
            />
          </Flex>
        </div>
      </div>

      <Grid>
        <Grid.Col sm="12" lg="6" className="d-flex flex-column gap-16">
          <FavoritesWidget />
        </Grid.Col>
        <Grid.Col sm="12" lg="6" className="d-flex flex-column gap-16">
          <CreateDocumentWidget onSuccess={onCreateDocumentSuccess} />
        </Grid.Col>
      </Grid>
    </WidgetCard>
  );
}
