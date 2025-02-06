import {
  List,
  Loading,
  ToolbarItem,
  useEdificeClient,
} from '@edifice.io/react';
import { IconReadMail, IconUnreadMail, IconDelete } from '@edifice.io/react/icons';
import clsx from 'clsx';
import { KeyboardEvent, useEffect, useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate, useParams, useSearchParams } from 'react-router-dom';
import { MessagePreview } from '~/features/Message/message-preview';
import { MessageMetadata } from '~/models';
import { useFolderMessages, useMarkRead, useMarkUnread, useTrashMessage } from '~/services';
import { useAppActions, useSelectedMessageIds } from '~/store/actions';

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

  const hasUnreadMessages = useMemo(() => {
    return messages?.some(
      (message) =>
        selectedIds.length &&
        selectedIds.includes(message.id) &&
        message.unread,
    );
  }, [selectedIds, messages]);

  const hasReadMessages = useMemo(() => {
    return messages?.some(
      (message) =>
        selectedIds.length &&
        selectedIds.includes(message.id) &&
        !message.unread,
    );
  }, [selectedIds, messages]);

  const canBeMovetoTrash = useMemo(() => {
    if(folderId == 'trash') return;
    return messages?.some(
      (message) =>
        selectedIds.length &&
        selectedIds.includes(message.id)
    );
  }, [selectedIds, messages, folderId]);

  const handleMarkAsReadClick = () => {
    markAsReadQuery.mutate({ id: selectedIds });
  };

  const handleMarkAsUnreadClick = () => {
    markAsUnreadQuery.mutate({ id: selectedIds });
  };

  const handleMessageKeyUp = (
    event: KeyboardEvent,
    message: MessageMetadata,
  ) => {
    if (event.key === ' ' || event.key === 'Enter') {
      handleMessageClick(message);
    }
  }

  const handleMoveToTrash = () => {
    moveToTrashQuery.mutate({ id: selectedIds });
    setCurrent((prev) => prev + 1);
  };

  const handleMessageClick = (message: MessageMetadata) => {
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
        hidden: !canBeMovetoTrash
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
