import { ButtonSkeleton } from '@edifice.io/react';

export function MessageActionDropdownSkeleton({
  className = '',
}: {
  className?: string;
}) {
  const classNameContainer = `d-flex ${className} col-6 justify-content-end gap-12`;
  return (
    <div className={classNameContainer}>
      <ButtonSkeleton className=" col-5 col-md-3"></ButtonSkeleton>
      <ButtonSkeleton className="col-2 col-md-1"></ButtonSkeleton>
    </div>
  );
}
