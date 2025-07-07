import { Toolbar, ToolbarItem } from '@edifice.io/react';
import {
  IconCheck,
  IconClock,
  IconExternalLink,
  IconStar,
} from '@edifice.io/react/icons';
import { IconBlog } from '@edifice.io/react/icons/apps';
import { IconTeacher } from '@edifice.io/react/icons/audience';
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

  const isMobileView = window.innerWidth <= 768;

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
    { id: 'all', name: 'my.apps.tabs.all', icon: <IconCheck /> },
    { id: 'favorites', name: 'my.apps.tabs.favorites', icon: <IconStar /> },
    {
      id: 'communication',
      name: 'my.apps.tabs.communication',
      icon: <IconBlog />,
    },
    { id: 'pedagogy', name: 'my.apps.tabs.pedagogy', icon: <IconTeacher /> },
    {
      id: 'organisation',
      name: 'my.apps.tabs.organisation',
      icon: <IconClock />,
    },
  ];

  const categories: Category[] = hasConnectors
    ? [
        ...baseCategories,
        {
          id: 'connector',
          name: 'my.apps.tabs.connector',
          icon: <IconExternalLink />,
        },
      ]
    : baseCategories;

  const filterToolbar: ToolbarItem[] = categories.map<ToolbarItem>(
    (category) => {
      const isActive = activeCategory === category.id;

      return isMobileView
        ? {
            type: 'icon',
            name: category.id,
            props: {
              className: clsx('fw-normal', {
                'bg-secondary-200 fw-bold': isActive,
              }),
              icon: category.icon,
              onClick: () => {
                setActiveCategory(category.id);
                updateMyAppsPreferences.mutate({ tab: category.id });
              },
            },
          }
        : {
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
