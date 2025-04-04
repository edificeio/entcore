import { useAppActions } from '~/store';

/**
 * Custom hook that provides handlers for signature-related actions.
 */
export function useSignatureHandlers() {
  const { setOpenedModal /*setSelectedFolders*/ } = useAppActions();

  const closeModal = () => setOpenedModal(undefined);

  const save = () => {
    alert('todo');
  };

  return {
    /** Close the signature modal. */
    closeModal,
    /** Save the signature. */
    save,
  };
}
