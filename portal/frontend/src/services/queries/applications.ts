import { useQuery } from "@tanstack/react-query";
import { applicationsService } from "../api";
import mockData from '~/mocks/mockApplications.json';

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

  const displayedApps = query.data?.apps.filter((app) => app.display);

  return {
    applications: displayedApps,
    isLoading: query.isLoading,
    isError: query.isError,
  };
};