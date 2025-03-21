import { useEdificeClient } from '@edifice.io/react';
import { MessageBase, Recipients } from '~/models';

export default function useMessageCciToDisplay(
  message: MessageBase,
): Recipients | null {
  const { user } = useEdificeClient();
  const cci = message.cci;
  const isFromCurrentUser = user?.userId === message.from.id;
  const hasCci = cci && (cci.users.length > 0 || cci.groups.length > 0);

  if (!hasCci) return null;
  if (isFromCurrentUser) return cci;

  const currentUserInCci = cci.users.find((u) => u.id === user?.userId);
  if (!currentUserInCci) return null;

  return {
    users: [currentUserInCci],
    groups: [],
  };
}
