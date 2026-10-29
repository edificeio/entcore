import {
  mockCurrentUserPreview,
  mockMessageFromMeToMe,
  mockMessageOfOutbox,
} from '~/mocks';
import { getMessageOriginFolderId } from './utils';

const userId = mockCurrentUserPreview.id;

describe('getMessageOriginFolderId', () => {
  it('returns the drafts for a draft', () => {
    expect(
      getMessageOriginFolderId(
        { ...mockMessageOfOutbox, state: 'DRAFT' },
        userId,
      ),
    ).toBe('draft');
  });

  it('returns the outbox for a message sent by the user to others', () => {
    expect(getMessageOriginFolderId(mockMessageOfOutbox, userId)).toBe(
      'outbox',
    );
  });

  it('returns the inbox for a message received by the user', () => {
    expect(
      getMessageOriginFolderId(
        {
          ...mockMessageOfOutbox,
          from: {
            id: 'someone-else',
            displayName: 'Someone',
            profile: 'Teacher',
          },
          to: { users: [mockCurrentUserPreview], groups: [] },
        },
        userId,
      ),
    ).toBe('inbox');
  });

  it('returns the inbox for a message sent by the user to himself', () => {
    expect(getMessageOriginFolderId(mockMessageFromMeToMe, userId)).toBe(
      'inbox',
    );
  });
});
