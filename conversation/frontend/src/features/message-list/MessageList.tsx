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
import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useSearchParams } from 'react-router-dom';
import { useSelectedFolder } from '~/hooks';
import { useFolderMessages } from '~/services';
import { useAppActions } from '~/store/actions';
import { MessageItem } from './components/MessageItem';
import useToolbarActions from './hooks/useToolbarActions';
import useToolbarVisibility from './hooks/useToolbarVisibility';

export function MessageList() {
  const { folderId } = useSelectedFolder();
  const [searchParams] = useSearchParams();

  const { appCode } = useEdificeClient();
  const { t } = useTranslation(appCode);
  const { setSelectedMessageIds } = useAppActions();

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

  const {
    canBeMoveToFolder,
    canBeMovetoTrash,
    canEmptyTrash,
    canMarkAsReadMessages,
    canMarkAsUnReadMessages,
    isInFolder,
    isTrashMessage,
  } = useToolbarVisibility(messages);

  const [keyList, setKeyList] = useState(0);

  //handle reload list when search params change
  useEffect(() => {
    setKeyList((prev) => prev + 1);
  }, [searchParams]);

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
          <MessageItem
            message={message}
            checked={!!checked}
            checkbox={checkbox}
          />
        )}
      />
      {isLoadingMessage && (
        <Loading isLoading={true} className="justify-content-center my-12" />
      )}
    </>
  );
}
