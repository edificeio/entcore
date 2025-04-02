import { queryOptions, useQuery } from '@tanstack/react-query';
import { configService } from '..';

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
};

export const useGlobalConfig = () => {
  return useQuery(configQueryOptions.getGlobalConfig());
};
