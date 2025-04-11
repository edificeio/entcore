import { EditorInstance } from '@edifice.io/react/editor';

export const hasMark = (extensionName: string, editor: EditorInstance | null) =>
  !!editor?.extensionManager.splittableMarks.includes(extensionName);
