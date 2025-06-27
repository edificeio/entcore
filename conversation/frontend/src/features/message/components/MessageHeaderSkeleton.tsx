import { Avatar, TextSkeleton } from '@edifice.io/react';

export function MessageHeaderSkeleton() {
  return (
    <div>
      <TextSkeleton className="placeholder col-7" size="lg"></TextSkeleton>
      <div className="d-flex align-items-center mt-16 gap-12 small">
        <Avatar
          alt=""
          size="sm"
          variant="circle"
          className="align-self-start mt-4 placeholder"
        />
        <div className="col-8">
          <TextSkeleton className="d-block col-6"></TextSkeleton>
          <TextSkeleton className="col-3"></TextSkeleton>
        </div>
      </div>
    </div>
  );
}
