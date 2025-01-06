import { useLoaderData } from 'react-router-dom';
import { Folder } from '~/models';
import { t } from 'i18next';

export function MobileMenu() {
  // See `layout` loader
  const { foldersTree, actions } = useLoaderData() as {
    foldersTree: Folder[];
    actions: Record<string, boolean>;
  };

  if (!foldersTree || !actions) {
    return null;
  }

  return <>{t('TODO Combo')}</>;
}
