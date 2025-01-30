import {
  Dropdown,
  SearchBar,
  useBreakpoint,
  useEdificeClient,
  useEdificeTheme,
} from '@edifice.io/react';
import { IconFilter } from '@edifice.io/react/icons';
import { useCallback, useEffect, useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useSearchParams } from 'react-router-dom';
import { useAppActions } from '~/store/actions';

export function FolderHeader() {
  const { theme } = useEdificeTheme();
  const { appCode } = useEdificeClient();
  const { t } = useTranslation(appCode);
  const { md } = useBreakpoint();
  const [searchParams, setSearchParams] = useSearchParams();
  const { setSelectedMessageIds } = useAppActions();

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
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const handleSearchTextChange = (
    event: React.ChangeEvent<HTMLInputElement>,
  ) => {
    setSearchText(event.target.value);

    if (event.target.value === '') {
      searchParams.delete('search');
      setSearchParams(searchParams, { replace: true });
    }
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

  return (
    <div className="d-flex gap-16 align-items-center justify-content-between px-16 px-md-24 py-16 border-bottom">
      <SearchBar
        placeholder={t('search.placeholder')}
        onChange={handleSearchTextChange}
        onClick={handleSearchClick}
        isVariant={false}
        size="lg"
        value={searchText}
        data-testid="search-bar"
      />
      {!theme?.is1d && (
        <Dropdown data-testid="filter-dropdown">
          <Dropdown.Trigger
            label={!md ? '' : t('filter')}
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
