import { useEffect } from 'react';

/** Time a folded folder must be hovered by dragged messages before unfolding, in milliseconds. */
export const UNFOLD_ON_DRAG_HOVER_DELAY = 2000;

/**
 * Unfolds a folder of the tree by pressing Enter on its fold/unfold arrow.
 * The Tree of @edifice.io/react keeps its unfolded nodes internally and exposes no way to change them,
 * so this relies on its markup : `li#treeitem-<id>[aria-expanded]` holding the arrow `[role=button][aria-label]`.
 * A click would not do : @dnd-kit stops every click from reaching React while dragging.
 */
function unfoldTreeItem(folderId: string) {
  const treeItem = document.getElementById(`treeitem-${folderId}`);
  if (treeItem?.getAttribute('aria-expanded') !== 'false') return;

  const unfoldButton = treeItem.querySelector<HTMLElement>(
    ':scope > div > .action-container > [role="button"][aria-label]',
  );
  unfoldButton?.dispatchEvent(
    new KeyboardEvent('keydown', {
      code: 'Enter',
      key: 'Enter',
      bubbles: true,
    }),
  );
}

/**
 * Unfolds the folder hovered by dragged messages once hovered long enough,
 * to let them be dropped into its sub-folders.
 * @param hoveredFolderId the hovered folder, undefined when nothing is dragged over a folder.
 */
export function useUnfoldOnDragHover(hoveredFolderId: string | undefined) {
  useEffect(() => {
    if (!hoveredFolderId) return;
    const timer = setTimeout(
      () => unfoldTreeItem(hoveredFolderId),
      UNFOLD_ON_DRAG_HOVER_DELAY,
    );
    return () => clearTimeout(timer);
  }, [hoveredFolderId]);
}
