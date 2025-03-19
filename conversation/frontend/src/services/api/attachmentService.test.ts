import { describe, expect, test } from 'vitest';
import { baseUrl, attachmentService } from '.';
import { odeServices } from 'edifice-ts-client';

describe('Conversation Attachment Mutation Methods', () => {
  test('makes a POST request to attach a file or blob to a message', async () => {
    const messageId = 'f43d3783';
    const uploadedFile = new File(["Le sage ne dit pas ce qu'il sait, le sot ne sait pas ce qu'il dit."], "Koan.txt", {
      type: "text/plain",
    });

    const postFileMock = vi.spyOn(odeServices.http(), 'postFile').mockResolvedValue(undefined);

    const response = await attachmentService.attach(messageId, uploadedFile);

    expect(postFileMock).toHaveBeenCalledWith(`${baseUrl}/message/${messageId}/attachment`, uploadedFile);
    expect(response).toBeUndefined();

    postFileMock.mockRestore();
  });
});
