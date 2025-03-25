import {
  List,
  Loading,
  ToolbarItem,
  useEdificeClient,
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
  useFolderMessages,
  useUpdateFolderBadgeCountLocal,
} from '~/services';
import { useAppActions } from '~/store/actions';
import { MessagePreview } from './components/MessagePreview/MessagePreview';
import useSelectedMessages from './hooks/useSelectedMessages';
import useToolbarActions from './hooks/useToolbarActions';

export function MessageList() {
  const navigate = useNavigate();
  const { folderId } = useSelectedFolder();
  const [searchParams] = useSearchParams();
  const { appCode } = useEdificeClient();
  const { t } = useTranslation(appCode);
  const { setSelectedMessageIds } = useAppActions();
  const { updateFolderBadgeCountLocal } = useUpdateFolderBadgeCountLocal();
  const { user } = useEdificeClient();
  const {
    messages,
    isPending: isLoadingMessage,
    isFetchingNextPage: isLoadingNextPage,
    hasNextPage,
    fetchNextPage,
  } = useFolderMessages(folderId!);
  const {
    handleDelete,
    handleEmptyTrash,
    handleMarkAsReadClick,
    handleMarkAsUnreadClick,
    handleMoveToFolder,
    handleMoveToTrash,
    handleRemoveFromFolder,
    handleRestore,
  } = useToolbarActions(messages);

  const [keyList, setKeyList] = useState(0);

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
    setKeyList((prev) => prev + 1);
  }, [searchParams, folderId, messages?.length]);

  const isInTrash = folderId === 'trash';
  const isInDraft = folderId === 'draft';

  const selectedMessages = useSelectedMessages(messages);

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

  const canEmptyTrash = useMemo(() => {
    if (!isInTrash) return false;
    return messages.length > 0;
  }, [folderId, messages]);

  const handleMessageKeyUp = (
    event: KeyboardEvent,
    message: MessageMetadata,
  ) => {
    if (event.key === 'Enter') {
      handleMessageClick(message);
    }
  };

  const handleMessageClick = (message: MessageMetadata) => {
    if (message.unread && !isInDraft) {
      updateFolderBadgeCountLocal(folderId!, -1);
    }
    navigate(`message/${message.id}`);
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
    {
      type: 'button',
      name: 'empty-trash',
      props: {
        children: (
          <>
            <IconDelete />
            <span>{t('empty.trash')}</span>
          </>
        ),
        onClick: handleEmptyTrash,
        hidden: !canEmptyTrash,
      },
    },
  ];

  if (!messages?.length) return null;
  return (
    <>
      <List
        data={messages.map((message) => ({ ...message, _id: message.id }))}
        items={toolbar}
        isCheckable={true}
        onSelectedItems={setSelectedMessageIds}
        className="ps-16 ps-md-24"
        key={keyList}
        renderNode={(message, checkbox, checked) => (
          <div
            className={clsx('d-flex gap-24 px-16 py-12 mb-2 overflow-hidden', {
              'bg-secondary-200': checked,
              'fw-bold bg-primary-200 gray-800': message.unread,
            })}
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
      {isLoadingMessage && (
        <Loading isLoading={true} className="justify-content-center my-12" />
      )}
    </>
  );
}
