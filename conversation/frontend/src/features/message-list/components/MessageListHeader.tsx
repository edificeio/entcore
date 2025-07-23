import {
  Dropdown,
  SearchBar,
  useBreakpoint,
  useEdificeClient,
} from '@edifice.io/react';
import { IconFilter } from '@edifice.io/react/icons';
import { useCallback, useEffect, useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useSearchParams } from 'react-router-dom';
import { useSelectedFolder } from '~/hooks/useSelectedFolder';
import { useActionsStore } from '~/store/actions';

export function MessageListHeader() {
  const { appCode } = useEdificeClient();
  const { t } = useTranslation(appCode);
  const { lg } = useBreakpoint();
  const { folderId } = useSelectedFolder();
  const [searchParams, setSearchParams] = useSearchParams();
  const setSelectedMessageIds = useActionsStore.use.setSelectedMessageIds();
  const [isSearchFocused, setIsSearchFocused] = useState(false);
  const [isSearchDisabled, setIsSearchDisabled] = useState(true);

  const filterEnum = {
    unread: 'UNREAD',
  };
  const [searchText, setSearchText] = useState('');

  const filters = useMemo(
    () => (searchParams.get('unread') === 'true' ? [filterEnum.unread] : []),
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [searchParams],
  );

  useEffect(() => {
    const srch = searchParams.get('search');
    if (srch) {
      setSearchText(srch);
    } else {
      setSearchText('');
    }
  }, [searchParams]);

  const handleSearchTextChange = (
    event: React.ChangeEvent<HTMLInputElement>,
  ) => {
    const value = event.target.value;
    setSearchText(value);

    if (value === '') {
      searchParams.delete('search');
      setSearchParams(searchParams, { replace: true });
    }

    setIsSearchDisabled(value.length < 3);
  };

  const handleSearchClick = () => {
    if (searchText && searchText.length > 2) {
      searchParams.set('search', searchText);
    } else {
      searchParams.delete('search');
    }
    setSearchParams(searchParams, { replace: true });
  };

  const handleUnreadFilterChange = useCallback(() => {
    const unread = searchParams.get('unread') === 'true';
    if (!unread) {
      searchParams.set('unread', 'true');
    } else {
      searchParams.delete('unread');
    }
    setSelectedMessageIds([]);
    setSearchParams(searchParams, { replace: true });
  }, [searchParams, setSearchParams, setSelectedMessageIds]);

  const handleSearchFocus = () => {
    setIsSearchFocused(true);
  };

  const handleSearchBlur = () => {
    setIsSearchFocused(false);
  };

  return (
    <div className="d-flex gap-16 align-items-center justify-content-between px-16 px-lg-24 py-16 border-bottom">
      <SearchBar
        placeholder={
          isSearchFocused
            ? t('search.placeholder.focused')
            : t('search.placeholder')
        }
        onChange={handleSearchTextChange}
        onClick={handleSearchClick}
        isVariant={false}
        size="lg"
        value={searchText}
        data-testid="search-bar"
        onFocus={handleSearchFocus}
        onBlur={handleSearchBlur}
        buttonDisabled={isSearchDisabled}
      />
      {!['outbox', 'draft'].includes(folderId!) && (
        <Dropdown data-testid="filter-dropdown">
          <Dropdown.Trigger
            label={!lg ? '' : t('filter')}
            badgeContent={filters.length}
            variant="ghost"
            icon={<IconFilter />}
          />
          <Dropdown.Menu>
            <Dropdown.CheckboxItem
              model={filters}
              onChange={handleUnreadFilterChange}
              value={filterEnum.unread}
              key={filterEnum.unread}
            >
              {t('filter.unread')}
            </Dropdown.CheckboxItem>
          </Dropdown.Menu>
        </Dropdown>
      )}
    </div>
  );
}
