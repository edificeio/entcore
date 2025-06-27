import { ButtonSkeleton } from '@edifice.io/react';

export function MessageNavigationSkeleton() {
  return (
    <article className="d-flex flex-column gap-16">
      <div className="d-print-none border-bottom px-16 py-4 d-flex align-items-center justify-content-between col-12">
        <ButtonSkeleton className="col-3 col-md-2"></ButtonSkeleton>
        <div className="d-flex col-9 col-md-6 justify-content-end gap-8">
          <ButtonSkeleton className="col-5 col-md-3"></ButtonSkeleton>
          <ButtonSkeleton className="col-2 col-md-1"></ButtonSkeleton>
        </div>
      </div>
    </article>
  );
}
