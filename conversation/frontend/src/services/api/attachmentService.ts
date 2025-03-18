import { odeServices } from 'edifice-ts-client';

/**
 * Creates a message attachment service with the specified base URL.
 *
 * @param baseURL The base URL for the folder service API.
 * @returns A service to interact with folders.
 */
export const createAttachmentService = (baseURL: string) => ({

    attach(messageId: string, payload: File | Blob) {
        return odeServices
            .http()
            .postFile<{ id: string }>(`${baseURL}/message/${messageId}/attachment`, payload);
    },

});
