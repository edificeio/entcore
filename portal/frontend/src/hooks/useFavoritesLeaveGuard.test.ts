import { renderHook } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { useFavoritesReorderStore } from '~/store/favoritesReorderStore';
import { useConfirmLeaveReorder } from './useConfirmLeaveReorder';
import { useUnsavedOrderWarning } from './useUnsavedOrderWarning';

const dispatchBeforeUnload = () => {
  const event = new Event('beforeunload', { cancelable: true });
  window.dispatchEvent(event);
  return event;
};

beforeEach(() => {
  useFavoritesReorderStore.setState({ hasUnsavedOrder: false });
  vi.restoreAllMocks();
});

describe('useUnsavedOrderWarning', () => {
  it('warns before leaving the page while the order is not saved', () => {
    renderHook(() => useUnsavedOrderWarning(true));

    expect(useFavoritesReorderStore.getState().hasUnsavedOrder).toBe(true);
    expect(dispatchBeforeUnload().defaultPrevented).toBe(true);
  });

  it('does not warn when the order did not change', () => {
    renderHook(() => useUnsavedOrderWarning(false));

    expect(useFavoritesReorderStore.getState().hasUnsavedOrder).toBe(false);
    expect(dispatchBeforeUnload().defaultPrevented).toBe(false);
  });

  it('stops warning once the favorites list is gone', () => {
    const { unmount } = renderHook(() => useUnsavedOrderWarning(true));
    unmount();

    expect(useFavoritesReorderStore.getState().hasUnsavedOrder).toBe(false);
    expect(dispatchBeforeUnload().defaultPrevented).toBe(false);
  });
});

describe('useConfirmLeaveReorder', () => {
  it('lets the user leave without asking when nothing is unsaved', () => {
    const confirm = vi.spyOn(window, 'confirm');
    const { result } = renderHook(() => useConfirmLeaveReorder());

    expect(result.current.confirmLeave()).toBe(true);
    expect(confirm).not.toHaveBeenCalled();
  });

  it('asks the user before leaving an unsaved order', () => {
    useFavoritesReorderStore.setState({ hasUnsavedOrder: true });
    const confirm = vi.spyOn(window, 'confirm').mockReturnValue(false);
    const { result } = renderHook(() => useConfirmLeaveReorder());

    expect(result.current.confirmLeave()).toBe(false);
    expect(confirm).toHaveBeenCalledTimes(1);
  });
});
