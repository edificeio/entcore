import { odeServices } from 'edifice-ts-client';

/**
 * Creates a message attachment service with the specified base URL.
 *
 * @param baseURL The base URL for the folder service API.
 * @returns A service to interact with folders.
 */
export const createAttachmentService = (baseURL: string) => ({
  attach(messageId: string, payload: File | Blob) {
    const formData = new FormData();
    formData.append('file', payload);
    return odeServices.http().postFile<{
      id: string;
    }>(`${baseURL}/message/${messageId}/attachment`, formData);
  },

  detach(messageId: string, attachmentId: string) {
    return odeServices.http().delete<{
      fileId: string;
      fileSize: number;
    }>(`${baseURL}/message/${messageId}/attachment/${attachmentId}`);
  },
});
