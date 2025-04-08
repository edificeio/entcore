import { hasMark } from './has-mark';
import { EditorInstance } from '@edifice.io/react/editor';

export const hasTextStyle = (
  styleName: string,
  editor: EditorInstance | null,
) =>
  editor?.extensionManager.extensions.find(
    (item) => item.name === styleName && hasMark('textStyle', editor),
  );
