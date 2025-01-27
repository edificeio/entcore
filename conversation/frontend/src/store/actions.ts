import { createStore, useStore } from 'zustand';
import { Folder } from '~/models';

type FolderModal = null | 'create' | 'move' | 'rename' | 'trash';

interface State {
  selectedMessageIds: string[];
  selectedFolders: Folder[];
  openFolderModal: FolderModal;
  foldersTree: Folder[];
}

type Action = {
  actions: {
    setSelectedMessageIds: (value: string[]) => void;
    setSelectedFolders: (value: Folder[]) => void;
    setOpenFolderModal: (value: FolderModal) => void;
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
  selectedFolders: [],
  openFolderModal: null,
  foldersTree: [],
};

const store = createStore<State & Action>()((set) => ({
  ...initialState,
  actions: {
    setSelectedMessageIds: (selectedMessageIds: string[]) =>
      set({ selectedMessageIds }),
    setSelectedFolders: (selectedFolders: Folder[]) => set({ selectedFolders }),
    setOpenFolderModal: (openFolderModal: FolderModal) =>
      set({ openFolderModal }),
    setFoldersTree: (foldersTree: Folder[]) => set(() => ({ foldersTree })),
  },
}));

// Selectors
const selectedMessageIds = (state: ExtractState<typeof store>) =>
  state.selectedMessageIds;
const selectedFolders = (state: ExtractState<typeof store>) =>
  state.selectedFolders;
const setOpenFolderModal = (state: ExtractState<typeof store>) =>
  state.openFolderModal;
const foldersTree = (state: ExtractState<typeof store>) => state.foldersTree;
const actionsSelector = (state: ExtractState<typeof store>) => state.actions;

// Getters
export const getSelectedMessageIds = () => selectedMessageIds(store.getState());
export const getSelectedFolders = () => selectedFolders(store.getState());
export const getOpenFolderModal = () => setOpenFolderModal(store.getState());
export const getFoldersTree = () => foldersTree(store.getState());

// React Store
function useAppStore<U>(selector: Params<U>[1]) {
  return useStore(store, selector);
}

// Hooks
export const useSelectedMessageIds = () => useAppStore(selectedMessageIds);
export const useSelectedFolders = () => useAppStore(selectedFolders);
export const useOpenFolderModal = () => useAppStore(setOpenFolderModal);
export const useFoldersTree = () => useAppStore(foldersTree);
export const useAppActions = () => useAppStore(actionsSelector);
