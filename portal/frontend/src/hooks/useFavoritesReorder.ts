import { useToast } from '@edifice.io/react';
import { useTranslation } from 'react-i18next';
import { useApplications } from '~/services';
import { useUpdateUserPreferences } from '~/services/queries/preferences';
import { useUserPreferencesStore } from '~/store/userPreferencesStore';

export const useFavoritesReorder = () => {
  const { t } = useTranslation('common');
  const toast = useToast();
  const { applications } = useApplications();
  const { bookmarks, setBookmarks } = useUserPreferencesStore();
  const updatePreferences = useUpdateUserPreferences();

  const saveOrder = (orderedNames: string[]) => {
    const previousBookmarks = bookmarks;
    // Bookmarks of apps hidden from the list must not be dropped.
    const newBookmarks = [
      ...orderedNames,
      ...bookmarks.filter((name) => !orderedNames.includes(name)),
    ];

    setBookmarks(newBookmarks);
    updatePreferences.mutate(
      {
        bookmarks: newBookmarks,
        applications: applications?.map((app) => app.name) ?? [],
      },
      {
        onError: () => {
          setBookmarks(previousBookmarks);
          toast.error(t('e500'));
        },
      },
    );
  };

  return { saveOrder };
};
