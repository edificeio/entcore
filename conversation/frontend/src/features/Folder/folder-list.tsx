import {
  Button,
  EmptyScreen,
  List,
  Loading,
  ToolbarItem,
  useEdificeClient,
} from '@edifice.io/react';
import {
  IconEdit,
  IconReadMail,
  IconUnreadMail,
} from '@edifice.io/react/icons';
import illuMessagerie from '@images/emptyscreen/illu-messagerie.svg';
import clsx from 'clsx';
import { useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import { MessagePreview } from '~/features/Message/message-preview';
import { useSelectedFolder } from '~/hooks';
import { MessageMetadata } from '~/models';
import { useFolderMessages, useMarkRead, useMarkUnread } from '~/services';
import { useAppActions, useSelectedMessageIds } from '~/store/actions';

export function FolderList() {
  const navigate = useNavigate();

  const { folderId } = useSelectedFolder();
  const { appCode } = useEdificeClient();
  const { t } = useTranslation(appCode);
  const { setSelectedMessageIds } = useAppActions();

  const selectedIds = useSelectedMessageIds();
  const markAsReadQuery = useMarkRead();
  const markAsUnreadQuery = useMarkUnread();

  const {
    messages,
    isPending: isLoadingMessage,
    isFetchingNextPage: isLoadingNextPage,
    hasNextPage,
    fetchNextPage,
  } = useFolderMessages(folderId!);

  const handleMarkAsReadClick = () => {
    markAsReadQuery.mutate({ id: selectedIds });
  };

  const handleMarkAsUnreadClick = () => {
    markAsUnreadQuery.mutate({ id: selectedIds });
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
        hidden: !selectedIds.length,
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
        hidden: !selectedIds.length,
      },
    },
  ];

  const handleMessageClick = (message: MessageMetadata) => {
    navigate(message.id);
  };

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

  return (
    <>
      {!!messages?.length && (
        <List
          data={messages.map((message) => ({ ...message, _id: message.id }))}
          items={toolbar}
          isCheckable={true}
          onSelectedItems={setSelectedMessageIds}
          className="ps-16 ps-md-24"
          renderNode={(message, checkbox, checked) => (
            <div
              className={clsx(
                'd-flex gap-24 px-16 ps-md-24 py-12 mb-2 overflow-hidden',
                {
                  'bg-secondary-200': checked,
                  'fw-bold bg-primary-200': message.unread,
                },
              )}
              onClick={() => handleMessageClick(message)}
              tabIndex={0}
              role="button"
              key={message.id}
              data-testid="message-item"
            >
              <div className="d-flex align-items-center gap-8 g-col-3 flex-fill overflow-hidden">
                {checkbox}
                <MessagePreview message={message} />
              </div>
            </div>
          )}
        />
      )}
      {isLoadingMessage && (
        <Loading isLoading={true} className="justify-content-center my-12" />
      )}
      {!isLoadingMessage && !messages?.length && (
        <div className="d-flex flex-column gap-24 align-items-center justify-content-center">
          <EmptyScreen
            imageSrc={illuMessagerie}
            title={t('folder.empty.title')}
            text={t('folder.empty.text')}
          />
          <div>
            <Button>
              <IconEdit />
              {t('new.message')}
            </Button>
          </div>
        </div>
      )}
    </>
  );
}
