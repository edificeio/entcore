import { IQuotaAndUsage } from '@edifice.io/client';
import { useEdificeClient } from '@edifice.io/react';

/**
 * Custom hook to retrieve the used space and quota information.
 *
 * This hook uses the `useEdificeClient` hook to get the client instance and session query.
 * If the client is not initialized, it returns default values for usage and quota.
 * Otherwise, it extracts the quota and usage information from the session query data.
 *
 * @returns An object containing the `usage` and `quota` values.
 * @property {number} usage - The amount of space used, in bytes.
 * @property {number} quota - The total quota available, in bytes.
 */
export function useUsedSpace() {
  const { init, sessionQuery } = useEdificeClient();

  if (!init || !sessionQuery?.data) {
    return {
      usage: 0,
      quota: 0,
    };
  }
  const quotaAndUsage = sessionQuery.data.quotaAndUsage as IQuotaAndUsage;
  return {
    usage: quotaAndUsage.storage,
    quota: quotaAndUsage.quota,
  };
}
