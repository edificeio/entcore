import { Button } from '@edifice.io/react';

export function MessageNavigationSkeleton() {
  return (
    <article className="d-flex flex-column gap-16">
      <div className="d-print-none border-bottom px-16 py-4 d-flex align-items-center justify-content-between col-12">
        <Button
          className="placeholder col-3 col-md-2"
          color="tertiary"
          disabled
        ></Button>
        <div className="d-flex col-9 col-md-6 justify-content-end gap-8">
          <Button
            className="placeholder col-5 col-md-3"
            color="tertiary"
            disabled
          ></Button>
          <Button
            className="placeholder col-2 col-md-1"
            color="tertiary"
            disabled
          ></Button>
        </div>
      </div>
    </article>
  );
}
