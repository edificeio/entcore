import { DragEndEvent, DragStartEvent } from '@dnd-kit/core';
import { act } from 'react';
import { mockCurrentUserPreview, mockMessageOfOutbox } from '~/mocks';
import { renderHook, wrapper } from '~/mocks/setup';
import { MessageMetadata } from '~/models';
import { useConfirmModalStore } from '~/store';
import { useActionsStore } from '~/store/actions';
import {
  canDropMessages,
  getDraggedMessageIds,
  useMessageDragAndDrop,
} from './useMessageDragAndDrop';

const mocks = vi.hoisted(() => ({
  useSelectedFolder: vi.fn(),
  moveMutate: vi.fn(),
  trashMutate: vi.fn(),
  folderMessages: [] as MessageMetadata[],
}));

vi.mock('~/hooks/useSelectedFolder', () => ({
  useSelectedFolder: mocks.useSelectedFolder,
}));

vi.mock('~/services', async () => {
  const actual =
    await vi.importActual<typeof import('~/services')>('~/services');
  return {
    ...actual,
    useMoveMessage: () => ({ mutate: mocks.moveMutate }),
    useTrashMessage: () => ({ mutate: mocks.trashMutate }),
    useFolderUtils: () => ({ getFolderNameById: () => 'My folder' }),
    useFolderMessages: () => ({ messages: mocks.folderMessages }),
  };
});

vi.mock('@edifice.io/react', async () => {
  const actual =
    await vi.importActual<typeof import('@edifice.io/react')>(
      '@edifice.io/react',
    );
  return {
    ...actual,
    useToast: () => ({ success: vi.fn() }),
    useEdificeClient: () => ({
      appCode: 'conversation',
      user: { userId: mockCurrentUserPreview.id },
    }),
  };
});

/** A message received by the current user */
const receivedMessage = (id: string): MessageMetadata => ({
  ...mockMessageOfOutbox,
  id,
  from: { id: 'someone-else', displayName: 'Someone', profile: 'Teacher' },
  to: { users: [mockCurrentUserPreview], groups: [] },
});

/** A message sent by the current user to others */
const sentMessage = (id: string): MessageMetadata => ({
  ...mockMessageOfOutbox,
  id,
});

const dragStart = (messageId: string) =>
  ({
    active: { id: messageId, data: { current: { messageId } } },
  }) as unknown as DragStartEvent;

const dragEnd = (messageId: string, targetFolderId?: string) =>
  ({
    active: { id: messageId, data: { current: { messageId } } },
    over: targetFolderId
      ? { id: 'droppable', data: { current: { id: targetFolderId } } }
      : null,
  }) as unknown as DragEndEvent;

describe('getDraggedMessageIds', () => {
  it('drags the whole selection when the dragged message is selected', () => {
    expect(getDraggedMessageIds('b', ['a', 'b', 'c'])).toEqual(['a', 'b', 'c']);
  });

  it('drags only the dragged message when it is not selected', () => {
    expect(getDraggedMessageIds('d', ['a', 'b'])).toEqual(['d']);
  });
});

describe('canDropMessages', () => {
  it.each([
    ['inbox', 'folder-1', ['inbox']],
    ['outbox', 'folder-1', ['outbox']],
    ['folder-2', 'folder-1', ['inbox']],
    ['inbox', 'trash', ['inbox']],
    ['draft', 'trash', ['draft']],
    ['folder-1', 'trash', ['inbox']],
    ['folder-1', 'inbox', ['inbox']],
    ['folder-1', 'outbox', ['outbox']],
    ['folder-1', 'draft', ['draft']],
    ['folder-1', 'inbox', ['inbox', 'outbox']],
    ['folder-1', 'outbox', ['inbox', 'outbox']],
  ])(
    'allows dropping from %s to %s messages from %j',
    (source, target, origins) => {
      expect(canDropMessages(source, target, origins)).toBe(true);
    },
  );

  it.each([
    ['trash', 'folder-1', ['inbox']],
    ['draft', 'folder-1', ['draft']],
    ['folder-1', 'folder-1', ['inbox']],
    ['inbox', 'inbox', ['inbox']],
    ['outbox', 'inbox', ['outbox']],
    ['draft', 'inbox', ['draft']],
    ['trash', 'inbox', ['inbox']],
    ['folder-1', 'outbox', ['inbox']],
    ['folder-1', 'inbox', ['outbox']],
    ['folder-1', 'draft', ['inbox', 'outbox']],
    [undefined, 'folder-1', ['inbox']],
  ])(
    'forbids dropping from %s to %s messages from %j',
    (source, target, origins) => {
      expect(canDropMessages(source, target, origins)).toBe(false);
    },
  );
});

