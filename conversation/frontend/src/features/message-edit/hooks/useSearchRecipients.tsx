import { ChangeEvent, useEffect, useReducer } from 'react';

import { OptionListItemType, useDebounce } from '@edifice.io/react';
import { Visible } from '~/models/visible';
import { userService } from '~/services';

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
  onRecipientSelected: (recipient: Visible) => void;
}

export const useSearchRecipients = ({
  recipientType,
  onRecipientSelected,
}: useSearchRecipientsProps) => {
  const [state, dispatch] = useReducer(reducer, initialState);

  const debouncedSearchInputValue = useDebounce<string>(
    state.searchInputValue,
    500,
  );

  useEffect(() => {
    search(debouncedSearchInputValue);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [debouncedSearchInputValue]);

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
    // start search from 1 caracter length for non Adml but start from 3 for Adml
    if (debouncedSearchInputValue.length >= searchMinLength) {
      const resSearchVisibles = await userService.searchVisible(
        debouncedSearchInputValue,
      );

      dispatch({
        type: 'addApiResult',
        payload: resSearchVisibles,
      });

      const adaptedResults: OptionListItemType[] = resSearchVisibles
        .filter((visible) => {
          return visible.usedIn
            .map((ui) => ui.toLowerCase())
            .includes(recipientType);
        })
        .map((searchResult: Visible) => {
          return {
            value: searchResult.id,
            label: searchResult.displayName,
          };
        });

      dispatch({
        type: 'addResult',
        payload: adaptedResults,
      });
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

  const handleSearchResultsChange = (visible: Visible) => {
    onRecipientSelected(visible);
  };

  const showSearchNoResults = (): boolean => {
    return (
      !state.isSearching &&
      debouncedSearchInputValue.length > searchMinLength &&
      state.searchResults.length === 0
    );
  };

  const showSearchLoading = (): boolean => {
    return state.isSearching;
  };

  const searchMinLength = 3;

  return {
    state,
    searchMinLength,
    showSearchLoading,
    showSearchNoResults,
    handleSearchInputChange,
    handleSearchResultsChange,
  };
};
