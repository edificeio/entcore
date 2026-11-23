import { act, render, screen } from '@testing-library/react';
import { useState } from 'react';
import { RouterProvider, createMemoryRouter } from 'react-router-dom';
import { useUnsavedChangesGuard } from './useUnsavedChangesGuard';

/**
 * jsdom's AbortSignal is not accepted by Node's Request, which the router
 * creates on every navigation. These routes have no loader, so the signal
 * can safely be dropped.
 */
class RequestWithoutSignal extends Request {
  constructor(input: RequestInfo | URL, init: RequestInit = {}) {
    const { signal: _signal, ...rest } = init;
    super(input, rest);
  }
}
vi.stubGlobal('Request', RequestWithoutSignal);

let guard: ReturnType<typeof useUnsavedChangesGuard>;
let setWhen: (when: boolean) => void;

const Guarded = ({ initialWhen }: { initialWhen: boolean }) => {
  const [when, setWhenState] = useState(initialWhen);
  setWhen = setWhenState;
  guard = useUnsavedChangesGuard(when);
  return <div>guarded</div>;
};

const renderGuard = (when: boolean) => {
  const router = createMemoryRouter(
    [
      { path: '/customize', element: <Guarded initialWhen={when} /> },
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

  it('lets the navigation through once the user confirms', async () => {
    const router = renderGuard(true);

    await act(() => router.navigate('/other'));
    await act(async () => guard.blocker.proceed?.());

    expect(router.state.location.pathname).toBe('/other');
    expect(screen.getByText('other')).toBeInTheDocument();
  });

  it('stays on the page when the user cancels', async () => {
    const router = renderGuard(true);

    await act(() => router.navigate('/other'));
    act(() => guard.blocker.reset?.());

    expect(guard.blocker.state).toBe('unblocked');
    expect(router.state.location.pathname).toBe('/customize');
  });

  it('does not block a change of query string only', async () => {
    const router = renderGuard(true);

    await act(() => router.navigate('/customize?callback=/other'));

    expect(guard.blocker.state).toBe('unblocked');
    expect(router.state.location.search).toBe('?callback=/other');
  });

  it('releases a blocked navigation once there is nothing left to protect', async () => {
    const router = renderGuard(true);

    await act(() => router.navigate('/other'));
    act(() => setWhen(false));

    expect(guard.blocker.state).toBe('unblocked');
    expect(dispatchBeforeUnload()).toBe(false);
  });

  it('does not block anything without unsaved changes', async () => {
    const router = renderGuard(false);

    expect(dispatchBeforeUnload()).toBe(false);
    await act(() => router.navigate('/other'));
    expect(router.state.location.pathname).toBe('/other');
  });

  it('warns before unloading the page, unless bypassed', () => {
    renderGuard(true);

    expect(dispatchBeforeUnload()).toBe(true);
    act(() => guard.bypass());
    expect(dispatchBeforeUnload()).toBe(false);
  });

  it('lets in-app navigation through once bypassed', async () => {
    const router = renderGuard(true);

    act(() => guard.bypass());
    await act(() => router.navigate('/other'));

    expect(router.state.location.pathname).toBe('/other');
  });
});
