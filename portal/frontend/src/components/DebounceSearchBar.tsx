import { SearchBar } from '@edifice.io/react';
import { useDebounce } from '@edifice.io/react';
import { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { useConfirmLeaveReorder } from '~/hooks/useConfirmLeaveReorder';

interface DebounceSearchBarProps {
  onDebouncedChange: (value: string) => void;
  applicationLength: number;
}

export const DebounceSearchBar = ({
  onDebouncedChange,
  applicationLength,
}: DebounceSearchBarProps) => {
  const { t } = useTranslation('common');
  const { confirmLeave } = useConfirmLeaveReorder();
  const [search, setSearch] = useState('');
  const debouncedSearch =
    applicationLength > 100 ? useDebounce(search, 500) : search;

  useEffect(() => {
    onDebouncedChange(debouncedSearch);
  }, [debouncedSearch, onDebouncedChange]);

  return (
    <SearchBar
      clearable
      isVariant
      value={search}
      onChange={(e) => {
        const value = e.target.value;
        const isStartingSearch = !search && !!value;
        if (isStartingSearch && !confirmLeave()) return;
        setSearch(value);
      }}
      placeholder={t('my.apps.search')}
      className="my-apps-search"
      size="md"
    />
  );
};
