import { Avatar, Flex, Grid, Heading, IconButton, useUser } from '@edifice.io/react';
import { IconSettings, IconUserSearch, IconUsers } from '@edifice.io/react/icons';
import { useTranslation } from 'react-i18next';
import { CreateDocumentWidget } from '~/components/CreateDocumentWidget';
import { FavoritesWidget } from '~/components/FavoritesWidget';
import type { ListWidgetItem } from '../ListWidget';
import { MediacentreWidget } from '../MediacentreWidget';
import { WidgetCard } from '../WidgetCard';
import './WelcomeWidget.css';

const PROFILE_LABELS: Record<string, string> = {
  ENSEIGNANT: 'Enseignant.e',
  ELEVE: 'Élève',
  PERSRELELEVE: 'Parent',
  PERSEDUCNAT: 'Personnel',
  SUPERADMIN: 'Administrateur',
};

export interface WelcomeWidgetProps {
  mediacentreItems?: ListWidgetItem[];
  isMediacentreLoading?: boolean;
}

export function WelcomeWidget({
  mediacentreItems = [],
  isMediacentreLoading = false,
}: WelcomeWidgetProps) {
  const { t } = useTranslation();
  const { user, avatar } = useUser();

  const firstName = user?.firstName ?? '';
  const profile = user?.type ?? '';

  return (
    <WidgetCard className="welcome-widget">
      <Grid className="welcome-widget-header mb-16 align-items-start">
        <Grid.Col sm="3" md="3" lg="8">
          <Grid className="align-items-center">
            <Grid.Col sm="1" md="1" lg="1">
              <Avatar alt={firstName} src={avatar} size="md" variant="circle" />
            </Grid.Col>
            <Grid.Col sm="3" md="3" lg="11">
              <Heading level="h2" headingStyle="h4" className="mb-0 fw-bold">
                {t('homepage.widget.welcome.greeting', 'Bonjour')} {firstName}
              </Heading>
              <span className="text-muted small">
                {t(`homepage.profile.${profile.toLowerCase()}`, PROFILE_LABELS[profile] ?? profile)}
              </span>
            </Grid.Col>
          </Grid>
        </Grid.Col>
        <Grid.Col
          sm="1"
          md="1"
          lg="4"
          className="d-flex justify-content-end align-items-center"
        >
          <Flex align="center" gap="12">
            <a href="/classes" className="welcome-header-link">
              <IconUsers width={19} height={19} />
              <span>{t('homepage.widget.welcome.classes', 'Mes classes')}</span>
            </a>
            <a href="/annuaire" className="welcome-header-link">
              <IconUserSearch width={19} height={19} />
              <span>{t('homepage.widget.welcome.directory', 'Annuaire')}</span>
            </a>
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
          <CreateDocumentWidget />
        </Grid.Col>
        <Grid.Col sm="12" lg="6" className="d-flex flex-column gap-16">
          <MediacentreWidget items={mediacentreItems} isLoading={isMediacentreLoading} />
        </Grid.Col>
      </Grid>
    </WidgetCard>
  );
}
