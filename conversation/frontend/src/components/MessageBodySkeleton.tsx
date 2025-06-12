import { Button } from '@edifice.io/react';
import { Message } from '~/models';
import './MessageBody.css';

export interface MessageBodyProps {
  message: Message;
  editMode?: boolean;
  isPrint?: boolean;
  onMessageChange?: (message: Message) => void;
}

export function MessageBodySkeleton({ editMode }: { editMode?: boolean }) {
  if (editMode) {
    return (
      <div className="d-flex flex-column gap-16 position-relative flex-fill">
        <div className="d-flex col-12 gap-8 py-8 px-16">
          <Button
            className="placeholder col-2 flex-shrink-1"
            color="tertiary"
            disabled
          ></Button>
          <Button
            className="placeholder col-4"
            color="tertiary"
            disabled
          ></Button>
          <Button
            className="placeholder col-4"
            color="tertiary"
            disabled
          ></Button>
          <Button
            className="placeholder col-2"
            color="tertiary"
            disabled
          ></Button>
        </div>
        <div className="d-flex flex-column gap-8 px-16">
          <span className="placeholder col-10 "></span>
          <span className="placeholder col-7 "></span>
          <span className="placeholder col-8 "></span>
          <span className="placeholder col-6 "></span>
        </div>
      </div>
    );
  }

  return (
    <p>
      <span className="placeholder col-10 "></span>
      <span className="placeholder col-7 "></span>
      <span className="placeholder col-8 "></span>
    </p>
  );
}
