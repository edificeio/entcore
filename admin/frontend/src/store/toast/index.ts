import { create } from 'zustand';

/** A short message confirming the outcome of an action, already translated. */
export interface Toast {
  severity: 'success' | 'error';
  message: string;
}

interface ToastState {
  /** Last toast, kept while it closes so its content does not vanish mid-animation. */
  toast: Toast | null;
  open: boolean;
  showToast: (toast: Toast) => void;
  closeToast: () => void;
}

/**
 * Global toast, so that a page can confirm an action after navigating away
 * (e.g. back to the reports list once a case is processed).
 */
export const useToastStore = create<ToastState>((set) => ({
  toast: null,
  open: false,
  showToast: (toast) => set({ toast, open: true }),
  closeToast: () => set({ open: false }),
}));

/** Show a toast from anywhere, including outside React components. */
export const showToast = (toast: Toast) =>
  useToastStore.getState().showToast(toast);
