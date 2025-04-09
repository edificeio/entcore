import {
  queryOptions,
  useMutation,
  useQuery,
  useQueryClient,
} from '@tanstack/react-query';
import { configService } from '..';
import { SignaturePreferences } from '~/models/signature';

/**
 * Provides query options for config-related operations.
 */
export const configQueryOptions = {
  /**
   * Base query key for config-related queries.
   */
  base: ['config'] as const,

  /**
   * Retrieves the global configuration.
   * @returns A configuration object.
   */
  getGlobalConfig() {
    return queryOptions({
      queryKey: [...configQueryOptions.base, 'global'] as const,
      queryFn: async () => {
        const data = await configService.getGlobalConfig();
        return {
          maxDepth: data['max-depth'] ?? 2,
          recallDelayMinutes: data['recall-delay-minutes'] ?? 60,
        };
      },
      staleTime: Infinity,
    });
  },

  getSignaturePreferences() {
    return queryOptions({
      queryKey: [...configQueryOptions.base, 'signature'] as const,
      queryFn: async () => {
        const data = await configService.getSignaturePreferences();
        return data;
      },
      staleTime: 15 * 60 * 1000,
    });
  },
};

export const useGlobalConfig = () => {
  return useQuery(configQueryOptions.getGlobalConfig());
};

export const useSignaturePreferences = () => {
  return useQuery(configQueryOptions.getSignaturePreferences());
};

/**
 * Hook to save signature preferences.
 */
export const useSetSignaturePreferences = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (preferences: SignaturePreferences) =>
      configService.setSignaturePreferences(preferences),
    onSuccess: async (_unused, preferences) => {
      // Optimistic update
      queryClient.setQueryData(
        configQueryOptions.getSignaturePreferences().queryKey,
        preferences,
      );
    },
  });
};
