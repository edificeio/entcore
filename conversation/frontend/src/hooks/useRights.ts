import { useActionsStore } from '~/store/actions';

/**
 * This hook checks the workflows rights the current user may have.
 * Workflow rights are always loaded by the root loader.
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

  return { canCreateDraft, canRecallMessages };
}
