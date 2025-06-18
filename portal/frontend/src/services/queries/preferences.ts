import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { preferencesService } from '../api';
import mockPreferences from '~/mocks/mockUserPreferences.json';


export const useUserPreferences = () => {
  return useQuery({
    queryKey: ['user-preferences'],
    queryFn: async () => {
      if (import.meta.env.DEV) {
        return mockPreferences;
      }
      return preferencesService.getUserPreferences();
    },
  });
};

export const useUpdateUserPreferences = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: preferencesService.updateUserPreferences,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['user-preferences'] });
    },
  });
};
