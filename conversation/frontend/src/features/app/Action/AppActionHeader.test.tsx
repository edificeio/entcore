import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '~/mocks/setup';
import { AppActionHeader } from './AppActionHeader';

const mocks = vi.hoisted(() => ({
  useEdificeClient: vi.fn(),
}));

vi.mock('@edifice.io/react', async () => {
  const actual = await vi.importActual('@edifice.io/react');
  return {
    ...actual,
    useEdificeClient: mocks.useEdificeClient,
  };
});

describe('AppActionHeader', () => {
  afterEach(() => {
    vi.clearAllMocks();
  });

  it.each(['ENSEIGNANT', 'PERSEDUCNAT'])(
    'shows the absence menu item for a %s profile',
    async (profile) => {
      mocks.useEdificeClient.mockReturnValue({ user: { type: profile } });

      const { user } = render(<AppActionHeader />);
      await user.click(screen.getByTestId('dropdown').querySelector('button')!);

      expect(
        screen.getByText('conversation.absence.menu.label'),
      ).toBeInTheDocument();
    },
  );

  it.each(['ELEVE', 'PERSRELELEVE', 'SUPERADMIN'])(
    'hides the absence menu item for a %s profile',
    async (profile) => {
      mocks.useEdificeClient.mockReturnValue({ user: { type: profile } });

      const { user } = render(<AppActionHeader />);
      await user.click(screen.getByTestId('dropdown').querySelector('button')!);

      expect(
        screen.queryByText('conversation.absence.menu.label'),
      ).not.toBeInTheDocument();
    },
  );

  it('opens the absence modal by portal when the menu item is clicked', async () => {
    mocks.useEdificeClient.mockReturnValue({ user: { type: 'ENSEIGNANT' } });

    const { user } = render(<AppActionHeader />);
    await user.click(screen.getByTestId('dropdown').querySelector('button')!);
    await user.click(screen.getByText('conversation.absence.menu.label'));

    expect(document.getElementById('modalAbsence')).toBeInTheDocument();
  });
});
