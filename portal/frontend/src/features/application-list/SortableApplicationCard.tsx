import { AnimateLayoutChanges, useSortable } from '@dnd-kit/sortable';
import { CSS } from '@dnd-kit/utilities';
import clsx from 'clsx';
import { DropSide } from '~/hooks/useApplicationsSort';
import { Application } from '~/models/application';
import { ApplicationCardContent } from './ApplicationCardContent';

// noShiftStrategy shifts no card during the drag: every card that moves on drop must slide.
const animateLayoutChanges: AnimateLayoutChanges = () => true;

export function SortableApplicationCard({
  application,
  dropSide,
}: {
  application: Application;
  dropSide?: DropSide;
}) {
  const {
    attributes,
    listeners,
    setNodeRef,
    transform,
    transition,
    isDragging,
    isSorting,
  } = useSortable({ id: application.name, animateLayoutChanges });

  return (
    <div
      ref={setNodeRef}
      data-id={application.name}
      className={clsx(
        'rounded application-card sortable-application-card position-relative py-12 px-4',
        isDragging && 'is-dragging',
        isSorting && 'is-sorting',
        dropSide && `drop-${dropSide}`,
      )}
      style={{ transform: CSS.Translate.toString(transform), transition }}
      {...attributes}
      {...listeners}
    >
      <ApplicationCardContent application={application} />
    </div>
  );
}
