import { createStore, useStore } from 'zustand';
interface State {
  selectedMessageIds: string[];
}

type Action = {
  actions: {
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
  selectedMessageIds: [],
};

const store = createStore<State & Action>()((set) => ({
  ...initialState,
  actions: {
    setSelectedMessageIds: (selectedMessageIds: string[]) => set({ selectedMessageIds }),
  },
}));

// Selectors
const selectedMessageIds = (state: ExtractState<typeof store>) =>  state.selectedMessageIds;
const actionsSelector = (state: ExtractState<typeof store>) => state.actions;

// Getters
export const getSelectedMessageIds = () => selectedMessageIds(store.getState());

// React Store
function useAppStore<U>(selector: Params<U>[1]) {
  return useStore(store, selector);
}

// Hooks
export const useSelectedMessageIds = () => useAppStore(selectedMessageIds);
export const useAppActions = () => useAppStore(actionsSelector);
