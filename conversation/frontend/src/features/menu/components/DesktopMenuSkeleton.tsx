import { Button } from '@edifice.io/react';
import './DesktopMenu.css';

/** The navigation menu among folders, intended for desktop resolutions */
export function DesktopMenuSkeleton() {
  return (
    <div className="d-flex flex-column gap-4">
      <Button
        className="placeholder col-12 p-4"
        color="tertiary"
        disabled
      ></Button>
      <Button
        className="placeholder col-7 p-4"
        color="tertiary"
        disabled
      ></Button>
      <Button
        className="placeholder col-10 p-4"
        color="tertiary"
        disabled
      ></Button>
      <Button
        className="placeholder col-8 p-4"
        color="tertiary"
        disabled
      ></Button>
      <div className="border-bottom pt-8 mb-12"></div>
      <Button
        className="placeholder col-8 bg-gray-700 p-4"
        color="tertiary"
        disabled
      ></Button>
    </div>
  );
}
