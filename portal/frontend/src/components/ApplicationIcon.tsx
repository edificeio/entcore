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
    <a
      className="application-icon rounded bg-primary mb-8 d-flex align-items-center justify-content-center mx-auto"
      href={data.address}
      rel={data.isExternal ? 'noopener noreferrer' : undefined}
      style={{ width: 64, height: 64 }}
      target={data.isExternal ? '_blank' : undefined}
    >
      {isImage ? (
        <img src={data.icon} alt={appName} />
      ) : (
        <i className={iconClassName} />
      )}
    </a>
  );
}
