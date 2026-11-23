import { act, fireEvent, render, screen } from '@testing-library/react';
import { RouterProvider, createMemoryRouter } from 'react-router-dom';
import { Component as CustomizePage } from './customize';

/** See useUnsavedChangesGuard.test.tsx: jsdom's AbortSignal breaks Node's Request. */
class RequestWithoutSignal extends Request {
  constructor(input: RequestInfo | URL, init: RequestInit = {}) {
    const { signal: _signal, ...rest } = init;
    super(input, rest);
  }
}
vi.stubGlobal('Request', RequestWithoutSignal);

const mocks = vi.hoisted(() => ({
  form: { isDirty: true, isSaving: false, saveChanges: vi.fn() },
}));

vi.mock('@edifice.io/react', async () => {
  const actual =
    await vi.importActual<typeof import('@edifice.io/react')>(
      '@edifice.io/react',
    );
  const PageLayoutMock = ({ children }: { children: React.ReactNode }) => (
    <div>{children}</div>
  );
  PageLayoutMock.Header = () => null;
  PageLayoutMock.Content = ({ children }: { children: React.ReactNode }) => (
    <div>{children}</div>
  );
  return {
    ...actual,
    PageLayout: PageLayoutMock,
    useEdificeClient: () => ({ init: true }),
    useBreakpoint: () => ({ md: false, lg: false }),
    useBackground: () => ({ background: 'default' }),
  };
});

vi.mock('~/hooks/useCustomizationForm', () => ({
  useCustomizationForm: () => mocks.form,
}));
vi.mock('~/components/CustomizationForm', () => ({
  CustomizationForm: () => null,
}));
vi.mock('~/hooks/useI18n', () => ({
  useI18n: () => ({ t: (key: string) => key, common_t: (key: string) => key }),
}));

const renderPage = (initialEntries = ['/customize']) => {
  const router = createMemoryRouter(
    [
      { path: '/', element: <div>home</div> },
      { path: '/other', element: <div>other</div> },
      { path: '/customize', element: <CustomizePage /> },
    ],
    { initialEntries, initialIndex: initialEntries.length - 1 },
  );
  render(<RouterProvider router={router} />);
  return router;
};

const isLeaveModalOpen = () =>
  screen.queryByText('homepage.customize.leave.title') !== null;

const dispatchBeforeUnload = () => {
  const event = new Event('beforeunload', { cancelable: true });
  window.dispatchEvent(event);
  return event.defaultPrevented;
};

describe('Customize page – leaving with unsaved changes', () => {
  beforeEach(() => {
    mocks.form.isDirty = true;
    mocks.form.saveChanges.mockReset();
  });

  afterEach(() => vi.restoreAllMocks());

  it('opens the leave modal on a blocked in-app navigation, and stays on cancel', async () => {
    const router = renderPage(['/', '/customize']);

    await act(() => router.navigate(-1));
    expect(isLeaveModalOpen()).toBe(true);

    fireEvent.click(screen.getByTestId('customize-leave-back-button'));

    expect(router.state.location.pathname).toBe('/customize');
    expect(dispatchBeforeUnload()).toBe(true);
  });

  it('lets the blocked navigation through when the user confirms', async () => {
    const router = renderPage(['/', '/customize']);

    await act(() => router.navigate('/other'));
    await act(async () =>
      fireEvent.click(screen.getByTestId('customize-leave-cancel-button')),
    );

    expect(router.state.location.pathname).toBe('/other');
  });

  it('goes back in history from the back button once confirmed', async () => {
    vi.spyOn(window.history, 'length', 'get').mockReturnValue(2);
    const router = renderPage(['/other', '/customize']);

    fireEvent.click(screen.getByTestId('customize-back-button'));
    expect(isLeaveModalOpen()).toBe(true);
    await act(async () =>
      fireEvent.click(screen.getByTestId('customize-leave-cancel-button')),
    );

    expect(router.state.location.pathname).toBe('/other');
  });

  it('goes to the home page from the back button when there is no history', async () => {
    const router = renderPage(['/customize']);

    fireEvent.click(screen.getByTestId('customize-back-button'));
    await act(async () =>
      fireEvent.click(screen.getByTestId('customize-leave-cancel-button')),
    );

    expect(router.state.location.pathname).toBe('/');
  });

  it('only disables the guard right before the reload of a save', () => {
    renderPage();

    fireEvent.click(screen.getByText('save'));
    expect(dispatchBeforeUnload()).toBe(true);

    const [{ onBeforeReload }] = mocks.form.saveChanges.mock.calls[0];
    act(() => onBeforeReload());
    expect(dispatchBeforeUnload()).toBe(false);
  });
});
