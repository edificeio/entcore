import { AbsenceSettings } from '~/models/absence';
import { useAbsenceSettings, useSaveAbsenceSettings } from '~/services';

export function useAbsenceModalContainer() {
  const { data: settings, isPending: isLoadingSettings } = useAbsenceSettings();
  const { mutateAsync, isPending: isSaving } = useSaveAbsenceSettings();

  async function handleSave(payload: AbsenceSettings) {
    await mutateAsync(payload);
  }

  return {
    settings: settings ?? undefined,
    isLoadingSettings,
    isSaving,
    handleSave,
  };
}
