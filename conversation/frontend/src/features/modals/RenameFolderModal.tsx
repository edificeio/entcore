import { Button, FormControl, Input, Label, Modal } from '@edifice.io/react';
import { useCallback, useEffect, useRef } from 'react';
import { useI18n } from '~/hooks';
import { searchFolder } from '~/services';
import { useAppActions, useSelectedFolders } from '~/store';
import { useFolderActions } from './hooks';
import './FolderModalInDropdown.css';

export function RenameFolderModal() {
  const { t, common_t } = useI18n();
  const { setOpenedModal } = useAppActions();
  const selectedFolders = useSelectedFolders();
  const { renameFolder, isActionPending, foldersTree } = useFolderActions();
  const refInputName = useRef<HTMLInputElement>(null);

  const selectedFolderId =
    selectedFolders.length > 0 ? selectedFolders[0].id : undefined;
  const found =
    selectedFolderId && foldersTree
      ? searchFolder(selectedFolderId, foldersTree)
      : undefined;
  const currentName = found?.folder.name || undefined;

  const handleRenameClick = useCallback(() => {
    const renamed = renameFolder(refInputName.current?.value || '');
    if (renamed === false) {
      refInputName.current?.focus();
    }
  }, [renameFolder]);

  useEffect(() => {
    if (isActionPending === false) setOpenedModal(undefined);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isActionPending]);

  useEffect(() => {
    if (refInputName.current) {
      if (currentName) refInputName.current.value = currentName;
      refInputName.current.select();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  if (!foldersTree) return <></>;

  const handleCloseFolderModal = () => setOpenedModal(undefined);

  return (
    <Modal
      size="sm"
      id="modalFolderRename"
      isOpen={true}
      onModalClose={handleCloseFolderModal}
    >
      <Modal.Header onModalClose={handleCloseFolderModal}>
        {t('folder.rename.title')}
      </Modal.Header>

      <Modal.Body>
        <FormControl id="modalFolderNewName" isRequired={true}>
          <Label>{t('folder.rename.name.label')}</Label>
          <Input
            ref={refInputName}
            placeholder={t('folder.rename.name.placeholder')}
            size="md"
            type="text"
            maxLength={50}
            autoComplete="off"
            data-testid="inputNewName"
          />
        </FormControl>
      </Modal.Body>

      <Modal.Footer>
        <Button
          type="button"
          color="tertiary"
          variant="ghost"
          onClick={handleCloseFolderModal}
        >
          {common_t('cancel')}
        </Button>
        <Button
          type="button"
          color="primary"
          variant="filled"
          onClick={handleRenameClick}
          isLoading={isActionPending === true}
          disabled={isActionPending === true}
        >
          {common_t('save')}
        </Button>
      </Modal.Footer>
    </Modal>
  );
}
