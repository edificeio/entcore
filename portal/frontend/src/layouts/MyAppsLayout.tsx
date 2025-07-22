import { ApplicationList } from '~/features/application-list/ApplicationList';
import { useTranslation } from 'react-i18next';
import { ToolbarCategories } from '~/components/ToolbarCategories';
import { useHydrateUserPreferences } from '~/hooks/useHydrateUserPreferences';
import './my-apps.css';
import { useApplications } from '~/services';
import { Flex, SearchBar, TextSkeleton } from '@edifice.io/react';

import { useMemo, useState } from 'react';
import MyAppOnboardingModal from '~/components/MyAppOnboardingModal';

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

  if (!isReady)
    //Skeleton
    return (
      <div className={classLayout}>
        <header className="d-flex justify-content-between my-apps-header">
          <div>
            <TextSkeleton className="my-apps-skeleton-title mb-4" />
          </div>
          <Flex gap="16" className="p-3 align-items-center" align="end">
            <div>
              <TextSkeleton className="my-apps-skeleton-searchBar" />
            </div>
            <div>
              <TextSkeleton className="d-inline-block my-apps-skeleton-notification-btn" />
            </div>
          </Flex>
        </header>
        <div
          className="d-flex flex-wrap gap-16 justify-content-center align-items-center mx-auto col"
          style={{ maxWidth: 1091 }}
        >
          <TextSkeleton className="my-apps-skeleton-toolbar" />
        </div>

        <div
          className="d-flex flex-wrap gap-16 justify-content-center align-items-center mx-auto col"
          style={{ maxWidth: 1091 }}
        >
          {Array.from({ length: 12 }).map((_, idx) => (
            <Flex
              key={idx}
              direction="column"
              className="justify-content-center align-items-center"
              gap="16"
            >
              <TextSkeleton className="my-apps-skeleton-app-box" />
              <TextSkeleton className="my-apps-skeleton-app-text" />
            </Flex>
          ))}
        </div>
      </div>
    );

  return (
    <>
      <div className={classLayout}>
        <header className="d-flex justify-content-between my-apps-header">
          <h1 className="m-0 h3 text-info">{t('navbar.applications')}</h1>
          <Flex gap="16" className="p-3" align="end">
            <div>
              <SearchBar
                clearable
                isVariant
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                placeholder={t('my.apps.search')}
                className="my-apps-search"
                size="md"
              />
            </div>
            <div>
              <MyAppOnboardingModal />
            </div>
          </Flex>
        </header>
        {!search.length && <ToolbarCategories />}

        <ApplicationList
          applications={filteredApps}
          isSearch={search.length ? true : false}
        />
      </div>
    </>
  );
};
