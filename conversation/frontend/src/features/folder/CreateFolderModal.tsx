import { useAppActions, useOpenFolderModal } from '~/store';
import {
  Button,
  Checkbox,
  FormControl,
  Input,
  Label,
  Modal,
  OptionsType,
  Select,
} from '@edifice.io/react';
import { useCallback, useEffect, useRef, useState } from 'react';
import { useFolderActions, useI18n } from '~/hooks';

export function CreateFolderModal() {
  const { t, common_t } = useI18n();
  const { setOpenFolderModal } = useAppActions();
  const { createFolder, isActionPending } = useFolderActions();
  const folderModal = useOpenFolderModal();
  const { foldersTree } = useFolderActions();
  const [checked, setChecked] = useState(false);
  const [subFolderId, setSubfolderId] = useState<string | undefined>(undefined);
  const refInputName = useRef<HTMLInputElement>(null);

  useEffect(() => {
    if (isActionPending === false) setOpenFolderModal(null);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isActionPending]);

  const handleCreateClick = useCallback(() => {
    const created = createFolder(
      refInputName.current?.value,
      checked ? subFolderId : undefined,
    );
    if (created === false) {
      refInputName.current?.focus();
    }
  }, [checked, createFolder, subFolderId]);

  const handleSubfolderCheckChange = useCallback(() => {
    const newValue = !checked;
    setChecked(newValue);
  }, [checked]);

  const handleCloseFolderModal = () => setOpenFolderModal(null);

  const handleOptionChange = (option: OptionsType | string) =>
    setSubfolderId(typeof option === 'object' ? option.value : option);

  const folderOptions = foldersTree.map((f) => ({
    label: f.name,
    value: f.id,
  }));

  return (
    <>
      {folderModal === 'create' && (
        <Modal
          size="sm"
          id="modalFolderNew"
          isOpen={true}
          onModalClose={handleCloseFolderModal}
        >
          <Modal.Header onModalClose={handleCloseFolderModal}>
            {t('folder.new.title')}
          </Modal.Header>

          <Modal.Body>
            <FormControl id="modalFolderNewName" isRequired={true}>
              <Label>{t('folder.new.name.label')}</Label>
              <Input
                ref={refInputName}
                placeholder={t('folder.new.name.placeholder')}
                size="md"
                type="text"
                maxLength={50}
              />
            </FormControl>
            <div className="mt-24"></div>
            <Checkbox
              checked={checked}
              label={t('folder.new.subfolder.label')}
              onChange={handleSubfolderCheckChange}
            />
            <div className="mt-8"></div>
            <Select
              disabled={!checked}
              size="md"
              placeholderOption={t('folder.new.subfolder.placeholder')}
              overflow={true}
              block={true}
              options={folderOptions}
              onValueChange={handleOptionChange}
            />
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
              onClick={handleCreateClick}
              disabled={isActionPending === true}
            >
              {common_t('create')}
            </Button>
          </Modal.Footer>
        </Modal>
      )}
    </>
  );
}
