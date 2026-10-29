import { Button } from '@edifice.io/react';
import { IconListOrder } from '@edifice.io/react/icons';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useFavoritesReorder } from '~/hooks/useFavoritesReorder';
import { useUnsavedOrderWarning } from '~/hooks/useUnsavedOrderWarning';
import { Application } from '~/models/application';
import { ApplicationListGrid } from './ApplicationListGrid';
import { SortableApplicationGrid } from './SortableApplicationGrid';

const MIN_APPS_TO_REORDER = 2;

export function FavoriteApplicationList({
  applications,
}: {
  applications: Application[];
}) {
  const { t } = useTranslation('common');
  const { saveOrder } = useFavoritesReorder();
  const [draftOrder, setDraftOrder] = useState<Application[] | null>(null);

  const isOrderChanged =
    !!draftOrder &&
    draftOrder.some((app, index) => app.name !== applications[index]?.name);
  useUnsavedOrderWarning(isOrderChanged);

  if (applications.length < MIN_APPS_TO_REORDER) {
    return <ApplicationListGrid applications={applications} />;
  }

  const handleSave = () => {
    if (!draftOrder) return;
    saveOrder(draftOrder.map((app) => app.name));
    setDraftOrder(null);
  };

  return (
    <div className="d-flex flex-column gap-32">
      {draftOrder ? (
        <SortableApplicationGrid
          applications={draftOrder}
          onReorder={setDraftOrder}
        />
      ) : (
        <ApplicationListGrid applications={applications} />
      )}

      <div className="d-flex justify-content-center">
        {draftOrder ? (
          <Button
            data-id="btn-save-favorites-order"
            type="button"
            onClick={handleSave}
          >
            {t('save')}
          </Button>
        ) : (
          <Button
            data-id="btn-reorder-favorites"
            type="button"
            variant="outline"
            leftIcon={<IconListOrder />}
            onClick={() => setDraftOrder(applications)}
          >
            {t('my.apps.favorites.reorder')}
          </Button>
        )}
      </div>
    </div>
  );
}
