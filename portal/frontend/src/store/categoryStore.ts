import { create } from 'zustand';
import { CategoryId } from '~/models/category';

type CategoryStore = {
  activeCategory: CategoryId;
  setActiveCategory: (category: CategoryId) => void;
};

export const useCategoryStore = create<CategoryStore>((set) => ({
  activeCategory: 'none',
  setActiveCategory: (category) => set({ activeCategory: category }),
}));