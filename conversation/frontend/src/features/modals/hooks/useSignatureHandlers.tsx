import { SignaturePreferences } from '~/models/signature';
import { useSetSignaturePreferences } from '~/services';
import { useAppActions } from '~/store';

/**
 * Custom hook that provides logic for signature-related actions.
 */
export function useSignatureHandlers() {
  const { setOpenedModal } = useAppActions();
  const mutation = useSetSignaturePreferences();

  const closeModal = () => setOpenedModal(undefined);

  const save = (preferences: SignaturePreferences) => {
    return mutation.mutateAsync(preferences);
  };

  return {
    isSaving: mutation.isPending,
    /** Close the signature modal. */
    closeModal,
    /** Save the signature. */
    save,
  };
}
