import { QueryClient } from '@tanstack/react-query';
import {
  LoaderFunctionArgs,
  useNavigate,
  useSearchParams,
} from 'react-router-dom';
import { MessageMetadata } from '~/models';
import {
  folderQueryOptions,
  useFolderMessages,
  useMarkRead,
  useMarkUnread,
} from '~/services';
import { MessagePreview } from './message-preview';
import {
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
  useEdificeClient,
  useEdificeTheme,
} from '@edifice.io/react';
import clsx from 'clsx';
import { useEffect, useMemo, useRef, useState } from 'react';
import { useAppActions, useSelectedMessageIds } from '~/store/actions';
import { useTranslation } from 'react-i18next';
import { useSelectedFolder } from '~/hooks';
import illuMessagerie from '@images/emptyscreen/illu-messagerie.svg';

export const loader =
  (_queryClient: QueryClient) =>
  async ({ params, request }: LoaderFunctionArgs) => {
    const { searchParams } = new URL(request.url);
    const search = searchParams.get('search');
    const unread = searchParams.get('unread');
    const messagesQuery = folderQueryOptions.getMessages(params.folderId!, {
      search: search && search !== '' ? search : undefined,
      unread: unread === 'true' ? true : undefined,
    });
    const messages = await _queryClient.ensureInfiniteQueryData(messagesQuery);
    return { messages };
  };

export function Component() {
  const { folderId } = useSelectedFolder();
  const { theme } = useEdificeTheme();
  const navigate = useNavigate();

  const { appCode } = useEdificeClient();
  const { t } = useTranslation(appCode);

  const filterEnum = {
    unread: 'UNREAD',
  };
  const [searchText, setSearchText] = useState<string>('');
  const filterRef = useRef<string[]>([]);

  const { setSelectedMessageIds } = useAppActions();
  const selectedIds = useSelectedMessageIds();
  const [searchParams, setSearchParams] = useSearchParams();
  const markAsReadQuery = useMarkRead();
  const markAsUnreadQuery = useMarkUnread();
  const { md } = useBreakpoint();

  const {
    messages,
    isPending: isLoadingMessage,
    isFetchingNextPage: isLoadingNextPage,
    hasNextPage,
    fetchNextPage,
  } = useFolderMessages(folderId!);

  const setFilterUnread = (value: boolean) => {
    filterRef.current = value
      ? [...filterRef.current, filterEnum.unread]
      : filterRef.current.filter((filter) => filter !== filterEnum.unread);
  };

  const handleSearchTextChange = (
    event: React.ChangeEvent<HTMLInputElement>,
  ) => {
    const newText = event.target.value;
    setSearchText(newText === '' ? '' : newText);
  };

  const handleSearchClick = () => {
    updateSearchParams();
  };

  const handleMarkAsReadClick = () => {
    markAsReadQuery.mutate({ id: selectedIds });
  };

  const handleMarkAsUnreadClick = () => {
    markAsUnreadQuery.mutate({ id: selectedIds });
  };

  const updateSearchParams = () => {
    const params = new URLSearchParams();
    if (searchText && searchText !== '') {
      params.set('search', searchText);
    }
    if (filterRef.current.includes(filterEnum.unread)) {
      params.set('unread', 'true');
    }
    setSearchParams(params, { replace: true });
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

  const handleFilterChange = (filter: string | number) => {
    setFilterUnread(!filterRef.current.includes(filter as string));
    updateSearchParams();
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

  useEffect(() => {
    const search = searchParams.get('search');
    if (search) {
      setSearchText(search);
    }
    const unread = searchParams.get('unread');
    setFilterUnread(!!unread);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [searchParams]);

  return (
    <>
      <div className="d-flex gap-16 align-items-center justify-content-between px-16 px-md-24 py-16 border-bottom">
        <SearchBar
          placeholder="Search messages"
          onChange={handleSearchTextChange}
          onClick={handleSearchClick}
          isVariant={false}
          defaultValue={searchText}
        />
        {!theme?.is1d && (
          <Dropdown>
            <Dropdown.Trigger
              label={!md ? '' : t('filter')}
              size="sm"
              variant="ghost"
              icon={<IconFilter />}
            />
            <Dropdown.Menu>
              <Dropdown.CheckboxItem
                model={filterRef.current}
                onChange={handleFilterChange}
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
        data={messages.map((message) => ({ ...message, _id: message.id }))
        }
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
