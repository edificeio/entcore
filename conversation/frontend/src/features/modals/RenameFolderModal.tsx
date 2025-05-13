import { Button, FormControl, Input, Label, Modal } from '@edifice.io/react';
import { useCallback, useEffect, useRef, useState } from 'react';
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
  const [newFolderName, setNewFolderName] = useState('');

  const handleRenameClick = useCallback(
    (event: React.FormEvent<HTMLFormElement>) => {
      event.preventDefault();
      if (!newFolderName) return;
      const renamed = renameFolder(refInputName.current?.value || '');
      if (renamed === false) {
        refInputName.current?.focus();
      }
    },
    [renameFolder, newFolderName],
  );

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

  const handleNameChanged = (e: React.ChangeEvent<HTMLInputElement>) => {
    setNewFolderName(e.target.value);
  };
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
        <form id="modalFolderNewNameForm" onSubmit={handleRenameClick}>
          <FormControl id="modalFolderNewName" isRequired={true}>
            <Label>{t('folder.rename.name.label')}</Label>
            <Input
              ref={refInputName}
              placeholder={t('folder.rename.name.placeholder')}
              size="md"
              type="text"
              maxLength={50}
              autoComplete="off"
              onChange={handleNameChanged}
              data-testid="inputNewName"
            />
          </FormControl>
        </form>
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
          type="submit"
          color="primary"
          variant="filled"
          isLoading={isActionPending === true}
          disabled={isActionPending === true || !newFolderName}
        >
          {common_t('save')}
        </Button>
      </Modal.Footer>
    </Modal>
  );
}
