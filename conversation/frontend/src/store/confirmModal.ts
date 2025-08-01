import { create } from 'zustand';
import { ReactNode } from 'react';
import { ModalSize } from '@edifice.io/react';

type ConfirmModalVariant = 'yes/no' | 'ok/cancel';

interface ConfirmModalState {
  isOpen: boolean;
  id: string;
  header: ReactNode;
  body: ReactNode;
  variant?: ConfirmModalVariant;
  okText?: string;
  koText?: string;
  size?: ModalSize;

  onSuccess: () => void;
  onCancel: () => void;
}

interface ConfirmModalActions {
  openModal: (options: {
    id: string;
    header: ReactNode;
    body: ReactNode;
    variant?: ConfirmModalVariant;
    okText?: string;
    koText?: string;
    size?: ModalSize;
    onSuccess: () => void;
    onCancel?: () => void;
  }) => void;
  closeModal: () => void;
}

type ConfirmModalStore = ConfirmModalState & ConfirmModalActions;

export const useConfirmModalStore = create<ConfirmModalStore>((set) => ({
  isOpen: false,
  id: '',
  header: '',
  body: '',
  variant: 'ok/cancel',
  okText: undefined,
  koText: undefined,
  onSuccess: () => {},
  onCancel: () => {},

  openModal: ({
    id,
    header,
    body,
    variant,
    okText,
    koText,
    size,
    onSuccess,
    onCancel,
  }) =>
    set({
      isOpen: true,
      id,
      header,
      body,
      variant,
      okText,
      koText,
      size,
      onSuccess: () => {
        if (onSuccess) onSuccess();
        set({ isOpen: false });
      },
      onCancel: () => {
        if (onCancel) onCancel();
        set({ isOpen: false });
      },
    }),

  closeModal: () => set({ isOpen: false }),
}));
