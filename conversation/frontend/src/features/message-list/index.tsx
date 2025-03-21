import {
  List,
  Loading,
  ToolbarItem,
  useEdificeClient,
  useToast,
} from '@edifice.io/react';
import {
  IconDelete,
  IconFolderDelete,
  IconFolderMove,
  IconReadMail,
  IconRestore,
  IconUnreadMail,
} from '@edifice.io/react/icons';
import clsx from 'clsx';
import {
  KeyboardEvent,
  useCallback,
  useEffect,
  useMemo,
  useState,
} from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useSelectedFolder } from '~/hooks';
import { MessageMetadata } from '~/models';
import {
  isInRecipient,
  useDeleteMessage,
  useFolderMessages,
  useMarkRead,
  useMarkUnread,
  useMoveMessage,
  useRestoreMessage,
  useTrashMessage,
  useUpdateFolderBadgeCountLocal,
} from '~/services';
import { useConfirmModalStore } from '~/store';
import { useAppActions, useSelectedMessageIds } from '~/store/actions';
import { useFolderHandlers } from '../menu/hooks/useFolderHandlers';
import { MessagePreview } from './components/MessagePreview/MessagePreview';

export function MessageList() {
  const navigate = useNavigate();

  const { folderId } = useSelectedFolder();
  const [searchParams] = useSearchParams();
  const { appCode } = useEdificeClient();
  const { t } = useTranslation(appCode);
  const { setSelectedMessageIds } = useAppActions();
  const [current, setCurrent] = useState(0);
  const { success } = useToast();

  const selectedIds = useSelectedMessageIds();
  const markAsReadQuery = useMarkRead();
  const markAsUnreadQuery = useMarkUnread();
  const moveToTrashQuery = useTrashMessage();
  const restoreQuery = useRestoreMessage();
  const deleteMessage = useDeleteMessage();
  const moveMessage = useMoveMessage();

  const { updateFolderBadgeCountLocal } = useUpdateFolderBadgeCountLocal();
  const { handleMoveMessage } = useFolderHandlers();
  const { user } = useEdificeClient();

  const { openModal } = useConfirmModalStore();

  const {
    messages,
    isPending: isLoadingMessage,
    isFetchingNextPage: isLoadingNextPage,
    hasNextPage,
    fetchNextPage,
  } = useFolderMessages(folderId!);

  // Handle infinite scroll
  useEffect(() => {
    const handleScroll = () => {
      if (
        isLoadingMessage ||
        isLoadingNextPage ||
        window.innerHeight + document.documentElement.scrollTop <
          document.documentElement.offsetHeight - 250
      ) {
        return;
      }
      fetchNextPage();
    };

    window.addEventListener('scroll', handleScroll);
    return () => window.removeEventListener('scroll', handleScroll);
  }, [isLoadingMessage, isLoadingNextPage, fetchNextPage, hasNextPage]);

  useEffect(() => {
    setCurrent((prev) => prev + 1);
  }, [searchParams, folderId]);

  const isInTrash = folderId === 'trash';
  const isInDraft = folderId === 'draft';

  const selectedMessages = useMemo(() => {
    return (
      messages?.filter((message) => selectedIds.includes(message.id)) || []
    );
  }, [selectedIds, messages]);

  const canShowMarkActions = useCallback(
    (unread: boolean) => {
      return (
        !['draft', 'outbox', 'trash'].includes(folderId!) &&
        // Check if the selected messages are not drafts and are unread or read depending on the action
        selectedMessages.some(
          (message) => message.unread === unread && message.state !== 'DRAFT',
        ) &&
        // Check if the selected messages are not sent by the user
        !selectedMessages.some(
          (message) =>
            message.from.id === user?.userId &&
            !isInRecipient(message, user.userId),
        )
      );
    },
    [folderId, selectedMessages, user],
  );

  const canMarkAsReadMessages = useMemo(() => {
    return canShowMarkActions(true);
  }, [canShowMarkActions]);

  const canMarkAsUnReadMessages = useMemo(() => {
    return canShowMarkActions(false);
  }, [canShowMarkActions]);

  const canBeMovetoTrash = useMemo(() => {
    if (isInTrash) return false;
    return selectedMessages.length > 0;
  }, [isInTrash, selectedMessages]);

  const canBeMoveToFolder = useMemo(() => {
    if (isInTrash || isInDraft) return false;
    return selectedMessages.length > 0;
  }, [isInTrash, isInDraft, selectedMessages]);

  const isTrashMessage = useMemo(() => {
    if (!isInTrash) return false;
    return selectedMessages.length > 0;
  }, [isInTrash, selectedMessages]);

  const isInFolder = useMemo(() => {
    if (folderId && ['trash', 'inbox', 'outbox', 'draft'].includes(folderId))
      return;
    return selectedMessages.length > 0;
  }, [folderId, selectedMessages]);

  const handleMarkAsReadClick = () => {
    markAsReadQuery.mutate({ messages: selectedMessages });
  };

  const handleMarkAsUnreadClick = () => {
    markAsUnreadQuery.mutate({ messages: selectedMessages });
  };

  const handleMessageKeyUp = (
    event: KeyboardEvent,
    message: MessageMetadata,
  ) => {
    if (event.key === ' ' || event.key === 'Enter') {
      handleMessageClick(message);
    }
  };

  const handleMoveToTrash = () => {
    moveToTrashQuery.mutate({ id: selectedIds });
    setCurrent((prev) => prev + 1);
  };

  const handleMessageClick = (message: MessageMetadata) => {
    if (message.unread && !isInDraft) {
      updateFolderBadgeCountLocal(folderId!, -1);
    }
    navigate(`message/${message.id}`);
  };

  const handleRestore = () => {
    restoreQuery.mutate({ id: selectedIds });
    setCurrent((prev) => prev + 1);
  };

  const handleDelete = () => {
    openModal({
      id: 'delete-modal',
      header: <>{t('delete.definitely')}</>,
      body: <p>{t('delete.definitely.confirm')}</p>,
      okText: t('confirm'),
      koText: t('cancel'),
      onSuccess: () => {
        deleteMessage.mutate({ id: selectedIds });
        setCurrent((prev) => prev + 1);
      },
    });
  };

  const handleMoveToFolder = () => {
    handleMoveMessage();
  };

  const handleRemoveFromFolder = () => {
    openModal({
      id: 'remove-from-folder-modal',
      header: <>{t('remove.from.folder')}</>,
      body: <p>{t('remove.from.folder.confirm')}</p>,
      okText: t('confirm'),
      koText: t('cancel'),
      onSuccess: async () => {
        const confirmMessage =
          selectedIds.length > 1
            ? t('messages.remove.from.folder')
            : t('message.remove.from.folder');

        await Promise.allSettled(
          selectedIds.map((id) =>
            moveMessage.mutateAsync({ folderId: 'inbox', id }),
          ),
        );

        success(confirmMessage);
        setCurrent((prev) => prev + 1);
      },
    });
  };

  const toolbar: ToolbarItem[] = [
    {
      type: 'button',
      name: 'read',
      props: {
        children: (
          <>
            <IconReadMail />
            <span>{t('tag.read')}</span>
          </>
        ),
        onClick: handleMarkAsReadClick,
        hidden: !canMarkAsReadMessages,
      },
    },
    {
      type: 'button',
      name: 'unread',
      props: {
        children: (
          <>
            <IconUnreadMail />
            <span>{t('tag.unread')}</span>
          </>
        ),
        onClick: handleMarkAsUnreadClick,
        hidden: !canMarkAsUnReadMessages,
      },
    },
    {
      type: 'button',
      name: 'delete',
      props: {
        children: (
          <>
            <IconDelete />
            <span>{t('delete')}</span>
          </>
        ),
        onClick: handleMoveToTrash,
        hidden: !canBeMovetoTrash,
      },
    },
    {
      type: 'button',
      name: 'restore',
      props: {
        children: (
          <>
            <IconRestore />
            <span>{t('restore')}</span>
          </>
        ),
        onClick: handleRestore,
        hidden: !isTrashMessage,
      },
    },
    {
      type: 'button',
      name: 'move',
      props: {
        children: (
          <>
            <IconFolderMove />
            <span>{t('move')}</span>
          </>
        ),
        onClick: handleMoveToFolder,
        hidden: !canBeMoveToFolder,
      },
    },
    {
      type: 'button',
      name: 'delete-definitely',
      props: {
        children: (
          <>
            <IconDelete />
            <span>{t('delete.definitely')}</span>
          </>
        ),
        onClick: handleDelete,
        hidden: !isTrashMessage,
      },
    },
    {
      type: 'button',
      name: 'remove-from-folder',
      props: {
        children: (
          <>
            <IconFolderDelete />
            <span>{t('remove.from.folder')}</span>
          </>
        ),
        onClick: handleRemoveFromFolder,
        hidden: !isInFolder,
      },
    },
  ];

  return (
    <>
      {!!messages?.length && (
        <List
          data={messages.map((message) => ({ ...message, _id: message.id }))}
          items={toolbar}
          isCheckable={true}
          onSelectedItems={setSelectedMessageIds}
          className="ps-16 ps-md-24"
          key={current}
          renderNode={(message, checkbox, checked) => (
            <div
              className={clsx(
                'd-flex gap-24 px-16 py-12 mb-2 overflow-hidden',
                {
                  'bg-secondary-200': checked,
                  'fw-bold bg-primary-200 gray-800': message.unread,
                },
              )}
              onClick={() => handleMessageClick(message)}
              onKeyUp={(event) => handleMessageKeyUp(event, message)}
              tabIndex={0}
              role="button"
              key={message.id}
              data-testid="message-item"
            >
              <div className="d-flex align-items-center gap-12 g-col-3 flex-fill overflow-hidden">
                <div className="ps-md-8">{checkbox}</div>
                <MessagePreview message={message} />
              </div>
            </div>
          )}
        />
      )}
      {isLoadingMessage && (
        <Loading isLoading={true} className="justify-content-center my-12" />
      )}
    </>
  );
}
