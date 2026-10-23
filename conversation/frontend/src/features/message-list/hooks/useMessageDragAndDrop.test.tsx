import { DragEndEvent, DragStartEvent } from '@dnd-kit/core';
import { act } from 'react';
import { renderHook, wrapper } from '~/mocks/setup';
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
  };
});

vi.mock('@edifice.io/react', async () => {
  const actual =
    await vi.importActual<typeof import('@edifice.io/react')>(
      '@edifice.io/react',
    );
  return { ...actual, useToast: () => ({ success: vi.fn() }) };
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
    ['inbox', 'folder-1'],
    ['outbox', 'folder-1'],
    ['folder-2', 'folder-1'],
    ['inbox', 'trash'],
    ['draft', 'trash'],
    ['folder-1', 'trash'],
    ['folder-1', 'inbox'],
  ])('allows dropping from %s to %s', (source, target) => {
    expect(canDropMessages(source, target)).toBe(true);
  });

  it.each([
    ['trash', 'folder-1'],
    ['draft', 'folder-1'],
    ['folder-1', 'folder-1'],
    ['inbox', 'inbox'],
    ['outbox', 'inbox'],
    ['draft', 'inbox'],
    ['trash', 'inbox'],
    ['folder-1', 'outbox'],
    [undefined, 'folder-1'],
  ])('forbids dropping from %s to %s', (source, target) => {
    expect(canDropMessages(source, target)).toBe(false);
  });
});

describe('useMessageDragAndDrop', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mocks.useSelectedFolder.mockReturnValue({ folderId: 'inbox' });
    useActionsStore.getState().setSelectedMessageIds(['a', 'b']);
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

  it('removes the selected messages from their folder when dropped on the inbox', () => {
    mocks.useSelectedFolder.mockReturnValue({ folderId: 'folder-1' });
    const { result } = renderHook(() => useMessageDragAndDrop(), { wrapper });

    act(() => result.current.handleDragStart(dragStart('a')));
    act(() => result.current.handleDragEnd(dragEnd('a', 'inbox')));

    expect(mocks.moveMutate).toHaveBeenCalledWith(
      { folderId: 'inbox', id: ['a', 'b'] },
      expect.anything(),
    );
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
