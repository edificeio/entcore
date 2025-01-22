import { matchPath } from 'react-router-dom';
import { basename } from '..';

function redirectTo(redirectPath: string) {
  window.location.replace(
    window.location.origin + basename.replace(/\/$/g, '') + redirectPath,
  );
}

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
export const manageRedirections = async () => {
  const pathLocation = window.location.pathname;
  const hashLocation = window.location.hash.substring(1);

  if (
    pathLocation === '/conversation' ||
    pathLocation === '/conversation/' ||
    pathLocation === '/conversation/conversation'
  ) {
    // Redirect to inbox
    redirectTo(asSubPath('inbox'));
    return;
  }

  // Check if the URL is an old format (angular root with hash) and redirect to the new format
  if (hashLocation) {
    const isFolder = matchPath('/:folderId', hashLocation);
    if (isFolder?.params.folderId) {
      redirectTo(asSubPath(isFolder.params.folderId));
      return;
    }

    const isReadMessage = matchPath('/read-mail/:mailId', hashLocation);
    if (isReadMessage?.params.mailId) {
      redirectTo(asSubPath('inbox', 'message', isReadMessage.params.mailId));
      return;
    }

    const isWriteMessage = matchPath('/write-mail', hashLocation);
    if (isWriteMessage) {
      redirectTo(asSubPath('draft'));
      return;
    }

    const isEditMessage = matchPath('/write-mail/:mailId', hashLocation);
    if (isEditMessage?.params.mailId) {
      redirectTo(asSubPath('draft', 'message', isEditMessage.params.mailId));
      return;
    }

    const isPrintMessage = matchPath('/printMail/:mailId', hashLocation);
    if (isPrintMessage?.params.mailId) {
      redirectTo(`/print/${isPrintMessage.params.mailId})`);
      return;
    }
  }

  return null;
};
