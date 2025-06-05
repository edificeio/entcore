import { Application } from "~/models/application";
import './application-icon.css';
import { useTranslation } from 'react-i18next';

export function ApplicationIcon({ data }: { data: Application }) {
  const { t } = useTranslation();
  return (
    <div style={{ width: 107, height: 127, textAlign: 'center' }}>
      <a href={data.address} className="application-icon rounded bg-primary">
        <i className={`${data.icon} ${data.displayName}`} />
      </a>
      <h1 className="small text-gray-900" style={{ fontFamily: 'Arimo' }}>
        {t(data.name)}
      </h1>
    </div>
  );
}