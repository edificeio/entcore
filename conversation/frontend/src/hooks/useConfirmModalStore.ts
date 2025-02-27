import { create } from "zustand";
import { ReactNode } from "react";
import { ConfirmModalVariant } from "~/components/ConfirmModal/ConfirmModal";

type ConfirmModalState = {
  isOpen: boolean;
  id: string;
  header: ReactNode;
  body: ReactNode;
  variant?: ConfirmModalVariant;
  okText?: string;
  koText?: string;
  onSuccess: () => void;
  onCancel: () => void;
  openModal: (options: {
    id: string;
    header: ReactNode;
    body: ReactNode;
    variant?: ConfirmModalVariant;
    okText?: string;
    koText?: string;
    onSuccess: () => void;
    onCancel?: () => void;
  }) => void;
  closeModal: () => void;
};

export const useConfirmModalStore = create<ConfirmModalState>((set) => ({
  isOpen: false,
  id: "",
  header: "",
  body: "",
  variant: "ok/cancel",
  okText: undefined,
  koText: undefined,
  onSuccess: () => {},
  onCancel: () => {},

  openModal: ({ id, header, body, variant, okText, koText, onSuccess, onCancel }) =>
    set({
      isOpen: true,
      id,
      header,
      body,
      variant,
      okText,
      koText,
      onSuccess: () => {
        onSuccess();
        set({ isOpen: false });
      },
      onCancel: onCancel || (() => set({ isOpen: false })),
    }),

  closeModal: () => set({ isOpen: false }),
}));
