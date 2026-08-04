import { renderHook } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { useActionsStore } from '~/store';
import { useRights } from './useRights';

const mocks = vi.hoisted(() => ({
  useEdificeClient: vi.fn(),
}));

vi.mock('@edifice.io/react', async () => {
  const actual =
    await vi.importActual<typeof import('@edifice.io/react')>(
      '@edifice.io/react',
    );
  return {
    ...actual,
    useEdificeClient: mocks.useEdificeClient,
  };
});

describe('useRights', () => {
  afterEach(() => {
    useActionsStore.getState().setWorkflows({});
  });

  describe('workflow rights', () => {
    it('reflects the granted workflows from the store', () => {
      mocks.useEdificeClient.mockReturnValue({ user: {} });
      useActionsStore.getState().setWorkflows({
        'org.entcore.conversation.controllers.ConversationController|createDraft': true,
        'org.entcore.conversation.controllers.ApiController|recallMessage': true,
        'org.entcore.conversation.controllers.ConversationController|noReply': true,
      });

      const { result } = renderHook(useRights);

      expect(result.current.canCreateDraft).toBe(true);
      expect(result.current.canRecallMessages).toBe(true);
      expect(result.current.canSetNoReply).toBe(true);
    });

    it('defaults to false when a workflow is missing from the store', () => {
      mocks.useEdificeClient.mockReturnValue({ user: {} });
      useActionsStore.getState().setWorkflows({});

      const { result } = renderHook(useRights);

      expect(result.current.canCreateDraft).toBe(false);
      expect(result.current.canRecallMessages).toBe(false);
      expect(result.current.canSetNoReply).toBe(false);
    });
  });

  describe('canManageAbsence', () => {
    it.each(['ENSEIGNANT', 'PERSEDUCNAT'])(
      'is true for a %s profile',
      (type) => {
        mocks.useEdificeClient.mockReturnValue({ user: { type } });

        const { result } = renderHook(useRights);

        expect(result.current.canManageAbsence).toBe(true);
      },
    );

    it.each(['ELEVE', 'PERSRELELEVE', 'SUPERADMIN'])(
      'is false for a %s profile',
      (type) => {
        mocks.useEdificeClient.mockReturnValue({ user: { type } });

        const { result } = renderHook(useRights);

        expect(result.current.canManageAbsence).toBe(false);
      },
    );

    it('is false when there is no user type', () => {
      mocks.useEdificeClient.mockReturnValue({ user: {} });

      const { result } = renderHook(useRights);

      expect(result.current.canManageAbsence).toBe(false);
    });
  });
});
