import { useTranslation } from 'react-i18next';
import { ApplicationWrapper } from '~/components/ApplicationWrapper';
import { useApplications } from '~/services';
import { useCategoryStore } from '~/store/categoryStore';
import { getAppName } from '~/utils/get-app-name';
import { ApplicationListGrid } from './ApplicationListGrid';

export function ApplicationList() {
  const { applications, isLoading, isError } = useApplications();
  const { t } = useTranslation('common');
  const { activeCategory } = useCategoryStore();

  if (isLoading) return <div>Chargement des applications...</div>;
  if (isError || !applications)
    return <div>Erreur lors du chargement des applications.</div>;

  const sortedApps = [...applications].sort((a, b) =>
    getAppName(a, t).localeCompare(getAppName(b, t)),
  );

  if (activeCategory === 'all') {
    const internalApps = sortedApps.filter(
      (app) => app.category !== 'connector',
    );
    const externalApps = sortedApps.filter(
      (app) => app.category === 'connector',
    );

    return (
      <>
        <ApplicationListGrid applications={internalApps} />
        {externalApps.length > 0 && (
          <>
            <div className="d-flex flex-wrap gap-16 flex-column">
              <div className="w-full bg-gray-400" style={{ height: 1 }}></div>
              <h2
                className="small text-center my-8"
                style={{ fontFamily: 'Arimo' }}
              >
                {t('my.apps.services.title')}
              </h2>
            </div>
            <ApplicationListGrid applications={externalApps} />
          </>
        )}
      </>
    );
  }

  // autres catégories
  const filteredApps = sortedApps.filter(
    (app) => app.category === activeCategory,
  );

  if (filteredApps.length === 0) {
    return (
      <div className="empty-screen text-center">
        Aucune application dans la catégorie {activeCategory}.
      </div>
    );
  }

  return <ApplicationListGrid applications={filteredApps} />;
}
