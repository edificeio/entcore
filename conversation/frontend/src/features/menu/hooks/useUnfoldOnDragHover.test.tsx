import { Tree, TreeHandlers, TreeItem } from '@edifice.io/react';
import { act, useRef } from 'react';
import { render, renderHook, screen } from '~/mocks/setup';
import {
  UNFOLD_ON_DRAG_HOVER_DELAY,
  useUnfoldOnDragHover,
} from './useUnfoldOnDragHover';

const nodes: TreeItem[] = [
  {
    id: 'folder_A',
    name: 'Root folder A',
    children: [{ id: 'folder_A_1', name: 'Sub Folder A.1' }],
  },
  { id: 'folder_B', name: 'Root folder B' },
];

/** A folders tree, with one of its folders hovered by dragged messages. */
function HoveredTree({ hoveredFolderId }: { hoveredFolderId?: string }) {
  const treeRef = useRef<TreeHandlers>(null);
  useUnfoldOnDragHover(hoveredFolderId, treeRef);
  return <Tree ref={treeRef} nodes={nodes} onTreeItemClick={vi.fn()} />;
}

describe('useUnfoldOnDragHover', () => {
  beforeEach(() => {
    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('unfolds a folded folder hovered for 2 seconds', () => {
    render(<HoveredTree hoveredFolderId="folder_A" />);

    act(() => vi.advanceTimersByTime(1999));
    expect(screen.queryByText('Sub Folder A.1')).not.toBeInTheDocument();

    act(() => vi.advanceTimersByTime(1));
    expect(screen.getByText('Sub Folder A.1')).toBeInTheDocument();
  });

  it('does not unfold a folder left before the delay', () => {
    const { rerender } = render(<HoveredTree hoveredFolderId="folder_A" />);

    act(() => vi.advanceTimersByTime(UNFOLD_ON_DRAG_HOVER_DELAY - 1));
    rerender(<HoveredTree hoveredFolderId="folder_B" />);
    act(() => vi.advanceTimersByTime(UNFOLD_ON_DRAG_HOVER_DELAY));

    expect(screen.queryByText('Sub Folder A.1')).not.toBeInTheDocument();
  });

  it('does not unfold anything when the drag ends', () => {
    const { rerender } = render(<HoveredTree hoveredFolderId="folder_A" />);

    rerender(<HoveredTree hoveredFolderId={undefined} />);
    act(() => vi.advanceTimersByTime(UNFOLD_ON_DRAG_HOVER_DELAY));

    expect(screen.queryByText('Sub Folder A.1')).not.toBeInTheDocument();
  });

  it('keeps an already unfolded folder unfolded', () => {
    const { rerender } = render(<HoveredTree hoveredFolderId="folder_A" />);
    act(() => vi.advanceTimersByTime(UNFOLD_ON_DRAG_HOVER_DELAY));

    // Hovered again later, during another drag
    rerender(<HoveredTree hoveredFolderId={undefined} />);
    rerender(<HoveredTree hoveredFolderId="folder_A" />);
    act(() => vi.advanceTimersByTime(UNFOLD_ON_DRAG_HOVER_DELAY));

    expect(screen.getByText('Sub Folder A.1')).toBeInTheDocument();
  });

  it('does nothing when the tree is gone', () => {
    renderHook(() => useUnfoldOnDragHover('folder_A', { current: null }));

    expect(() =>
      act(() => vi.advanceTimersByTime(UNFOLD_ON_DRAG_HOVER_DELAY)),
    ).not.toThrow();
  });
});
