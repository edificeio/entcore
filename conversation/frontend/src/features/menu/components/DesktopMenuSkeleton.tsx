import { ButtonSkeleton } from '@edifice.io/react';
import './DesktopMenu.css';

/** The navigation menu among folders, intended for desktop resolutions */
export function DesktopMenuSkeleton() {
  return (
    <div className="d-flex flex-column gap-4">
      <ButtonSkeleton className="col-12 p-4"></ButtonSkeleton>
      <ButtonSkeleton className="col-7 p-4"></ButtonSkeleton>
      <ButtonSkeleton className="col-10 p-4"></ButtonSkeleton>
      <ButtonSkeleton className="col-8 p-4"></ButtonSkeleton>
      <div className="border-bottom pt-8 mb-12"></div>
      <ButtonSkeleton className="col-8 p-4"></ButtonSkeleton>
    </div>
  );
}
