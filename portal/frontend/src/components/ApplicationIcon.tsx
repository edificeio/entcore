import { Application } from '~/models/application';
import { useTranslation } from 'react-i18next';
import { getIconClass } from '~/utils/icon-class-name';
import { getAppName } from '~/utils/get-app-name';
import clsx from 'clsx';
import { useState } from 'react';
import { FavoriteStarIcon } from './FavoriteStarIcon';

export function ApplicationIcon({
  data,
  isFavorite = false,
}: {
  data: Application;
  isFavorite?: boolean;
}) {
  const { t } = useTranslation('common');
  const [hasError, setHasError] = useState(false);
  const isImage = data.icon.includes('/');
  const iconClassName = getIconClass(data);
  const appName = getAppName(data, t);
  const classApplicationIcon = clsx(
    `application-icon rounded mb-8 d-flex align-items-center justify-content-center mx-auto`,
    data.color && `bg-${data.color}`,
    !data.color && 'bg-secondary',
    data.category === 'connector' && 'application-icon-connector bg-white',
    'text-white fw-bold fs-4',
  );

  return (
    <span className="position-relative" style={{ display: 'inline-block' }}>
      <FavoriteStarIcon isFavorite={isFavorite} />
      <span className={classApplicationIcon}>
        {isImage && !hasError ? (
          <img
            className="w-full h-full"
            src={data.icon}
            alt={appName}
            onError={() => setHasError(true)}
          />
        ) : !isImage ? (
          <i className={iconClassName} />
        ) : (
          <span>{appName.charAt(0).toUpperCase()}</span>
        )}
      </span>
    </span>
  );
}
