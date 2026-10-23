import {
  DndContext,
  DragOverlay,
  Modifier,
  MouseSensor,
  pointerWithin,
  useSensor,
  useSensors,
} from '@dnd-kit/core';
import { IconMail } from '@edifice.io/react/icons';
import { createContext, ReactNode, useContext } from 'react';
import { useI18n } from '~/hooks/useI18n';
import { useMessageDragAndDrop } from '../hooks/useMessageDragAndDrop';

/** Tells whether the dragged messages, if any, can be dropped on a folder. */
const CanDropMessagesOnContext = createContext<(folderId: string) => boolean>(
  () => false,
);

/** Returns whether the dragged messages, if any, can be dropped on a folder. */
export const useCanDropMessagesOn = () => useContext(CanDropMessagesOnContext);

/** Gap between the mouse pointer and the dragged messages overlay, in pixels. */
const OVERLAY_POINTER_OFFSET = 12;

/**
 * The overlay is much smaller than the dragged message line :
 * stick it next to the mouse pointer instead of the line's top-left corner.
 */
const stickToPointer: Modifier = ({
  activatorEvent,
  activeNodeRect,
  transform,
}) => {
  if (!(activatorEvent instanceof MouseEvent) || !activeNodeRect) {
    return transform;
  }
  return {
    ...transform,
    x:
      transform.x +
      activatorEvent.clientX -
      activeNodeRect.left +
      OVERLAY_POINTER_OFFSET,
    y:
      transform.y +
      activatorEvent.clientY -
      activeNodeRect.top +
      OVERLAY_POINTER_OFFSET,
  };
};

/**
 * Lets messages of the list be dragged onto the folders of the desktop menu.
 * Mouse only : touch devices keep using the "Move" modal.
 */
export function MessageDragAndDrop({ children }: { children: ReactNode }) {
  const { t } = useI18n();
  const {
    draggedMessageIds,
    canDropOn,
    handleDragStart,
    handleDragEnd,
    handleDragCancel,
  } = useMessageDragAndDrop();
  // A small move is required before dragging, so that clicks keep opening messages.
  const sensors = useSensors(
    useSensor(MouseSensor, { activationConstraint: { distance: 8 } }),
  );

  const count = draggedMessageIds.length;

  return (
    <DndContext
      sensors={sensors}
      collisionDetection={pointerWithin}
      onDragStart={handleDragStart}
      onDragEnd={handleDragEnd}
      onDragCancel={handleDragCancel}
    >
      <CanDropMessagesOnContext.Provider value={canDropOn}>
        {children}
      </CanDropMessagesOnContext.Provider>
      <DragOverlay dropAnimation={null} modifiers={[stickToPointer]}>
        {count > 0 && (
          <div
            className="d-inline-flex align-items-center gap-8 px-12 py-8 rounded bg-white border shadow"
            data-testid="message-drag-overlay"
          >
            <IconMail width={20} height={20} />
            <span className="fw-bold">
              {count > 1 ? t('messages.drag', { count }) : t('message.drag')}
            </span>
          </div>
        )}
      </DragOverlay>
    </DndContext>
  );
}
