import { mockSentMessage } from '~/mocks';
import { render, screen } from '~/mocks/setup';
import { SentToInactiveUsersModal } from './SentToInactiveUsersModal';

const message = { ...mockSentMessage };
message.inactiveCount = 321;
const onModalClose = () => {};

describe('Inactive users component', () => {
  it('should render successfully', async () => {
    const { baseElement } = render(
      <SentToInactiveUsersModal
        users={message.inactive}
        total={message.inactiveCount}
        onModalClose={onModalClose}
      />,
    );

    expect(baseElement).toBeTruthy();
  });

  it('should render inactive total count', async () => {
    render(
      <SentToInactiveUsersModal
        users={message.inactive}
        total={message.inactiveCount}
        onModalClose={onModalClose}
      />,
    );

    const consoleWarning = await screen.findByText('warning.inactive.console');

    expect(consoleWarning).toBeInTheDocument();
  });
});
