import { useQuery } from '@tanstack/react-query';
import { useMemo } from 'react';
import { applicationsService } from '../api';
import mockData from '~/mocks/mockApplications.json';
import enhanceData from '~/mocks/applications-list-enhance.json';
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

  const myAppsConfig = useQuery({
    queryKey: ['applicationsConfig'],
    queryFn: async () => {
      if (import.meta.env.DEV) {
        return enhanceData;
      }
      return applicationsService.getApplicationConfig();
    },
  });

  const bookmarks = useUserPreferencesStore((s) => s.bookmarks ?? []);

  const displayedApps = useMemo(() => {
    return query.data?.apps
      .map((app) => {
        var enhancement = myAppsConfig.data?.find((e) => e.name === app.name);

        const isLibrary =
          app.address?.includes('library.edifice.io') && !enhancement?.category;

        if (isLibrary) {
          const enhancementOriginal = myAppsConfig.data?.find(
            (e) => e.name === 'library-info',
          );

          if (enhancementOriginal) {
            enhancement = {
              ...enhancementOriginal,
              name: app.name,
            };
          }
        }

        const isFavorite = bookmarks.includes(app.name);

        return {
          ...app,
          ...{ appName: getAppName(app, t), category: 'connector' },
          ...(enhancement || {}),
          isFavorite,
        };
      })
      .filter((app) => app.display !== false);
  }, [query.data?.apps, myAppsConfig.data, bookmarks, t]);

  return {
    applications: displayedApps,
    isLoading: query.isLoading,
    isError: query.isError,
  };
};
