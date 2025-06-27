import { EditorSkeleton } from '@edifice.io/react/editor';
import { Message } from '~/models';
import './MessageBody.css';

export interface MessageBodyProps {
  message: Message;
  editMode?: boolean;
  isPrint?: boolean;
  onMessageChange?: (message: Message) => void;
}

export function MessageBodySkeleton({ editMode }: { editMode?: boolean }) {
  return <EditorSkeleton mode={editMode ? 'edit' : 'read'} variant="ghost" />;
}
