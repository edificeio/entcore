import { ChangeEvent, useEffect, useReducer } from 'react';

import { OptionListItemType, useDebounce } from '@edifice.io/react';
import { useQueryClient } from '@tanstack/react-query';
import { Visible } from '~/models/visible';
import { useDefaultBookmark, userQueryOptions } from '~/services/queries/user';

type State = {
  searchInputValue: string;
  searchResults: OptionListItemType[];
  searchAPIResults: Visible[];
  isSearching: boolean;
};

type Action =
  | { type: 'onChange'; payload: string }
  | { type: 'isSearching'; payload: boolean }
  | { type: 'addResult'; payload: OptionListItemType[] }
  | { type: 'addApiResult'; payload: Visible[] }
  | { type: 'on'; payload: Visible[] }
  | { type: 'updateSearchResult'; payload: OptionListItemType[] }
  | { type: 'emptyResult'; payload: OptionListItemType[] };

const initialState = {
  searchInputValue: '',
  searchResults: [],
  searchAPIResults: [],
  isSearching: false,
};

function reducer(state: State, action: Action) {
  switch (action.type) {
    case 'onChange':
      return { ...state, searchInputValue: action.payload };
    case 'isSearching':
      return { ...state, isSearching: action.payload };
    case 'addResult':
      return { ...state, searchResults: action.payload };
    case 'addApiResult':
      return { ...state, searchAPIResults: action.payload };
    case 'updateSearchResult':
      return { ...state, searchResults: action.payload };
    case 'emptyResult':
      return { ...state, searchResults: action.payload };
    default:
      throw new Error(`Unhandled action type`);
  }
}

export interface useSearchRecipientsProps {
  recipientType: 'to' | 'cc' | 'cci';
}

export const useSearchRecipients = ({
  recipientType,
}: useSearchRecipientsProps) => {
  const [state, dispatch] = useReducer(reducer, initialState);

  const debouncedSearchInputValue = useDebounce<string>(
    state.searchInputValue,
    500,
  );
  const queryClient = useQueryClient();
  const { data: defaultBookmarks } = useDefaultBookmark();

  useEffect(() => {
    search(debouncedSearchInputValue);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [debouncedSearchInputValue]);

  useEffect(() => {
    if (defaultBookmarks) {
      updateVisiblesFound(defaultBookmarks);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [defaultBookmarks]);

  const updateVisiblesFound = (visibles: Visible[]) => {
    dispatch({
      type: 'addApiResult',
      payload: visibles,
    });

    const adaptedResults: OptionListItemType[] = visibles.map(
      (searchResult: Visible) => {
        return {
          value: searchResult.id,
          label: searchResult.displayName,
          disabled: !searchResult.usedIn
            .map((ui) => ui.toLowerCase())
            .includes(recipientType),
        };
      },
    );

    dispatch({
      type: 'addResult',
      payload: adaptedResults,
    });
  };

  const handleSearchInputChange = (event: ChangeEvent<HTMLInputElement>) => {
    const value = event.target.value;
    dispatch({
      type: 'onChange',
      payload: value,
    });
  };

  const search = async (debouncedSearchInputValue: string) => {
    dispatch({
      type: 'isSearching',
      payload: true,
    });
    if (
      defaultBookmarks ||
      debouncedSearchInputValue.length >= searchMinLength
    ) {
      let searchVisibles = defaultBookmarks || [];
      // start search from 1 caracter length for non Adml but start from 3 for Adml
      if (debouncedSearchInputValue.length >= searchMinLength) {
        searchVisibles = await queryClient.ensureQueryData(
          userQueryOptions.searchVisible(debouncedSearchInputValue),
        );
      }
      updateVisiblesFound(searchVisibles);
    } else {
      dispatch({
        type: 'emptyResult',
        payload: [],
      });
      Promise.resolve();
    }

    dispatch({
      type: 'isSearching',
      payload: false,
    });
  };

  const searchMinLength = 1;

  const hasSearchNoResults =
    !state.isSearching &&
    debouncedSearchInputValue.length > searchMinLength &&
    state.searchResults.length === 0;

  return {
    state,
    defaultBookmarks,
    searchMinLength,
    isSearchLoading: state.isSearching,
    hasSearchNoResults,
    handleSearchInputChange,
  };
};
