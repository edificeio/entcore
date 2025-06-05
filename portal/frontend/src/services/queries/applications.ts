import { useQuery } from "@tanstack/react-query";
import { applicationsService } from "../api";

export const useApplications = () => {
  const query = useQuery({
    queryKey: ['applications'],
    queryFn: () => applicationsService.getApplications(),
  });

  return {
    applications: query.data?.apps,
    isLoading: query.isLoading,
    isError: query.isError,
  };
};