import { useTranslation } from 'react-i18next';
import { ApplicationIcon } from '~/components/ApplicationIcon';
import { useApplications } from '~/services';

export function ApplicationList() {
  const { applications, isLoading, isError } = useApplications();
  const { t } = useTranslation('common');

  if (isLoading) return <div>Chargement des applications...</div>;
  if (isError) return <div>Erreur lors du chargement des applications.</div>;

  const sortedApps = [...(applications || [])].sort((a, b) =>
    t(a.prefix.substring(1)).localeCompare(t(b.prefix.substring(1))),
  );

  const internalApps = sortedApps.filter((app) => !app.isExternal);
  const externalApps = sortedApps.filter((app) => app.isExternal);

  return (
    <>
      <div
        className="d-flex flex-wrap gap-16 justify-content-center mx-auto"
        style={{ maxWidth: 1091 }}
      >
        {internalApps.map((app) => (
          <ApplicationIcon key={app.name} data={app} />
        ))}
      </div>
      {(externalApps.length && (
        <div className="d-flex flex-wrap gap-16 flex-column">
          <div className="w-full bg-gray-400" style={{ height: 1 }}></div>
          <h2
            className="small text-center my-8"
            style={{ fontFamily: 'Arimo' }}
          >
            Autres services connectés
          </h2>
          <div
            className="d-flex flex-wrap gap-32 justify-content-center mx-auto"
            style={{ maxWidth: 1091 }}
          >
            {internalApps.map((app) => (
              <ApplicationIcon key={app.name} data={app} />
            ))}
          </div>
        </div>
      )) ||
        null}
    </>
  );
}
