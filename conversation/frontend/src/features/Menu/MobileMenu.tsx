import { useRouteLoaderData } from 'react-router-dom';
import { Folder } from '~/models';
import { t } from 'i18next';

export function MobileMenu() {
  const { foldersTree, actions } = useRouteLoaderData('layout') as {
    foldersTree: Folder[];
    actions: Record<string, boolean>;
  };

  if (!foldersTree || !actions) {
    return null;
  }

  return <>{t('TODO Combo')}</>;
}
