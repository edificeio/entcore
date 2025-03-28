import { createStore, useStore } from 'zustand';
import { Folder, Message } from '~/models';

type FolderModal =
  | null
  | 'create'
  | 'move'
  | 'rename'
  | 'trash'
  | 'move-message'
  | 'add-attachment-to-workspace';

interface State {
  selectedMessageIds: string[];
  selectedFolders: Folder[];
  openFolderModal: FolderModal;
  messageUpdated?: Message;
  messageUpdatedNeedToSave: boolean;
}

type Action = {
  actions: {
    setSelectedMessageIds: (value: string[]) => void;
    setSelectedFolders: (value: Folder[]) => void;
    setOpenFolderModal: (value: FolderModal) => void;
    setMessageUpdated: (value: Message) => void;
    setMessageUpdatedNeedToSave: (value: boolean) => void;
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
  messageUpdated: undefined,
  messageUpdatedNeedToSave: false,
};

const store = createStore<State & Action>()((set) => ({
  ...initialState,
  actions: {
    setSelectedMessageIds: (selectedMessageIds: string[]) =>
      set({ selectedMessageIds }),
    setSelectedFolders: (selectedFolders: Folder[]) => set({ selectedFolders }),
    setOpenFolderModal: (openFolderModal: FolderModal) =>
      set({ openFolderModal }),
    setMessageUpdated: (message: Message) => set({ messageUpdated: message }),
    setMessageUpdatedNeedToSave: (messageUpdatedNeedToSave: boolean) =>
      set({ messageUpdatedNeedToSave }),
  },
}));

// Selectors
const selectedMessageIds = (state: ExtractState<typeof store>) =>
  state.selectedMessageIds;
const selectedFolders = (state: ExtractState<typeof store>) =>
  state.selectedFolders;
const setOpenFolderModal = (state: ExtractState<typeof store>) =>
  state.openFolderModal;
const setMessageUpdated = (state: ExtractState<typeof store>) =>
  state.messageUpdated;
const setMessageUpdatedNeedToSave = (state: ExtractState<typeof store>) =>
  state.messageUpdatedNeedToSave;
const actionsSelector = (state: ExtractState<typeof store>) => state.actions;

// Getters
export const getSelectedMessageIds = () => selectedMessageIds(store.getState());
export const getSelectedFolders = () => selectedFolders(store.getState());
export const getOpenFolderModal = () => setOpenFolderModal(store.getState());
export const getMessageUpdated = () => setMessageUpdated(store.getState());
export const getMessageUpdatedNeedToSave = () =>
  setMessageUpdatedNeedToSave(store.getState());

// React Store
function useAppStore<U>(selector: Params<U>[1]) {
  return useStore(store, selector);
}

// Hooks
export const useSelectedMessageIds = () => useAppStore(selectedMessageIds);
export const useSelectedFolders = () => useAppStore(selectedFolders);
export const useOpenFolderModal = () => useAppStore(setOpenFolderModal);
export const useMessageUpdated = () => useAppStore(setMessageUpdated);
export const useMessageUpdatedNeedToSave = () =>
  useAppStore(setMessageUpdatedNeedToSave);
export const useAppActions = () => useAppStore(actionsSelector);
