import { create } from 'zustand';
import { createSelectors } from './createSelectors';
import { Message } from '~/models';

interface MessageState {
  message: Message | undefined;
  messageNeedToSave: boolean;
  setMessage: (value: Message | undefined) => void;
  setMessageNeedToSave: (value: boolean) => void;
}

export const useMessageStore = createSelectors(
  create<MessageState>((set) => ({
    message: undefined,
    messageNeedToSave: false,
    setMessage: (message) => set({ message }),
    setMessageNeedToSave: (messageNeedToSave) => set({ messageNeedToSave }),
  })),
);
