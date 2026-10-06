import { useActionsStore } from '~/store/actions';

/**
 * This hook checks the workflow rights the current user may have, always
 * loaded by the root loader. They are keyed by backend `controller|action`,
 * or by the right's name when the backend overrides it (`conversation.absence`).
 */
export function useRights() {
  const actions = useActionsStore.use.workflows();
  const canCreateDraft =
    actions?.[
      'org.entcore.conversation.controllers.ConversationController|createDraft'
    ] ?? false;
  const canRecallMessages =
    actions?.[
      'org.entcore.conversation.controllers.ApiController|recallMessage'
    ] ?? false;

  const canSetNoReply =
    actions?.[
      'org.entcore.conversation.controllers.ConversationController|noReply'
    ] ?? false;

  const canManageAbsence = actions?.['conversation.absence'] ?? false;

  return { canCreateDraft, canRecallMessages, canSetNoReply, canManageAbsence };
}
