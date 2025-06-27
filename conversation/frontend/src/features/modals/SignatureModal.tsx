import { Button, Loading, Modal, Switch } from '@edifice.io/react';
import { useI18n } from '~/hooks/useI18n';
import { useSignatureHandlers } from './hooks';
import { Suspense, useCallback, useEffect, useRef, useState } from 'react';
import { useSignaturePreferences } from '~/services';
import {
  SignatureEditor,
  SignatureEditorRef,
} from '~/components/SignatureEditor/components/SignatureEditor';
import { SIGNATURE_EMPTY_CONTENT } from '~/components/SignatureEditor';

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
  const initialSignatureContent = useRef(SIGNATURE_EMPTY_CONTENT);
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
      await save({
        useSignature,
        signature: editor?.current?.getHtmlContent() as string,
      });
      handleCloseModal();
    }
    handleSave();
  }, [handleCloseModal, save, useSignature]);

  const handleLengthChange = (isValid: boolean) => {
    const currentContent = editor?.current?.getHtmlContent() as string;
    const needSignatureContentInitialization =
      initialSignatureContent.current === SIGNATURE_EMPTY_CONTENT &&
      currentContent !== SIGNATURE_EMPTY_CONTENT;

    if (needSignatureContentInitialization) {
      const isOriginalSignatureEmpty =
        preferencesQuery.data?.signature === SIGNATURE_EMPTY_CONTENT;
      if (isOriginalSignatureEmpty) {
        initialSignatureContent.current = SIGNATURE_EMPTY_CONTENT;
      } else {
        initialSignatureContent.current = currentContent;
      }
    }
    setHasContentChanged(initialSignatureContent.current !== currentContent);
    setIsLengthValid(isValid);
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
