import { Avatar } from '@edifice.io/react';

export function MessageHeaderSkeleton() {
  return (
    <div>
      <h4 className="placeholder col-7"></h4>
      <div className="d-flex align-items-center mt-16 gap-12 small">
        <Avatar
          alt=""
          size="sm"
          variant="circle"
          className="align-self-start mt-4 placeholder"
        />
        <div className="col-8">
          <div className="d-block placeholder col-6"></div>
          <div className="placeholder col-3"></div>
        </div>
      </div>
    </div>
  );
}
