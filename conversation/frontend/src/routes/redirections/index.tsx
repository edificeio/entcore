import { matchPath } from 'react-router-dom';

function asSubPath(folderId: string, ...segments: string[]) {
  if (!folderId) return '/inbox';
  let base: string;
  switch (folderId?.toLowerCase()) {
    case 'inbox':
    case 'outbox':
    case 'draft':
    case 'trash':
      base = `/${folderId.toLowerCase()}`;
      break;
    default:
      base = `/folder/${folderId}`;
  }
  if (segments.length > 0) {
    base += '/' + segments.join('/');
  }
  return base;
}

/** Check old format URL and redirect if needed */
export const manageRedirections = (): string | null => {
  const pathLocation = window.location.pathname;
  const hashLocation = window.location.hash.substring(1);

  if (!hashLocation) {
    if (
      pathLocation === '/' ||
      pathLocation === '/conversation' ||
      pathLocation === '/conversation/' ||
      pathLocation === '/conversation/conversation'
    ) {
      return '/inbox';
    }
  } else {
    // Check if the URL is an old format (angular root with hash) and redirect to the new format
    const isFolder = matchPath('/:folderId', hashLocation);
    if (isFolder?.params.folderId) {
      return asSubPath(isFolder.params.folderId);
    }

    const isReadMessage = matchPath('/read-mail/:mailId', hashLocation);
    if (isReadMessage?.params.mailId) {
      return asSubPath('inbox', 'message', isReadMessage.params.mailId);
    }

    const isWriteMessage = matchPath('/write-mail', hashLocation);
    if (isWriteMessage) {
      return asSubPath('draft');
    }

    const isWriteToRecipient = matchPath(
      '/write-mail/:recipientId/:recipientType',
      hashLocation,
    );
    if (
      isWriteToRecipient?.params.recipientId &&
      isWriteToRecipient.params.recipientType
    ) {
      // eslint-disable-next-line prefer-const
      let { recipientId, recipientType } = isWriteToRecipient.params;
      recipientType = recipientType.toLowerCase();
      if (['user', 'group', 'favorite'].includes(recipientType)) {
        return `${asSubPath('draft', 'create')}?${recipientType}=${recipientId}`;
      }
    }

    const isWriteToUser = matchPath('/write-mail/:userId', hashLocation);
    if (isWriteToUser?.params.userId) {
      return `${asSubPath('draft', 'create')}?user=${isWriteToUser.params.userId}`;
    }

    const isPrintMessage = matchPath('/printMail/:mailId', hashLocation);
    if (isPrintMessage?.params.mailId) {
      return `/print/${isPrintMessage.params.mailId}`;
    }
  }

  // No redirection needed
  return null;
};
