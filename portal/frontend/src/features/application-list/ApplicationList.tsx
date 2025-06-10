import { useTranslation } from 'react-i18next';
import { ApplicationWrapper } from '~/components/ApplicationWrapper';
import { useApplications } from '~/services';
import { getAppName } from '~/utils/get-app-name';

export function ApplicationList() {
  const { applications, isLoading, isError } = useApplications();
  const { t } = useTranslation('common');

  if (isLoading) return <div>Chargement des applications...</div>;
  if (isError) return <div>Erreur lors du chargement des applications.</div>;

  const sortedApps = [...(applications || [])].sort((a, b) => {
    return getAppName(a, t).localeCompare(getAppName(b, t));
  });

  const internalApps = sortedApps.filter((app) => !app.isExternal);
  const externalApps = sortedApps.filter((app) => app.isExternal);

  return (
    <>
      <div
        className="d-flex flex-wrap gap-16 justify-content-center mx-auto"
        style={{ maxWidth: 1091 }}
      >
        {internalApps.map((app) => (
          <ApplicationWrapper key={app.name} data={app} />
        ))}
      </div>
      {(externalApps.length && (
        <div className="d-flex flex-wrap gap-16 flex-column">
          <div className="w-full bg-gray-400" style={{ height: 1 }}></div>
          <h2
            className="small text-center my-8"
            style={{ fontFamily: 'Arimo' }}
          >
            {t('my.apps.services.title')}
          </h2>
          <div
            className="d-flex flex-wrap gap-32 justify-content-center mx-auto"
            style={{ maxWidth: 1091 }}
          >
            {externalApps.map((app) => (
              <ApplicationWrapper key={app.name} data={app} />
            ))}
          </div>
        </div>
      )) ||
        null}
    </>
  );
}
