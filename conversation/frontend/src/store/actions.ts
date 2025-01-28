import { createStore, useStore } from 'zustand';
import { Folder } from '~/models';

type FolderModal = null | 'create' | 'move' | 'rename' | 'delete';

interface State {
  openFolderModal: FolderModal;
  selectedMessageIds: string[];
  foldersTree: Folder[];
}

type Action = {
  actions: {
    setOpenFolderModal: (value: FolderModal) => void;
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
  openFolderModal: null,
  selectedMessageIds: [],
  foldersTree: [],
};

const store = createStore<State & Action>()((set) => ({
  ...initialState,
  actions: {
    setOpenFolderModal: (openFolderModal: FolderModal) =>
      set({ openFolderModal }),
    setSelectedMessageIds: (selectedMessageIds: string[]) =>
      set({ selectedMessageIds }),
    setFoldersTree: (foldersTree: Folder[]) => set(() => ({ foldersTree })),
  },
}));

// Selectors
const setOpenFolderModal = (state: ExtractState<typeof store>) =>
  state.openFolderModal;
const selectedMessageIds = (state: ExtractState<typeof store>) =>
  state.selectedMessageIds;
const foldersTree = (state: ExtractState<typeof store>) => state.foldersTree;
const actionsSelector = (state: ExtractState<typeof store>) => state.actions;

// Getters
export const getOpenFolderModal = () => setOpenFolderModal(store.getState());
export const getSelectedMessageIds = () => selectedMessageIds(store.getState());
export const getFoldersTree = () => foldersTree(store.getState());

// React Store
function useAppStore<U>(selector: Params<U>[1]) {
  return useStore(store, selector);
}

// Hooks
export const useOpenFolderModal = () => useAppStore(setOpenFolderModal);
export const useSelectedMessageIds = () => useAppStore(selectedMessageIds);
export const useFoldersTree = () => useAppStore(foldersTree);
export const useAppActions = () => useAppStore(actionsSelector);
