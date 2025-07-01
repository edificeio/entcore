import { create } from 'zustand';
import { createSelectors } from './createSelectors';

interface ScrollState {
  currentScrollPosition: number;
  setCurrentScrollPosition: (position: number) => void;
  savedScrollPosition: number;
  setSavedScrollPosition: (position: number) => void;
}

export const useScrollStore = createSelectors(
  create<ScrollState>((set) => ({
    savedScrollPosition: 0,
    setSavedScrollPosition: (position) =>
      set({ savedScrollPosition: position }),
    currentScrollPosition: 0,
    setCurrentScrollPosition: (position) =>
      set({ currentScrollPosition: position }),
  })),
);
