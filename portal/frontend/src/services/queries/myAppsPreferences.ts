import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { createMyAppsPreferencesService } from '../api/myAppsPreferencesService';

const myAppsPreferencesService = createMyAppsPreferencesService();

export const useMyAppsPreferences = () => {
  return useQuery({
    queryKey: ['my-apps-preferences'],
    queryFn: async () => {
      return myAppsPreferencesService.getMyAppsPreferences();
    },
  });
};

export const useUpdateMyAppsPreferences = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: myAppsPreferencesService.updateMyAppsPreferences,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['my-apps-preferences'] });
    },
  });
};
