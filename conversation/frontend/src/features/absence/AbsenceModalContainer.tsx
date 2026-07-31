import { AbsenceModal } from './AbsenceModal';
import { useAbsenceModalContainer } from './hooks';

export interface AbsenceModalContainerProps {
  isOpen: boolean;
  onModalClose: () => void;
}

export function AbsenceModalContainer({
  isOpen,
  onModalClose,
}: AbsenceModalContainerProps) {
  const { settings, isLoadingSettings, isSaving, handleSave } =
    useAbsenceModalContainer();

  // `useAbsenceModal` reads `settings` only once, as `useForm`'s
  // `defaultValues` — mounting `AbsenceModal` before the fetch settles would
  // pre-fill it with `undefined` and never catch up once the data arrives.
  if (isOpen && isLoadingSettings) {
    return null;
  }

  return (
    <AbsenceModal
      isOpen={isOpen}
      onModalClose={onModalClose}
      settings={settings}
      isSaving={isSaving}
      onSave={handleSave}
    />
  );
}
