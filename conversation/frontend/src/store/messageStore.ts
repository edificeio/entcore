import { createStore, useStore } from 'zustand';
import { Message } from '~/models';

interface State {
  message: Message | undefined;
  messageNeedToSave: boolean;
}

type Action = {
  actions: {
    setMessage: (value: Message | undefined) => void;
    setMessageNeedToSave: (value: boolean) => void;
  };
};

type ExtractState<S> = S extends {
  getState: () => infer T;
}
  ? T
  : never;

type Params<U> = Parameters<typeof useStore<typeof store, U>>;

const initialState: State = {
  message: undefined,
  messageNeedToSave: false,
};

const store = createStore<State & Action>()((set) => ({
  ...initialState,
  actions: {
    setMessage: (message: Message | undefined) => set({ message }),
    setMessageNeedToSave: (messageNeedToSave: boolean) =>
      set({ messageNeedToSave }),
  },
}));

// Selectors
const setMessage = (state: ExtractState<typeof store>) => state.message;
const setMessageNeedToSave = (state: ExtractState<typeof store>) =>
  state.messageNeedToSave;
const actionsSelector = (state: ExtractState<typeof store>) => state.actions;

// React Store
function useMessagerieStore<U>(selector: Params<U>[1]) {
  return useStore(store, selector);
}

// Hooks
export const useMessage = () => useMessagerieStore(setMessage);
export const useMessageNeedToSave = () =>
  useMessagerieStore(setMessageNeedToSave);
export const useMessageActions = () => useMessagerieStore(actionsSelector);
