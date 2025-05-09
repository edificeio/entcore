import { BookmarkWithDetails } from '@edifice.io/client';
import { useCallback, useEffect, useState } from 'react';
import { RecipientType } from '~/features/message-edit/components/RecipientListEdit';
import { Message } from '~/models';
import {
  type VisibleGroupData,
  type VisibleUserData,
} from '~/services/api/userService';
import { useBookmarkById, useSearchVisible } from '~/services/queries/user';

export function useAdditionalRecipients(
  recipientType: RecipientType,
  userIds: string[],
  groupIds: string[],
  bookmarkIds: string[],
) {
  const { getVisibleUserById, getVisibleGroupById } = useSearchVisible();
  const { getBookmarkById } = useBookmarkById();

  const [recipients, setRecipients] = useState<
    [VisibleUserData[], VisibleGroupData[], BookmarkWithDetails[]] | undefined
  >();

  const addRecipientsToMessage = useCallback(
    (message: Message) => {
      if (recipients) {
        const users = new Map<string, VisibleUserData>(),
          groups = new Map<string, VisibleGroupData>(),
          addToUsers = (user: any) => users.set(user.id, user),
          addToGroups = (group: any) => groups.set(group.id, group);

        message[recipientType]?.users.forEach(addToUsers);
        message[recipientType]?.groups.forEach(addToGroups);
        recipients[0].forEach(addToUsers);
        recipients[1].forEach(addToGroups);
        recipients[2].forEach((bookmark) => {
          bookmark.users.forEach(addToUsers);
          bookmark.groups
            .map((group) => ({ ...group, nbUsers: 0 }))
            .forEach(addToGroups);
        });
        message[recipientType] = {
          users: [...users.values()],
          groups: [...groups.values()],
        };
      }
      return message;
    },
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [recipients],
  );

  // Get recipients data from their ID
  useEffect(() => {
    const allUsersPromise = Promise.allSettled(
      userIds.map((id) => getVisibleUserById(id)),
    ).then((results) =>
      results
        .filter((promise) => promise.status === 'fulfilled')
        .map((success) => success.value),
    );
    const allGroupsPromise = Promise.allSettled(
      groupIds.map((id) => getVisibleGroupById(id)),
    ).then((results) =>
      results
        .filter((promise) => promise.status === 'fulfilled')
        .map((success) => success.value),
    );
    const allBookmarksPromise = Promise.allSettled(
      bookmarkIds.map((id) => getBookmarkById(id)),
    ).then((results) =>
      results
        .filter((promise) => promise.status === 'fulfilled')
        .map((success) => success.value),
    );

    Promise.all([allUsersPromise, allGroupsPromise, allBookmarksPromise]).then(
      (recipients) => {
        setRecipients(recipients);
      },
    );
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return {
    addRecipientsToMessage,
  };
}
