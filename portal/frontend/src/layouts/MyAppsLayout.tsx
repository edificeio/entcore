import { ApplicationList } from "~/features/application-list/ApplicationList"
import { useTranslation } from 'react-i18next';

export const MyAppLayout = () => {
  const { t } = useTranslation('common');
  return (
    <div className="d-flex flex-column gap-24 px-8 py-24">
      <header>
        <h1 className="m-0 h3 text-info">{t('navbar.applications')}</h1>
      </header>
      <ApplicationList />
    </div>
  );
}