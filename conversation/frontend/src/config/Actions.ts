import { ACTION, IAction } from 'edifice-ts-client';

export const existingActions: IAction[] = [
  {
    id: ACTION.CREATE,
    workflow:
      'org.entcore.conversation.controllers.ConversationController|createDraft',
  },
];
