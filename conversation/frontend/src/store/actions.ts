import { createStore, useStore } from 'zustand';
interface State {
  openPrintModal: boolean;
}

type Action = {
  actions: {
    setOpenPrintModal: (value: boolean) => void;
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
};

const store = createStore<State & Action>()((set) => ({
  ...initialState,
  actions: {
    setOpenPrintModal: (openPrintModal: boolean) => set({ openPrintModal }),
  },
}));

// Selectors
const openPrintModal = (state: ExtractState<typeof store>) =>
  state.openPrintModal;
const actionsSelector = (state: ExtractState<typeof store>) => state.actions;
// Getters
export const getOpenPrintModal = () => openPrintModal(store.getState());
// React Store
function useAppStore<U>(selector: Params<U>[1]) {
  return useStore(store, selector);
}

// Hooks
export const useOpenPrintModal = () => useAppStore(openPrintModal);
export const useAppActions = () => useAppStore(actionsSelector);
