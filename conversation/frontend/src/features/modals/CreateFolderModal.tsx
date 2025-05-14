import {
  Button,
  Dropdown,
  FormControl,
  Input,
  Label,
  Modal,
  Switch,
  Tree,
  TreeItem,
} from '@edifice.io/react';
import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { useI18n } from '~/hooks';
import { buildTree, searchFolder } from '~/services';
import { useAppActions } from '~/store';
import { useFolderActions } from './hooks';

export function CreateFolderModal() {
  const { t, common_t } = useI18n();
  const { setOpenedModal } = useAppActions();
  const { createFolder, isActionPending, foldersTree } = useFolderActions();
  const [checked, setChecked] = useState(false);
  const [subFolderId, setSubfolderId] = useState<string | undefined>(undefined);
  const refInputName = useRef<HTMLInputElement>(null);
  const refDropdownTrigger = useRef<HTMLButtonElement>(null);
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

  // Render a user's folder, to be used in a Tree
  const renderFolderTreeItem = ({
    node,
  }: {
    node: TreeItem;
    hasChildren?: boolean;
    isChild?: boolean;
  }) => <span>{node.name}</span>;

  const handleCloseFolderModal = () => setOpenedModal(undefined);
  const handleTreeItemClick = (folderId: string) => {
    setSubfolderId(folderId);
    // Close dropdown
    refDropdownTrigger.current?.click();
  };

  const handleNameChanged = (e: React.ChangeEvent<HTMLInputElement>) => {
    setNewFolderName(e.target.value);
  };

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
                  ref={refDropdownTrigger}
                  disabled={!checked}
                  label={dropdownLabel}
                ></Dropdown.Trigger>
                <Dropdown.Menu>
                  <Tree
                    nodes={userFolders}
                    onTreeItemClick={handleTreeItemClick}
                    renderNode={renderFolderTreeItem}
                    selectedNodeId={subFolderId}
                    shouldExpandAllNodes={true}
                    showIcon={false}
                  />
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
            disabled={isActionPending === true || !newFolderName}
          >
            {common_t('create')}
          </Button>
        </Modal.Footer>
      </form>
    </Modal>
  );
}
