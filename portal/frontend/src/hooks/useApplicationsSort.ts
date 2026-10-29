import {
  ClientRect,
  DragEndEvent,
  DragMoveEvent,
  DragStartEvent,
  KeyboardSensor,
  PointerSensor,
  UniqueIdentifier,
  useSensor,
  useSensors,
} from '@dnd-kit/core';
import { arrayMove, sortableKeyboardCoordinates } from '@dnd-kit/sortable';
import { useState } from 'react';
import { Application } from '~/models/application';

export type DropSide = 'before' | 'after';

type DropTarget = {
  applicationName: UniqueIdentifier;
  side: DropSide;
  index: number;
};

const getCenterX = (rect: ClientRect) => rect.left + rect.width / 2;

const getDropSide = (
  { active, over, activatorEvent }: DragMoveEvent,
  isMovingForward: boolean,
): DropSide => {
  const activeRect = active.rect.current.translated;
  // The keyboard sensor stacks the dragged card on its target: follow the move direction.
  if (activatorEvent instanceof KeyboardEvent || !activeRect || !over) {
    return isMovingForward ? 'after' : 'before';
  }
  return getCenterX(activeRect) > getCenterX(over.rect) ? 'after' : 'before';
};

const getDropIndex = (
  activeIndex: number,
  overIndex: number,
  side: DropSide,
) => {
  const insertIndex = side === 'after' ? overIndex + 1 : overIndex;
  // Taking the dragged card out first shifts the following ones back by one.
  return activeIndex < insertIndex ? insertIndex - 1 : insertIndex;
};

export const useApplicationsSort = (
  applications: Application[],
  onReorder: (applications: Application[]) => void,
) => {
  const [activeName, setActiveName] = useState<UniqueIdentifier | null>(null);
  const [dropTarget, setDropTarget] = useState<DropTarget | null>(null);

  const sensors = useSensors(
    useSensor(PointerSensor),
    useSensor(KeyboardSensor, {
      coordinateGetter: sortableKeyboardCoordinates,
    }),
  );

  const indexOf = (id: UniqueIdentifier) =>
    applications.findIndex((app) => app.name === id);

  const activeApplication = applications.find((app) => app.name === activeName);

  const resetDrag = () => {
    setActiveName(null);
    setDropTarget(null);
  };

  const handleDragStart = ({ active }: DragStartEvent) =>
    setActiveName(active.id);

  const handleDragMove = (event: DragMoveEvent) => {
    const { active, over } = event;
    if (!over) {
      setDropTarget(null);
      return;
    }

    const activeIndex = indexOf(active.id);
    const overIndex = indexOf(over.id);
    const side = getDropSide(event, activeIndex < overIndex);
    const index = getDropIndex(activeIndex, overIndex, side);
    const nextTarget =
      index === activeIndex ? null : { applicationName: over.id, side, index };

    setDropTarget((current) =>
      current?.applicationName === nextTarget?.applicationName &&
      current?.side === nextTarget?.side
        ? current
        : nextTarget,
    );
  };

  const handleDragEnd = ({ active }: DragEndEvent) => {
    if (dropTarget) {
      onReorder(arrayMove(applications, indexOf(active.id), dropTarget.index));
    }
    resetDrag();
  };

  return {
    sensors,
    activeApplication,
    dropTarget,
    handleDragStart,
    handleDragMove,
    handleDragEnd,
    handleDragCancel: resetDrag,
  };
};
