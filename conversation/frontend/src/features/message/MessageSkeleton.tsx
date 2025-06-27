import { MessageActionDropdownSkeleton } from '~/components/MessageActionDropdown/MessageActionDropdownSkeleton';
import { MessageBodySkeleton } from '~/components/MessageBodySkeleton';
import { MessageNavigationSkeleton } from './MessageNavigationSkeleton';
import { MessageHeaderSkeleton } from './components/MessageHeaderSkeleton';

export function MessageSkeleton() {
  return (
    <article className="d-flex flex-column gap-16">
      <MessageNavigationSkeleton />
      <div className="d-flex flex-column gap-16 p-16 ps-lg-24 pt-0">
        <MessageHeaderSkeleton />
        <div className="ms-lg-48">
          <MessageBodySkeleton editMode={false} />
        </div>
        <footer className="d-print-none d-flex justify-content-end gap-12 pt-24 border-top ">
          <MessageActionDropdownSkeleton />
        </footer>
      </div>
    </article>
  );
}
