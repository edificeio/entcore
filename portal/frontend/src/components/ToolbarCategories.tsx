import { Toolbar, ToolbarItem } from '@edifice.io/react';
import clsx from 'clsx';
import { useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { Category, CategoryId } from '~/models/category';
import { useApplications } from '~/services';
import {
  useMyAppsPreferences,
  useUpdateMyAppsPreferences,
} from '~/services/queries/myAppsPreferences';
import { useCategoryStore } from '~/store/categoryStore';

export const ToolbarCategories = () => {
  const { applications, isLoading, isError } = useApplications();
  const { setActiveCategory, activeCategory } = useCategoryStore();
  const { t } = useTranslation('common');

  const {
    data: myAppsPreferences,
    isError: isErrorMyApps,
    isLoading: isLoadingMyApps,
  } = useMyAppsPreferences();
  const updateMyAppsPreferences = useUpdateMyAppsPreferences();

  const hasConnectors = applications?.some(
    (app) => app.category === 'connector',
  );

  useEffect(() => {
    if (isLoadingMyApps) return; // skeleton

    if (
      isErrorMyApps ||
      !myAppsPreferences ||
      (myAppsPreferences.tab === 'connector' && !hasConnectors)
    ) {
      setActiveCategory('all');
    } else {
      setActiveCategory((myAppsPreferences.tab ?? 'all') as CategoryId);
    }
  }, [
    myAppsPreferences,
    isErrorMyApps,
    isLoadingMyApps,
    setActiveCategory,
    hasConnectors,
  ]);

  if (isLoading) return <div>Chargement des applications...</div>;
  if (isError) return <div>Erreur lors du chargement des applications.</div>;

  const baseCategories: Category[] = [
    { id: 'all', name: 'my.apps.tabs.all' },
    { id: 'favorites', name: 'my.apps.tabs.favorites' },
    {
      id: 'communication',
      name: 'my.apps.tabs.communication',
    },
    { id: 'pedagogy', name: 'my.apps.tabs.pedagogy' },
    {
      id: 'organisation',
      name: 'my.apps.tabs.organisation',
    },
  ];

  const categories: Category[] = hasConnectors
    ? [
        ...baseCategories,
        {
          id: 'connector',
          name: 'my.apps.tabs.connector',
        },
      ]
    : baseCategories;

  const filterToolbar: ToolbarItem[] = categories.map<ToolbarItem>(
    (category) => {
      const isActive = activeCategory === category.id;

      return {
        type: 'button',
        name: category.id,
        props: {
          className: clsx('fw-normal', {
            'bg-secondary-200 fw-bold': isActive,
          }),
          children: (
            <span data-id={`tab-${category.id}`}>{t(category.name)}</span>
          ),
          onClick: () => {
            setActiveCategory(category.id);
            updateMyAppsPreferences.mutate({ tab: category.id });
          },
        },
      };
    },
  );
  return (
    <div className="toolbar-categories-wrapper">
      <div className="toolbar-categories">
        <Toolbar
          data-id="tabs-categories"
          variant="no-shadow"
          className="toolbar-categories px-4 py-2 ms-md-16 border border-secondary-300 rounded-3"
          items={filterToolbar}
        />
      </div>
    </div>
  );
};
