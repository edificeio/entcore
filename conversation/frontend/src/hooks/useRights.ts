import { useEdificeClient } from '@edifice.io/react';
import { useActionsStore } from '~/store/actions';

/** Only these profiles can have an absence message: teachers and non-teaching staff. */
const ABSENCE_ALLOWED_PROFILES = ['ENSEIGNANT', 'PERSEDUCNAT'];

/**
 * This hook checks the rights the current user may have. Most are workflow
 * rights, always loaded by the root loader, keyed by backend
 * `controller|action`. `canManageAbsence` is not one of those: it's a
 * client-side heuristic based on the user's profile (`user.type`), not a
 * backend-granted workflow right.
 */
export function useRights() {
  const { user } = useEdificeClient();
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

  const canManageAbsence =
    !!user?.type && ABSENCE_ALLOWED_PROFILES.includes(user.type);

  return { canCreateDraft, canRecallMessages, canSetNoReply, canManageAbsence };
}
