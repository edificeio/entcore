import { ReactNode } from 'react';
import { MessageRecipientList } from '~/components/MessageRecipientList';
import { MessageMetadata } from '~/models';

export interface RecipientListPreviewProps {
  message: MessageMetadata;
  head?: ReactNode;
}

export function RecipientListPreview({
  message,
  head,
}: RecipientListPreviewProps) {
  const to = message.to;
  const cc = message.cc;
  const cci = message.cci ?? { users: [], groups: [] };
  const recipients = {
    users: [...to.users, ...cc.users, ...cci.users],
    groups: [...to.groups, ...cc.groups, ...cci.groups],
  };
  return (
    <MessageRecipientList
      head={head}
      recipients={recipients}
      color="text-gray-800"
      truncate
    />
  );
}
