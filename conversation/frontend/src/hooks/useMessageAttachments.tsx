import { useToast } from '@edifice.io/react';
import { odeServices } from 'edifice-ts-client';
import { Attachment, Message } from '~/models';
import { baseUrl, useCreateOrUpdateDraft } from '~/services';

import { useState } from 'react';
import {
  useAttachFiles,
  useDetachFile,
  useDownloadAttachment,
} from '~/services/queries/attachment';
import { useI18n } from './useI18n';

export function useMessageAttachments({ id }: Message) {
  const attachFileMutation = useAttachFiles();
  const detachFileMutation = useDetachFile();
  const downloadAttachmentMutation = useDownloadAttachment();
  const [detachInProgress, setDetachInProgress] = useState(new Set<string>());
  const toast = useToast();
  const { t } = useI18n();

  // These hooks is required when attaching files to a blank new draft, without id.
  const createOrUpdateDraft = useCreateOrUpdateDraft();

  const downloadAllUrl = `${baseUrl}/message/${id}/allAttachments`;

  const getDownloadUrl = (attachementId: string) =>
    `${baseUrl}/message/${id}/attachment/${attachementId}`;

  async function attachFiles(files: FileList | null) {
    if (!id) {
      // Save this new draft to get its id
      const promise = await createOrUpdateDraft();
      if (promise) id = promise.id;
    }
    const mutateVars: { draftId: string; files: File[] } = {
      draftId: id,
      files: [],
    };
    for (let i = 0; files && i < files.length; i++) {
      const file = files.item(i);
      if (file) mutateVars.files.push(file);
    }
    attachFileMutation.mutateAsync(mutateVars);
  }

  function detachFile(attachmentId: string) {
    setDetachInProgress((prev) => new Set(prev).add(attachmentId));
    return detachFileMutation.mutateAsync(
      {
        draftId: id,
        attachmentId,
      },
      {
        onError: () => {
          setDetachInProgress((prev) => {
            const newSet = new Set(prev);
            newSet.delete(attachmentId);
            return newSet;
          });
        },
      },
    );
  }

  function detachFiles(attachments: Attachment[]) {
    return Promise.all(attachments.map(({ id }) => detachFile(id)));
  }

  /**
   * Copy files to workspace
   * @param attachments - The attachments to copy
   * @param selectedFolderId - The folder ID to copy the files to
   * @returns {Promise<boolean>} - Returns true if the operation was successful, false otherwise
   */
  async function copyToWorkspace(
    attachments: Attachment[],
    selectedFolderId: string,
  ) {
    const files = await downloadAttachements(attachments);
    return sendFilesToWorkspace(files, selectedFolderId);
  }

  async function downloadAttachements(attachments: Attachment[]) {
    const downloadFilesPromises = attachments.map(async (attachment) => {
      const attachmentBlob = await downloadAttachmentMutation.mutateAsync({
        messageId: id,
        attachmentId: attachment.id,
      });

      if (!attachmentBlob) return;

      return new File([attachmentBlob], attachment.filename, {
        type: attachment.contentType,
      });
    });

    const files = await Promise.all(downloadFilesPromises);
    return files.filter((file) => !!file);
  }

  async function sendFilesToWorkspace(files: File[], selectedFolderId: string) {
    const total = files.length;
    try {
      const addFilesPromises = files.map((file) =>
        odeServices.workspace().saveFile(file, {
          parentId: selectedFolderId,
        }),
      );
      const results = await Promise.allSettled(addFilesPromises);
      const succeeded = results.filter((r) => r.status === 'fulfilled').length;
      const failed = total - succeeded;

      const groupedErrors = groupFileErrors(files, results);
      const errorMessage = formatMultiErrorMessage(groupedErrors, t);

      if (succeeded === 0) {
        toast.error(
          t('conversation.copyToWorkspace.notify.allFailed', {
            count: total,
            details: errorMessage,
          }),
        );
        return false;
      }

      if (failed > 0) {
        toast.warning(
          t('conversation.copyToWorkspace.notify.partialFailed', {
            succeeded,
            failed,
            count: total,
            details: errorMessage,
          }),
        );
      } else {
        toast.success(
          t('conversation.copyToWorkspace.notify.success', { count: total }),
        );
      }
      return succeeded > 0;
    } catch (error: any) {
      const errorMessage = t('conversation.copyToWorkspace.notify.error', {
        count: total,
      });
      toast.error(errorMessage);
      return false;
    }
  }

  return {
    attachFiles,
    copyToWorkspace,
    detachFile,
    detachFiles,
    detachInProgress,
    downloadAllUrl,
    getDownloadUrl,
    isMutating: attachFileMutation.isPending || detachFileMutation.isPending,
  };
}

interface GroupedError {
  error: string;
  files: string[];
}

export function groupFileErrors(
  files: File[],
  results: PromiseSettledResult<unknown>[],
): GroupedError[] {
  const errorMap = new Map<string, string[]>();

  results.forEach((result, index) => {
    if (result.status === 'rejected') {
      const error = result.reason?.error || 'e400';
      const filename = files[index].name;
      if (!errorMap.has(error)) {
        errorMap.set(error, []);
      }
      errorMap.get(error)?.push(filename);
    }
  });

  return Array.from(errorMap.entries()).map(([error, files]) => ({
    error,
    files,
  }));
}

export function formatMultiErrorMessage(
  groupedErrors: GroupedError[],
  t: (key: string, params?: any) => string,
): string {
  return groupedErrors
    .map(({ error, files }) => {
      const filesCount = files.length;
      const filesList = files.join(', ');
      return t('conversation.copyToWorkspace.error.details', {
        count: filesCount,
        files: filesList,
        error: t(error),
      });
    })
    .join('\n');
}
