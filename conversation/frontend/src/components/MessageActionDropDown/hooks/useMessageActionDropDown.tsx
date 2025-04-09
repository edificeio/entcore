import { useEdificeClient, useToast } from '@edifice.io/react';
import {
  IconDelete,
  IconFolderDelete,
  IconFolderMove,
  IconMailRecall,
  IconPrint,
  IconRedo,
  IconRestore,
  IconSave,
  IconSend,
  IconUndo,
  IconUndoAll,
  IconUnreadMail,
} from '@edifice.io/react/icons';
import { useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { useFolderHandlers } from '~/features/menu/hooks/useFolderHandlers';
import { useI18n, useRecall, useSelectedFolder } from '~/hooks';
import { Message, Recipients } from '~/models';
import {
  isInRecipient,
  useCreateOrUpdateDraft,
  useDeleteMessage,
  useMarkUnread,
  useMoveMessage,
  useRestoreMessage,
  useSendDraft,
  useTrashMessage,
} from '~/services';
import { useAppActions, useConfirmModalStore } from '~/store';

export function useMessageActionDropDown(message: Message, actions?: string[]) {
  const { t } = useI18n();
  const markAsUnreadQuery = useMarkUnread();
  const navigate = useNavigate();
  const { openModal } = useConfirmModalStore();
  const deleteMessage = useDeleteMessage();
  const restoreQuery = useRestoreMessage();
  const { canRecall, handleRecall } = useRecall();
  const moveToTrashQuery = useTrashMessage();
  const createOrUpdateDraft = useCreateOrUpdateDraft();
  const moveMessage = useMoveMessage();
  const { handleMoveMessage } = useFolderHandlers();
  const { setSelectedMessageIds } = useAppActions();
  const sendDraftQuery = useSendDraft();

  const { folderId } = useSelectedFolder();
  const { user } = useEdificeClient();
  const { success } = useToast();

  // Hidden condition's
  const isInFolder = useMemo(() => {
    if (folderId && ['trash', 'inbox', 'outbox'].includes(folderId)) return;
    return true;
  }, [folderId]);

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

  const isMessageValid = useMemo(() => {
    return (
      !message.trashed &&
      !['draft', 'outbox', 'trash'].includes(folderId!) &&
      message.subject.length > 0 &&
      (message.to.users.length > 0 ||
        message.to.groups.length > 0 ||
        message.cc.users.length > 0 ||
        message.cc.groups.length > 0 ||
        (message.cci &&
          (message.cci.users.length > 0 || message.cci.groups.length > 0)))
    );
  }, [message, folderId]);

  const hasActionsList = (idAction: string) => {
    return !actions || actions.includes(idAction);
  };

  // Handlers
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

  const handleDraftSaveClick = async () => {
    const promise = await createOrUpdateDraft();
    if (promise) {
      if (promise && promise.id) navigate(`/draft/message/${promise.id}`);
    }
  };

  const handleMarkAsUnreadClick = () => {
    markAsUnreadQuery.mutate({ messages: [message] });
    navigate(`../..`, { relative: 'path' });
  };

  const recipientToIds = (recipients: Recipients): string[] => {
    return [
      ...new Set([
        ...(recipients.users || []).map((user) => user.id),
        ...(recipients.groups || []).map((group) => group.id),
      ]),
    ];
  };

  const handleSendClick = () => {
    sendDraftQuery.mutate({
      draftId: message.id,
      payload: {
        subject: message.subject,
        body: message.body,
        to: recipientToIds(message.to),
        cc: recipientToIds(message.cc),
        cci: message.cci ? recipientToIds(message.cci) : undefined,
      },
    });
    navigate(`/inbox`);
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

  // Buttons
  const actionButtons = [
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
      action: handleSendClick,
      hidden: message.state !== 'DRAFT' || message.trashed,
      disabled: !isMessageValid,
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

  const dropdownOptions = [
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
      label: t('recall'),
      id: 'recall',
      icon: <IconMailRecall />,
      action: () => handleRecall(message),
      hidden: !canRecall(message),
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
      label: t('move'),
      id: 'move',
      icon: <IconFolderMove />,
      action: () => {
        setSelectedMessageIds([message.id]);
        handleMoveMessage();
      },
      hidden:
        !hasActionsList('move') || message.state === 'DRAFT' || message.trashed,
    },
    {
      label: t('remove.from.folder'),
      id: 'remove-from-folder-modal',
      icon: <IconFolderDelete />,
      action: handleRemoveFromFolder,
      hidden:
        !hasActionsList('remove-from-folder-modal') ||
        !isInFolder ||
        message.state === 'DRAFT',
    },
  ];

  return {
    isInFolder,
    canReplyAll,
    canMarkUnread,
    actionButtons,
    dropdownOptions,
  };
}
