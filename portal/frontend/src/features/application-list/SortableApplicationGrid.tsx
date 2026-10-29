import { closestCenter, DndContext, DragOverlay } from '@dnd-kit/core';
import { SortableContext, SortingStrategy } from '@dnd-kit/sortable';
import { useApplicationsSort } from '~/hooks/useApplicationsSort';
import { Application } from '~/models/application';
import { ApplicationCardContent } from './ApplicationCardContent';
import { SortableApplicationCard } from './SortableApplicationCard';

type Props = {
  applications: Application[];
  onReorder: (applications: Application[]) => void;
};

const noShiftStrategy: SortingStrategy = () => null;

// Above the favorite stars (z-index: 2), below the insertion bar (z-index: 4).
const DRAG_OVERLAY_Z_INDEX = 3;

export function SortableApplicationGrid({ applications, onReorder }: Props) {
  const {
    sensors,
    activeApplication,
    dropTarget,
    handleDragStart,
    handleDragMove,
    handleDragEnd,
    handleDragCancel,
  } = useApplicationsSort(applications, onReorder);

  return (
    <DndContext
      sensors={sensors}
      collisionDetection={closestCenter}
      onDragStart={handleDragStart}
      onDragMove={handleDragMove}
      // onDragMove still carries the previous `over` when the target changes.
      onDragOver={handleDragMove}
      onDragEnd={handleDragEnd}
      onDragCancel={handleDragCancel}
    >
      <SortableContext
        items={applications.map((app) => app.name)}
        strategy={noShiftStrategy}
      >
        <div
          data-id="applications-list-sortable"
          className="d-flex flex-wrap gap-16 justify-content-center mx-auto"
          style={{ maxWidth: 1091 }}
        >
          {applications.map((application) => (
            <SortableApplicationCard
              key={application.name}
              application={application}
              dropSide={
                dropTarget?.applicationName === application.name
                  ? dropTarget.side
                  : undefined
              }
            />
          ))}
        </div>
      </SortableContext>

      <DragOverlay
        className="application-card-overlay"
        zIndex={DRAG_OVERLAY_Z_INDEX}
      >
        {activeApplication && (
          <div className="rounded application-card position-relative py-12 px-4 border border-secondary bg-gray-200">
            <ApplicationCardContent application={activeApplication} />
          </div>
        )}
      </DragOverlay>
    </DndContext>
  );
}
