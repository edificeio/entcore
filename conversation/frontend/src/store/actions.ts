import { createStore, useStore } from 'zustand';
import { Folder } from '~/models';

interface State {
  selectedMessageIds: string[];
  foldersTree: Folder[];
}

type Action = {
  actions: {
    setSelectedMessageIds: (value: string[]) => void;
    setFoldersTree: (folders: Folder[]) => void;
  };
};

type ExtractState<S> = S extends {
  getState: () => infer T;
}
  ? T
  : never;

type Params<U> = Parameters<typeof useStore<typeof store, U>>;

const initialState = {
  selectedMessageIds: [],
  foldersTree: [],
};

const store = createStore<State & Action>()((set) => ({
  ...initialState,
  actions: {
    setSelectedMessageIds: (selectedMessageIds: string[]) =>
      set({ selectedMessageIds }),
    setFoldersTree: (foldersTree: Folder[]) => set(() => ({ foldersTree })),
  },
}));

// Selectors
const selectedMessageIds = (state: ExtractState<typeof store>) =>
  state.selectedMessageIds;
const foldersTree = (state: ExtractState<typeof store>) => state.foldersTree;
const actionsSelector = (state: ExtractState<typeof store>) => state.actions;

// Getters
export const getSelectedMessageIds = () => selectedMessageIds(store.getState());
export const getFoldersTree = () => foldersTree(store.getState());

// React Store
function useAppStore<U>(selector: Params<U>[1]) {
  return useStore(store, selector);
}

// Hooks
export const useSelectedMessageIds = () => useAppStore(selectedMessageIds);
export const useFoldersTree = () => useAppStore(foldersTree);
export const useAppActions = () => useAppStore(actionsSelector);
