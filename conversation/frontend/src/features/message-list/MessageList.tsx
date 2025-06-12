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
import { useEffect, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useLocation, useSearchParams } from 'react-router-dom';
import { useSelectedFolder } from '~/hooks/useSelectedFolder';
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
  const location = useLocation();
  const shouldScrollToTop = location.state?.scrollToTop;

  const {
    messages,
    isPending: isLoadingMessages,
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
  const listRef = useRef<HTMLDivElement>(null);
  const observer = useRef<IntersectionObserver | null>(null);

  //handle reload list when search params change
  useEffect(() => {
    setKeyList((prev) => prev + 1);
  }, [searchParams]);

  useEffect(() => {
    const messageListItems =
      listRef.current?.getElementsByClassName('message-list-item');

    if (messageListItems && shouldScrollToTop) {
      messageListItems[0].scrollIntoView({
        block: 'center',
      });
    }

    if (isLoadingMessages || isLoadingNextPage) return;
    if (observer.current) observer.current.disconnect();
    observer.current = new IntersectionObserver(
      (entries) => {
        if (entries[0].isIntersecting && hasNextPage) {
          fetchNextPage();
        }
      },
      { threshold: 0.1 },
    );
    if (messageListItems) {
      observer.current.observe(messageListItems[messageListItems.length - 1]);
    }
  }, [
    messages,
    hasNextPage,
    isLoadingMessages,
    isLoadingNextPage,
    fetchNextPage,
  ]);

  useEffect(() => {
    return () => {
      if (observer.current) observer.current.disconnect();
    };
  }, []);

  const toolbarItemsData = [
    {
      visibility: showMarkAsReadMessages,
      name: t('tag.read'),
      icon: <IconReadMail />,
      onClick: handleMarkAsReadClick,
    },
    {
      visibility: showMarkAsUnReadMessages,
      name: t('tag.unread'),
      icon: <IconUnreadMail />,
      onClick: handleMarkAsUnreadClick,
    },

    {
      visibility: showTrashActions,
      name: t('restore'),
      icon: <IconRestore />,
      onClick: handleRestore,
    },
    {
      visibility: showMoveToFolder,
      name: t('move.first.caps'),
      icon: <IconFolderMove />,
      onClick: handleMoveToFolder,
    },
    {
      visibility: showTrashActions,
      name: t('delete.definitely'),
      icon: <IconDelete />,
      onClick: handleDelete,
    },
    {
      visibility: showFolderActions,
      name: t('remove.from.folder'),
      icon: <IconFolderDelete />,
      onClick: handleRemoveFromFolder,
    },
    {
      visibility: showEmptyTrash,
      name: t('empty.trash'),
      icon: <IconDelete />,
      onClick: handleEmptyTrash,
    },
    {
      visibility: showMoveToTrash,
      name: t('delete'),
      icon: <IconDelete />,
      onClick: handleMoveToTrash,
    },
  ];

  const toolbar: ToolbarButtonItem[] = toolbarItemsData.map(
    ({ name, visibility, icon, onClick }) => ({
      type: 'button',
      name,
      visibility,
      tooltip: name,
      props: {
        leftIcon: icon,
        children: name,
        onClick,
      },
    }),
  );

  if (!messages?.length) return null;
  return (
    <div ref={listRef}>
      <List
        data={messages.map((message) => ({ ...message, _id: message.id }))}
        items={toolbar}
        isCheckable={true}
        onSelectedItems={setSelectedMessageIds}
        toolbarOptions={{ shouldHideLabelsOnMobile: true, sticky: true }}
        className="ps-16 ps-lg-24"
        key={keyList}
        renderNode={(message, checkbox, checked) => (
          <MessageItem
            message={message}
            checked={!!checked}
            checkbox={checkbox}
          />
        )}
      />
      {isLoadingMessages && (
        <Loading isLoading={true} className="justify-content-center my-12" />
      )}
    </div>
  );
}
