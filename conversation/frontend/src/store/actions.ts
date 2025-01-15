import { createStore, useStore } from 'zustand';
interface State {
  openPrintModal: boolean;
  searchMessageList: string;
  filterUnreadMessageList: boolean;
  selectedMessageIds: string[];
}

type Action = {
  actions: {
    setOpenPrintModal: (value: boolean) => void;
    setSearchMessageList: (value: string) => void;
    setFilterUnreadMessageList: (value: boolean) => void;
    setSelectedMessageIds: (value: string[]) => void;
  };
};

type ExtractState<S> = S extends {
  getState: () => infer T;
}
  ? T
  : never;

type Params<U> = Parameters<typeof useStore<typeof store, U>>;

const initialState = {
  openPrintModal: false,
  searchMessageList: '',
  filterUnreadMessageList: false,
  selectedMessageIds: [],
};

const store = createStore<State & Action>()((set) => ({
  ...initialState,
  actions: {
    setOpenPrintModal: (openPrintModal: boolean) => set({ openPrintModal }),
    setSearchMessageList: (searchMessageList: string) => set({ searchMessageList }),
    setFilterUnreadMessageList: (filterUnreadMessageList: boolean) => set({ filterUnreadMessageList }),
    setSelectedMessageIds: (selectedMessageIds: string[]) => set({ selectedMessageIds }),
  },
}));

// Selectors
const openPrintModal = (state: ExtractState<typeof store>) =>
  state.openPrintModal;
const searchMessageList = (state: ExtractState<typeof store>) =>
  state.searchMessageList;
const filterUnreadMessageList = (state: ExtractState<typeof store>) =>
  state.filterUnreadMessageList;
const selectedMessageIds = (state: ExtractState<typeof store>) =>  state.selectedMessageIds;
const actionsSelector = (state: ExtractState<typeof store>) => state.actions;

// Getters
export const getOpenPrintModal = () => openPrintModal(store.getState());
export const getSearchMessageList = () => searchMessageList(store.getState());
export const getFilterUnreadMessageList = () => filterUnreadMessageList(store.getState());
export const getSelectedMessageIds = () => selectedMessageIds(store.getState());

// React Store
function useAppStore<U>(selector: Params<U>[1]) {
  return useStore(store, selector);
}

// Hooks
export const useOpenPrintModal = () => useAppStore(openPrintModal);
export const useSearchMessageList = () => useAppStore(searchMessageList);
export const useFilterUnreadMessageList = () => useAppStore(filterUnreadMessageList);
export const useSelectedMessageIds = () => useAppStore(selectedMessageIds);
export const useAppActions = () => useAppStore(actionsSelector);
