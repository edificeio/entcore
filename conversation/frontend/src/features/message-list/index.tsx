import {
  List,
  Loading,
  ToolbarItem,
  useEdificeClient,
} from '@edifice.io/react';
import {
  IconDelete,
  IconFolderMove,
  IconReadMail,
  IconRestore,
  IconUnreadMail,
} from '@edifice.io/react/icons';
import clsx from 'clsx';
import { KeyboardEvent, useEffect, useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate, useParams, useSearchParams } from 'react-router-dom';
import { MessageMetadata } from '~/models';
import {
  useDeleteMessage,
  useFolderMessages,
  useMarkRead,
  useMarkUnread,
  useRestoreMessage,
  useTrashMessage,
  useUpdateFolderBadgeCountLocal,
} from '~/services';
import { useConfirmModalStore } from '~/store';
import { useAppActions, useSelectedMessageIds } from '~/store/actions';
import { MessagePreview } from './message-preview';
import { useFolderHandlers } from '../menu/hooks/useFolderHandlers';

export function MessageList() {
  const navigate = useNavigate();

  const { folderId } = useParams();
  const [searchParams] = useSearchParams();
  const { appCode } = useEdificeClient();
  const { t } = useTranslation(appCode);
  const { setSelectedMessageIds } = useAppActions();
  const [current, setCurrent] = useState(0);

  const selectedIds = useSelectedMessageIds();
  const markAsReadQuery = useMarkRead();
  const markAsUnreadQuery = useMarkUnread();
  const moveToTrashQuery = useTrashMessage();
  const restoreQuery = useRestoreMessage();
  const deleteMessage = useDeleteMessage();
  const { updateFolderBadgeCountLocal } = useUpdateFolderBadgeCountLocal();
  const { handleMoveMessage } = useFolderHandlers()
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

  const hasUnreadMessages = useMemo(() => {
    if (isInTrash) return false;
    return selectedMessages.some(
      (message) => message.unread && message.state !== 'DRAFT',
    );
  }, [isInTrash, selectedMessages]);

  const hasReadMessages = useMemo(() => {
    if (isInTrash) return false;
    return selectedMessages.some(
      (message) => !message.unread && message.state !== 'DRAFT',
    );
  }, [isInTrash, selectedMessages]);

  const canBeMovetoTrash = useMemo(() => {
    if (isInTrash) return false;
    return selectedMessages.length > 0;
  }, [isInTrash, selectedMessages]);

  const canBeMovetoFolder = useMemo(() => {
    if (isInTrash || isInDraft) return false;
    return selectedMessages.length > 0;
  }, [isInTrash, isInDraft, selectedMessages]);

  const isTrashMessage = useMemo(() => {
    if (!isInTrash) return false;
    return selectedMessages.length > 0;
  }, [isInTrash, selectedMessages]);

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
    if (message.unread) {
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
        hidden: !hasUnreadMessages,
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
        hidden: !hasReadMessages,
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
        hidden: !canBeMovetoFolder,
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
