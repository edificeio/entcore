import {
  Avatar,
  Flex,
  Grid,
  Heading,
  IconButton,
  useUser,
} from '@edifice.io/react';
import {
  IconSettings,
  IconUserSearch,
  IconUsers,
} from '@edifice.io/react/icons';
import { useTranslation } from 'react-i18next';
import { CreateDocumentWidget } from '~/components/CreateDocumentWidget';
import { FavoritesWidget } from '~/components/FavoritesWidget';
import { MediacentreWidget } from '../MediacentreWidget';
import { WidgetCard } from '../ui/WidgetCard';
import './WelcomeWidget.css';

const PROFILE_LABELS: Record<string, string> = {
  ENSEIGNANT: 'Enseignant.e',
  ELEVE: 'Élève',
  PERSRELELEVE: 'Parent',
  PERSEDUCNAT: 'Personnel',
  SUPERADMIN: 'Administrateur',
};

export function WelcomeWidget() {
  const { t } = useTranslation();
  const { user, avatar } = useUser();

  const firstName = user?.firstName ?? '';
  const profile = user?.type ?? '';

  return (
    <WidgetCard className="welcome-widget">
      <Grid className="welcome-widget-header mb-16 align-items-center">
        <Grid.Col sm="12" md="8" lg="8">
          <Grid className="align-items-center">
            <Grid.Col sm="2" md="2" lg="1">
              <Avatar alt={firstName} src={avatar} size="md" variant="circle" />
            </Grid.Col>
            <Grid.Col sm="10" md="10" lg="11">
              <Heading level="h2" headingStyle="h4" className="mb-0 fw-bold">
                {t('homepage.widget.welcome.greeting', 'Bonjour')} {firstName}
              </Heading>
              <span className="text-muted small">
                {t(
                  `homepage.profile.${profile.toLowerCase()}`,
                  PROFILE_LABELS[profile] ?? profile,
                )}
              </span>
            </Grid.Col>
          </Grid>
        </Grid.Col>
        <Grid.Col
          sm="12"
          md="4"
          lg="4"
          className="welcome-widget-actions d-flex align-items-center"
        >
          <Flex align="center" gap="12" className="flex-wrap">
            <IconButton
              icon={<IconSettings width={20} height={20} />}
              variant="outline"
              className="welcome-header-settings"
              aria-label={t('homepage.widget.welcome.settings', 'Paramètres')}
            />
          </Flex>
        </Grid.Col>
      </Grid>

      <Grid>
        <Grid.Col sm="12" lg="6" className="d-flex flex-column gap-16">
          <FavoritesWidget />
        </Grid.Col>
        <Grid.Col sm="12" lg="6" className="d-flex flex-column gap-16">
          <CreateDocumentWidget />
        </Grid.Col>
      </Grid>
    </WidgetCard>
  );
}
