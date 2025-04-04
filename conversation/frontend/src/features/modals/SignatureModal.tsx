import { Button, Modal } from '@edifice.io/react';
import { useEffect } from 'react';
import { useI18n } from '~/hooks';
import { useSignatureHandlers } from './hooks/useSignatureHandlers';

export function SignatureModal() {
  const { t, common_t } = useI18n();
  const { closeModal: handleCloseModal, save: handleSaveClick } =
    useSignatureHandlers();

  const isActionPending = false;

  useEffect(() => {
    if (isActionPending === false) handleCloseModal();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isActionPending]);

  return (
    <Modal
      size="lg"
      id="modalSignature"
      isOpen={true}
      onModalClose={handleCloseModal}
    >
      <Modal.Header onModalClose={handleCloseModal}>
        {t('signature.modal.title')}
      </Modal.Header>

      <Modal.Body>
        <></>
      </Modal.Body>

      <Modal.Footer>
        <Button
          type="button"
          color="tertiary"
          variant="ghost"
          onClick={handleCloseModal}
        >
          {common_t('cancel')}
        </Button>
        <Button
          type="button"
          color="primary"
          variant="filled"
          onClick={handleSaveClick}
          isLoading={isActionPending}
          disabled={isActionPending}
        >
          {common_t('save')}
        </Button>
      </Modal.Footer>
    </Modal>
  );
}
