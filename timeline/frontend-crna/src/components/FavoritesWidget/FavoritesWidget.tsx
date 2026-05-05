import { AppIcon, Button, Flex, useBookmark } from '@edifice.io/react';
import { useTranslation } from 'react-i18next';
import { WidgetCard } from '~/components/WidgetCard';
import './FavoritesWidget.css';

export function FavoritesWidget() {
  const { t } = useTranslation();
  const bookmarkedApps = useBookmark() ?? [];

  return (
    <WidgetCard
      className="favorites-widget"
      title={t('homepage.widget.favorites.title', 'Favoris')}
      action={
        <Button
          color="tertiary"
          variant="ghost"
          size="sm"
          onClick={() => window.open('/welcome', '_self')}
          className="voir-tout"
        >
          {t('homepage.widget.see.all', 'Voir tout')} →
        </Button>
      }
      backgroundColor="#f7f7f7"
    >
      <Flex gap="16" wrap="wrap">
        {bookmarkedApps.slice(0, 6).map((app) => (
          <a
            key={app.name}
            href={app.address}
            title={app.displayName}
            target={app.isExternal ? '_blank' : undefined}
            rel={app.isExternal ? 'noopener noreferrer' : undefined}
          >
            <AppIcon
              app={app}
              size="48"
              variant="circle"
              iconFit="ratio"
            />
          </a>
        ))}
      </Flex>
    </WidgetCard>
  );
}
