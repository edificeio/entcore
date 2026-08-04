import { mockAbsenceSettings } from '~/mocks';
import { render, screen, waitFor } from '~/mocks/setup';
import { queryClient } from '~/providers';
import { absenceService } from '~/services';
import { AbsenceReminderBanner } from './AbsenceReminderBanner';

// The tiptap `Editor` rendered deep inside `AbsenceModal` isn't worth
// driving through jsdom for a test that only covers the banner's own
// rendering and wiring; `AbsenceModal.test.tsx` covers the modal itself.
vi.mock('./AbsenceModal', () => ({
  AbsenceModal: ({ isOpen }: { isOpen: boolean }) =>
    isOpen ? <div data-testid="absence-modal-stub" /> : null,
}));

describe('AbsenceReminderBanner', () => {
  beforeEach(() => {
    queryClient.clear();
  });

  it('is not rendered while there is no active absence', async () => {
    vi.spyOn(absenceService, 'getSettings').mockResolvedValueOnce({
      ...mockAbsenceSettings,
      enabled: false,
    });

    render(<AbsenceReminderBanner />);

    await waitFor(() =>
      expect(screen.queryByRole('alert')).not.toBeInTheDocument(),
    );
  });

  it('shows the banner and opens the settings modal from its edit button', async () => {
    const { user } = render(<AbsenceReminderBanner />);

    await waitFor(() => expect(screen.getByRole('alert')).toBeInTheDocument());
    expect(screen.queryByTestId('absence-modal-stub')).not.toBeInTheDocument();

    await user.click(
      screen.getByTestId('conversation-absence-banner-button-edit'),
    );

    expect(screen.getByTestId('absence-modal-stub')).toBeInTheDocument();
  });
});
