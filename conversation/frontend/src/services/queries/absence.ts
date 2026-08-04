import {
  queryOptions,
  useMutation,
  useQuery,
  useQueryClient,
} from '@tanstack/react-query';
import { AbsenceSettings, AbsenceSettingsResponse } from '~/models/absence';
import { absenceService } from '..';

export const absenceQueryKeys = {
  all: () => ['absence'] as const,
  settings: () => [...absenceQueryKeys.all(), 'settings'] as const,
};

/**
 * `{}` (no settings ever saved) becomes `null` — a query's `data` can never
 * be `undefined`, TanStack Query reserves that for "not fetched yet".
 */
function toAbsenceSettings(
  dto: Partial<AbsenceSettingsResponse>,
): AbsenceSettings | null {
  if (
    dto.enabled === undefined ||
    dto.startAt === undefined ||
    dto.endAt === undefined ||
    dto.bodyJson === undefined
  ) {
    return null;
  }
  return {
    enabled: dto.enabled,
    startAt: dto.startAt,
    endAt: dto.endAt,
    bodyJson: dto.bodyJson,
  };
}

/**
 * Provides query options for absence-related operations.
 */
export const absenceQueryOptions = {
  /**
   * Retrieves the current user's absence settings.
   * @returns the settings, or `null` if none have ever been saved.
   */
  getSettings() {
    return queryOptions({
      queryKey: absenceQueryKeys.settings(),
      queryFn: async (): Promise<AbsenceSettings | null> => {
        const data = await absenceService.getSettings();
        return toAbsenceSettings(data);
      },
      staleTime: 60 * 1000,
    });
  },
};

export const useAbsenceSettings = (enabled = true) => {
  return useQuery({ ...absenceQueryOptions.getSettings(), enabled });
};

/**
 * Hook to save absence settings (creation, modification or deactivation).
 * Optimistically updates the query cache on success.
 */
export const useSaveAbsenceSettings = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (payload: AbsenceSettings) =>
      absenceService.saveSettings(payload),
    onSuccess: (data) => {
      queryClient.setQueryData(
        absenceQueryKeys.settings(),
        toAbsenceSettings(data),
      );
    },
  });
};
