import { describe, expect, test } from 'vitest';
import { baseUrl, messageService } from '.';
import { mockFullMessage } from '~/mocks';
import { odeServices } from 'edifice-ts-client';

describe('Conversation Message GET Methods', () => {
  test('makes a GET request to get a full message', async () => {
    const response = await messageService.getById('f43d3783');

    expect(response).toBeDefined();
    expect(response).toStrictEqual(mockFullMessage);
  });
});

describe('Conversation Message Mutation Methods', () => {
  test('makes a POST request to mark a message as read', async () => {
    const response = await messageService.toggleUnread('f43d3783', false);
    expect(response).toBeUndefined();
  });

  test('makes a POST request to mark two messages as unread', async () => {
    const response = await messageService.toggleUnread([
      'f43d3783',
      '4d14920b',
    ]);
    expect(response).toBeUndefined();
  });

  test('makes a PUT request to restore a trashed message', async () => {
    const response = await messageService.restore('f43d3783');
    expect(response).toBeUndefined();
  });

  test('makes a PUT request to delete a message', async () => {
    const response = await messageService.delete(['f43d3783']);
    expect(response).toBeUndefined();
  });

  test('makes a POST request to create a draft message', async () => {
    const response = await messageService.createDraft({
      body: 'New content',
    });

    expect(response).toBeDefined();
    expect(response).toHaveProperty('id');
  });

  test('makes a POST request to update a draft message', async () => {
    const response = await messageService.updateDraft('message_draft', {
      body: 'New content',
    });

    expect(response).toBeUndefined();
  });

  test('makes a POST request to send a draft message', async () => {
    const response = await messageService.send('message_draft', {
      body: 'New content',
    });

    expect(response).toBeDefined();
    expect(response).toHaveProperty('id');
    expect(response).toHaveProperty('body');
    expect(response).toHaveProperty('sent');
  });
  
  test('makes a PUT request to move messages to trash', async () => {
    const messageIds = ['f43d3783', '4d14920b'];
  
    const putMock = vi.spyOn(odeServices.http(), 'put').mockResolvedValue(undefined);
  
    const response = await messageService.moveToFolder('trash', messageIds);
  
    expect(putMock).toHaveBeenCalledWith(`${baseUrl}/trash`, { id: messageIds });
    expect(response).toBeUndefined();
  
    putMock.mockRestore();
  });

  test('makes a PUT request to restore a single message from trash', async () => {
    const messageId = 'f43d3783';

    const putMock = vi.spyOn(odeServices.http(), 'put').mockResolvedValue(undefined);

    const response = await messageService.restore([messageId]);

    expect(putMock).toHaveBeenCalledWith(`${baseUrl}/restore`, { id: [messageId] });
    expect(response).toBeUndefined();

    putMock.mockRestore();
  });

  test('makes a PUT request to delete messages', async () => {
    const messageIds = ['f43d3783', '4d14920b'];
  
    // Corrige le mock pour intercepter un "PUT" au lieu d'un "DELETE"
    const putMock = vi.spyOn(odeServices.http(), 'put').mockResolvedValue(undefined);
  
    await messageService.delete(messageIds);
  
    expect(putMock).toHaveBeenCalledTimes(1);
    expect(putMock).toHaveBeenCalledWith(`${baseUrl}/delete`, {
      id: messageIds,
    });
  
    putMock.mockRestore();
  });
});
