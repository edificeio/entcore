import {
  Button,
  Dropdown,
  FormControl,
  Input,
  Label,
  Modal,
  Switch,
} from '@edifice.io/react';
import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { useI18n } from '~/hooks';
import { Folder } from '~/models';
import { buildTree, searchFolder } from '~/services';
import { useAppActions } from '~/store';
import { useFolderActions } from './hooks';

type FolderItem = { name: string; folder: Folder; canHaveChildren?: boolean };

function flatFolders(folders: Folder[], prefix?: string) {
  const SUB_FOLDER_INDICATOR = '>\u00A0\u00A0\u00A0\u00A0\u00A0';
  const items: FolderItem[] = [];
  folders.forEach((folder) => {
    const name = `${prefix || ''}${folder.name}`;
    const item: FolderItem = {
      name,
      folder,
      //Not more than 3 levels of folders
      canHaveChildren:
        prefix !== `${SUB_FOLDER_INDICATOR}${SUB_FOLDER_INDICATOR}`,
    };
    items.push(item);

    if (folder.subFolders) {
      const subItems = flatFolders(
        folder.subFolders,
        `${prefix || ''}${SUB_FOLDER_INDICATOR}`,
      );
      items.push(...subItems);
    }
  });
  return items;
}

export function CreateFolderModal() {
  const { t, common_t } = useI18n();
  const { setOpenedModal } = useAppActions();
  const { createFolder, isActionPending, foldersTree } = useFolderActions();
  const [checked, setChecked] = useState(false);
  const [subFolderId, setSubfolderId] = useState<string | undefined>(undefined);
  const refInputName = useRef<HTMLInputElement>(null);
  const [newFolderName, setNewFolderName] = useState('');

  useEffect(() => {
    if (isActionPending === false) setOpenedModal(undefined);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isActionPending]);

  const handleCreateClick = useCallback(
    (event: React.FormEvent<HTMLFormElement>) => {
      event.preventDefault();
      if (!newFolderName) return;

      const created = createFolder(
        refInputName.current?.value,
        checked ? subFolderId : undefined,
      );
      if (created === false) {
        refInputName.current?.focus();
      }
    },
    [checked, createFolder, subFolderId, newFolderName],
  );

  const dropdownLabel = useMemo(() => {
    if (subFolderId && foldersTree) {
      const folderNode = searchFolder(subFolderId, foldersTree);
      if (folderNode) return folderNode.folder.name;
    }
    return t('folder.new.subfolder.placeholder');
  }, [subFolderId, foldersTree, t]);

  const handleSubfolderCheckChange = useCallback(() => {
    const newValue = !checked;
    setChecked(newValue);
  }, [checked]);

  const userFolders = useMemo(() => {
    return foldersTree ? buildTree(foldersTree, 2) : null;
  }, [foldersTree]);

  useEffect(() => {
    refInputName.current?.focus();
  }, []);

  if (!userFolders) return null;

  const handleCloseFolderModal = () => setOpenedModal(undefined);
  const handleItemClick = (folderItem: FolderItem) => {
    setSubfolderId(folderItem.folder.id);
  };

  const handleNameChanged = (e: React.ChangeEvent<HTMLInputElement>) => {
    setNewFolderName(e.target.value);
  };

  const menu = foldersTree ? flatFolders(foldersTree) : [];

  return (
    <Modal
      size="sm"
      id="modalFolderNew"
      isOpen={true}
      onModalClose={handleCloseFolderModal}
    >
      <form id="modalFolderNewForm" onSubmit={handleCreateClick}>
        <Modal.Header onModalClose={handleCloseFolderModal}>
          {t('folder.new.title')}
        </Modal.Header>

        <Modal.Body>
          <FormControl id="modalFolderNew" isRequired={true}>
            <Label>{t('folder.new.name.label')}</Label>
            <Input
              ref={refInputName}
              placeholder={t('folder.new.name.placeholder')}
              size="md"
              type="text"
              onChange={handleNameChanged}
              maxLength={50}
              autoComplete="off"
            />
          </FormControl>
          {userFolders.length > 0 && (
            <>
              <div className="mt-24"></div>

              <Switch
                checked={checked}
                label={t('folder.new.subfolder.label')}
                onChange={handleSubfolderCheckChange}
              />
              <div className="mt-8"></div>
              <Dropdown block>
                <Dropdown.Trigger
                  disabled={!checked}
                  label={dropdownLabel}
                ></Dropdown.Trigger>
                <Dropdown.Menu>
                  {menu.map((item, index) => (
                    <Dropdown.Item
                      key={item.name + index}
                      onClick={() => handleItemClick(item)}
                      disabled={!item.canHaveChildren}
                    >
                      {item.name}
                    </Dropdown.Item>
                  ))}
                </Dropdown.Menu>
              </Dropdown>
            </>
          )}
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
            disabled={
              isActionPending === true ||
              !newFolderName ||
              (checked && !subFolderId)
            }
          >
            {common_t('create')}
          </Button>
        </Modal.Footer>
      </form>
    </Modal>
  );
}
