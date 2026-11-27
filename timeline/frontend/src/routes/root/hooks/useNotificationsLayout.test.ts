import { act, renderHook } from '@testing-library/react';
import { useNotificationsLayout } from './useNotificationsLayout';

const STORAGE_KEY = 'timeline:notificationsOpen';

const mocks = vi.hoisted(() => ({
  useUser: vi.fn(),
  updateOverlayOpen: vi.fn(),
}));

vi.mock('@edifice.io/react', () => ({
  useBreakpoint: () => ({ sm: true, md: true }),
  useOverlay: () => ({ updateOverlayOpen: mocks.updateOverlayOpen }),
  useUser: mocks.useUser,
}));

const asUser = (userId?: string) =>
  mocks.useUser.mockReturnValue({ user: userId ? { userId } : undefined });

const renderLayout = () =>
  renderHook(() => useNotificationsLayout({ isWidgetsPanelOpen: false }));

const stored = () => JSON.parse(localStorage.getItem(STORAGE_KEY) ?? 'null');

describe('useNotificationsLayout', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('stores the open state for the current user only', () => {
    asUser('user-1');
    const { result } = renderLayout();

    act(() => result.current.toggleNotifications());

    expect(result.current.isSidebarOpen).toBe(true);
    expect(stored()).toEqual({ 'user-1': true });
  });

  it("does not override another user's open state when closing", () => {
    localStorage.setItem(
      STORAGE_KEY,
      JSON.stringify({ 'user-1': true, 'user-2': true }),
    );
    asUser('user-2');
    const { result } = renderLayout();
    expect(result.current.isSidebarOpen).toBe(true);

    act(() => result.current.closeNotifications());

    expect(result.current.isSidebarOpen).toBe(false);
    expect(stored()).toEqual({ 'user-1': true });
  });

  it("restores the user's state once the session is loaded", () => {
    localStorage.setItem(STORAGE_KEY, JSON.stringify({ 'user-1': true }));
    asUser(undefined);
    const { result, rerender } = renderLayout();
    expect(result.current.isSidebarOpen).toBe(false);

    asUser('user-1');
    rerender();

    expect(result.current.isSidebarOpen).toBe(true);
    expect(stored()).toEqual({ 'user-1': true });
  });

  it('ignores the legacy shared boolean format', () => {
    localStorage.setItem(STORAGE_KEY, 'true');
    asUser('user-1');
    const { result } = renderLayout();

    expect(result.current.isSidebarOpen).toBe(false);
    expect(stored()).toEqual({});
  });

  it('recovers from an invalid stored value', () => {
    localStorage.setItem(STORAGE_KEY, '{');
    asUser('user-1');
    const { result } = renderLayout();

    expect(result.current.isSidebarOpen).toBe(false);
    expect(stored()).toEqual({});
  });

  it('does not write anything while the user is unknown', () => {
    asUser(undefined);
    const { result } = renderLayout();

    act(() => result.current.toggleNotifications());

    expect(result.current.isSidebarOpen).toBe(true);
    expect(localStorage.getItem(STORAGE_KEY)).toBeNull();
  });

  it('still toggles the panel when the storage is unavailable', () => {
    vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => {
      throw new Error('QuotaExceededError');
    });
    asUser('user-1');
    const { result } = renderLayout();

    act(() => result.current.toggleNotifications());

    expect(result.current.isSidebarOpen).toBe(true);
  });
});
