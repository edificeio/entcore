import { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { Recipients } from '~/models';
import {
  type VisibleGroupData,
  type VisibleUserData,
} from '~/services/api/userService';
import { useBookmarkById, useSearchVisible } from '~/services/queries/user';

export function useAdditionalRecipients() {
  const { getVisibleUserById, getVisibleGroupById } = useSearchVisible();
  const { getBookmarkById } = useBookmarkById();
  const [searchParams] = useSearchParams();

  const [recipients, setRecipients] = useState<Recipients | undefined>();
  const userIds = searchParams.getAll('user');
  const groupIds = searchParams.getAll('group');
  const bookmarkIds = searchParams.getAll('favorite');

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
        const users = new Map<string, VisibleUserData>(),
          groups = new Map<string, VisibleGroupData>(),
          addToUsers = (user: any) => users.set(user.id, user),
          addToGroups = (group: any) => groups.set(group.id, group);

        recipients[0].forEach(addToUsers);
        recipients[1].forEach(addToGroups);
        recipients[2].forEach((bookmark) => {
          bookmark.users.forEach(addToUsers);
          bookmark.groups
            .map((group) => ({ ...group, nbUsers: 0 }))
            .forEach(addToGroups);
        });
        setRecipients({
          users: [...users.values()],
          groups: [...groups.values()],
        });
      },
    );
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return {
    recipients,
  };
}
