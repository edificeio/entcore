import { useTranslation } from 'react-i18next';
import { useAppActions, useFoldersTree, useOpenFolderModal } from '~/store';
import {
  Button,
  Checkbox,
  FormControl,
  Input,
  Label,
  Modal,
  OptionsType,
  Select,
  useEdificeClient,
} from '@edifice.io/react';
import { useCallback, useState } from 'react';

export function CreateFolderModal() {
  const { appCode } = useEdificeClient();
  const { t } = useTranslation(appCode);
  const { t: t_common } = useTranslation('common');
  const foldersTree = useFoldersTree();
  const { setOpenFolderModal } = useAppActions();
  const folderModal = useOpenFolderModal();

  const [checked, setChecked] = useState(false);
  const [folderOptions, setFolderOptions] = useState<OptionsType[]>([]);
  const [subFolderId, setSubfolderId] = useState<string | undefined>(undefined);

  // Used to regenerate folders options, and reset the Select component
  const computeFolderOptions = useCallback(
    () =>
      foldersTree.map((f) => ({
        label: f.name,
        value: f.id,
      })),
    [foldersTree],
  );

  const handleSubfolderCheckChange = useCallback(() => {
    const newValue = !checked;
    setChecked(newValue);
    if (newValue === false) {
      setFolderOptions([]);
    } else {
      setFolderOptions(computeFolderOptions());
    }
  }, [checked, computeFolderOptions]);

  const handleCloseFolderModal = () => setOpenFolderModal(null);
  const handleOptionChange = (option: OptionsType | string) =>
    setSubfolderId(typeof option === 'object' ? option.value : option);
  const handleCreateClick = () => true;

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
            <FormControl id="modalFolderNewName">
              <Label>{t('folder.new.name.label')}</Label>
              <Input
                placeholder={t('folder.new.name.placeholder')}
                size="md"
                type="text"
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
              {t_common('cancel')}
            </Button>
            <Button
              type="button"
              color="primary"
              variant="filled"
              onClick={handleCreateClick}
            >
              {t_common('create')}
            </Button>
          </Modal.Footer>
        </Modal>
      )}
    </>
  );
}
