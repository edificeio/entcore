import { Application } from '~/models/application';
import { useTranslation } from 'react-i18next';
import { getIconClass } from '~/utils/icon-class-name';
import './application-icon.css';
import { getAppName } from '~/utils/get-app-name';

export function ApplicationIcon({ data }: { data: Application }) {
  const { t } = useTranslation('common');
  const isImage = data.icon.includes('/');
  const iconClassName = getIconClass(data);
  const appName = getAppName(data, t);

  return (
    <span
      className="application-icon rounded bg-primary mb-8 d-flex align-items-center justify-content-center mx-auto"
      style={{ width: 64, height: 64 }}
    >
      {isImage ? (
        <img src={data.icon} alt={appName} />
      ) : (
        <i className={iconClassName} />
      )}
    </span>
  );
}
