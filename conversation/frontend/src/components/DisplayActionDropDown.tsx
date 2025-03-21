import {
  Button,
  Dropdown,
  IconButton,
  IconButtonProps,
  useEdificeClient,
  useToast,
} from '@edifice.io/react';
import {
  IconDelete,
  IconFolderDelete,
  IconOptions,
  IconPrint,
  IconRedo,
  IconRestore,
  IconSave,
  IconSend,
  IconUndo,
  IconUndoAll,
  IconUnreadMail,
} from '@edifice.io/react/icons';
import { RefAttributes, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { useI18n, useSelectedFolder } from '~/hooks';
import { Message } from '~/models';
import {
  isInRecipient,
  useCreateOrUpdateDraft,
  useDeleteMessage,
  useMarkUnread,
  useMoveMessage,
  useRestoreMessage,
  useTrashMessage,
} from '~/services';
import { useConfirmModalStore } from '~/store';

export interface DisplayActionDropDownProps {
  message: Message;
  appearance?: {
    variant?: 'outline' | 'ghost';
    btnColor?: 'tertiary' | 'primary';
  };
  actions?: string[];
}

export function DisplayActionDropDown({
  message,
  actions,
  appearance = {
    variant: 'outline',
    btnColor: 'primary',
  },
}: DisplayActionDropDownProps) {
  const { t } = useI18n();
  const markAsUnreadQuery = useMarkUnread();
  const navigate = useNavigate();
  const { openModal } = useConfirmModalStore();
  const deleteMessage = useDeleteMessage();
  const restoreQuery = useRestoreMessage();
  const moveToTrashQuery = useTrashMessage();
  const createOrUpdateDraft = useCreateOrUpdateDraft();
  const moveMessage = useMoveMessage();

  const { folderId } = useSelectedFolder();
  const { user } = useEdificeClient();
  const { success } = useToast();

  const isInFolder = useMemo(() => {
    if (folderId && ['trash', 'inbox', 'outbox', 'draft'].includes(folderId))
      return;
    return true;
  }, [folderId]);

  const buttonAction = [
    {
      label: t('reply'),
      id: 'reply',
      icon: <IconUndo />,
      action: () => {
        alert('reply');
      },
      hidden: message.state === 'DRAFT' || message.trashed,
    },
    {
      label: t('submit'),
      id: 'submit',
      icon: <IconSend />,
      action: () => {
        alert('submit');
        console.log('submit', message);
      },
      hidden: message.state !== 'DRAFT' || message.trashed,
    },
    {
      label: t('restore'),
      id: 'restore',
      icon: <IconRestore />,
      action: () => {
        restoreQuery.mutate({ id: message.id });
        navigate('/trash');
      },
      hidden: !message.trashed,
    },
  ];

  const canReplyAll = useMemo(() => {
    const { to, cc, cci } = message;

    // It's message to myself with cci
    const isMeWithCci =
      to.users.length === 1 &&
      to.users[0].id === user?.userId &&
      (cci?.groups.length || cci?.users.length);

    // Count number of recipients
    const hasRecipients =
      to.users.length + to.groups.length + cc.users.length + cc.groups.length >
      1;

    return (
      (isMeWithCci || hasRecipients) &&
      message.state !== 'DRAFT' &&
      !message.trashed
    );
  }, [message, user]);

  const canMarkUnread = useMemo(() => {
    return (
      message.state !== 'DRAFT' &&
      !message.trashed &&
      !['draft', 'outbox', 'trash'].includes(folderId!) &&
      isInRecipient(message, user!.userId)
    );
  }, [message, folderId, user]);

  const handleDeleteClick = () => {
    openModal({
      id: 'delete-modal',
      header: <>{t('delete.definitely')}</>,
      body: <p>{t('delete.definitely.confirm')}</p>,
      okText: t('confirm'),
      koText: t('cancel'),
      onSuccess: () => {
        deleteMessage.mutate({ id: message.id });
        navigate('/trash');
      },
    });
  };

  const handleDraftSaveClick = () => {
    createOrUpdateDraft();
  };

  const handleMarkAsUnreadClick = () => {
    markAsUnreadQuery.mutate({ messages: [message] });
    navigate(`../..`, { relative: 'path' });
  };

  const hasActionsList = (idAction: string) => {
    return !actions || actions.includes(idAction);
  };

  const handleRemoveFromFolder = () => {
    openModal({
      id: 'remove-from-folder-modal',
      header: <>{t('remove.from.folder')}</>,
      body: <p>{t('remove.from.folder.confirm')}</p>,
      okText: t('confirm'),
      koText: t('cancel'),
      onSuccess: async () => {
        moveMessage.mutate(
          {
            folderId: 'inbox',
            id: message.id,
          },
          {
            onSuccess: () => {
              navigate(`/folder/${folderId}`);
              success(t('message.remove.from.folder'));
            },
          },
        );
      },
    });
  };

  const options = [
    {
      label: t('replyall'),
      id: 'replyall',
      icon: <IconUndoAll />,
      action: () => {
        alert('reply all');
      },
      hidden: !hasActionsList('replyall') || !canReplyAll,
    },
    {
      label: t('transfer'),
      id: 'transfer',
      icon: <IconRedo />,
      action: () => {
        alert('transfer');
      },
      hidden:
        !hasActionsList('transfer') ||
        message.state === 'DRAFT' ||
        message.trashed,
    },
    {
      label: t('tag.unread'),
      id: 'unread',
      icon: <IconUnreadMail />,
      action: handleMarkAsUnreadClick,
      hidden: !hasActionsList('unread') || !canMarkUnread,
    },
    {
      label: t('draft.save'),
      id: 'save',
      icon: <IconSave />,
      action: handleDraftSaveClick,
      hidden:
        !hasActionsList('save') ||
        (message.state !== 'DRAFT' && !message.trashed),
    },
    {
      label: t('trash'),
      id: 'trash',
      icon: <IconDelete />,
      action: () => {
        moveToTrashQuery.mutate({ id: message.id });
        navigate(`../..`, { relative: 'path' });
      },
      hidden: !hasActionsList('trash') || message.trashed,
    },
    {
      label: t('delete'),
      id: 'delete',
      icon: <IconDelete />,
      action: handleDeleteClick,
      hidden: !hasActionsList('delete') || !message.trashed,
    },
    {
      label: t('print'),
      id: 'print',
      icon: <IconPrint />,
      action: () => {
        alert('print');
      },
      hidden: !hasActionsList('print') || message.state === 'DRAFT',
    },
    {
      label: t('remove.from.folder'),
      id: 'remove-from-folder-modal',
      icon: <IconFolderDelete />,
      action: handleRemoveFromFolder,
      hidden: !hasActionsList('remove-from-folder-modal') || !isInFolder,
    },
  ];

  return (
    <div className="d-flex align-items-center gap-12">
      {buttonAction
        .filter((o) => !o.hidden)
        .map((option) => (
          <Button
            key={option.id}
            color={appearance.btnColor}
            variant={appearance.variant}
            leftIcon={option.icon}
            onClick={option.action}
          >
            {option.label}
          </Button>
        ))}
      <Dropdown>
        {(
          triggerProps: JSX.IntrinsicAttributes &
            Omit<IconButtonProps, 'ref'> &
            RefAttributes<HTMLButtonElement>,
        ) => {
          // If no options availables
          const visibleOptions = options.filter((o) => !o.hidden);
          if (visibleOptions.length === 0) {
            return null;
          }

          return (
            <div data-testid="dropdown">
              <IconButton
                {...triggerProps}
                type="button"
                size="sm"
                color={appearance.btnColor}
                variant={appearance.variant}
                icon={<IconOptions />}
              />
              <Dropdown.Menu>
                {visibleOptions.flatMap((option, index, array) => {
                  const elements = [
                    <Dropdown.Item
                      key={option.id}
                      icon={option.icon}
                      onClick={option.action}
                    >
                      {option.label}
                    </Dropdown.Item>,
                  ];

                  // Separator
                  const separatorAfterIds = ['replyall', 'transfer'];
                  if (
                    separatorAfterIds.includes(option.id) &&
                    array
                      .slice(index + 1)
                      .some((o) => !separatorAfterIds.includes(o.id))
                  ) {
                    elements.push(
                      <Dropdown.Separator key={`separator-${option.id}`} />,
                    );
                  }

                  return elements;
                })}
              </Dropdown.Menu>
            </div>
          );
        }}
      </Dropdown>
    </div>
  );
}
