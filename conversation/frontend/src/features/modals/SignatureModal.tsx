import { Button, Checkbox, Loading, Modal } from '@edifice.io/react';
import { useI18n } from '~/hooks';
import { useSignatureHandlers } from './hooks/useSignatureHandlers';
import { Editor, EditorRef } from '@edifice.io/react/editor';
import { Suspense, useRef } from 'react';
import { useSignaturePreferences } from '~/services';

export function SignatureModal() {
  const { t, common_t } = useI18n();
  const preferencesQuery = useSignaturePreferences();
  const {
    isSaving,
    closeModal: handleCloseModal,
    save,
  } = useSignatureHandlers();

  const editor = useRef<EditorRef>(null);

  const isLocked =
    preferencesQuery.data?.useSignature === false ||
    preferencesQuery.isPending ||
    isSaving;

  const handleToggleChange = () => {};

  const handleSaveClick = async () => {
    await save({
      useSignature: true,
      signature: editor?.current?.getContent('html') as string,
    });
    handleCloseModal();
  };

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
        <Suspense fallback={<Loading isLoading={preferencesQuery.isPending} />}>
          <Checkbox
            label={t('signature.modal.toggle.label')}
            checked={preferencesQuery.data?.useSignature === true}
            onChange={handleToggleChange}
          />
          <p>
            <em>
              <small>{t('signature.modal.toggle.hint')}</small>
            </em>
          </p>
          <Editor
            ref={editor}
            id="signatureBody"
            content={preferencesQuery.data?.signature ?? ''}
            mode={isLocked ? 'read' : 'edit'}
            variant="ghost"
            placeholder={t('signature.modal.editor.placeholder')}
          />
        </Suspense>
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
          isLoading={isSaving}
          disabled={isLocked}
        >
          {common_t('save')}
        </Button>
      </Modal.Footer>
    </Modal>
  );
}
