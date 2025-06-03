import { Button } from '@edifice.io/react';

export function MessageActionDropdownSkeleton({
  className = '',
}: {
  className?: string;
}) {
  const classNameContainer = `d-flex ${className} col-6 justify-content-end gap-12`;
  return (
    <div className={classNameContainer}>
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
  );
}
