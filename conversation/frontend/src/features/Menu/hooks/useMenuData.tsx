import { Badge } from '@edifice.io/react';
import { useSelectedFolder } from '~/hooks';
import { SystemFolder } from '~/models';
import { useMessagesCount } from '~/services';

/** Render a badge with a counter */
function renderBadge(count: number) {
  return (
    <>
      {count > 0 && (
        <Badge
          variant={{
            level: 'info',
            type: 'notification',
          }}
        >
          {count}
        </Badge>
      )}
    </>
  );
}

export function useMenuData() {
  const { folderId, userFolder } = useSelectedFolder();

  const inbox = useMessagesCount('inbox', { unread: true }).data?.count ?? 0;
  const draft = useMessagesCount('draft').data?.count ?? 0;

  const selectedSystemFolderId: SystemFolder | undefined =
    typeof folderId === 'string' && !userFolder
      ? (folderId as SystemFolder)
      : undefined;
  const selectedUserFolderId =
    typeof userFolder === 'object' ? folderId : undefined;

  return {
    counters: {
      inbox,
      draft,
    },
    selectedSystemFolderId,
    selectedUserFolderId,
    renderBadge,
  };
}
