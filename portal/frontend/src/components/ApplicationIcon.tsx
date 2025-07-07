import { Application } from '~/models/application';
import { useTranslation } from 'react-i18next';
import { getAppName } from '~/utils/get-app-name';
import clsx from 'clsx';
import { useState } from 'react';
import { FavoriteStarIcon } from './FavoriteStarIcon';
import { Image, useEdificeIcons } from '@edifice.io/react';
import * as IconSprites from '@edifice.io/react/icons/apps';

export function ApplicationIcon({
  data,
  isFavorite = false,
}: {
  data: Application;
  isFavorite?: boolean;
}) {
  const { t } = useTranslation('common');
  const { isIconUrl, getIconCode } = useEdificeIcons();

  const [hasError, setHasError] = useState(false);

  const classApplicationIcon = clsx(
    `application-icon rounded mb-8 d-flex align-items-center justify-content-center mx-auto`,
    data.color && `bg-${data.color}`,
    !data.color && 'bg-secondary',
    data.category === 'connector' && 'application-icon-connector bg-white',
    'text-white fw-bold fs-4',
  );

  const appName = getAppName(data, t);

  const iconCode = data.icon ? getIconCode(data.icon) : '';
  const isIconURL = isIconUrl(data.icon);
  const appCode = iconCode || 'placeholder';

  const IconComponent =
    IconSprites[
      `Icon${appCode
        .split('-')
        .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
        .join('')}` as keyof typeof IconSprites
    ] ?? IconSprites.IconPlaceholder;

  return (
    <span className="position-relative" style={{ display: 'inline-block' }}>
      <FavoriteStarIcon isFavorite={isFavorite} />
      <span className={classApplicationIcon}>
        {isIconURL ? (
          !hasError ? (
            <Image
              className="w-full h-full"
              src={data.icon}
              alt={appName}
              onError={() => setHasError(true)}
            />
          ) : (
            <span style={{ fontSize: '4.5rem', fontWeight: 'bold', lineHeight: 1 }}>
              {appName.charAt(0).toUpperCase()}
            </span>
          )
        ) : (
          <IconComponent style={{ width: '4.5rem', height: '4.5rem' }} />
        )}
      </span>
    </span>
  );
}
