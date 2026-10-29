import { create } from 'zustand';

type FavoritesReorderStore = {
  hasUnsavedOrder: boolean;
  setHasUnsavedOrder: (hasUnsavedOrder: boolean) => void;
};

export const useFavoritesReorderStore = create<FavoritesReorderStore>(
  (set) => ({
    hasUnsavedOrder: false,
    setHasUnsavedOrder: (hasUnsavedOrder) => set({ hasUnsavedOrder }),
  }),
);
