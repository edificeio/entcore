import { RecipientType } from '~/features/message-edit/components/RecipientListEdit';
import { Message, Recipients } from '~/models';
import { useBookmarkById, useSearchVisible } from '~/services/queries/user';

export function useAdditionalRecipients(
  recipientType: RecipientType,
  userIds?: string[],
  groupIds?: string[],
  bookmarkIds?: string[],
) {
  const { getVisibleUserById, getVisibleGroupById } = useSearchVisible();
  const { getBookmarkById } = useBookmarkById();

  const allUsersPromise = Promise.allSettled(
    userIds?.map((userId) => getVisibleUserById(userId)) ?? [],
  );
  const allGroupsPromise = Promise.allSettled(
    groupIds?.map((groupId) => getVisibleGroupById(groupId)) ?? [],
  );
  const allBookmarksPromise = Promise.allSettled(
    bookmarkIds?.map((bookmarkId) => getBookmarkById(bookmarkId)) ?? [],
  );

  async function addRecipientsById(message: Message): Promise<Message> {
    const allUsersSettled = await allUsersPromise;
    const allGroupsSettled = await allGroupsPromise;
    const allBookmarksSettled = await allBookmarksPromise;
    const recipients: Recipients = {
      users: [],
      groups: [],
      ...message[recipientType],
    };

    allUsersSettled
      .filter((promise) => promise.status === 'fulfilled')
      .map((success) => success.value)
      .forEach((user) => recipients.users.push(user));

    allGroupsSettled
      .filter((promise) => promise.status === 'fulfilled')
      .map((success) => success.value)
      .forEach((group) => recipients.groups.push(group));

    allBookmarksSettled
      .filter((promise) => promise.status === 'fulfilled')
      .map((success) => success.value)
      .forEach((bookmark) => {
        recipients.users.push(...bookmark.users);
        recipients.groups.push(...bookmark.groups);
      });

    return { ...message };
  }

  return {
    addRecipientsById,
  };
}