describe('useMessageDragAndDrop', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mocks.useSelectedFolder.mockReturnValue({ folderId: 'inbox' });
    useActionsStore.getState().setSelectedMessageIds(['a', 'b']);
    useConfirmModalStore.getState().closeModal();
    mocks.folderMessages = [
      receivedMessage('a'),
      sentMessage('b'),
      receivedMessage('z'),
    ];
  });

  it('exposes the dragged messages while dragging', () => {
    const { result } = renderHook(() => useMessageDragAndDrop(), { wrapper });

    act(() => result.current.handleDragStart(dragStart('a')));
    expect(result.current.draggedMessageIds).toEqual(['a', 'b']);

    act(() => result.current.handleDragEnd(dragEnd('a')));
    expect(result.current.draggedMessageIds).toEqual([]);
  });

  it('moves the selected messages when dropped on a user folder', () => {
    const { result } = renderHook(() => useMessageDragAndDrop(), { wrapper });

    act(() => result.current.handleDragStart(dragStart('b')));
    act(() => result.current.handleDragEnd(dragEnd('b', 'folder-1')));

    expect(mocks.moveMutate).toHaveBeenCalledWith(
      { folderId: 'folder-1', id: ['a', 'b'] },
      expect.anything(),
    );
    expect(mocks.trashMutate).not.toHaveBeenCalled();
  });

  it('only accepts the origin folders of the dragged messages', () => {
    mocks.useSelectedFolder.mockReturnValue({ folderId: 'folder-1' });
    const { result } = renderHook(() => useMessageDragAndDrop(), { wrapper });

    act(() => result.current.handleDragStart(dragStart('a')));

    expect(result.current.canDropOn('inbox')).toBe(true);
    expect(result.current.canDropOn('outbox')).toBe(true);
    expect(result.current.canDropOn('draft')).toBe(false);
    expect(result.current.canDropOn('folder-2')).toBe(true);
  });

  it('asks for confirmation before putting messages back to their origin', () => {
    mocks.useSelectedFolder.mockReturnValue({ folderId: 'folder-1' });
    const { result } = renderHook(() => useMessageDragAndDrop(), { wrapper });

    act(() => result.current.handleDragStart(dragStart('a')));
    act(() => result.current.handleDragEnd(dragEnd('a', 'outbox')));

    expect(useConfirmModalStore.getState().isOpen).toBe(true);
    expect(mocks.moveMutate).not.toHaveBeenCalled();
  });

  it('puts every message back to its own origin once confirmed', () => {
    mocks.useSelectedFolder.mockReturnValue({ folderId: 'folder-1' });
    const { result } = renderHook(() => useMessageDragAndDrop(), { wrapper });

    act(() => result.current.handleDragStart(dragStart('a')));
    act(() => result.current.handleDragEnd(dragEnd('a', 'outbox')));
    act(() => useConfirmModalStore.getState().onSuccess());

    // "inbox" is the API target removing messages from their user's folder
    expect(mocks.moveMutate).toHaveBeenCalledWith(
      { folderId: 'inbox', id: ['a', 'b'] },
      expect.anything(),
    );
  });

  it('refuses a system folder which is not the origin of the dragged message', () => {
    mocks.useSelectedFolder.mockReturnValue({ folderId: 'folder-1' });
    const { result } = renderHook(() => useMessageDragAndDrop(), { wrapper });

    act(() => result.current.handleDragStart(dragStart('z')));
    act(() => result.current.handleDragEnd(dragEnd('z', 'outbox')));

    expect(useConfirmModalStore.getState().isOpen).toBe(false);
    expect(mocks.moveMutate).not.toHaveBeenCalled();
  });

  it('trashes only the dragged message when it is not selected', () => {
    const { result } = renderHook(() => useMessageDragAndDrop(), { wrapper });

    act(() => result.current.handleDragStart(dragStart('z')));
    act(() => result.current.handleDragEnd(dragEnd('z', 'trash')));

    expect(mocks.trashMutate).toHaveBeenCalledWith({ id: ['z'] });
    expect(mocks.moveMutate).not.toHaveBeenCalled();
  });

  it('does nothing when dropped outside a target', () => {
    const { result } = renderHook(() => useMessageDragAndDrop(), { wrapper });

    act(() => result.current.handleDragStart(dragStart('a')));
    act(() => result.current.handleDragEnd(dragEnd('a')));

    expect(mocks.moveMutate).not.toHaveBeenCalled();
    expect(mocks.trashMutate).not.toHaveBeenCalled();
  });

  it('does nothing when the drop is forbidden', () => {
    mocks.useSelectedFolder.mockReturnValue({ folderId: 'draft' });
    const { result } = renderHook(() => useMessageDragAndDrop(), { wrapper });

    act(() => result.current.handleDragStart(dragStart('a')));
    act(() => result.current.handleDragEnd(dragEnd('a', 'folder-1')));

    expect(mocks.moveMutate).not.toHaveBeenCalled();
  });
});
