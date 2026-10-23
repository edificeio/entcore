import { DndContext, useDndContext } from '@dnd-kit/core';
import { render, screen, waitFor } from '~/mocks/setup';
import { DesktopMenu } from './DesktopMenu';

/** Lists the folder ids registered as drop targets, and the tree item holding their node. */
function DropTargetsProbe() {
  const { droppableContainers } = useDndContext();
  return (
    <ul data-testid="drop-targets">
      {[...droppableContainers.values()].map((container) => (
        <li key={container.id}>
          {`${container.data.current?.id}=${container.node.current?.closest('[role="treeitem"]')?.id}`}
        </li>
      ))}
    </ul>
  );
}

describe('DesktopMenu drop targets', () => {
  it('makes the inbox, the trash and every user folder, sub-folders included, a drop target of its own', async () => {
    const { user } = render(
      <DndContext>
        <DesktopMenu />
        <DropTargetsProbe />
      </DndContext>,
    );

    // Unfold "Root folder A" to display its sub-folder
    const rootFolderA = await screen.findByText('Root folder A');
    const unfoldButton = rootFolderA
      .closest('[role="treeitem"]')!
      .querySelector('[role="button"]')!;
    await user.click(unfoldButton);
    await screen.findByText('Sub Folder A.1');

    await waitFor(() => {
      const targets = screen
        .getAllByRole('listitem')
        .map((item) => item.textContent)
        .filter((text) => text?.includes('='));
      expect(targets).toEqual(
        expect.arrayContaining([
          expect.stringMatching(/^inbox=/),
          expect.stringMatching(/^trash=/),
          'folder_A=treeitem-folder_A',
          'folder_A_1=treeitem-folder_A_1',
          'folder_B=treeitem-folder_B',
        ]),
      );
    });
  });
});
