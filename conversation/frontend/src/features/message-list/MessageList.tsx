import {
  List,
  Loading,
  ToolbarButtonItem,
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
import { useSelectedFolder } from '~/hooks/useSelectedFolder';
import { useFolderMessages } from '~/services';
import { useAppActions } from '~/store/actions';
import { MessageItem } from './components/MessageItem';
import useToolbarActions from './hooks/useToolbarActions';
import useToolbarVisibility from './hooks/useToolbarVisibility';

export function MessageList() {
  const { folderId } = useSelectedFolder();
  console.log('===folderId:', folderId);
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
    showEmptyTrash,
    showFolderActions,
    showMarkAsReadMessages,
    showMarkAsUnReadMessages,
    showMoveToFolder,
    showMoveToTrash,
    showTrashActions,
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

  const toolbarItemsData = [
    {
      visibility: showMarkAsReadMessages,
      label: t('tag.read'),
      icon: <IconReadMail />,
      onClick: handleMarkAsReadClick,
    },
    {
      visibility: showMarkAsUnReadMessages,
      label: t('tag.unread'),
      icon: <IconUnreadMail />,
      onClick: handleMarkAsUnreadClick,
    },

    {
      visibility: showTrashActions,
      label: t('restore'),
      icon: <IconRestore />,
      onClick: handleRestore,
    },
    {
      visibility: showMoveToFolder,
      label: t('move.first.caps'),
      icon: <IconFolderMove />,
      onClick: handleMoveToFolder,
    },
    {
      visibility: showTrashActions,
      label: t('delete.definitely'),
      icon: <IconDelete />,
      onClick: handleDelete,
    },
    {
      visibility: showFolderActions,
      label: t('remove.from.folder'),
      icon: <IconFolderDelete />,
      onClick: handleRemoveFromFolder,
    },
    {
      visibility: showEmptyTrash,
      label: t('empty.trash'),
      icon: <IconDelete />,
      onClick: handleEmptyTrash,
    },
    {
      visibility: showMoveToTrash,
      label: t('delete'),
      icon: <IconDelete />,
      onClick: handleMoveToTrash,
    },
  ];

  const toolbar: ToolbarButtonItem[] = toolbarItemsData.map(
    ({ label, visibility, icon, onClick }) => ({
      type: 'button',
      name: label,
      visibility,
      tooltip: label,
      props: {
        leftIcon: icon,
        children: label,
        onClick,
      },
    }),
  );

  if (!messages?.length) return null;
  return (
    <>
      <List
        data={messages.map((message) => ({ ...message, _id: message.id }))}
        items={toolbar}
        isCheckable={true}
        onSelectedItems={setSelectedMessageIds}
        toolbarOptions={{ shouldHideLabelsOnMobile: true }}
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
