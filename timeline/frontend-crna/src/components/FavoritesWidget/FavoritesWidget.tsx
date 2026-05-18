import { AppIcon, ButtonBeta, useBookmark } from '@edifice.io/react';
import { IconArrowRight, IconStarFull } from '@edifice.io/react/icons';
import { useTranslation } from 'react-i18next';
import { WidgetPanel } from '../ui/WidgetPanel';
import './FavoritesWidget.css';

export function FavoritesWidget() {
  const { t } = useTranslation();
  const bookmarkedApps = useBookmark() ?? [];

  return (
    <WidgetPanel
      title={t('homepage.widget.favorites.title', 'Favoris')}
      action={
        <ButtonBeta
          color="default"
          variant="ghost"
          onClick={() => window.open('/welcome', '_self')}
          rightIcon={<IconArrowRight />}
        >
          {t('homepage.widget.favorites.all', 'Mes applis')}
        </ButtonBeta>
      }
    >
      <div className="favorites-widget-apps-card">
        {bookmarkedApps.length === 0 ? (
          <div className="favorites-widget-empty">
            <IconStarFull width={32} height={32} color="#FFD422" />
            <p>
              {t(
                'homepage.widget.favorites.empty',
                'Ajouter des applications à vos favoris pour les retrouver içi et y accéder rapidement !',
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
