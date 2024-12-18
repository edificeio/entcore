import { RightRole } from 'edifice-ts-client';
import { createStore, useStore } from 'zustand';

type UserRights = Record<RightRole, boolean>;

/**
 * Basic store for managing "rights" array
 * Use this store with `checkUserRight` utils
 * You can check rights in a react-router loader
 * And set userRights with the store to get a stable global state
 * 
 * const userRights = await checkUserRight(rights);
  const { setUserRights } = useUserRightsStore.getState();
  setUserRights(userRights);
 */
interface State {
  userRights: UserRights;
}

type Action = {
  actions: {
    setUserRights: (userRights: UserRights) => void;
  };
};

type ExtractState<S> = S extends {
  getState: () => infer T;
}
  ? T
  : never;

type Params<U> = Parameters<typeof useStore<typeof store, U>>;

const initialState = {
  userRights: {
    creator: false,
    contrib: false,
    manager: false,
    read: false,
  },
};

const store = createStore<State & Action>()((set) => ({
  ...initialState,
  actions: {
    setUserRights: (userRights: UserRights) => set(() => ({ userRights })),
  },
}));

// Selectors
const userRights = (state: ExtractState<typeof store>) => state.userRights;
const actionsSelector = (state: ExtractState<typeof store>) => state.actions;

// Getters
export const getUserRights = () => userRights(store.getState());
export const getUserRightsActions = () => actionsSelector(store.getState());

// React Store
function useUserRightsStore<U>(selector: Params<U>[1]) {
  return useStore(store, selector);
}

// Hooks
export const useUserRights = () => useUserRightsStore(userRights);
export const useUserRightsActions = () => useUserRightsStore(actionsSelector);
