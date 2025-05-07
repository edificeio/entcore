import { BookmarkWithDetails } from '@edifice.io/client';
import { useCallback, useEffect, useState } from 'react';
import { RecipientType } from '~/features/message-edit/components/RecipientListEdit';
import { Message, Recipients } from '~/models';
import {
  type VisibleData,
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
    [VisibleUserData[], VisibleData[], BookmarkWithDetails[]] | undefined
  >();

  const addRecipientsToMessage = useCallback(
    (message: Message) => {
      if (recipients) {
        const sets = {
          users: new Set(message[recipientType]?.users),
          groups: new Set(message[recipientType]?.groups),
        };
        const addToUsers = (user: VisibleUserData) => sets.users.add(user);
        const addToGroups = (group: VisibleData) => sets.groups.add(group);
        recipients[0].forEach(addToUsers);
        recipients[1].forEach(addToGroups);
        recipients[2].forEach((bookmark) => {
          bookmark.users.forEach(addToUsers);
          bookmark.groups.forEach(addToGroups);
        });
        message[recipientType] = {
          users: [...sets.users],
          groups: [...sets.groups],
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
