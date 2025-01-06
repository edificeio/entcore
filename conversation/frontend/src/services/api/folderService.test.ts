import { describe, expect, test } from 'vitest';
import { folderService } from '.';
import { mockFolderTree, mockMessagesOfInbox } from '~/mocks';

describe('Conversation Folder GET Methods', () => {
  test("makes a GET request to get user's folders tree with at depth 2", async () => {
    const response = await folderService.getTree(2);

    expect(response).toBeDefined();
    expect(response).toHaveLength(2);
    expect(response).toStrictEqual(mockFolderTree);
  });

  test('makes a GET request to get list of messages from inbox', async () => {
    const response = await folderService.getMessages('inbox');

    expect(response).toBeDefined();
    expect(response).toStrictEqual(mockMessagesOfInbox);
  });
});

describe('Conversation Folder Mutation Methods', () => {
  test('makes a POST request to create a new folder', async () => {
    const response = await folderService.create({
      parentId: 'folder_A',
      name: 'folder_A_2',
    });

    expect(response).toHaveProperty('id');
  });

  test('makes a PUT request to rename a folder', async () => {
    const response = await folderService.rename('folder_A_2', 'Sub folder A.2');

    expect(response).toStrictEqual({});
  });

  test('makes a DELETE request to trash a folder', async () => {
    const response = await folderService.trash('folder_A_2');

    expect(response).toStrictEqual({});
  });
});
