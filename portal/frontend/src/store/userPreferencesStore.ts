import { create } from 'zustand';

type State = {
  applications: string[];
  bookmarks: string[];
  isHydrated: boolean;
  toggleBookmark: (appName: string) => void;
  setBookmarks: (bookmarks: string[]) => void;
  setPreferences: (prefs: {
    applications: string[];
    bookmarks: string[];
  }) => void;
};

export const useUserPreferencesStore = create<State>((set) => ({
  applications: [],
  bookmarks: [],
  isHydrated: false,
  toggleBookmark: (appName) =>
    set((state) => {
      const exists = state.bookmarks.includes(appName);
      return {
        bookmarks: exists
          ? state.bookmarks.filter((b) => b !== appName)
          : [...state.bookmarks, appName],
      };
    }),
  setBookmarks: (bookmarks) => set({ bookmarks }),
  setPreferences: ({ applications, bookmarks }) =>
    set({ applications, bookmarks, isHydrated: true }),
}));
