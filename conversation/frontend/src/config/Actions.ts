import { ACTION, ActionType, IAction } from '@edifice.io/client';

export const existingActions: IAction[] = [
  {
    id: ACTION.CREATE,
    workflow:
      'org.entcore.conversation.controllers.ConversationController|createDraft',
  },
  {
    id: 'recall' as ActionType,
    workflow:
      'org.entcore.conversation.controllers.ApiController|recallMessage',
  },
];
