import { ApplicationWrapper } from '~/components/ApplicationWrapper';
import { Application } from '~/models/application';
import { useApplications } from '~/services';
import { useUpdateUserPreferences } from '~/services/queries/preferences';
import { useUserPreferencesStore } from '~/store/userPreferencesStore';

export function ApplicationListGrid({
  applications,
  isConnectors,
}: {
  applications: Application[];
  isConnectors?: boolean;
}) {
  const { applications: displayedApps } = useApplications();
  const { toggleBookmark } = useUserPreferencesStore();
  const updatePreferences = useUpdateUserPreferences();

  const dataId = isConnectors
    ? 'applications-list-connectors'
    : 'applications-list';

  const handleToggleFavorite = (appName: string) => {
    toggleBookmark(appName);

    updatePreferences.mutate({
      bookmarks: useUserPreferencesStore.getState().bookmarks,
      applications: displayedApps?.map((app) => app.name) ?? [],
    });
  };
  return (
    <div
      data-id={dataId}
      className="d-flex flex-wrap gap-16 justify-content-center mx-auto"
      style={{ maxWidth: 1091 }}
    >
      {applications.map((application) => (
        <ApplicationWrapper
          key={application.name}
          data={application}
          onToggleFavorite={handleToggleFavorite}
        />
      ))}
    </div>
  );
}
