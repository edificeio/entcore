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
import { useI18n } from '~/hooks/useI18n';
import { useRecall } from '~/hooks/useRecall';
import { useSelectedFolder } from '~/hooks/useSelectedFolder';
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
import { useMessageActions } from '~/store/messageStore';

export interface MessageActionDropdownProps {
  message: Message;
  actions?: string[];
  setInactiveUsers: (inactiveUsers: string[] | undefined) => void;
}

export function useMessageActionDropdown({
  message,
  setInactiveUsers,
  actions,
}: MessageActionDropdownProps) {
  const { t } = useI18n();
  const { setMessageNeedToSave } = useMessageActions();
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

  const isFromMe = message.from?.id === user?.userId;

  // Hidden condition's
  const isInFolder = useMemo(() => {
    if (folderId && ['trash', 'inbox', 'outbox'].includes(folderId)) return;
    return true;
  }, [folderId]);

  const canTransfer = useMemo(() => {
    return (
      (message.state === 'SENT' || (message.state === 'RECALL' && isFromMe)) &&
      message.trashed !== true
    );
  }, [isFromMe, message.state, message.trashed]);

  const canReply = useMemo(() => {
    return message.state === 'SENT' && message.trashed !== true;
  }, [message.state, message.trashed]);

  const canReplyAll = useMemo(() => {
    const { to, cc, cci } = message;

    // It's message to myself with cci
    const isMeWithCci =
      to.users.length === 1 &&
      to.users[0].id === user?.userId &&
      (cci?.groups.length || cci?.users.length);

    // Count number of recipients
    const hasRecipients =
      to.users.length + cc.users.length > 1 ||
      to.groups.length ||
      cc.groups.length;

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
      (message.subject.length > 0 ||
        message.body.length > 0 ||
        message.attachments.length > 0) &&
      (message.to.users.length > 0 ||
        message.to.groups.length > 0 ||
        message.cc.users.length > 0 ||
        message.cc.groups.length > 0 ||
        (message.cci &&
          (message.cci.users.length > 0 || message.cci.groups.length > 0)))
    );
  }, [message]);

  const hasActionsList = (idAction: string) => {
    return !actions || actions.includes(idAction);
  };

  // Handlers
  const handleDeleteClick = () => {
    openModal({
      id: 'delete-modal',
      header: <>{t('delete.definitely')}</>,
      body: <p>{t('delete.definitely.confirm')}</p>,
      okText: t('delete'),
      koText: t('cancel'),
      size: 'sm',
      onSuccess: () => {
        deleteMessage.mutate(
          { id: message.id },
          {
            onSuccess: () => {
              navigate('/trash');
            },
          },
        );
      },
    });
  };

  const handleDraftSaveClick = async () => {
    await createOrUpdateDraft(true);
    setMessageNeedToSave(false);
    navigate('/inbox');
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
    sendDraftQuery.mutate(
      {
        draftId: message.id,
        payload: {
          subject: message.subject,
          body: message.body,
          to: recipientToIds(message.to),
          cc: recipientToIds(message.cc),
          cci: message.cci ? recipientToIds(message.cci) : undefined,
        },
        inReplyToId:
          message.id !== message.parent_id ? message.parent_id : undefined,
      },
      {
        onSuccess: (response) => {
          if (response.inactive.length > 0 || response.undelivered.length > 0) {
            if (response.inactive.length > 0) {
              setInactiveUsers(response.inactive);
            } else {
              setInactiveUsers(undefined);
            }
          } else {
            navigate(`/inbox`);
          }
        },
      },
    );
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
        navigate(`/draft/create?reply=${message.id}`);
      },
      hidden:
        folderId === 'outbox' || (!canReply && message.state !== 'RECALL'),
      disabled: message.state === 'RECALL',
    },
    {
      label: t('submit'),
      id: 'submit',
      icon: <IconSend />,
      action: handleSendClick,
      hidden: message.state !== 'DRAFT' || message.trashed,
      disabled: !isMessageValid || sendDraftQuery.isPending,
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
    {
      label: t('transfer'),
      id: 'transfer',
      icon: <IconRedo />,
      action: () => {
        navigate(`/draft/create?transfer=${message.id}`);
      },
      hidden: folderId !== 'outbox' || !canTransfer,
    },
  ];

  const dropdownOptions = [
    {
      label: t('reply'),
      id: 'reply',
      icon: <IconUndo />,
      action: () => {
        navigate(`/draft/create?reply=${message.id}`);
      },
      hidden: !canReply,
    },
    {
      label: t('replyall'),
      id: 'replyall',
      icon: <IconUndoAll />,
      action: () => {
        navigate(`/draft/create?replyall=${message.id}`);
      },
      hidden: !hasActionsList('replyall') || !canReplyAll,
    },
    {
      label: t('transfer'),
      id: 'transfer',
      icon: <IconRedo />,
      action: () => {
        navigate(`/draft/create?transfer=${message.id}`);
      },
      hidden: !hasActionsList('transfer') || !canTransfer,
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
        folderId === 'trash' ||
        !hasActionsList('save') ||
        (message.state !== 'DRAFT' && !message.trashed),
    },
    {
      label: t('move.first.caps'),
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
    {
      label: t('print'),
      id: 'print',
      icon: <IconPrint />,
      action: () => {
        print();
      },
      hidden: !hasActionsList('print') || message.state === 'DRAFT',
    },
    {
      label: t('trash.action'),
      id: 'trash',
      icon: <IconDelete />,
      action: () => {
        moveToTrashQuery.mutate({ id: message.id });
        navigate(`/${folderId}`, { relative: 'path' });
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
  ];

  return {
    isInFolder,
    canReply,
    canReplyAll,
    canMarkUnread,
    actionButtons,
    dropdownOptions,
  };
}
