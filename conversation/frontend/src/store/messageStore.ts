import { create } from 'zustand';
import { Message } from '~/models';
import { createSelectors } from './createSelectors';

interface MessageState {
  message: Message | undefined;
  messageNeedToSave: boolean;
  inactives: { users: string[]; total: number } | undefined;
  setMessage: (value: Message | undefined) => void;
  setMessageNeedToSave: (value: boolean) => void;
  setInactives: (value: { users: string[]; total: number } | undefined) => void;
}

export const useMessageStore = createSelectors(
  create<MessageState>((set) => ({
    message: undefined,
    messageNeedToSave: false,
    inactives: undefined,
    setMessage: (message) => set({ message }),
    setMessageNeedToSave: (messageNeedToSave) => set({ messageNeedToSave }),
    setInactives: (inactives) => set({ inactives }),
  })),
);
