import { forwardRef } from 'react';
import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '~/mocks/setup';
import { AppActionHeader } from './AppActionHeader';

const mocks = vi.hoisted(() => ({
  useRights: vi.fn(),
}));

vi.mock('~/hooks/useRights', () => ({
  useRights: mocks.useRights,
}));

// The tiptap `Editor` of the absence modal focuses itself asynchronously. In
// jsdom (no layout, no `Range.getClientRects`) that throws, sometimes after the
// test has ended, and vitest then fails the run. These tests only check that
// the modal opens, so the editor is stubbed, as in `AbsenceModalForm.test.tsx`.
vi.mock('@edifice.io/react/editor', async (importOriginal) => ({
  ...(await importOriginal<typeof import('@edifice.io/react/editor')>()),
  Editor: forwardRef(function EditorStub() {
    return <div data-testid="absence-editor-stub" />;
  }),
}));

describe('AppActionHeader', () => {
  afterEach(() => {
    vi.clearAllMocks();
  });

  it('shows the absence menu item when the user can manage absence', async () => {
    mocks.useRights.mockReturnValue({ canManageAbsence: true });

    const { user } = render(<AppActionHeader />);
    await user.click(screen.getByTestId('dropdown').querySelector('button')!);

    expect(
      screen.getByText('conversation.absence.menu.label'),
    ).toBeInTheDocument();
  });

  it('hides the absence menu item when the user cannot manage absence', async () => {
    mocks.useRights.mockReturnValue({ canManageAbsence: false });

    const { user } = render(<AppActionHeader />);
    await user.click(screen.getByTestId('dropdown').querySelector('button')!);

    expect(
      screen.queryByText('conversation.absence.menu.label'),
    ).not.toBeInTheDocument();
  });

  it('opens the absence modal by portal when the menu item is clicked', async () => {
    mocks.useRights.mockReturnValue({ canManageAbsence: true });

    const { user } = render(<AppActionHeader />);
    await user.click(screen.getByTestId('dropdown').querySelector('button')!);
    await user.click(screen.getByText('conversation.absence.menu.label'));

    expect(document.getElementById('modalAbsence')).toBeInTheDocument();
  });
});
