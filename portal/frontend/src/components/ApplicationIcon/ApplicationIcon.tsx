import { Image, useEdificeIcons } from '@edifice.io/react';
import * as IconSprites from '@edifice.io/react/icons/apps';
import clsx from 'clsx';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Application } from '~/models/application';
import { getAppName } from '~/utils/get-app-name';
import { FavoriteStarIcon } from '../FavoriteStarIcon';

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
    !data.color && 'bg-white',
    data.category === 'connector' && 'application-icon-connector bg-white p-0',
    'text-white fw-bold fs-4',
  );

  const appName = getAppName(data, t);

  const iconCode = data.icon ? getIconCode(data.icon) : '';
  const isIconURL = isIconUrl(data.icon);
  const appCode = iconCode || 'placeholder';
  const webapp: IWebApp = {
    name: data.name,
    address: data.address,
    display: data.display,
    displayName: data.displayName,
    icon: data.icon,
    isExternal: data.isExternal,
    scope: data.scope,
    version: data.version,
  };

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
              loading="lazy"
              onError={() => setHasError(true)}
            />
          ) : (
            <span className="application-icon-src">
              {appName.charAt(0).toUpperCase()}
            </span>
          )
        ) : (
          <IconComponent className="application-icon-app" />
        )}
      </span>

      {data.version === 'BETA' && (
        <Badge
          variant={{ type: 'beta', app: webapp }}
          className="myapps-beta-badge"
        />
      )}
    </span>
  );
}
