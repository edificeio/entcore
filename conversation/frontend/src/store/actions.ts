import { create } from 'zustand';
import { Config } from '~/config';
import { Folder } from '~/models';
import { createSelectors } from './createSelectors';

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
  inactives: { users: string[]; total: number };
  setWorkflows: (workflows: Record<string, boolean>) => void;
  setConfig: (config: Config) => void;
  setSelectedMessageIds: (value: string[]) => void;
  setSelectedFolders: (value: Folder[]) => void;
  setOpenedModal: (value: OpenedModal) => void;
  setInactives: (value: { users: string[]; total: number }) => void;
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
    inactives: { users: [], total: 0 },
    setWorkflows: (workflows) => set({ workflows }),
    setConfig: (config) => set({ config }),
    setSelectedMessageIds: (selectedMessageIds) => set({ selectedMessageIds }),
    setSelectedFolders: (selectedFolders) => set({ selectedFolders }),
    setOpenedModal: (openedModal) => set({ openedModal }),
    setInactives: (inactives) => set({ inactives }),
  })),
);
