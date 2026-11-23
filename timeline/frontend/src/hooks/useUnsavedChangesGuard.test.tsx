import { act, render, screen } from '@testing-library/react';
import { RouterProvider, createMemoryRouter } from 'react-router-dom';
import { useUnsavedChangesGuard } from './useUnsavedChangesGuard';

let guard: ReturnType<typeof useUnsavedChangesGuard>;

const Guarded = ({ when }: { when: boolean }) => {
  guard = useUnsavedChangesGuard(when);
  return <div>guarded</div>;
};

const renderGuard = (when: boolean) => {
  const router = createMemoryRouter(
    [
      { path: '/customize', element: <Guarded when={when} /> },
      { path: '/other', element: <div>other</div> },
    ],
    { initialEntries: ['/customize'] },
  );
  render(<RouterProvider router={router} />);
  return router;
};

const dispatchBeforeUnload = () => {
  const event = new Event('beforeunload', { cancelable: true });
  window.dispatchEvent(event);
  return event.defaultPrevented;
};

describe('useUnsavedChangesGuard', () => {
  it('blocks in-app navigation while there are unsaved changes', async () => {
    const router = renderGuard(true);

    await act(() => router.navigate('/other'));

    expect(guard.blocker.state).toBe('blocked');
    expect(screen.getByText('guarded')).toBeInTheDocument();
  });

  it('stays on the page when the user cancels', async () => {
    const router = renderGuard(true);

    await act(() => router.navigate('/other'));
    act(() => guard.blocker.reset?.());

    expect(guard.blocker.state).toBe('unblocked');
    expect(router.state.location.pathname).toBe('/customize');
  });

  it('does not warn before unloading without unsaved changes', () => {
    renderGuard(false);

    expect(dispatchBeforeUnload()).toBe(false);
  });

  it('warns before unloading the page, unless bypassed', () => {
    renderGuard(true);

    expect(dispatchBeforeUnload()).toBe(true);
    act(() => guard.bypass());
    expect(dispatchBeforeUnload()).toBe(false);
  });
});
