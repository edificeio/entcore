import { Button, Loading, Modal, Switch, useToast } from '@edifice.io/react';
import { Suspense, useCallback, useEffect, useRef, useState } from 'react';
import {
  SignatureEditor,
  SignatureEditorRef,
} from '~/components/SignatureEditor/components/SignatureEditor';
import { useI18n } from '~/hooks/useI18n';
import { useSignaturePreferences } from '~/services';
import { useSignatureHandlers } from './hooks';

export function SignatureModal() {
  const { t, common_t } = useI18n();
  const preferencesQuery = useSignaturePreferences();
  const toast = useToast();

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
  const [hasContentChanged, setHasContentChanged] = useState(false);

  useEffect(() => {
    if (typeof preferencesQuery.data?.useSignature !== 'undefined')
      setUseSignature(preferencesQuery.data.useSignature);
  }, [preferencesQuery.data]);

  function handleToggleChange() {
    setUseSignature((previousState) => !previousState);
  }

  // Vérifier si des modifications ont été apportées
  const hasChanged = () => {
    const originalUseSignature = !!preferencesQuery.data?.useSignature;
    return useSignature !== originalUseSignature || hasContentChanged;
  };

  const lockSaveButton =
    !isLengthValid || preferencesQuery.isPending || isSaving || !hasChanged();

  const handleSaveClick = useCallback(() => {
    async function handleSave() {
      try {
        await save({
          useSignature,
          signature: editor?.current?.getHtmlContent() as string,
        });
        toast.success(t('signature.notify.saved'));
        handleCloseModal();
      } catch (error) {
        toast.error(t('signature.notify.error'));
        console.error('error:', error);
      }
    }
    handleSave();
  }, [handleCloseModal, save, useSignature]);

  const handleLengthChange = (isValid: boolean) => {
    setIsLengthValid(isValid);
  };

  const handleContentChange = () => {
    setHasContentChanged(true);
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
          <Switch
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
            onLengthChange={handleLengthChange}
            onContentChange={handleContentChange}
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
