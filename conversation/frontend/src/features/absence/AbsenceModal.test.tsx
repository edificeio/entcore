import { describe, expect, it } from 'vitest';
import { absenceService } from '~/services';
import { render, screen } from '~/mocks/setup';
import { queryClient } from '~/providers';
import { AbsenceModal } from './AbsenceModal';

describe('AbsenceModal', () => {
  beforeEach(() => {
    queryClient.clear();
  });

  it('shows the skeleton while the settings are loading', () => {
    // Never resolves: keeps the modal in its loading state for the
    // duration of the test.
    vi.spyOn(absenceService, 'getSettings').mockReturnValue(
      new Promise(() => {}),
    );

    render(<AbsenceModal isOpen={true} onModalClose={vi.fn()} />);

    expect(
      screen.getByTestId('conversation-absence-modal-skeleton'),
    ).toBeInTheDocument();
    expect(
      screen.queryByTestId('conversation-absence-modal-button-save'),
    ).not.toBeInTheDocument();
  });
});
