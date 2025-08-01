import {
  Button,
  Dropdown,
  FormControl,
  FormText,
  Input,
  Label,
  Modal,
  Switch,
  useToast,
} from '@edifice.io/react';
import { IconFolder } from '@edifice.io/react/icons';
import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { useI18n } from '~/hooks/useI18n';
import { Folder } from '~/models';
import { buildTree, searchFolder } from '~/services';
import { useActionsStore } from '~/store/actions';
import { useFolderActions } from './hooks';

/**
 * Custom typing of a TreeItem exposing a user folder
 * @param name The name of the folder
 * @param folder The folder
 * @returns FolderItem[]
 */
type FolderItem = { name: string; folder: Folder };

function flatFolders(folders: Folder[], prefix?: string) {
  const items: FolderItem[] = [];
  folders.forEach((folder) => {
    const name = `${prefix || ''}${folder.name}`;
    const item: FolderItem = { name, folder };
    items.push(item);
    const isOnSecondFolderLevel = !!prefix;
    if (folder.subFolders && !isOnSecondFolderLevel) {
      const parentFolderIndicator = `${name || ''} / `;
      const subItems = flatFolders(folder.subFolders, parentFolderIndicator);
      items.push(...subItems);
    }
  });
  return items;
}

export function CreateFolderModal() {
  const { t, common_t } = useI18n();
  const { error } = useToast();
  const setOpenedModal = useActionsStore.use.setOpenedModal();
  const openedModal = useActionsStore.use.openedModal();
  const { createFolder, isActionPending, foldersTree } = useFolderActions();
  const [checked, setChecked] = useState(false);
  const [subFolderId, setSubfolderId] = useState<string | undefined>(undefined);
  const refInputName = useRef<HTMLInputElement>(null);
  const [newFolderName, setNewFolderName] = useState('');
  const [nameError, setNameError] = useState<string | undefined>(undefined);
  const { maxDepth } = useActionsStore.use.config();

  const handleCreateClick = useCallback(
    (event: React.FormEvent<HTMLFormElement>) => {
      event.preventDefault();
      if (!newFolderName) return;

      try {
        const created = createFolder(
          refInputName.current?.value,
          checked ? subFolderId : undefined,
        );
        if (created === false) {
          refInputName.current?.focus();
        }
      } catch (err) {
        if (err instanceof Error) {
          setNameError(err.message);
        } else {
          error(t('conversation.error.new.folder'));
        }
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
    return foldersTree ? buildTree(foldersTree, maxDepth) : null;
  }, [foldersTree, maxDepth]);

  /**
   * When the create action is no longer pending, close the modal.
   * If the modal was opened from the "create-then-move" action, switch to the "move-message" modal.
   * This is to allow the user to move a message to a newly created folder.
   */
  useEffect(() => {
    if (isActionPending === false) {
      setOpenedModal(
        openedModal === 'create-then-move' ? 'move-message' : undefined,
      );
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isActionPending]);

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
    setNameError(undefined);
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

        <Modal.Body className={'d-flex flex-column gap-24'}>
          <FormControl
            id="modalFolderNew"
            isRequired={true}
            status={nameError ? 'invalid' : undefined}
          >
            <Label>{t('folder.new.name.label')}</Label>
            <Input
              ref={refInputName}
              placeholder={t('folder.new.name.placeholder')}
              size="md"
              type="text"
              onChange={handleNameChanged}
              maxLength={50}
              showCounter
              autoComplete="off"
            />
            {nameError && <FormText>{nameError}</FormText>}
          </FormControl>
          {userFolders.length > 0 && (
            <div className="d-flex flex-column gap-8">
              <Switch
                checked={checked}
                label={t('folder.new.subfolder.label')}
                onChange={handleSubfolderCheckChange}
              />
              <div className="position-relative">
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
                        icon={<IconFolder />}
                      >
                        {item.name}
                      </Dropdown.Item>
                    ))}
                  </Dropdown.Menu>
                </Dropdown>
              </div>
            </div>
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
