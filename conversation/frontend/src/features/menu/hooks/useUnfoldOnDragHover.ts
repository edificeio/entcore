import { TreeHandlers } from '@edifice.io/react';
import { RefObject, useEffect } from 'react';

/** Time a folded folder must be hovered by dragged messages before unfolding, in milliseconds. */
export const UNFOLD_ON_DRAG_HOVER_DELAY = 2000;

/**
 * Unfolds the folder hovered by dragged messages once hovered long enough,
 * to let them be dropped into its sub-folders.
 * @param hoveredFolderId the hovered folder, undefined when nothing is dragged over a folder.
 * @param treeRef the folders tree, whose folder gets unfolded.
 */
export function useUnfoldOnDragHover(
  hoveredFolderId: string | undefined,
  treeRef: RefObject<TreeHandlers>,
) {
  useEffect(() => {
    if (!hoveredFolderId) return;
    const timer = setTimeout(
      () => treeRef.current?.expandNode(hoveredFolderId),
      UNFOLD_ON_DRAG_HOVER_DELAY,
    );
    return () => clearTimeout(timer);
  }, [hoveredFolderId, treeRef]);
}
