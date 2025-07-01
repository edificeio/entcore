import { useTranslation } from 'react-i18next';
import { useCategoryStore } from '~/store/categoryStore';
import { getAppName } from '~/utils/get-app-name';
import { ApplicationListGrid } from './ApplicationListGrid';
import { EmptyCategory } from '~/components/EmptyCategory';
import { Application } from '~/models/application';

type Props = {
  isSearch?: boolean;
  applications: Application[];
};

export function ApplicationList({ applications, isSearch }: Props) {
  const { t } = useTranslation('common');
  const { activeCategory } = useCategoryStore();

  if (isSearch) {
    if (applications.length) {
      return <ApplicationListGrid applications={applications} />;
    } else {
      return <EmptyCategory category="search" />;
    }
  }

  if (activeCategory === 'none') return null;

  const sortedApps = [...applications].sort((a, b) =>
    getAppName(a, t).localeCompare(getAppName(b, t)),
  );

  if (activeCategory === 'all') {
    if (!applications.length) {
      return <EmptyCategory category="all" />;
    }

    const [internalApps, externalApps] = sortedApps.reduce<
      [Application[], Application[]]
    >(
      ([internals, externals], app) =>
        app.category === 'connector'
          ? [internals, [...externals, app]]
          : [[...internals, app], externals],
      [[], []],
    );

    return (
      <>
        <ApplicationListGrid applications={internalApps} />
        {externalApps.length > 0 && (
          <>
            <div className="d-flex flex-wrap gap-16 flex-column">
              <div className="w-full bg-gray-400" style={{ height: 1 }} />
              <h2
                className="small text-center my-8"
                style={{ fontFamily: 'Arimo' }}
              >
                {t('my.apps.services.title')}
              </h2>
            </div>
            <ApplicationListGrid applications={externalApps} isConnectors />
          </>
        )}
      </>
    );
  }

  const filteredApps =
    activeCategory === 'favorites'
      ? sortedApps
          .filter((app) => app.isFavorite === true)
          .sort((a, b) => {
            if (a.category === 'connector' && b.category !== 'connector')
              return 1;
            if (a.category !== 'connector' && b.category === 'connector')
              return -1;
            return 0;
          })
      : sortedApps.filter((app) => app.category === activeCategory);

  return filteredApps.length > 0 ? (
    <ApplicationListGrid applications={filteredApps} />
  ) : (
    <EmptyCategory category={activeCategory} />
  );
}
