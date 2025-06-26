import { ApplicationList } from '~/features/application-list/ApplicationList';
import { useTranslation } from 'react-i18next';
import { ToolbarCategories } from '~/components/ToolbarCategories';
import { useHydrateUserPreferences } from '~/hooks/useHydrateUserPreferences';
import './my-apps.css';
import { useApplications } from '~/services';
import { SearchBar } from '@edifice.io/react';
import { useMemo, useState } from 'react';

export const MyAppLayout = ({ theme }: { theme: string }) => {
  const { t } = useTranslation('common');
  const classLayout = `d-flex flex-column gap-24 px-8 py-24 my-apps-layout theme-${theme}`;

  const { isHydrated } = useHydrateUserPreferences();
  const { applications, isLoading, isError } = useApplications();
  const [search, setSearch] = useState('');

  const isReady =
    isHydrated && !isLoading && !isError && applications !== undefined;

  const normalizeLoose = (str: string) =>
    str
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .toLocaleLowerCase();

  const filteredApps = useMemo(() => {
    if (!applications) return [];

    const searchNorm = normalizeLoose(search);

    return applications.filter((app) =>
      normalizeLoose(app.appName).includes(searchNorm),
    );
  }, [applications, search]);

  if (!isReady) return <div>skeleton</div>;

  return (
    <div className={classLayout}>
      <header className="d-flex justify-content-between align-items-center">
        <h1 className="m-0 h3 text-info">{t('navbar.applications')}</h1>
        <SearchBar
          isVariant
          onChange={(e) => setSearch(e.target.value)}
          placeholder="Search something...."
          size="md"
        />
      </header>
      {!search.length && <ToolbarCategories />}
      <ApplicationList
        applications={filteredApps}
        isSearch={search.length ? true : false}
      />
    </div>
  );
};
