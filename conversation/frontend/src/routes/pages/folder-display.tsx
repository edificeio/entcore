import { QueryClient } from '@tanstack/react-query';
import { LoaderFunctionArgs, useNavigate } from 'react-router-dom';
import { MessageMetadata } from '~/models';
import {
  folderQueryOptions,
  useFolderMessages,
  useMarkRead,
  useMarkUnread,
} from '~/services';
import { MessagePreview } from './message-preview';
import {
  IconAdd,
  IconEdit,
  IconFilter,
  IconReadMail,
  IconUnreadMail,
} from '@edifice.io/react/icons';
import {
  Button,
  Dropdown,
  EmptyScreen,
  List,
  Loading,
  SearchBar,
  ToolbarItem,
  useBreakpoint,
  useEdificeTheme,
} from '@edifice.io/react';
import clsx from 'clsx';
import { useEffect, useState } from 'react';
import {
  useAppActions,
  useFilterUnreadMessageList,
  useSelectedMessageIds,
} from '~/store/actions';
import { useTranslation } from 'react-i18next';
import { useSelectedFolder } from '~/hooks';
import illuMessagerie from '@images/emptyscreen/illu-messagerie.svg';

export const loader =
  (_queryClient: QueryClient) =>
  async ({ params }: LoaderFunctionArgs) => {
    const messagesQuery = folderQueryOptions.getMessages(params.folderId!);
    const messages = await _queryClient.ensureQueryData(messagesQuery);
    return { messages };
  };

export function Component() {
  const { folderId } = useSelectedFolder();
  const { theme } = useEdificeTheme();
  const navigate = useNavigate();

  const { t } = useTranslation('conversation');

  const [searchText, setSearchText] = useState('');
  const {
    setSearchMessageList,
    setSelectedMessageIds,
    setFilterUnreadMessageList,
  } = useAppActions();
  const selectedIds = useSelectedMessageIds();
  const filterUnread = useFilterUnreadMessageList();
  const markAsReadQuery = useMarkRead();
  const markAsUnreadQuery = useMarkUnread();
  const { md } = useBreakpoint();
  const filterEnum = {
    unread: 'UNREAD',
  };

  const {
    messages,
    isPending: isLoadingMessage,
    isFetchingNextPage: isLoadingNextPage,
    hasNextPage,
    fetchNextPage,
  } = useFolderMessages(folderId!);

  const handlerChangeSearchText = (
    event: React.ChangeEvent<HTMLInputElement>,
  ) => {
    const newText = event.target.value;
    setSearchText(newText);

    if (newText === '') {
      setSearchMessageList(newText);
    }
  };

  const handleClickSearch = () => {
    setSearchMessageList(searchText);
  };

  const handleMarkAsRead = () => {
    markAsReadQuery.mutate({ id: selectedIds });
  };

  const handleMarkAsUnread = () => {
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
        onClick: () => {
          handleMarkAsRead();
        },
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
        onClick: () => {
          handleMarkAsUnread();
        },
        hidden: !selectedIds.length,
      },
    },
  ];

  const handleClickNavigate = (message: MessageMetadata) => {
    navigate(message.id);
  };

  const handleChangeFilter = (filter: string) => {
    if (filter === filterEnum.unread) {
      setFilterUnreadMessageList(!filterUnread);
    }
  };

  // Handle infite scroll
  useEffect(() => {
    const handleScroll = () => {
      if (
        isLoadingMessage || isLoadingNextPage ||
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
      <div className="d-flex gap-16 align-items-center justify-content-between px-16 px-md-24 py-16 border-bottom">
        <SearchBar
          placeholder="Search messages"
          onChange={handlerChangeSearchText}
          onClick={handleClickSearch}
          isVariant={false}
        />
        {!theme?.is1d && (
          <Dropdown>
            <Dropdown.Trigger
              label={!md ? '' : t('filter')}
              size="sm"
              variant='ghost'
              icon={<IconFilter />}
            />
            <Dropdown.Menu>
              <Dropdown.CheckboxItem
                model={filterUnread ? [filterEnum.unread] : []}
                onChange={(value) => {
                  handleChangeFilter(value as string);
                }}
                value={filterEnum.unread}
                key={filterEnum.unread}
              >
                {t('filter.unread')}
              </Dropdown.CheckboxItem>
            </Dropdown.Menu>
          </Dropdown>
        )}
      </div>
      <List
        data={messages?.length === 0 ? undefined : messages?.map((message) => ({ ...message, _id: message.id }))}
        items={toolbar}
        isCheckable={true}
        onSelectedItems={(selectedIds) => setSelectedMessageIds(selectedIds)}
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
            style={{ '--edifice-columns': 8 } as React.CSSProperties}
            onClick={() => handleClickNavigate(message)}
            tabIndex={0}
            role="button"
            key={message.id}
          >
            <div className="d-flex align-items-center gap-8 g-col-3 flex-fill overflow-hidden">
              {checkbox}
              <MessagePreview message={message} />
            </div>
          </div>
        )}
      />
      {isLoadingMessage && (
        <Loading isLoading={true} className="justify-content-center my-12" />
      )}
      {!isLoadingMessage && !messages?.length && (
        <div className="d-flex flex-column gap-24 align-items-center justify-content-center">
          <EmptyScreen imageSrc={illuMessagerie} title={t('folder.empty.title')} text={t('folder.empty.text')}/>
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
