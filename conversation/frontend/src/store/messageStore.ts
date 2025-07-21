import { create } from 'zustand';
import { Message } from '~/models';
import { createSelectors } from './createSelectors';

interface MessageState {
  message: Message | undefined;
  messageNeedToSave: boolean;
  inactiveUsers: string[] | undefined;
  setMessage: (value: Message | undefined) => void;
  setMessageNeedToSave: (value: boolean) => void;
  setInactiveUsers: (value: string[] | undefined) => void;
}

export const useMessageStore = createSelectors(
  create<MessageState>((set) => ({
    message: undefined,
    messageNeedToSave: false,
    inactiveUsers: undefined,
    setMessage: (message) => set({ message }),
    setMessageNeedToSave: (messageNeedToSave) => set({ messageNeedToSave }),
    setInactiveUsers: (inactiveUsers) => set({ inactiveUsers }),
  })),
);
