import { ApplicationList } from '~/features/application-list/ApplicationList';
import { useTranslation } from 'react-i18next';
import './my-apps.css';

export const MyAppLayout = ({ theme }: { theme: string }) => {
  const { t } = useTranslation('common');
  const classLayout = `d-flex flex-column gap-24 px-8 py-24 my-apps-layout theme-${theme}`;
  return (
    <div className={classLayout}>
      <header>
        <h1 className="m-0 h3 text-info">{t('navbar.applications')}</h1>
      </header>
      <ApplicationList />
    </div>
  );
};
