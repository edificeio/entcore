import { Dropdown, useEdificeClient, useLibraryUrl } from '@edifice.io/react';
import {
  IconInfoCircle,
  IconExternalLink,
  IconStar,
  IconLibrary,
} from '@edifice.io/react/icons';
import { useTranslation } from 'react-i18next';
import { Application } from '~/models/application';
import { openInNewTab } from '~/utils/open-in-new-tab';

export function ApplicationMenu({ data }: { data: Application }) {
  const { t } = useTranslation('common');
  const libraryUrl = useLibraryUrl(data.displayName);
  const { currentLanguage } = useEdificeClient();
  const helpUrl =
    currentLanguage && data.help?.[currentLanguage]
      ? data.help[currentLanguage]
      : (data.help?.['en'] ?? undefined);

  const dropdownItems = [
    <Dropdown.Item
      key="open"
      onClick={
        data.category === 'connector'
          ? openInNewTab(data.address)
          : () => window.open(data.address)
      }
      icon={<IconExternalLink />}
    >
      {t('my.apps.open.application')}
    </Dropdown.Item>,

    <Dropdown.Item
      key="favorite"
      onClick={() =>
        console.log(data.isFavorite ? 'remove favorite' : 'add favorite')
      }
      icon={<IconStar />}
    >
      {data.isFavorite
        ? t('my.apps.remove.favorite')
        : t('my.apps.add.favorite')}
    </Dropdown.Item>,

    data.libraries && (
      <Dropdown.Item
        key="examples"
        onClick={openInNewTab(libraryUrl)}
        icon={<IconLibrary />}
      >
        {t('my.apps.examples')}
      </Dropdown.Item>
    ),

    helpUrl && (
      <Dropdown.Item
        key="help"
        onClick={openInNewTab(helpUrl)}
        icon={<IconInfoCircle />}
      >
        {t('my.apps.infos')}
      </Dropdown.Item>
    ),
  ].filter(Boolean);

  return <Dropdown.Menu>{dropdownItems}</Dropdown.Menu>;
}
