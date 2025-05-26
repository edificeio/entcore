import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { preferencesService } from '../api';

export const useUserPreferences = () => {
  return useQuery({
    queryKey: ['user-preferences'],
    queryFn: async () => {
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
