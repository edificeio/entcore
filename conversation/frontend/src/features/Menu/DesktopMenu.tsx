import {
  IconDelete,
  IconDepositeInbox,
  IconSend,
  IconSmiley,
  IconWrite,
} from '@edifice.io/react/icons';
import { Menu } from '@edifice.io/react';
import { NOOP } from '@edifice.io/utilities';
import { useLoaderData } from 'react-router-dom';
import { Folder } from '~/models';
import { t } from 'i18next';

export function DesktopMenu() {
  // See `layout` loader
  const { foldersTree, actions } = useLoaderData() as {
    foldersTree: Folder[];
    actions: Record<string, boolean>;
  };

  if (!foldersTree || !actions) {
    return null;
  }

  return (
    <>
      <Menu label="generic.folders">
        <Menu.Item>
          <Menu.Button
            leftIcon={<IconDepositeInbox />}
            onClick={NOOP}
            rightIcon={<IconSmiley />}
          >
            {t('inbox')}
          </Menu.Button>
          <Menu.Button
            leftIcon={<IconSend />}
            onClick={NOOP}
            rightIcon={<IconSmiley />}
          >
            {t('outbox')}
          </Menu.Button>
          <Menu.Button
            leftIcon={<IconWrite />}
            onClick={NOOP}
            rightIcon={<IconSmiley />}
          >
            {t('draft')}
          </Menu.Button>
          <Menu.Button
            leftIcon={<IconDelete />}
            onClick={NOOP}
            rightIcon={<IconSmiley />}
          >
            {t('trash')}
          </Menu.Button>
        </Menu.Item>
      </Menu>
      TODO {JSON.stringify(foldersTree)}
      <div className="w-100 border-bottom"></div>
      TODO Mes dossiers
      <div className="w-100 border-bottom"></div>
      TODO Espace utilisé
    </>
  );
}
