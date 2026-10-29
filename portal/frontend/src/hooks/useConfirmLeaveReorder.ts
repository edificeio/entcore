import { useTranslation } from 'react-i18next';
import { useFavoritesReorderStore } from '~/store/favoritesReorderStore';

export const useConfirmLeaveReorder = () => {
  const { t } = useTranslation('common');
  const hasUnsavedOrder = useFavoritesReorderStore(
    (state) => state.hasUnsavedOrder,
  );

  const confirmLeave = () =>
    !hasUnsavedOrder || window.confirm(t('my.apps.favorites.reorder.leave'));

  return { confirmLeave };
};
