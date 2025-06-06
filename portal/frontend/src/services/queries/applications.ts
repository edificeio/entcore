import { useQuery } from "@tanstack/react-query";
import { applicationsService } from "../api";

export const useApplications = () => {
  const query = useQuery({
    queryKey: ['applications'],
    queryFn: () => applicationsService.getApplications(),
  });

  const displayedApps = query.data?.apps.filter((app) => app.display);

  return {
    applications: displayedApps,
    isLoading: query.isLoading,
    isError: query.isError,
  };
};