import { AppIcon, Button, Flex, useBookmark } from '@edifice.io/react';
import { useTranslation } from 'react-i18next';
import { WidgetCard } from '~/components/WidgetCard';

export function FavoritesWidget() {
  const { t } = useTranslation();
  const bookmarkedApps = useBookmark() ?? [];

  return (
    <WidgetCard
      title={t('homepage.widget.favorites.title', 'Favoris')}
      action={
        <Button
          color="tertiary"
          variant="ghost"
          size="sm"
          onClick={() => window.open('/welcome', '_self')}
          style={{ color: '#3030D1' }}
        >
          {t('homepage.widget.see.all', 'Voir tout')} →
        </Button>
      }
      backgroundColor="#F2F2F2"
    >
      <WidgetCard>
        <Flex gap="8" wrap="wrap">
          {bookmarkedApps.slice(0, 6).map((app) => (
            <a
              key={app.name}
              href={app.address}
              title={app.displayName}
              target={app.isExternal ? '_blank' : undefined}
              rel={app.isExternal ? 'noopener noreferrer' : undefined}
            >
              <AppIcon app={app} size="40" variant="rounded" iconFit="ratio" />
            </a>
          ))}
        </Flex>
      </WidgetCard>
    </WidgetCard>
  );
}
