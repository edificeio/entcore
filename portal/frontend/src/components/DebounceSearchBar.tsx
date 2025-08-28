import { SearchBar } from '@edifice.io/react';
import { useDebounce } from '@edifice.io/react';
import { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';

interface DebounceSearchBarProps {
  onDebouncedChange: (value: string) => void;
}

export const DebounceSearchBar = ({
  onDebouncedChange,
}: DebounceSearchBarProps) => {
  const { t } = useTranslation('common');
  const [search, setSearch] = useState('');
  const debouncedSearch = useDebounce(search, 500);

  useEffect(() => {
    onDebouncedChange(debouncedSearch);
  }, [debouncedSearch, onDebouncedChange]);

  return (
    <SearchBar
      clearable
      isVariant
      value={search}
      onChange={(e) => setSearch(e.target.value)}
      placeholder={t('my.apps.search')}
      className="my-apps-search"
      size="md"
    />
  );
};
