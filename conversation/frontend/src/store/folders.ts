import { createStore, useStore } from 'zustand';
import { Folder } from '~/models';

/**
 * Basic store for managing "folders" tree
 * Usage :
 * const foldersTree = getFoldersTree();
 * const { foldersTree } = useFoldersTreeStore.getState();
 * setFoldersTree(userRights);
 */
interface FolderStore {
  tree: Folder[];
}

type Action = {
  actions: {
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
  tree: [],
};

const store = createStore<FolderStore & Action>()((set) => ({
  ...initialState,
  actions: {
    setFoldersTree: (folders: Folder[]) => set(() => ({ tree: folders })),
  },
}));

// Selectors
const foldersTree = (state: ExtractState<typeof store>) => state.tree;
const actionsSelector = (state: ExtractState<typeof store>) => state.actions;

// Getters
export const getFoldersTree = () => foldersTree(store.getState());

// React Store
function useFoldersStore<U>(selector: Params<U>[1]) {
  return useStore(store, selector);
}

// Hooks
export const useFoldersTree = () => useFoldersStore(foldersTree);
export const useFoldersActions = () => useFoldersStore(actionsSelector);
