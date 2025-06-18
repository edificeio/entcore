import { Dropdown, useEdificeClient, useLibraryUrl } from '@edifice.io/react';
import {
  IconInfoCircle,
  IconExternalLink,
  IconStar,
  IconLibrary,
} from '@edifice.io/react/icons';
import { useTranslation } from 'react-i18next';
import { Application } from '~/models/application';

export function ApplicationMenu({ data }: { data: Application }) {
  const { t } = useTranslation('common');
  const libraryUrl = useLibraryUrl(data.displayName);
  const { currentLanguage } = useEdificeClient();
  const helpUrl =
    currentLanguage && data.help?.[currentLanguage]
      ? data.help[currentLanguage]
      : (data.help?.['en'] ?? undefined);
  const dataIdFavorite = data.isFavorite
    ? 'btn-remove-favorite'
    : 'btn-add-favorite';

  const openInNewTab = (url: string, e?: React.MouseEvent) => {
    e?.preventDefault();
    window.open(url, '_blank', 'noopener,noreferrer');
  };

  const dropdownItems = [
    <Dropdown.Item
      data-id="btn-open"
      key="open"
      onClick={(e) => {
        e.preventDefault();
        if (data.category === 'connector') {
          openInNewTab(data.address);
        } else {
          window.open(data.address, '_self');
        }
      }}
      icon={<IconExternalLink />}
    >
      {t('my.apps.open.application')}
    </Dropdown.Item>,

    <Dropdown.Item
      data-id={dataIdFavorite}
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

    data.libraries && libraryUrl && (
      <Dropdown.Item
        data-id="btn-libraries"
        key="examples"
        onClick={(e) => openInNewTab(libraryUrl, e)}
        icon={<IconLibrary />}
      >
        {t('my.apps.examples')}
      </Dropdown.Item>
    ),

    helpUrl && (
      <Dropdown.Item
        data-id="btn-help"
        key="help"
        onClick={(e) => openInNewTab(helpUrl, e)}
        icon={<IconInfoCircle />}
      >
        {t('my.apps.infos')}
      </Dropdown.Item>
    ),
  ].filter(Boolean);

  return <Dropdown.Menu>{dropdownItems}</Dropdown.Menu>;
}
