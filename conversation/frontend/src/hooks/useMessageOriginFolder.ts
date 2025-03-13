import { useEdificeClient } from '@edifice.io/react';
import { MessageMetadata } from '~/models';

export function useMessageOriginFolder(message: MessageMetadata) {
  const { user } = useEdificeClient();
  const isUserAuthor = message.from.id === user?.userId;
  if (message.state === 'DRAFT') {
    return 'draft';
  } else if (isUserAuthor) {
    return 'outbox';
  } else {
    return 'inbox';
  }
}
