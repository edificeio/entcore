import { afterEach, describe, expect, it } from 'vitest';

import { act, fireEvent, render, screen, waitFor } from '~/mocks/setup';
import { showToast, useToastStore } from '~/store/toast';

describe('Toaster', () => {
  afterEach(() => {
    act(() => useToastStore.getState().closeToast());
  });

  it('shows the last toast as an alert', async () => {
    // The toaster is mounted by the app providers
    render(<div />);

    act(() =>
      showToast({
        severity: 'success',
        message: 'Contenu conservé. Le signalement a été traité.',
      }),
    );

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Contenu conservé. Le signalement a été traité.',
    );
  });

  it('closes on its close button', async () => {
    render(<div />);

    act(() => showToast({ severity: 'error', message: 'Oups' }));
    fireEvent.click(await screen.findByRole('button', { name: 'Fermer' }));

    await waitFor(() =>
      expect(screen.queryByRole('alert')).not.toBeInTheDocument(),
    );
  });
});
