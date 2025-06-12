import { create } from 'zustand';
import { Category } from '~/models/category';

type CategoryStore = {
  activeCategory: Category;
  setActiveCategory: (category: Category) => void;
};

export const useCategoryStore = create<CategoryStore>((set) => ({
  activeCategory: 'all',
  setActiveCategory: (category) => set({ activeCategory: category }),
}));