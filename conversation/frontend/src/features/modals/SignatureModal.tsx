import { Button, Checkbox, Loading, Modal } from '@edifice.io/react';
import { useI18n } from '~/hooks';
import { useSignatureHandlers } from './hooks';
import { Suspense, useCallback, useEffect, useRef, useState } from 'react';
import { useSignaturePreferences } from '~/services';
import {
  SignatureEditor,
  SignatureEditorRef,
} from '~/components/SignatureEditor/components/SignatureEditor';

export function SignatureModal() {
  const { t, common_t } = useI18n();
  const preferencesQuery = useSignaturePreferences();
  const {
    isSaving,
    closeModal: handleCloseModal,
    save,
  } = useSignatureHandlers();

  const editor = useRef<SignatureEditorRef>(null);

  const [useSignature, setUseSignature] = useState(
    preferencesQuery.data?.useSignature ?? false,
  );
  const [isLengthValid, setIsLengthValid] = useState(true);

  useEffect(() => {
    if (typeof preferencesQuery.data?.useSignature !== 'undefined')
      setUseSignature(preferencesQuery.data.useSignature);
  }, [preferencesQuery.data]);

  function handleToggleChange() {
    setUseSignature((previousState) => !previousState);
  }

  const lockSaveButton =
    !isLengthValid || preferencesQuery.isPending || isSaving;

  const handleSaveClick = useCallback(() => {
    async function handleSave() {
      await save({
        useSignature,
        signature: editor?.current?.getHtmlContent() as string,
      });
      handleCloseModal();
    }
    handleSave();
  }, [handleCloseModal, save, useSignature]);

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
            checked={useSignature}
            onChange={handleToggleChange}
          />
          <p className="my-8 py-4">
            <em>
              <small>{t('signature.modal.toggle.hint')}</small>
            </em>
          </p>
          <SignatureEditor
            ref={editor}
            content={preferencesQuery.data?.signature ?? ''}
            mode={'edit'}
            placeholder={t('signature.modal.editor.placeholder')}
            onLengthChange={setIsLengthValid}
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
          disabled={lockSaveButton}
        >
          {common_t('save')}
        </Button>
      </Modal.Footer>
    </Modal>
  );
}
