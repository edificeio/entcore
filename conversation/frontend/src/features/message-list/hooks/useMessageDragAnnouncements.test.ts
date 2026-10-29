import { Active, Over } from '@dnd-kit/core';
import { renderHook } from '@testing-library/react';
import { useMessageDragAnnouncements } from './useMessageDragAnnouncements';

vi.mock('~/hooks/useI18n', () => ({
  useI18n: () => ({
    t: (key: string, options?: { folderName?: string }) =>
      options?.folderName ? `${key} ${options.folderName}` : key,
  }),
}));

vi.mock('~/services', () => ({
  useFolderUtils: () => ({
    getFolderNameById: (id: string) =>
      id === 'folder-1' ? 'My folder' : undefined,
  }),
}));

const active = { id: 'message-1', data: { current: {} } } as unknown as Active;
const over = (folderId: string) =>
  ({ id: 'drop-zone', data: { current: { id: folderId } } }) as unknown as Over;

describe('useMessageDragAnnouncements', () => {
  const canDropOn = (folderId: string) => folderId !== 'outbox';

  it('announces the start and the cancellation of a drag', () => {
    const { result } = renderHook(() => useMessageDragAnnouncements(canDropOn));

    expect(result.current.onDragStart({ active })).toBe('drag.announce.start');
    expect(result.current.onDragCancel({ active, over: null })).toBe(
      'drag.announce.cancel',
    );
  });

  it('names the hovered folder instead of its id', () => {
    const { result } = renderHook(() => useMessageDragAnnouncements(canDropOn));

    expect(result.current.onDragOver({ active, over: over('folder-1') })).toBe(
      'drag.announce.over My folder',
    );
    expect(result.current.onDragOver({ active, over: over('inbox') })).toBe(
      'drag.announce.over inbox.folder',
    );
  });

  it('tells when the hovered folder does not accept the messages', () => {
    const { result } = renderHook(() => useMessageDragAnnouncements(canDropOn));

    expect(result.current.onDragOver({ active, over: over('outbox') })).toBe(
      'drag.announce.over.forbidden outbox.folder',
    );
  });

  it('announces a drop only when it is accepted', () => {
    const { result } = renderHook(() => useMessageDragAnnouncements(canDropOn));

    expect(result.current.onDragEnd({ active, over: over('folder-1') })).toBe(
      'drag.announce.drop My folder',
    );
    expect(result.current.onDragEnd({ active, over: over('outbox') })).toBe(
      'drag.announce.cancel',
    );
    expect(result.current.onDragEnd({ active, over: null })).toBe(
      'drag.announce.cancel',
    );
  });

  it('says nothing when leaving every folder', () => {
    const { result } = renderHook(() => useMessageDragAnnouncements(canDropOn));

    expect(result.current.onDragOver({ active, over: null })).toBeUndefined();
  });
});
