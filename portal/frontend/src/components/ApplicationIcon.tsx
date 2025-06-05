import { Application } from "~/models/application";
import './application-icon.css';
import { useTranslation } from 'react-i18next';
import { getIconClass } from '~/utils/icon-class-name';

export function ApplicationIcon({ data }: { data: Application }) {
  const { t } = useTranslation('common');
  const iconClassName = getIconClass(data);
  return (
    <div style={{ width: 107, height: 127, textAlign: 'center' }}>
      <a
        href={data.address}
        className="application-icon rounded bg-primary mb-8"
      >
        <i className={iconClassName} />
      </a>
      <h1
        className="small text-gray-900"
        style={{
          fontFamily: 'Arimo',
          fontWeight: 400,
          textTransform: 'unset',
        }}
      >
        {t(data.prefix.substring(1))}
      </h1>
    </div>
  );
}