import { ApplicationList } from '~/features/application-list/ApplicationList';
import { useTranslation } from 'react-i18next';
import { ToolbarCategories } from '~/components/ToolbarCategories';
import { useHydrateUserPreferences } from '~/hooks/useHydrateUserPreferences';
import './my-apps.css';
import { useApplications } from '~/services';

export const MyAppLayout = ({ theme }: { theme: string }) => {
  const { t } = useTranslation('common');
  const classLayout = `d-flex flex-column gap-24 px-8 py-24 my-apps-layout theme-${theme}`;

  const { isHydrated } = useHydrateUserPreferences();
  const { applications, isLoading, isError } = useApplications();

  const isReady =
    isHydrated && !isLoading && !isError && applications !== undefined;
  if (!isReady) return <div>skeleton</div>;

  return (
    <div className={classLayout}>
      <header>
        <h1 className="m-0 h3 text-info">{t('navbar.applications')}</h1>
      </header>
      <ToolbarCategories />
      <ApplicationList applications={applications} />
    </div>
  );
};
