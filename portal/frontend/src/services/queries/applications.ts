import { useQuery } from '@tanstack/react-query';
import { applicationsService } from '../api';
import mockData from '~/mocks/mockApplications.json';
import enhanceData from '~/config/applications-list-enhance.json';
import { getAppName } from '~/utils/get-app-name';
import { useTranslation } from 'react-i18next';
import { useUserPreferencesStore } from '~/store/userPreferencesStore';

export const useApplications = () => {
  const { t } = useTranslation('common');

  const query = useQuery({
    queryKey: ['applications'],
    queryFn: async () => {
      if (import.meta.env.DEV) {
        return mockData;
      }
      return applicationsService.getApplications();
    },
  });

  const bookmarks = useUserPreferencesStore((s) => s.bookmarks ?? []);

  const displayedApps = query.data?.apps
    .map((app) => {
      const enhancement = enhanceData.apps.find((e) => e.name === app.name);
      const isFavorite = bookmarks.includes(app.name);
      return {
        ...{ appName: getAppName(app, t), category: 'connector' },
        ...app,
        ...(enhancement || {}),
        isFavorite,
      };
    })
    .filter((app) => app.display !== false);

  return {
    applications: displayedApps,
    isLoading: query.isLoading,
    isError: query.isError,
  };
};
