import { Toolbar, ToolbarItem } from '@edifice.io/react';
import clsx from 'clsx';
import { useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { Category } from '~/models/category';
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
      setActiveCategory((myAppsPreferences.tab ?? 'all') as Category);
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
    'all',
    'favorites',
    'communication',
    'pedagogy',
    'organisation',
  ];

  const categories: Category[] = hasConnectors
    ? [...baseCategories, 'connector']
    : baseCategories;

  const filterToolbar: ToolbarItem[] = categories.map<ToolbarItem>(
    (category) => {
      const isActive = activeCategory === category;
      const categoryDisplay = `tabs.${category}`;

      return {
        type: 'button',
        name: category,
        props: {
          className: clsx('fw-normal', {
            'bg-secondary-200 fw-bold': isActive,
          }),
          children: (
            <span data-id={`tab-${category}`}>{t(categoryDisplay)}</span>
          ),
          onClick: () => {
            setActiveCategory(category);
            updateMyAppsPreferences.mutate({ tab: category });
          },
        },
      };
    },
  );
  return (
    <div className="toolbar-categories-wrapper">
      <Toolbar
        data-id="tabs-categories"
        variant="no-shadow"
        className="toolbar-categories px-4 py-2 ms-md-16 border border-secondary-300 rounded-3"
        items={filterToolbar}
      />
    </div>
  );
};
