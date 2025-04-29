import { describe, expect, test } from 'vitest';
import { baseUrl, attachmentService } from '.';
import { odeServices } from '@edifice.io/client';

describe('Conversation Attachment Mutation Methods', () => {
  test('makes a POST request to attach a file or blob to a draft message', async () => {
    const messageId = 'f43d3783';
    const uploadedFile = new File(
      ["Le sage ne dit pas ce qu'il sait, le sot ne sait pas ce qu'il dit."],
      'Koan.txt',
      {
        type: 'text/plain',
      },
    );
    const formData = new FormData();
    formData.append('file', uploadedFile);

    const postFileMock = vi
      .spyOn(odeServices.http(), 'postFile')
      .mockResolvedValue(undefined);

    const response = await attachmentService.attach(messageId, uploadedFile);

    expect(postFileMock).toHaveBeenCalledWith(
      `${baseUrl}/message/${messageId}/attachment`,
      formData,
    );
    expect(response).toBeUndefined();

    postFileMock.mockRestore();
  });

  test('makes a DELETE request to detach an attachment from a draft message', async () => {
    const messageId = 'f43d3783';
    const attachmentId = 'ababab';

    const deleteFileMock = vi
      .spyOn(odeServices.http(), 'delete')
      .mockResolvedValue(undefined);

    await attachmentService.detach(messageId, attachmentId);

    expect(deleteFileMock).toHaveBeenCalledWith(
      `${baseUrl}/message/${messageId}/attachment/${attachmentId}`,
    );

    deleteFileMock.mockRestore();
  });
});
