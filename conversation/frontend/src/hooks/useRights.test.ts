import { renderHook } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { useActionsStore } from '~/store';
import { useRights } from './useRights';

describe('useRights', () => {
  afterEach(() => {
    useActionsStore.getState().setWorkflows({});
  });

  describe('workflow rights', () => {
    it('reflects the granted workflows from the store', () => {
      useActionsStore.getState().setWorkflows({
        'org.entcore.conversation.controllers.ConversationController|createDraft': true,
        'org.entcore.conversation.controllers.ApiController|recallMessage': true,
        'org.entcore.conversation.controllers.ConversationController|noReply': true,
        'conversation.absence': true,
      });

      const { result } = renderHook(useRights);

      expect(result.current.canCreateDraft).toBe(true);
      expect(result.current.canRecallMessages).toBe(true);
      expect(result.current.canSetNoReply).toBe(true);
      expect(result.current.canManageAbsence).toBe(true);
    });

    it('defaults to false when a workflow is missing from the store', () => {
      useActionsStore.getState().setWorkflows({});

      const { result } = renderHook(useRights);

      expect(result.current.canCreateDraft).toBe(false);
      expect(result.current.canRecallMessages).toBe(false);
      expect(result.current.canSetNoReply).toBe(false);
      expect(result.current.canManageAbsence).toBe(false);
    });
  });
});
