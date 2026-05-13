import { AppIcon, Button, useBookmark } from '@edifice.io/react';
import { IconExternalLink, IconStar } from '@edifice.io/react/icons';
import { useTranslation } from 'react-i18next';
import { WidgetPanel } from '../WidgetPanel';
import './FavoritesWidget.css';

export function FavoritesWidget() {
  const { t } = useTranslation();
  const bookmarkedApps = useBookmark() ?? [];

  return (
    <WidgetPanel
      title={t('homepage.widget.favorites.title', 'Favoris')}
      action={
        <Button
          color="tertiary"
          variant="ghost"
          size="sm"
          onClick={() => window.open('/welcome', '_self')}
          className="widget-action-link"
          rightIcon={<IconExternalLink />}
        >
          {t('homepage.widget.favorites.all', 'Mes applis')}
        </Button>
      }
    >
      <div className="favorites-widget-apps-card">
        {bookmarkedApps.length === 0 ? (
          <div className="favorites-widget-empty">
            <IconStar width={32} height={32} />
            <p>
              {t(
                'homepage.widget.favorites.empty',
                'Aucune application en favoris',
              )}
            </p>
          </div>
        ) : (
          <div className="favorites-widget-apps-grid">
            {bookmarkedApps.slice(0, 6).map((app) => (
              <a
                key={app.name}
                href={app.address}
                title={app.displayName}
                target={app.isExternal ? '_blank' : undefined}
                rel={app.isExternal ? 'noopener noreferrer' : undefined}
              >
                <AppIcon app={app} size="40" variant="square" />
              </a>
            ))}
          </div>
        )}
      </div>
    </WidgetPanel>
  );
}
