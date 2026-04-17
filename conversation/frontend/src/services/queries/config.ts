import { odeServices } from '@edifice.io/client';
import {
  queryOptions,
  useMutation,
  useQuery,
  useQueryClient,
} from '@tanstack/react-query';
import { Config } from '~/config';
import { PublicConf } from '~/models/publicConf';
import { SignaturePreferences } from '~/models/signature';
import { configService } from '..';

export const configQueryKeys = {
  all: () => ['config'] as const,
  global: () => [...configQueryKeys.all(), 'global'] as const,
  signature: () => [...configQueryKeys.all(), 'signature'] as const,
  publicConfig: () => [...configQueryKeys.all(), 'publicConfig'] as const,
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

  /**
   * @returns Query options for fetching the public configuration, including the Screeb app ID. The query is cached indefinitely since config values are not expected to change frequently.
   */
  getPublicConfig() {
    return queryOptions({
      queryKey: configQueryKeys.publicConfig(),
      queryFn: (): Promise<PublicConf> =>
        odeServices.conf().getPublicConf('actualites'),
      staleTime: Infinity,
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

export const usePublicConfig = () => {
  return useQuery(configQueryOptions.getPublicConfig());
};
