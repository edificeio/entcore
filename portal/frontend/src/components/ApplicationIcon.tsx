import { Application } from '~/models/application';
import { useTranslation } from 'react-i18next';
import { getIconClass } from '~/utils/icon-class-name';
import { getAppName } from '~/utils/get-app-name';
import clsx from 'clsx';

export function ApplicationIcon({ data }: { data: Application }) {
  const { t } = useTranslation('common');
  const isImage = data.icon.includes('/');
  const iconClassName = getIconClass(data);
  const appName = getAppName(data, t);
  const classApplicationIcon = clsx(
    `application-icon rounded mb-8 d-flex align-items-center justify-content-center mx-auto`,
    data.color && data.color,
    !data.color && 'bg-secondary',
  );
  return (
    <span className={classApplicationIcon} style={{ width: 64, height: 64 }}>
      {isImage ? (
        <img className="w-full h-full" src={data.icon} alt={appName} />
      ) : (
        <i className={iconClassName} />
      )}
    </span>
  );
}
