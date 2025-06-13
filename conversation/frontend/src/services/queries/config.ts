import {
  queryOptions,
  useMutation,
  useQuery,
  useQueryClient,
} from '@tanstack/react-query';
import { configService } from '..';
import { SignaturePreferences } from '~/models/signature';
import { Config } from '~/config';

export const configQueryKeys = {
  all: ['config'] as const,
  global: () => [...configQueryKeys.all, 'global'] as const,
  signature: () => [...configQueryKeys.all, 'signature'] as const,
};

/**
 * Provides query options for config-related operations.
 */
export const configQueryOptions = {
  /**
   * Retrieves the global configuration.
   * @returns A configuration object.
   */
  getGlobalConfig() {
    return queryOptions({
      queryKey: configQueryKeys.global(),
      queryFn: async (): Promise<Config> => {
        const data = await configService.getGlobalConfig();
        return {
          maxDepth: data['max-depth'] ?? 2,
          recallDelayMinutes: data['recall-delay-minutes'] ?? 60,
          getVisibleStrategy: data['get-visible-strategy'] ?? 'all-at-once',
        };
      },
      staleTime: Infinity,
    });
  },

  getSignaturePreferences() {
    return queryOptions({
      queryKey: configQueryKeys.signature(),
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
      queryClient.setQueryData(configQueryKeys.signature(), preferences);
    },
  });
};
