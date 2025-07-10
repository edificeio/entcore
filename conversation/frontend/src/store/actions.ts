import { create } from 'zustand';
import { createSelectors } from './createSelectors';
import { Config } from '~/config';
import { Folder } from '~/models';

type OpenedModal =
  | undefined
  | 'create'
  | 'create-then-move'
  | 'move'
  | 'rename'
  | 'trash'
  | 'move-message'
  | 'signature';

interface ActionsState {
  workflows: Record<string, boolean>;
  config: Config;
  selectedMessageIds: string[];
  selectedFolders: Folder[];
  openedModal: OpenedModal;
  inactiveUsers: string[];
  setWorkflows: (workflows: Record<string, boolean>) => void;
  setConfig: (config: Config) => void;
  setSelectedMessageIds: (value: string[]) => void;
  setSelectedFolders: (value: Folder[]) => void;
  setOpenedModal: (value: OpenedModal) => void;
  setInactiveUsers: (value: string[]) => void;
}

export const useActionsStore = createSelectors(
  create<ActionsState>((set) => ({
    workflows: {
      'org.entcore.conversation.controllers.ConversationController|createDraft': false,
      'org.entcore.conversation.controllers.ApiController|recallMessage': false,
    } as Record<string, boolean>,
    config: { maxDepth: 3, recallDelayMinutes: 60 } as Config,
    selectedMessageIds: [],
    selectedFolders: [],
    openedModal: undefined,
    inactiveUsers: [],
    setWorkflows: (workflows) => set({ workflows }),
    setConfig: (config) => set({ config }),
    setSelectedMessageIds: (selectedMessageIds) => set({ selectedMessageIds }),
    setSelectedFolders: (selectedFolders) => set({ selectedFolders }),
    setOpenedModal: (openedModal) => set({ openedModal }),
    setInactiveUsers: (inactiveUsers) => set({ inactiveUsers }),
  })),
);
