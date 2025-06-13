import { FormControl, Input } from '@edifice.io/react';

export function MessageEditHeaderSkeleton() {
  return (
    <FormControl
      id=""
      className="d-flex border-bottom align-items-center flex-fill ps-16 pe-16 py-8"
    >
      <Input
        size="md"
        className="border-0 placeholder"
        type="text"
        disabled={true}
      />
    </FormControl>
  );
}
