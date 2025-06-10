import { useQuery } from "@tanstack/react-query";
import { applicationsService } from "../api";
import mockData from '~/mocks/mockApplications.json';
import enhanceData from '~/config/applications-list-enhance.json';

export const useApplications = () => {
  const query = useQuery({
    queryKey: ['applications'],
    queryFn: async () => {
      if (import.meta.env.DEV) {
        return mockData;
      }
      return applicationsService.getApplications();
    },
  });

  const displayedApps = query.data?.apps
    .filter((app) => app.display !== false)
    .map((app) => {
      const enhancement = enhanceData.apps.find((e) => e.name === app.name);
      return {
        ...{ category: 'connector' },
        ...app,
        ...(enhancement || {}),
      };
    });

  return {
    applications: displayedApps,
    isLoading: query.isLoading,
    isError: query.isError,
  };
};