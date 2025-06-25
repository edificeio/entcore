import { FormControl, Input } from '@edifice.io/react';
import { MessageActionDropdownSkeleton } from '~/components/MessageActionDropdown/MessageActionDropdownSkeleton';
import { MessageBodySkeleton } from '~/components/MessageBodySkeleton';
import { MessageEditHeaderSkeleton } from './components/MessageEditHeaderSkeleton';

export function MessageEditSkeleton() {
  return (
    <div>
      <MessageEditHeaderSkeleton />
      <FormControl id="" className="border-bottom px-16 py-8">
        <Input
          size="md"
          className="border-0 placeholder"
          type="text"
          disabled={true}
        />
      </FormControl>
      <MessageBodySkeleton editMode={true} />
      <div className="d-print-none d-flex justify-content-end gap-12 pt-24 pe-16">
        <MessageActionDropdownSkeleton className="gap-12" />
      </div>
    </div>
  );
}
